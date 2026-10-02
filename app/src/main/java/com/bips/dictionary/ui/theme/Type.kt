package com.bips.dictionary.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * Two roles, ready for two bundled families (Phase 2):
 *   Display: headings and section titles (Manrope)
 *   Body:    anything that is read (Inter)
 * Both are the system font until the files are in res/font.
 */
private val Display: FontFamily = FontFamily.Default
private val Body: FontFamily = FontFamily.Default

private fun display(size: Float, line: Float, weight: FontWeight, tracking: Float) = TextStyle(
    fontFamily = Display, fontWeight = weight,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp,
)

private fun body(size: Float, line: Float, weight: FontWeight, tracking: Float) = TextStyle(
    fontFamily = Body, fontWeight = weight,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp,
)

internal val DictTypography = Typography(
    displayLarge = display(46f, 52f, FontWeight.Bold, -1.0f),
    displayMedium = display(38f, 44f, FontWeight.Bold, -0.8f),
    displaySmall = display(32f, 38f, FontWeight.Bold, -0.6f),
    headlineLarge = display(34f, 41f, FontWeight.Bold, -0.6f),
    headlineMedium = display(29f, 36f, FontWeight.Bold, -0.5f),
    headlineSmall = display(23f, 29f, FontWeight.SemiBold, -0.3f),
    titleLarge = display(20f, 26f, FontWeight.Bold, -0.2f),
    titleMedium = body(16f, 22f, FontWeight.SemiBold, -0.05f),
    titleSmall = display(15f, 20f, FontWeight.Bold, 0.1f),
    bodyLarge = body(15.5f, 21f, FontWeight.Medium, 0f),
    bodyMedium = body(13.5f, 19f, FontWeight.Normal, 0.05f),
    bodySmall = body(12.5f, 17.5f, FontWeight.Normal, 0.1f),
    labelLarge = body(14f, 18f, FontWeight.SemiBold, 0.05f),
    labelMedium = body(12f, 16f, FontWeight.SemiBold, 0.1f),
    labelSmall = body(11f, 15f, FontWeight.SemiBold, 0.15f),
)
