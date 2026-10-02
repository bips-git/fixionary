package com.bips.dictionary.data

/** Phase 1 only: replaced by the prebuilt SQLite dictionary in Phase 4. */
data class Entry(
    val headword: String,
    val pos: String,
    val ipa: String,
    val definition: String,
    val example: String,
    val synonyms: List<String>,
    val antonyms: List<String>,
)

object SampleData {
    val entries: List<Entry> = listOf(
        Entry("serene", "adjective", "/səˈriːn/", "calm, peaceful and untroubled",
            "a serene lake at dawn",
            listOf("calm", "tranquil", "placid", "peaceful"), listOf("agitated", "turbulent", "stormy")),
        Entry("serendipity", "noun", "/ˌsɛrənˈdɪpɪti/", "the occurrence of fortunate events by chance",
            "finding the shop was pure serendipity",
            listOf("luck", "fluke", "chance"), listOf("misfortune")),
        Entry("ephemeral", "adjective", "/ɪˈfɛmərəl/", "lasting for a very short time",
            "the ephemeral beauty of cherry blossom",
            listOf("fleeting", "transient", "momentary"), listOf("permanent", "enduring", "lasting")),
        Entry("eloquent", "adjective", "/ˈɛləkwənt/", "fluent and persuasive in speaking or writing",
            "an eloquent speech",
            listOf("articulate", "expressive", "fluent"), listOf("inarticulate", "tongue-tied")),
        Entry("resilient", "adjective", "/rɪˈzɪliənt/", "able to recover quickly from difficulties",
            "children are often remarkably resilient",
            listOf("tough", "hardy", "adaptable"), listOf("fragile", "brittle", "vulnerable")),
        Entry("candid", "adjective", "/ˈkændɪd/", "truthful and straightforward",
            "a candid answer",
            listOf("frank", "honest", "blunt"), listOf("evasive", "guarded", "deceitful")),
        Entry("lucid", "adjective", "/ˈluːsɪd/", "expressed clearly and easy to understand",
            "a lucid explanation",
            listOf("clear", "intelligible", "coherent"), listOf("confusing", "obscure", "vague")),
        Entry("tenacious", "adjective", "/tɪˈneɪʃəs/", "holding firmly to something; persistent",
            "a tenacious defender",
            listOf("persistent", "determined", "stubborn"), listOf("yielding", "irresolute", "flexible")),
    ).sortedBy { it.headword }

    const val WORD_OF_THE_DAY = "resilient"
    val initialRecents = listOf("serendipity", "ephemeral", "candid")

    fun find(headword: String): Entry? =
        entries.firstOrNull { it.headword.equals(headword.trim(), ignoreCase = true) }

    /** Case-insensitive prefix filter, limit 30; an empty query lists everything alphabetically. */
    fun search(query: String): List<Entry> {
        val q = query.trim().lowercase()
        return entries.filter { it.headword.startsWith(q) }.take(30)
    }
}
