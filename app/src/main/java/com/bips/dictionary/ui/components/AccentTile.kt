package com.bips.dictionary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Detect dark by background luminance (not isSystemInDarkTheme) so in-app theme overrides stay correct.
@Composable
internal fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Soft tinted fill behind a glyph or chip. */
@Composable
fun accentWash(accent: Color): Color = accent.copy(alpha = if (isDarkSurface()) 0.24f else 0.15f)

/** Glyph/text colour on top of [accentWash]: the accent itself in dark, darkened in light. */
@Composable
fun accentGlyph(accent: Color): Color =
    if (isDarkSurface()) accent else androidx.compose.ui.graphics.lerp(accent, Color.Black, 0.28f)

@Composable
fun AccentTile(icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accentWash(accent)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = accentGlyph(accent), modifier = Modifier.size(22.dp))
    }
}
