package com.amrjudy.kids

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.io.File

/** Records a short voice clip (raw PCM) and plays it back pitch-shifted by changing the sample rate. */
class VoiceEcho {
    private val sampleRate = 16000
    @Volatile private var data: ShortArray = ShortArray(0)
    @Volatile var busy = false
        private set

    fun hasRecording() = data.isNotEmpty()

    /** Blocking – call from a background thread. */
    @SuppressLint("MissingPermission")
    fun record(seconds: Int = 3) {
        busy = true
        try {
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val rec = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 4096)
            )
            val out = ShortArray(sampleRate * seconds)
            rec.startRecording()
            var read = 0
            while (read < out.size) {
                val n = rec.read(out, read, minOf(1024, out.size - read))
                if (n <= 0) break
                read += n
            }
            rec.stop(); rec.release()
            data = out.copyOf(read)
        } finally { busy = false }
    }

    /** factor > 1 = higher/faster (bird), factor < 1 = lower/slower (giant). Blocking. */
    fun play(factor: Float) {
        val pcm = data
        if (pcm.isEmpty()) return
        busy = true
        try {
            val rate = (sampleRate * factor).toInt().coerceIn(4000, 48000)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            val ms = (pcm.size * 1000L / rate) + 150
            Thread.sleep(ms)
            track.stop(); track.release()
        } finally { busy = false }
    }

    fun saveTo(file: File) {
        val pcm = data
        val bytes = ByteArray(pcm.size * 2)
        for (i in pcm.indices) {
            bytes[i * 2] = (pcm[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((pcm[i].toInt() shr 8) and 0xFF).toByte()
        }
        file.writeBytes(bytes)
    }

    fun loadFrom(file: File): Boolean {
        if (!file.exists()) return false
        val b = file.readBytes()
        val s = ShortArray(b.size / 2)
        for (i in s.indices) {
            s[i] = ((b[i * 2].toInt() and 0xFF) or (b[i * 2 + 1].toInt() shl 8)).toShort()
        }
        data = s
        return s.isNotEmpty()
    }
}
