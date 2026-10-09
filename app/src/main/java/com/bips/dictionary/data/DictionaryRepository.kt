package com.bips.dictionary.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Read-only queries over dictionary.db (schema v2). Every function runs on the IO dispatcher.
 * These SQL strings are mirrored in pipeline/gates_v2.py, which is where they are tested.
 */
class DictionaryRepository(private val db: SQLiteDatabase) {

    private val hitColumns = """
        SELECT w.headword,
          (SELECT y.pos FROM senses s JOIN synsets y ON y.id = s.synset_id WHERE s.word_id = w.id ORDER BY s.sense_order LIMIT 1),
          (SELECT y.definition FROM senses s JOIN synsets y ON y.id = s.synset_id WHERE s.word_id = w.id ORDER BY s.sense_order LIMIT 1)
        FROM words w
    """.trimIndent()

    /**
     * Prefix search: exact match first, then by commonness. An empty query returns the most common
     * words (words.id equals freq_rank, so that case is a plain primary-key scan).
     */
    suspend fun search(raw: String, limit: Int = 30): List<SearchHit> = withContext(Dispatchers.IO) {
        val q = fold(raw)
        val cursor = if (q.isEmpty()) {
            db.rawQuery("$hitColumns ORDER BY w.id LIMIT $limit", null)
        } else {
            db.rawQuery(
                "$hitColumns WHERE w.headword_lc >= ? AND w.headword_lc < ? " +
                    "ORDER BY (w.headword_lc = ?) DESC, w.freq_rank LIMIT $limit",
                arrayOf(q, q + "\uffff", q),
            )
        }
        val hits = cursor.use { c ->
            buildList { while (c.moveToNext()) add(SearchHit(c.getString(0), c.getString(1), c.getString(2))) }
        }
        // Few prefix matches usually means a typo: add spelling suggestions after them.
        if (q.length >= FUZZY_MIN_QUERY && hits.size < FUZZY_TRIGGER) hits + fuzzyHits(q, hits) else hits
    }

    // ---- typo tolerance ------------------------------------------------------------------

    private class FuzzyIndex(val ids: IntArray, val words: Array<String>)

    @Volatile
    private var fuzzyIndex: FuzzyIndex? = null

    /** The most common FUZZY_WORDS headwords (folded) held in memory, loaded on first need. */
    private fun fuzzyIndex(): FuzzyIndex = fuzzyIndex ?: run {
        val ids = ArrayList<Int>()
        val words = ArrayList<String>()
        db.rawQuery(
            "SELECT id, headword_lc FROM words WHERE id <= $FUZZY_WORDS ORDER BY id", null,
        ).use { c -> while (c.moveToNext()) { ids.add(c.getInt(0)); words.add(c.getString(1)) } }
        FuzzyIndex(ids.toIntArray(), words.toTypedArray()).also { fuzzyIndex = it }
    }

    /** Words within 1 (short query) or 2 (longer) edits of [q], closest and most common first. */
    private fun CoroutineScope.fuzzyHits(q: String, already: List<SearchHit>): List<SearchHit> {
        val index = fuzzyIndex()
        val maxEdits = if (q.length <= 4) 1 else 2
        val seen = already.mapTo(HashSet()) { it.headword }
        val found = ArrayList<Long>() // distance * 2^32 + id, so a plain sort orders by both
        val a = IntArray(MAX_WORD + 1)
        val b = IntArray(MAX_WORD + 1)
        val c = IntArray(MAX_WORD + 1)
        for (i in index.words.indices) {
            if (i and 4095 == 0) ensureActive()
            val w = index.words[i]
            if (w.length > MAX_WORD || kotlin.math.abs(w.length - q.length) > maxEdits) continue
            if (w.startsWith(q)) continue // already covered by the prefix search
            val d = editDistance(q, w, maxEdits, a, b, c)
            if (d <= maxEdits) found.add((d.toLong() shl 32) or index.ids[i].toLong())
        }
        found.sort()
        val out = ArrayList<SearchHit>()
        for (key in found) {
            if (out.size >= MAX_FUZZY) break
            val id = (key and 0xFFFFFFFFL).toString()
            db.rawQuery("$hitColumns WHERE w.id = ?", arrayOf(id)).use { cur ->
                if (cur.moveToFirst() && cur.getString(0) !in seen) {
                    out.add(SearchHit(cur.getString(0), cur.getString(1), cur.getString(2), fuzzy = true))
                }
            }
        }
        return out
    }

    /**
     * Damerau-Levenshtein (optimal string alignment): insert, delete, substitute, swap neighbours.
     * Gives up as soon as a whole row exceeds [max]. The three arrays are reusable scratch space.
     */
    private fun editDistance(s: String, t: String, max: Int, p2: IntArray, p1: IntArray, cur: IntArray): Int {
        var prev2 = p2
        var prev = p1
        var row = cur
        val m = t.length
        for (j in 0..m) prev[j] = j
        for (i in 1..s.length) {
            row[0] = i
            var rowMin = i
            for (j in 1..m) {
                val cost = if (s[i - 1] == t[j - 1]) 0 else 1
                var v = minOf(prev[j] + 1, row[j - 1] + 1, prev[j - 1] + cost)
                if (i > 1 && j > 1 && s[i - 1] == t[j - 2] && s[i - 2] == t[j - 1]) v = minOf(v, prev2[j - 2] + 1)
                row[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
            val tmp = prev2; prev2 = prev; prev = row; row = tmp
        }
        return prev[m]
    }

    // ---- search by meaning ---------------------------------------------------------------

    /** True when dictionary.db carries the definition index (added by pipeline/add_meaning_index.py). */
    val hasMeaningIndex: Boolean = db.rawQuery(
        "SELECT 1 FROM sqlite_master WHERE name = 'synset_fts'", null,
    ).use { it.moveToFirst() }

    /**
     * Finds words by what their definition says ("fear of heights" finds acrophobia). Every
     * meaningful word must appear in the definition; the last one may be a partial word.
     * Shorter definitions rank first (they are usually the more precise match), then commoner words.
     */
    suspend fun searchMeaning(raw: String, limit: Int = 30): List<SearchHit> = withContext(Dispatchers.IO) {
        if (!hasMeaningIndex) return@withContext emptyList()
        val tokens = fold(raw).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        val terms = tokens.filter { it !in STOP_WORDS }.ifEmpty { tokens }.take(6)
        if (terms.isEmpty()) return@withContext emptyList()
        val match = terms.mapIndexed { i, t -> if (i == terms.lastIndex) "$t*" else t }.joinToString(" ")

        val seen = HashSet<String>()
        val out = ArrayList<SearchHit>()
        db.rawQuery(
            "SELECT w.headword, y.pos, y.definition FROM synset_fts " +
                "JOIN synsets y ON y.id = synset_fts.rowid " +
                "JOIN senses s ON s.synset_id = y.id JOIN words w ON w.id = s.word_id " +
                "WHERE synset_fts MATCH ? ORDER BY length(y.definition), w.freq_rank LIMIT ${limit * 4}",
            arrayOf(match),
        ).use { c ->
            while (c.moveToNext() && out.size < limit) {
                if (seen.add(c.getString(0))) out.add(SearchHit(c.getString(0), c.getString(1), c.getString(2)))
            }
        }
        out
    }

    /** Full entry for a headword. Prefers the exact-case form ("book" before "Book"). */
    suspend fun entry(raw: String): WordEntry? = withContext(Dispatchers.IO) {
        val found = db.rawQuery(
            "SELECT id, headword FROM words WHERE headword_lc = ? ORDER BY (headword = ?) DESC, freq_rank LIMIT 1",
            arrayOf(fold(raw), raw.trim()),
        ).use { c -> if (c.moveToFirst()) c.getLong(0) to c.getString(1) else null }
            ?: return@withContext null
        val (id, headword) = found
        val idArg = arrayOf(id.toString())

        val senses = db.rawQuery(
            "SELECT y.pos, y.definition, y.example FROM senses s JOIN synsets y ON y.id = s.synset_id " +
                "WHERE s.word_id = ? ORDER BY s.sense_order",
            idArg,
        ).use { c -> buildList { while (c.moveToNext()) add(Sense(c.getString(0), c.getString(1), c.getStringOrNull(2))) } }
        if (senses.isEmpty()) return@withContext null

        // Synonyms: the other words that share a synset with one of this word's senses.
        val synonyms = LinkedHashSet<String>()
        db.rawQuery(
            "SELECT w2.headword FROM senses s1 " +
                "JOIN senses s2 ON s2.synset_id = s1.synset_id AND s2.word_id <> s1.word_id " +
                "JOIN words w2 ON w2.id = s2.word_id " +
                "WHERE s1.word_id = ? ORDER BY s1.sense_order, w2.freq_rank",
            idArg,
        ).use { c -> while (c.moveToNext()) synonyms.add(c.getString(0)) }

        val relations = linkedMapOf<String, LinkedHashSet<String>>()
        db.rawQuery("SELECT rel_type, related FROM relations WHERE word_id = ? ORDER BY rel_type, ord", idArg).use { c ->
            while (c.moveToNext()) relations.getOrPut(c.getString(0)) { LinkedHashSet() }.add(c.getString(1))
        }

        // Narrower terms: words whose broader term is this headword, most common first.
        val narrower = ArrayList<String>()
        db.rawQuery(
            "SELECT w.headword FROM relations r JOIN words w ON w.id = r.word_id " +
                "WHERE r.rel_type = 'hypernym' AND r.related = ? ORDER BY w.freq_rank LIMIT $MAX_NARROWER",
            arrayOf(headword),
        ).use { c -> while (c.moveToNext()) narrower.add(c.getString(0)) }

        val ipa = db.rawQuery("SELECT ipa FROM pronunciations WHERE word_id = ? LIMIT 1", idArg)
            .use { c -> if (c.moveToFirst()) c.getString(0) else null }

        fun rel(type: String) = relations[type]?.toList().orEmpty()
        // "Related" must not repeat words already shown in another section.
        val taken = HashSet<String>().apply {
            addAll(synonyms); addAll(rel("antonym")); addAll(rel("hypernym")); addAll(narrower)
        }
        WordEntry(
            headword = headword, ipa = ipa, senses = senses,
            synonyms = synonyms.toList(), antonyms = rel("antonym"),
            broader = rel("hypernym"), narrower = narrower,
            related = rel("related").filter { it !in taken }.take(MAX_RELATED),
        )
    }

    /** A different common word each day: deterministic, from ranks 2,000 to 11,999. */
    suspend fun wordOfTheDay(epochDay: Long): WordEntry? {
        val rank = 2000 + (epochDay * 7919 % 10_000)
        val headword = withContext(Dispatchers.IO) {
            db.rawQuery("SELECT headword FROM words WHERE id = ?", arrayOf(rank.toString()))
                .use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }
        return headword?.let { entry(it) }
    }

    private fun Cursor.getStringOrNull(i: Int): String? = if (isNull(i)) null else getString(i)

    private companion object {
        val STOP_WORDS = setOf(
            "a", "an", "the", "of", "to", "in", "on", "for", "and", "or", "is", "are", "that",
            "with", "by", "at", "as", "it", "be", "who", "which", "what", "something", "someone",
        )
        const val FUZZY_WORDS = 80_000
        const val FUZZY_MIN_QUERY = 3
        const val FUZZY_TRIGGER = 8
        const val MAX_FUZZY = 10
        const val MAX_WORD = 40
        const val MAX_NARROWER = 30
        const val MAX_RELATED = 20
    }
}
