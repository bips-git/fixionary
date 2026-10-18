package com.bips.dictionary.data

/**
 * The origin of a word, parsed from the `etymology.text` column.
 *
 * Database v4 stores one TAB-separated record per line (written by pipeline/etym.py):
 *   L <lang> <terms> <gloss> <rel>   a step of the lineage, nearest ancestor first
 *   N <text>                         a free note
 *   C <lang> <terms> <gloss>         a cognate
 * Older databases hold plain prose; that is kept in [legacy] and shown as one paragraph.
 */
data class EtymStep(val lang: String, val terms: List<String>, val gloss: String, val rel: String)

data class EtymCognate(val lang: String, val terms: List<String>, val gloss: String)

data class Etymology(
    val steps: List<EtymStep> = emptyList(),
    val notes: List<String> = emptyList(),
    val cognates: List<EtymCognate> = emptyList(),
    val legacy: String? = null,
)

/** Splits "a, b (c, d), e" on the commas that are outside brackets and quotes. */
private fun splitTerms(s: String): List<String> {
    val out = mutableListOf<String>()
    var depth = 0
    var start = 0
    for (i in s.indices) {
        when (s[i]) {
            '(', '“' -> depth++
            ')', '”' -> if (depth > 0) depth--
            ',' -> if (depth == 0 && s.getOrNull(i + 1) == ' ') {
                out += s.substring(start, i).trim()
                start = i + 1
            }
        }
    }
    out += s.substring(start).trim()
    return out.filter { it.isNotEmpty() }
}

fun parseEtymology(raw: String): Etymology {
    val steps = mutableListOf<EtymStep>()
    val notes = mutableListOf<String>()
    val cognates = mutableListOf<EtymCognate>()
    for (line in raw.split('\n')) {
        val f = line.split('\t')
        when {
            f[0] == "L" && f.size >= 3 ->
                steps += EtymStep(f[1], splitTerms(f[2]), f.getOrElse(3) { "" }, f.getOrElse(4) { "" })
            f[0] == "C" && f.size >= 3 ->
                cognates += EtymCognate(f[1], splitTerms(f[2]), f.getOrElse(3) { "" })
            f[0] == "N" && f.size >= 2 -> notes += f.drop(1).joinToString(" ")
        }
    }
    if (steps.isEmpty() && notes.isEmpty() && cognates.isEmpty()) return Etymology(legacy = raw)
    return Etymology(steps, notes, cognates)
}

private fun relPhrase(rel: String): String = when (rel) {
    "borrowed" -> "borrowed from"
    "inherited" -> "inherited from"
    "learned" -> "learned borrowing from"
    "derived" -> "derived from"
    "via" -> "via"
    "ultimately" -> "ultimately from"
    "probably" -> "probably from"
    "possibly" -> "possibly from"
    "perhaps" -> "perhaps from"
    else -> "from"
}

private fun pieceText(lang: String, terms: List<String>, gloss: String): String = buildString {
    if (lang.isNotEmpty()) append(lang).append(' ')
    append(terms.joinToString(", "))
    if (gloss.isNotEmpty()) append(" (“").append(gloss).append("”)")
}

/** Back to bare running text for the clipboard, close to how Wiktionary words it. */
fun Etymology.plainText(): String {
    legacy?.let { return it }
    val paragraphs = mutableListOf<String>()
    if (steps.isNotEmpty()) {
        val chain = steps.mapIndexed { i, s ->
            val phrase = relPhrase(s.rel).let { if (i == 0) it.replaceFirstChar { c -> c.uppercase() } else it }
            "$phrase ${pieceText(s.lang, s.terms, s.gloss)}"
        }.joinToString(", ")
        paragraphs += "$chain."
    }
    paragraphs += notes
    if (cognates.isNotEmpty()) {
        paragraphs += "Cognate with " + cognates.joinToString(", ") { pieceText(it.lang, it.terms, it.gloss) } + "."
    }
    return paragraphs.joinToString("\n\n")
}

/** Convenience for callers that only hold the raw column text. */
fun originPlainText(raw: String): String = parseEtymology(raw).plainText()
