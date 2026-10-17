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
import java.util.Locale

/**
 * Wraps Android's native TextToSpeech + SpeechRecognizer.
 * Offline-first: recognition prefers on-device models; when the internet is available
 * the system recogniser is free to use the (better) online engine automatically.
 * All calls must be made from the main thread.
 */
class SpeechHelper(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var recognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("ar"))
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setSpeechRate(0.8f)   // slower, clearer for small children
                tts?.setPitch(1.15f)
            }
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun speak(text: String, pitch: Float = 1.15f, rate: Float = 0.8f) {
        val t = tts ?: return
        t.setPitch(pitch)
        t.setSpeechRate(rate)
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utt-${System.nanoTime()}")
    }

    fun stopSpeaking() { tts?.stop() }

    fun recognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** Listens once. onResult gets all candidate phrases; onFail is called on any error / silence. */
    fun listen(onResult: (List<String>) -> Unit, onFail: () -> Unit, onRms: (Float) -> Unit = {}) {
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG")
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
