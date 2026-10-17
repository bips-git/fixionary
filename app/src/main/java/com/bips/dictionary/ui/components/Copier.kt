package com.bips.dictionary.ui.components

import android.content.ClipData
import android.os.Build
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Puts text on the clipboard and confirms it. A crisp haptic tick fires on every button copy.
 * Android 13+ shows its own "Copied" preview, so a snackbar is only shown on older versions
 * (showing both would be a duplicate).
 */
@Stable
class Copier internal constructor(
    private val scope: CoroutineScope,
    private val clipboard: Clipboard,
    private val haptic: HapticFeedback,
    private val snackbar: SnackbarHostState,
) {
    /** [what] reads in the confirmation: "Copied synonyms". [tick] is off for long-press, which already vibrates. */
    fun copy(what: String, text: String, tick: Boolean = true) {
        if (tick) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(what, text)))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar("Copied $what", duration = SnackbarDuration.Short)
            }
        }
    }
}

@Composable
fun rememberCopier(snackbar: SnackbarHostState): Copier {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    return remember(scope, clipboard, haptic, snackbar) { Copier(scope, clipboard, haptic, snackbar) }
}
