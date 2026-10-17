package com.amrjudy.kids

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

enum class Profile(val nameAr: String, val age: Int, val emoji: String) {
    AMR("عمرو", 3, "👦"),
    JUDY("جودي", 6, "👧")
}

class VideoItem(val title: String, val url: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("amrjudy", Context.MODE_PRIVATE)

    val speech = SpeechHelper(app)
    private val voicePlayer = VoiceEcho()

    init {
        speech.preferredVoice = prefs.getString("voice", null)
        // Anything the parent recorded is played instead of the robot voice, for every spoken text.
        speech.recordedPlayer = { text -> playRecorded(text) }
    }

    private fun playRecorded(text: String): Boolean {
        val f = File(getApplication<Application>().filesDir, "voice_${text.hashCode()}.pcm")
        if (!f.exists()) return false
        if (!voicePlayer.busy) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try { if (voicePlayer.loadFrom(f)) voicePlayer.play(1.0f) } catch (e: Exception) { }
            }
        }
        return true
    }

    // ---- Microphone permission (the activity sets micRequest) ----
    var micRequest: (() -> Unit)? = null

    fun hasMic(): Boolean =
        getApplication<Application>().checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Asks for the microphone permission (or opens the app settings if it was blocked). */
    fun askMic() {
        speech.speak("اسمح للميكروفون عشان أسمعك")
        micRequest?.invoke()
    }

    fun recordedKeys(): List<String> = prefs.getStringSet("voice_keys", emptySet())?.toList()?.sorted() ?: emptyList()

    fun chooseVoice(name: String) {
        prefs.edit().putString("voice", name).apply()
        speech.preferredVoice = name
    }
    fun savedVoice(): String? = prefs.getString("voice", null)

    var profile by mutableStateOf<Profile?>(null)
        private set
    var screen by mutableStateOf("home")
        private set
    var stars by mutableIntStateOf(prefs.getInt("stars", 0))
        private set
    var paused by mutableStateOf(false)
        private set
    var photoUri by mutableStateOf(prefs.getString("photo", null))
        private set
    var online by mutableStateOf(false)
        private set

    val stickerPool = listOf("🦄", "🐶", "🐱", "🐰", "🦁", "🐼", "🐸", "🐠", "🦋", "🚀", "🌈", "🍓", "⚽", "🎈", "🐘", "🐥")
    val unlockedStickers: Int get() = (stars / 5).coerceAtMost(stickerPool.size)

    private var timerJob: Job? = null
    val pauseAfterMs = 8 * 60 * 1000L

    fun refreshOnline() { online = speech.isOnline() }

    fun selectProfile(p: Profile) {
        profile = p
        screen = "menu"
        refreshOnline()
        startTimer()
    }

    fun go(s: String) { screen = s; refreshOnline() }

    fun back() {
        speech.stopSpeaking(); speech.cancelListening()
        screen = when (screen) {
            "menu" -> { profile = null; timerJob?.cancel(); "home" }
            "home" -> "home"
            else -> if (profile == null) "home" else "menu"
        }
    }

    fun addStars(n: Int) {
        stars += n
        prefs.edit().putInt("stars", stars).apply()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            delay(pauseAfterMs)
            paused = true
            speech.stopSpeaking(); speech.cancelListening()
            speech.speak("خلاص يا حبيبي، نرتاح شوية")
        }
    }

    fun resume() { paused = false; startTimer() }

    // ---- Parent settings ----
    fun checkPin(pin: String) = pin == (prefs.getString("pin", "1234") ?: "1234")
    fun setPin(pin: String) { prefs.edit().putString("pin", pin).apply() }
    fun setPhoto(uri: String?) {
        photoUri = uri
        prefs.edit().putString("photo", uri).apply()
    }

    /** Re-reads everything from the saved settings (after a restore from backup). */
    fun reloadFromPrefs() {
        stars = prefs.getInt("stars", 0)
        photoUri = prefs.getString("photo", null)
        speech.preferredVoice = prefs.getString("voice", null)
        videoVersion++
    }

    // ---- Parent-curated videos (links or videos from the phone) ----
    var videoVersion by mutableIntStateOf(0)
        private set

    fun videos(): List<VideoItem> =
        (prefs.getString("videos", "") ?: "").split("\n")
            .filter { it.contains("\t") }
            .map { line -> line.split("\t", limit = 2).let { VideoItem(it[0], it[1]) } }

    private fun saveVideos(list: List<VideoItem>) {
        prefs.edit().putString("videos", list.joinToString("\n") { it.title + "\t" + it.url }).apply()
        videoVersion++
    }

    fun addVideo(title: String, url: String) {
        val t = title.replace("\n", " ").replace("\t", " ").trim()
        val u = url.replace("\n", "").replace("\t", "").trim()
        if (t.isEmpty() || u.isEmpty()) return
        saveVideos(videos() + VideoItem(t, u))
    }

    fun removeVideo(index: Int) {
        val l = videos().toMutableList()
        if (index in l.indices) { l.removeAt(index); saveVideos(l) }
    }

    // ---- Custom (parent-recorded) voices ----
    private fun voiceFile(key: String) = File(getApplication<Application>().filesDir, "voice_${key.hashCode()}.pcm")
    fun hasCustomVoice(key: String) = voiceFile(key).exists()
    fun saveCustomVoice(key: String, echo: VoiceEcho) {
        echo.saveTo(voiceFile(key))
        val set = (prefs.getStringSet("voice_keys", emptySet()) ?: emptySet()).toMutableSet()
        set.add(key)
        prefs.edit().putStringSet("voice_keys", set).apply()
    }

    /** Plays the parent's recording for [key] if there is one; otherwise falls back to TTS. */
    fun sayWord(key: String, fallback: String = key) {
        val f = voiceFile(key)
        if (f.exists()) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                if (voicePlayer.loadFrom(f)) voicePlayer.play(1.0f) else speech.speak(fallback)
            }
        } else speech.speak(fallback)
    }

    override fun onCleared() {
        speech.shutdown()
        super.onCleared()
    }
}
