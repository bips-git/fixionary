package com.bips.dictionary.data

import java.text.Normalizer
import java.util.Locale

/**
 * Query normalisation. Must match `fold()` in pipeline/02_build_db.py exactly,
 * because `words.headword_lc` was produced by it: accent-fold, lowercase, collapse whitespace.
 */
fun fold(text: String): String {
    val decomposed = Normalizer.normalize(text.replace('_', ' '), Normalizer.Form.NFKD)
    val stripped = buildString(decomposed.length) {
        for (c in decomposed) if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) append(c)
    }
    return stripped.lowercase(Locale.ROOT).trim().split(Regex("\\s+")).joinToString(" ")
}
