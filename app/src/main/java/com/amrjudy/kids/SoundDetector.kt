package com.amrjudy.kids

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import kotlin.math.sqrt

/**
 * Detects whether the child made a sound (any sound: "meow", "moo", "aaa"...), without needing
 * speech recognition. Speech recognisers can't transcribe animal noises or babbling, so for
 * vocalisation games we reward the *attempt*: how long the child vocalised.
 */
class SoundDetector {

    /** Blocking – run on a background thread. Returns (voiced milliseconds, or -1 if the mic is unavailable). */
    @SuppressLint("MissingPermission")
    fun listen(seconds: Int = 4): Int {
        val rate = 16000
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, rate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 4096)
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return -1 }

        val win = rate / 10                       // 100 ms windows
        val buf = ShortArray(win)
        val levels = ArrayList<Double>()
        rec.startRecording()
        try {
            for (w in 0 until seconds * 10) {
                var read = 0
                while (read < win) {
                    val n = rec.read(buf, read, win - read)
                    if (n <= 0) break
                    read += n
                }
                var sum = 0.0
                for (i in 0 until read) sum += buf[i].toDouble() * buf[i].toDouble()
                levels.add(if (read > 0) sqrt(sum / read) / 32768.0 else 0.0)
            }
        } finally {
            rec.stop(); rec.release()
        }
        if (levels.isEmpty()) return 0
        val sorted = levels.sorted()
        val floor = sorted[(sorted.size * 3 / 10).coerceAtMost(sorted.size - 1)]   // background noise level
        val threshold = maxOf(floor * 2.5, 0.02)
        return levels.count { it > threshold } * 100
    }
}

/** Mic button for sound games: gives 0..3 stars by how long the child vocalised, or -1 if the mic failed. */
@Composable
fun SoundMicButton(vm: AppViewModel, modifier: Modifier = Modifier, onScore: (Int) -> Unit) {
    var listening by remember { mutableStateOf(false) }
    val detector = remember { SoundDetector() }
    val handler = remember { Handler(Looper.getMainLooper()) }
    BigCard(
        if (listening) "👂" else "🎤", if (listening) "اتكلم!" else "", if (listening) Pink else Mint,
        modifier.size(width = 110.dp, height = 110.dp), 44.sp
    ) {
        if (!listening) {
            vm.speech.stopSpeaking()          // don't record our own voice
            listening = true
            Thread {
                try { Thread.sleep(350) } catch (e: InterruptedException) { }
                val ms = try { detector.listen(4) } catch (e: Exception) { -1 }
                val stars = when {
                    ms < 0 -> -1
                    ms >= 1200 -> 3
                    ms >= 600 -> 2
                    ms >= 250 -> 1
                    else -> 0
                }
                handler.post { listening = false; onScore(stars) }
            }.start()
        }
    }
}
