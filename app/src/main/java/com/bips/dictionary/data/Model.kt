package com.bips.dictionary.data

/** One row in a result list: the headword with its first sense. */
data class SearchHit(val headword: String, val pos: String, val definition: String)

data class Sense(val pos: String, val definition: String, val example: String?)

data class WordEntry(
    val headword: String,
    val ipa: String?,
    val senses: List<Sense>,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val broader: List<String>,
    val narrower: List<String>,
    val related: List<String>,
)

sealed interface WordLoad {
    data object Loading : WordLoad
    data class Found(val entry: WordEntry) : WordLoad
    data object NotFound : WordLoad
}
