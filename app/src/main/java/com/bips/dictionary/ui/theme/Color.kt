package com.bips.dictionary.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * The app's own colour scheme: an indigo-violet brand, generated in CIELAB
 * (hue 302; tertiary rose at hue 350) at fixed tones. Values are checked in,
 * not regenerated at build time, so diffs stay reviewable.
 */

// Primary: hue 302, chroma 62.0
private val Primary10 = Color(0xFF000F5D)
private val Primary20 = Color(0xFF21227F)
private val Primary30 = Color(0xFF41389A)
private val Primary40 = Color(0xFF5D4FB5)
private val Primary80 = Color(0xFFCEBDFF)
private val Primary90 = Color(0xFFE7DEFF)

// Secondary: hue 302, chroma 22.0
private val Secondary10 = Color(0xFF1D1735)
private val Secondary20 = Color(0xFF332C4C)
private val Secondary30 = Color(0xFF4A4264)
private val Secondary40 = Color(0xFF62597D)
private val Secondary80 = Color(0xFFCCC1E9)
private val Secondary90 = Color(0xFFE7DEFF)

// Tertiary: hue 350, chroma 33.0
private val Tertiary10 = Color(0xFF3D0024)
private val Tertiary20 = Color(0xFF571839)
private val Tertiary30 = Color(0xFF713050)
private val Tertiary40 = Color(0xFF8C4968)
private val Tertiary80 = Color(0xFFFDB0D2)
private val Tertiary90 = Color(0xFFFFD8E8)

// Neutral: hue 302, chroma 5.5
private val Neutral0 = Color(0xFF000000)
private val Neutral4 = Color(0xFF100D16)
private val Neutral6 = Color(0xFF15121A)
private val Neutral10 = Color(0xFF1C1A22)
private val Neutral12 = Color(0xFF211E26)
private val Neutral17 = Color(0xFF2B2931)
private val Neutral20 = Color(0xFF312F37)
private val Neutral22 = Color(0xFF36343C)
private val Neutral24 = Color(0xFF3A3840)
private val Neutral87 = Color(0xFFDCD8E3)
private val Neutral90 = Color(0xFFE4E1EB)
private val Neutral92 = Color(0xFFEAE7F1)
private val Neutral94 = Color(0xFFEFECF7)
private val Neutral95 = Color(0xFFF2EFFA)
private val Neutral96 = Color(0xFFF5F2FD)
private val Neutral98 = Color(0xFFFAF8FF)
private val Neutral100 = Color(0xFFFFFFFF)

// Variant: hue 302, chroma 11.0
private val Variant30 = Color(0xFF494455)
private val Variant50 = Color(0xFF7A7487)
private val Variant60 = Color(0xFF938EA1)
private val Variant80 = Color(0xFFC9C4D8)
private val Variant90 = Color(0xFFE6DFF4)

internal val DictLightColors: ColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Neutral100,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary10,
    inversePrimary = Primary80,
    secondary = Secondary40,
    onSecondary = Neutral100,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = Neutral100,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = Variant90,
    onSurfaceVariant = Variant30,
    surfaceTint = Primary40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    outline = Variant50,
    outlineVariant = Variant80,
    scrim = Neutral0,
    surfaceBright = Neutral98,
    surfaceDim = Neutral87,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

internal val DictDarkColors: ColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary20,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    inversePrimary = Primary40,
    secondary = Secondary80,
    onSecondary = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary20,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = Variant30,
    onSurfaceVariant = Variant80,
    surfaceTint = Primary80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    outline = Variant60,
    outlineVariant = Variant30,
    scrim = Neutral0,
    surfaceBright = Neutral24,
    surfaceDim = Neutral6,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
)

/** Optional AMOLED pass over the dark scheme; containers still step up from black. */
internal fun ColorScheme.amoled(): ColorScheme = copy(
    background = Neutral0,
    surface = Neutral0,
    surfaceDim = Neutral0,
    surfaceContainerLowest = Neutral0,
    surfaceContainerLow = Neutral4,
    surfaceContainer = Neutral6,
    surfaceContainerHigh = Neutral10,
    surfaceContainerHighest = Neutral17,
    surfaceBright = Neutral17,
)

/** Per-section accent hues used by the coloured icon tiles. */
object Accents {
    val Blue = Color(0xFF5B8DEF)
    val Violet = Color(0xFF8B6CF0)
    val Green = Color(0xFF3DBE8B)
    val Rose = Color(0xFFE8629B)
    val Amber = Color(0xFFF0A23B)
    val Teal = Color(0xFF2FB5C8)
}
