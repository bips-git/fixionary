package com.bips.dictionary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun DictionaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: a wallpaper palette would repaint the app and erase its identity.
    dynamicColor: Boolean = false,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DictDarkColors
        else -> DictLightColors
    }
    if (darkTheme && amoled) scheme = scheme.amoled()

    MaterialTheme(
        colorScheme = scheme,
        typography = DictTypography,
        shapes = DictShapes,
        content = content,
    )
}
