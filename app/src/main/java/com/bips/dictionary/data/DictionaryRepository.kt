package com.bips.dictionary.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
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
        cursor.use { c -> buildList { while (c.moveToNext()) add(SearchHit(c.getString(0), c.getString(1), c.getString(2))) } }
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
        const val MAX_NARROWER = 30
        const val MAX_RELATED = 20
    }
}
