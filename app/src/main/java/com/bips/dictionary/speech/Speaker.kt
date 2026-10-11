package com.bips.dictionary.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Pronunciation through Android's built-in text-to-speech (no audio files, no network).
 * `ready` turns true once the engine is up AND has an offline voice for the chosen accent;
 * the UI hides the speaker button while it is false.
 */
class Speaker(context: Context, private var locale: Locale = Locale.US) {
    var ready by mutableStateOf(false)
        private set

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null

    /**
     * Starts the engine. Called only while a word page is on screen: the system speech service is a
     * heavy process, so it is not bound at app start and is released again by [shutdown].
     */
    fun prepare() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            val engine = tts
            if (status == TextToSpeech.SUCCESS && engine != null) {
                ready = isUsable(engine.setLanguage(locale))
            }
        }
    }

    /** Switch accent (used by the Settings screen in a later step), e.g. Locale.UK. */
    fun setAccent(newLocale: Locale) {
        locale = newLocale
        tts?.let { ready = isUsable(it.setLanguage(newLocale)) }
    }

    fun speak(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun isUsable(code: Int) =
        code != TextToSpeech.LANG_MISSING_DATA && code != TextToSpeech.LANG_NOT_SUPPORTED
}

/** One Speaker for as long as the calling composable is on screen. */
@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}
