package com.bips.dictionary.ui.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class SpeechAccent(val label: String, val locale: Locale) {
    US("American English", Locale.US), UK("British English", Locale.UK)
}

/** User settings: observable by Compose, saved in SharedPreferences (no extra dependency). */
class SettingsState(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(read("theme", ThemeMode.System)); private set
    var amoled by mutableStateOf(prefs.getBoolean("amoled", false)); private set
    var dynamicColor by mutableStateOf(prefs.getBoolean("dynamic", false)); private set
    var accent by mutableStateOf(read("accent", SpeechAccent.US)); private set

    fun updateTheme(v: ThemeMode) { themeMode = v; prefs.edit().putString("theme", v.name).apply() }
    fun updateAmoled(v: Boolean) { amoled = v; prefs.edit().putBoolean("amoled", v).apply() }
    fun updateDynamic(v: Boolean) { dynamicColor = v; prefs.edit().putBoolean("dynamic", v).apply() }
    fun updateAccent(v: SpeechAccent) { accent = v; prefs.edit().putString("accent", v.name).apply() }

    private inline fun <reified E : Enum<E>> read(key: String, default: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, default.name) ?: default.name) }.getOrDefault(default)
}
