package com.bips.dictionary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.bips.dictionary.ui.components.DictGroup
import com.bips.dictionary.ui.components.DictRow
import com.bips.dictionary.ui.components.DictScreen
import com.bips.dictionary.ui.components.accentWash
import com.bips.dictionary.ui.theme.Accents

/** One built-in promise of the app. Every one is permanently on, so the switches are display only. */
private class Feature(val title: String, val detail: String, val icon: ImageVector, val accent: Color)

private val features = listOf(
    Feature("Offline", "Every word lives on your phone. Works in flight mode.", Icons.Outlined.WifiOff, Accents.Blue),
    Feature("No ads", "Nothing to scroll past, ever.", Icons.Outlined.Block, Accents.Rose),
    Feature("No tracking", "No analytics, no telemetry, no crash reporters.", Icons.Outlined.VisibilityOff, Accents.Violet),
    Feature("No accounts", "No sign-up, no login. Favourites and history stay on this device.", Icons.Outlined.PersonOff, Accents.Teal),
    Feature("Open source", "GPL-3.0-or-later. Read it, change it, share it.", Icons.Outlined.Code, Accents.Amber),
    Feature("No premium tier", "Every feature is free. Nothing is locked.", Icons.Outlined.MoneyOff, Accents.Green),
    Feature("Light on storage", "One compact install, nothing extra to download.", Icons.Outlined.SdStorage, Accents.Blue),
    Feature("Modern look", "Material 3 with light, dark and AMOLED themes.", Icons.Outlined.AutoAwesome, Accents.Rose),
)

@Composable
fun FeaturesScreen(onBack: () -> Unit) {
    DictScreen(title = "Built in", onBack = onBack) {
        Text(
            "Always on. None of these can be switched off.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
        DictGroup(features) { FeatureRow(it) }
    }
}

@Composable
private fun FeatureRow(f: Feature) {
    // Tap a row to read why its switch cannot be turned off; tap again to go back to the description.
    var showNote by rememberSaveable(f.title) { mutableStateOf(false) }
    DictRow(
        title = f.title,
        subtitle = if (showNote) "Always on by design. There is no setting to turn it off." else f.detail,
        icon = f.icon,
        accent = f.accent,
        modifier = Modifier
            .background(accentWash(f.accent))
            .semantics(mergeDescendants = true) { stateDescription = "Always on" },
        onClick = { showNote = !showNote },
        trailing = {
            Switch(
                checked = true,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Accents.Green,
                    checkedBorderColor = Accents.Green,
                ),
            )
        },
    )
}
