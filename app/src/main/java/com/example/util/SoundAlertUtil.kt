package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object SoundAlertUtil {
    @Volatile
    private var lastOrderAlertTimeMs: Long = 0L

    @Volatile
    private var activeTrack: AudioTrack? = null

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (_: Exception) {
            toneGenerator = null
        }
    }

    fun playStockWarningBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        } catch (_: Exception) {}
    }

    fun playAlert(context: Context? = null) {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes a loud, unmistakable dual-burst brass POS service bell directly on USAGE_MEDIA
     * (in-app audio only — never uses NotificationManager or system notification tones).
     */
    fun playNewOrderAlert(context: Context? = null) {
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (now - lastOrderAlertTimeMs < 900L) return
            lastOrderAlertTimeMs = now
        }

        thread(isDaemon = true, name = "pos-order-chime") {
            try {
                playSynthesizedBellChime()
            } catch (_: Throwable) {
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 550)
                } catch (_: Throwable) {}
            }
        }
    }

    private data class BellStrike(
        val freqHz: Double,
        val durationSec: Double,
        val decayRate: Double,
        val isPause: Boolean = false
    )

    private fun playSynthesizedBellChime() {
        val sampleRate = 44100
        // High-cutthrough dual-burst brass counter bell (~1.65s total) designed to cut through kitchen noise:
        // Burst 1: rapid E6 -> G#6 -> B6 strike; short 90ms gap; Burst 2: crisp G#6 -> E7 loud ring!
        val sequence = listOf(
            BellStrike(1318.51, 0.14, 3.2),          // E6 sharp counter strike
            BellStrike(1661.22, 0.14, 3.2),          // G#6 sharp step up
            BellStrike(1975.53, 0.34, 2.4),          // B6 bright hold
            BellStrike(0.0, 0.09, 1.0, isPause = true),
            BellStrike(1661.22, 0.15, 2.8),          // G#6 second burst attention strike
            BellStrike(2637.02, 0.78, 1.9)           // E7 unmistakable resonant service bell ring
        )
        val totalSamples = sequence.sumOf { (it.durationSec * sampleRate).toInt() }
        val pcm = ShortArray(totalSamples)

        var offset = 0
        for (step in sequence) {
            val stepSamples = (step.durationSec * sampleRate).toInt()
            if (step.isPause) {
                offset += stepSamples
                continue
            }
            val attackSamples = (0.004 * sampleRate).toInt().coerceAtLeast(1)
            val freq = step.freqHz
            for (i in 0 until stepSamples) {
                val t = i.toDouble() / sampleRate
                val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
                val env = attack * exp(-step.decayRate * (i.toDouble() / stepSamples))
                // Metallic counter-bell harmonic stack (fundamental + 1.5x fifth shimmer + 2x octave + 2.76x bell mode)
                val rawWave = 0.62 * sin(2.0 * PI * freq * t) +
                    0.18 * sin(2.0 * PI * (freq * 1.5) * t) +
                    0.14 * sin(2.0 * PI * (freq * 2.0) * t) +
                    0.06 * sin(2.0 * PI * (freq * 2.756) * t)
                // Soft-saturated high drive (+gain) for maximum perceived loudness without harsh digital clipping
                val driven = (rawWave * env * 1.28).let { v -> v / (1.0 + 0.28 * kotlin.math.abs(v)) }
                val sample = (driven * 0.98 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcm[offset + i] = sample.toShort()
            }
            offset += stepSamples
        }

        synchronized(this) {
            try {
                activeTrack?.stop()
                activeTrack?.release()
            } catch (_: Throwable) {}
            activeTrack = null
        }

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(pcm.size * 2)
        val bufferSize = maxOf(minBuf, pcm.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bufferSize)
            .build()

        synchronized(this) {
            activeTrack = track
        }
        track.write(pcm, 0, pcm.size)
        track.setVolume(AudioTrack.getMaxVolume())
        track.play()

        // Allow full dual-burst chime to finish before releasing
        Thread.sleep(((totalSamples * 1000L) / sampleRate) + 150L)
        synchronized(this) {
            if (activeTrack === track) {
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {}
                activeTrack = null
            }
        }
    }
}
