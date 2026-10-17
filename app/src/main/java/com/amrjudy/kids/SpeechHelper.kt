package com.amrjudy.kids

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Wraps Android's native TextToSpeech + SpeechRecognizer, tuned for Egyptian Arabic (ar-EG).
 * Offline-first: recognition prefers on-device models; when the internet is available
 * the system recogniser is free to use the (better) online engine automatically.
 * All calls must be made from the main thread.
 */
class SpeechHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    /** If set and it returns true for a text, the text was played from a parent recording and TTS is skipped. */
    var recordedPlayer: ((String) -> Boolean)? = null

    /** Name of the voice the parent picked (saved by the ViewModel). */
    var preferredVoice: String? = null
        set(value) { field = value; if (ttsReady) applyVoice() }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                var r = tts?.setLanguage(Locale("ar", "EG"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    r = tts?.setLanguage(Locale("ar"))
                }
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(0.8f)   // slower, clearer for small children
                tts?.setPitch(1.1f)
                if (ttsReady) applyVoice()
            }
        }
    }

    /** All Arabic voices installed on the phone, Egyptian ones first. */
    fun arabicVoices(): List<Voice> {
        val found: List<Voice>? = try { tts?.voices?.toList() } catch (e: Exception) { null }
        val all = found ?: return emptyList()
        return all.filter { it.locale.language == "ar" }
            .sortedWith(
                compareByDescending<Voice> { it.locale.country == "EG" }
                    .thenBy { it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality }
            )
    }

    private fun applyVoice() {
        val voices = arabicVoices()
        if (voices.isEmpty()) return
        val chosen = voices.firstOrNull { it.name == preferredVoice }
            ?: voices.firstOrNull { it.locale.country == "EG" && !it.isNetworkConnectionRequired }
            ?: voices.firstOrNull { it.locale.country == "EG" }
            ?: voices.first()
        try { tts?.voice = chosen } catch (e: Exception) { }
    }

    fun isEgyptianVoiceAvailable(): Boolean = arabicVoices().any { it.locale.country == "EG" }

    fun openTtsSettings() {
        val intents = listOf(
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (i in intents) {
            try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(i); return } catch (e: Exception) { }
        }
    }

    /** Opens the system screen that downloads voice data for the default speech engine. */
    fun openInstallVoiceData() {
        try {
            val i = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) { openTtsSettings() }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private var englishMode = false

    /** Speaks English (for Judy's English lessons) and switches back to Arabic on the next speak(). */
    fun speakEn(text: String, rate: Float = 0.8f) {
        val t = tts ?: return
        if (!englishMode) {
            t.setLanguage(Locale.US)
            englishMode = true
        }
        t.setPitch(1.05f)
        t.setSpeechRate(rate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${System.nanoTime()}")
    }

    fun speak(text: String, pitch: Float = 1.1f, rate: Float = 0.8f) {
        if (recordedPlayer?.invoke(text) == true) return
        val t = tts ?: return
        if (englishMode) {
            var r = t.setLanguage(Locale("ar", "EG"))
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) t.setLanguage(Locale("ar"))
            applyVoice()
            englishMode = false
        }
        t.setPitch(pitch)
        t.setSpeechRate(rate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${System.nanoTime()}")
    }

    fun stopSpeaking() { tts?.stop() }

    fun recognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** Listens once. onResult gets all candidate phrases; onFail is called on any error / silence. */
    fun listen(
        onResult: (List<String>) -> Unit,
        onFail: () -> Unit,
        onRms: (Float) -> Unit = {},
        lang: String = "ar-EG"
    ) {
        if (!recognitionAvailable()) { onFail(); return }
        cancelListening()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) { onRms(rmsdB) }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onFail() }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                if (list.isEmpty()) onFail() else onResult(list)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 8)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        sr.startListening(intent)
    }

    fun cancelListening() {
        recognizer?.let { it.cancel(); it.destroy() }
        recognizer = null
    }

    fun shutdown() {
        cancelListening()
        tts?.stop(); tts?.shutdown(); tts = null
    }
}
