package com.bips.dictionary.ui.screens

/** Temporary in Phase 1 (cycled from the Home top bar); becomes a Settings screen with DataStore in Phase 6. */
enum class ThemeMode(val label: String) {
    System("System"), Light("Light"), Dark("Dark"), Amoled("AMOLED");

    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]
}
