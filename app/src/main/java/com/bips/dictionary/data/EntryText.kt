package com.bips.dictionary.data

/*
 * Plain-text renderings of a dictionary entry, used by the copy buttons.
 * Deliberately bare: only the text itself, with no labels, headings or credit line.
 * Pure functions so they are easy to unit test.
 */

/** Definitions of one part of speech. Numbered when there are several; each example on its own line. */
fun senseGroupText(senses: List<Sense>): String = senses.mapIndexed { i, s ->
    buildString {
        if (senses.size > 1) append(i + 1).append(". ")
        append(s.definition)
        s.example?.let { append("\n   “").append(it).append("”") }
    }
}.joinToString("\n")

/** "sprint, dash". Always the full list, not just the chips that are shown. */
fun wordListText(words: List<String>): String = words.joinToString(", ")

/**
 * The whole entry as bare blocks separated by blank lines: definitions (one block per part of
 * speech), the origin, then each word list. Blocks that are empty are skipped.
 */
fun entryText(e: WordEntry): String = buildList {
    e.senses.groupBy { it.pos }.forEach { (_, senses) -> add(senseGroupText(senses)) }
    e.etymology?.let { add(originPlainText(it)) }
    listOf(e.synonyms, e.antonyms, e.broader, e.narrower, e.related)
        .filter { it.isNotEmpty() }
        .forEach { add(wordListText(it)) }
}.joinToString("\n\n")
