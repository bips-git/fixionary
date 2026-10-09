package com.bips.dictionary.ui.screens

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.SectionHeader
import com.bips.dictionary.ui.theme.Accents

@Composable
fun SettingsScreen(settings: SettingsState, onBack: () -> Unit) {
    DictScreen(title = "Settings", onBack = onBack) {
        SectionHeader("Theme")
        DictGroup(ThemeMode.entries) { mode ->
            DictRow(
                title = mode.label,
                icon = Icons.Outlined.DarkMode,
                accent = Accents.Violet,
                onClick = { settings.updateTheme(mode) },
                trailing = { RadioButton(selected = settings.themeMode == mode, onClick = null) },
            )
        }

        SectionHeader("Appearance")
        val options = buildList {
            add(0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(1)
        }
        DictGroup(options) { i ->
            if (i == 0) DictRow(
                title = "Pure black",
                subtitle = "True black backgrounds in dark theme",
                icon = Icons.Outlined.Contrast,
                accent = Accents.Blue,
                onClick = { settings.updateAmoled(!settings.amoled) },
                trailing = { Switch(checked = settings.amoled, onCheckedChange = null) },
            ) else DictRow(
                title = "Wallpaper colours",
                subtitle = "Use your phone's Material You colours",
                icon = Icons.Outlined.Palette,
                accent = Accents.Rose,
                onClick = { settings.updateDynamic(!settings.dynamicColor) },
                trailing = { Switch(checked = settings.dynamicColor, onCheckedChange = null) },
            )
        }

        SectionHeader("Pronunciation")
        DictGroup(SpeechAccent.entries) { accent ->
            DictRow(
                title = accent.label,
                icon = Icons.Outlined.RecordVoiceOver,
                accent = Accents.Green,
                onClick = { settings.updateAccent(accent) },
                trailing = { RadioButton(selected = settings.accent == accent, onClick = null) },
            )
        }
    }
}
