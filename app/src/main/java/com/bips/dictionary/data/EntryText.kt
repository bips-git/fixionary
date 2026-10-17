package com.bips.dictionary.data

/*
 * Plain-text renderings of a dictionary entry, used by the copy buttons.
 * Pure functions so they are easy to unit test.
 */

/** One part-of-speech group: "run (verb)" then numbered definitions with their examples. */
fun senseGroupText(headword: String, pos: String, senses: List<Sense>): String = buildString {
    append(headword).append(" (").append(pos).append(")")
    senses.forEachIndexed { i, s ->
        append("\n").append(i + 1).append(". ").append(s.definition)
        s.example?.let { append("\n   “").append(it).append("”") }
    }
}

/** "Synonyms of run: sprint, dash". Always the full list, not just the chips that are shown. */
fun wordListText(label: String, headword: String, words: List<String>): String {
    val lead = if (label == "Related") "Related to" else "$label of"
    return "$lead $headword: ${words.joinToString(", ")}"
}

fun originText(headword: String, origin: String): String = "Origin of $headword\n$origin"

/** The whole entry, followed by a one-line source credit (Wiktionary is CC BY-SA, so it is named). */
fun entryText(e: WordEntry): String = buildString {
    append(e.headword)
    e.ipa?.let { append("  ").append(it) }
    e.formOf.forEach { append("\n").append(it.note.replaceFirstChar { c -> c.uppercase() }) }
    e.senses.groupBy { it.pos }.forEach { (pos, senses) ->
        append("\n\n").append(senseGroupText(e.headword, pos, senses))
    }
    e.etymology?.let { append("\n\n").append(originText(e.headword, it)) }
    listOf(
        "Synonyms" to e.synonyms,
        "Antonyms" to e.antonyms,
        "Broader terms" to e.broader,
        "Narrower terms" to e.narrower,
        "Related" to e.related,
    ).forEach { (label, words) ->
        if (words.isNotEmpty()) append("\n\n").append(wordListText(label, e.headword, words))
    }
    append("\n\n— Fixionary · Open English WordNet (CC BY 4.0)")
    if (e.etymology != null || e.senses.any { it.wiktionary }) append(" · Wiktionary (CC BY-SA 4.0)")
}
