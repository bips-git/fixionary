package com.bips.dictionary.data

/** One row in a result list: the headword with its first sense. */
data class SearchHit(
    val headword: String,
    val pos: String,
    val definition: String,
    /** True for a spelling suggestion (typo tolerance) rather than a prefix match. */
    val fuzzy: Boolean = false,
    /** Set when the query was an inflected form of this word, e.g. "past of run" for "ran". */
    val formNote: String? = null,
)

data class Sense(val pos: String, val definition: String, val example: String?, val wiktionary: Boolean = false)

/** This word is an inflected form of [lemma]; [note] reads like "simple past of run". */
data class FormOf(val lemma: String, val note: String)

data class WordEntry(
    val headword: String,
    val ipa: String?,
    val senses: List<Sense>,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val broader: List<String>,
    val narrower: List<String>,
    val related: List<String>,
    val etymology: String? = null,
    val formOf: List<FormOf> = emptyList(),
)

sealed interface WordLoad {
    data object Loading : WordLoad
    data class Found(val entry: WordEntry) : WordLoad
    data object NotFound : WordLoad
}
