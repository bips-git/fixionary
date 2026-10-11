package com.bips.dictionary

import android.graphics.Color as AndroidColor
import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.bips.dictionary.ui.screens.SettingsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.bips.dictionary.ui.screens.ThemeMode
import com.bips.dictionary.ui.theme.DictionaryTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Low-memory phones: when the app goes to the background, give back what can be rebuilt later. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) vm.trimMemory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings = remember { SettingsState(applicationContext) }
            val dark = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // Keep status/navigation bar icon colours in step with the in-app theme, not just the system's.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                )
                onDispose { }
            }

            DictionaryTheme(darkTheme = dark, amoled = settings.amoled, dynamicColor = settings.dynamicColor) {
                DictionaryApp(settings = settings)
            }
        }
    }
}
