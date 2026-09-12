package com.thinkblox.radiantrush.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * Tiny runtime synth used by Radiant Run.
 *
 * It intentionally ships no mp3/wav assets. A single AudioTrack mixes a lightweight
 * procedural background loop with short synthesized SFX voices on an audio thread.
 */
class ProceduralGameAudioEngine {
    enum class Cue {
        Countdown,
        Go,
        RadiantHit,
        PerfectHit,
        CorruptionHit,
        Miss,
        Fever,
        ComboBurst,
        FinalTick,
        RunComplete,
        CapsuleOpen,
        RewardReveal,
        ChestCharge,
        ChestOpen,
        ChestReveal,
    }

    private data class PendingCue(
        val cue: Cue,
        val value: Int,
    )

    private data class Voice(
        val cue: Cue,
        val value: Int,
        var ageSamples: Int = 0,
    )

    private val running = AtomicBoolean(true)
    private val cues = ConcurrentLinkedQueue<PendingCue>()

    @Volatile
    private var enabled = true

    @Volatile
    private var musicActive = false

    @Volatile
    private var feverActive = false

    @Volatile
    private var finalRushActive = false

    @Volatile
    private var audioTrack: AudioTrack? = null

    private val worker = thread(
        start = true,
        isDaemon = true,
        name = "RadiantRushAudio",
    ) {
        runMixer()
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            musicActive = false
            cues.clear()
        }
    }

    fun setMusicActive(value: Boolean) {
        musicActive = enabled && value
    }

    fun setIntensity(fever: Boolean, finalRush: Boolean) {
        feverActive = fever
        finalRushActive = finalRush
    }

    /**
     * value is deliberately generic: combo for hit cues, countdown number for countdown,
     * seconds-left for FinalTick, combo for ComboBurst, and rarity rank for RewardReveal.
     */
    fun play(cue: Cue, value: Int = 0) {
        if (enabled && running.get()) {
            cues.offer(PendingCue(cue, value))
        }
    }

    fun release() {
        if (!running.compareAndSet(true, false)) return
        cues.clear()
        try {
            audioTrack?.pause()
        } catch (_: Throwable) {
            // Best-effort shutdown only.
        }
        worker.interrupt()
        try {
            worker.join(350)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun runMixer() {
        var track: AudioTrack? = null
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val minBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val bufferBytes = maxOf(minBytes, BUFFER_SAMPLES * 4)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED) return
            audioTrack = track
            track.play()

            val voices = mutableListOf<Voice>()
            val buffer = ShortArray(BUFFER_SAMPLES)
            var sampleCursor = 0L

            while (running.get()) {
                var pending = cues.poll()
                while (pending != null) {
                    voices += Voice(pending.cue, pending.value)
                    pending = cues.poll()
                }

                if (!enabled) {
                    voices.clear()
                }

                for (index in buffer.indices) {
                    val absoluteTime = sampleCursor.toDouble() / SAMPLE_RATE.toDouble()
                    var mixed = if (enabled && musicActive) {
                        musicSample(
                            t = absoluteTime,
                            fever = feverActive,
                            finalRush = finalRushActive,
                        )
                    } else {
                        0.0
                    }

                    if (enabled) {
                        for (voice in voices) {
                            mixed += voiceSample(voice)
                            voice.ageSamples += 1
                        }
                    }

                    buffer[index] = (mixed.coerceIn(-0.92, 0.92) * Short.MAX_VALUE)
                        .toInt()
                        .toShort()
                    sampleCursor += 1L
                }

                voices.removeAll { it.ageSamples >= durationSamples(it.cue) }

                val written = track.write(
                    buffer,
                    0,
                    buffer.size,
                    AudioTrack.WRITE_BLOCKING,
                )
                if (written < 0) break
            }
        } catch (_: Throwable) {
            // Audio is enhancement-only. A device/audio-driver failure must never crash gameplay.
        } finally {
            audioTrack = null
            try {
                track?.stop()
            } catch (_: Throwable) {
                // Ignore shutdown races.
            }
            try {
                track?.release()
            } catch (_: Throwable) {
                // Ignore shutdown races.
            }
        }
    }

    private fun musicSample(t: Double, fever: Boolean, finalRush: Boolean): Double {
        val bpm = when {
            finalRush -> 154.0
            fever -> 144.0
            else -> 128.0
        }
        val beat = 60.0 / bpm
        val eighth = beat / 2.0
        val stepIndex = floor(t / eighth).toInt() % 8
        val stepPhase = (t % eighth) / eighth
        val beatPhase = (t % beat) / beat

        val notes = doubleArrayOf(261.63, 329.63, 392.0, 523.25, 392.0, 329.63, 440.0, 523.25)
        val note = notes[stepIndex]
        val arpEnvelope = exp(-stepPhase * if (fever) 3.5 else 5.2)
        val arp = sin(TAU * note * t) * arpEnvelope * if (fever) 0.10 else 0.075
        val sparkle = if (fever) {
            sin(TAU * note * 2.0 * t) * arpEnvelope * 0.035
        } else {
            0.0
        }

        val bassNote = if (stepIndex < 4) 130.81 else 110.0
        val bass = sin(TAU * bassNote * t) * 0.055

        val kickFrequency = 72.0 - 26.0 * beatPhase
        val kick = sin(TAU * kickFrequency * (t % beat)) * exp(-beatPhase * 11.0) * 0.12

        val rushTick = if (finalRush) {
            val sixteenth = beat / 4.0
            val tickPhase = (t % sixteenth) / sixteenth
            sin(TAU * 1_760.0 * t) * exp(-tickPhase * 18.0) * 0.025
        } else {
            0.0
        }

        return arp + sparkle + bass + kick + rushTick
    }

    private fun voiceSample(voice: Voice): Double {
        val total = durationSamples(voice.cue).coerceAtLeast(1)
        val progress = (voice.ageSamples.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
        val t = voice.ageSamples.toDouble() / SAMPLE_RATE.toDouble()

        return when (voice.cue) {
            Cue.Countdown -> {
                val frequency = 520.0 + (4 - voice.value.coerceIn(1, 3)) * 95.0
                sin(TAU * frequency * t) * exp(-progress * 7.0) * 0.24
            }

            Cue.Go -> {
                val f0 = 430.0
                val f1 = 1_050.0
                val sweep = f0 * t + ((f1 - f0) / (2.0 * cueDurationSeconds(voice.cue))) * t * t
                (sin(TAU * sweep) + 0.25 * sin(TAU * 2.0 * sweep)) * exp(-progress * 3.0) * 0.22
            }

            Cue.RadiantHit -> {
                // Musical combo lift: every successful hit climbs roughly 1.6 semitones.
                // This is much more audible than a small linear-Hz increase.
                val combo = voice.value.coerceIn(0, 16)
                val pitchScale = 2.0.pow((combo * 1.6) / 12.0)
                val frequency = 520.0 * pitchScale
                val env = exp(-progress * 8.5)
                (
                    sin(TAU * frequency * t) +
                        0.30 * sin(TAU * frequency * 2.0 * t) +
                        0.12 * sin(TAU * frequency * 3.0 * t)
                    ) * env * 0.19
            }

            Cue.PerfectHit -> {
                val combo = voice.value.coerceIn(0, 16)
                val pitchScale = 2.0.pow((combo * 1.6) / 12.0)
                val frequency = 660.0 * pitchScale
                val env = exp(-progress * 6.0)
                (
                    sin(TAU * frequency * t) +
                        0.42 * sin(TAU * frequency * 1.5 * t) +
                        0.25 * sin(TAU * frequency * 2.0 * t)
                    ) * env * 0.22
            }

            Cue.CorruptionHit -> {
                val frequency = 145.0 - 55.0 * progress
                val saw = 2.0 * (frequency * t - floor(frequency * t + 0.5))
                val wobble = sin(TAU * 41.0 * t)
                (saw * 0.68 + wobble * 0.32) * exp(-progress * 4.2) * 0.24
            }

            Cue.Miss -> {
                val env = exp(-progress * 10.0)
                sin(TAU * 190.0 * t) * env * 0.12
            }

            Cue.Fever -> {
                val f0 = 280.0
                val f1 = 1_380.0
                val sweep = f0 * t + ((f1 - f0) / (2.0 * cueDurationSeconds(voice.cue))) * t * t
                val env = (1.0 - progress) * 0.92 + 0.08
                (sin(TAU * sweep) + 0.30 * sin(TAU * 1.5 * sweep)) * env * 0.19
            }

            Cue.ComboBurst -> {
                val combo = voice.value.coerceAtLeast(10)
                val root = if (combo >= 20) 196.0 else if (combo >= 15) 174.61 else 164.81
                val sparkle = root * 4.0
                val impact = sin(TAU * root * t) * exp(-progress * 5.0) * 0.20
                val shine = (
                    sin(TAU * sparkle * t) +
                        0.35 * sin(TAU * sparkle * 1.5 * t)
                    ) * exp(-progress * 3.4) * 0.16
                impact + shine
            }

            Cue.FinalTick -> {
                val urgency = (6 - voice.value.coerceIn(1, 5)).coerceIn(1, 5)
                val frequency = 760.0 + urgency * 95.0
                sin(TAU * frequency * t) * exp(-progress * 9.0) * 0.18
            }

            Cue.RunComplete -> chordArpeggioSample(
                t = t,
                progress = progress,
                frequencies = doubleArrayOf(392.0, 523.25, 659.25),
                gain = 0.20,
            )

            Cue.CapsuleOpen -> {
                val f0 = 210.0
                val f1 = 980.0
                val sweep = f0 * t + ((f1 - f0) / (2.0 * cueDurationSeconds(voice.cue))) * t * t
                val shimmer = sin(TAU * (1_400.0 + 800.0 * progress) * t) * progress
                (sin(TAU * sweep) * 0.75 + shimmer * 0.25) * (1.0 - progress * 0.45) * 0.20
            }

            Cue.RewardReveal -> {
                val rarity = voice.value.coerceIn(0, 5)
                val base = when (rarity) {
                    0 -> 523.25
                    1 -> 587.33
                    2 -> 659.25
                    3 -> 698.46
                    4 -> 783.99
                    else -> 880.0
                }
                val freqs = if (rarity >= 4) {
                    doubleArrayOf(base, base * 1.25, base * 1.5, base * 2.0)
                } else {
                    doubleArrayOf(base, base * 1.25, base * 1.5)
                }
                val musical = chordArpeggioSample(
                    t,
                    progress,
                    freqs,
                    gain = if (rarity >= 4) 0.24 else if (rarity >= 3) 0.22 else 0.18,
                )
                val impact = if (rarity >= 3) {
                    sin(TAU * (92.0 + rarity * 8.0) * t) * exp(-progress * 8.0) * 0.18
                } else {
                    sin(TAU * 120.0 * t) * exp(-progress * 10.0) * 0.07
                }
                val glitter = if (rarity >= 2) {
                    sin(TAU * (1_760.0 + rarity * 220.0) * t) * exp(-progress * 4.5) * (0.025 + rarity * 0.008)
                } else {
                    0.0
                }
                musical + impact + glitter
            }

            Cue.ChestCharge -> {
                val f0 = 110.0
                val f1 = 780.0
                val sweep = f0 * t + ((f1 - f0) / (2.0 * cueDurationSeconds(voice.cue))) * t * t
                val pulse = sin(TAU * 6.0 * t) * 0.25 + 0.75
                (sin(TAU * sweep) + 0.22 * sin(TAU * sweep * 2.0)) * pulse * (0.45 + progress * 0.55) * 0.17
            }

            Cue.ChestOpen -> {
                val impact = sin(TAU * (88.0 - 28.0 * progress) * t) * exp(-progress * 7.5) * 0.30
                val crack = sin(TAU * 1_420.0 * t) * exp(-progress * 14.0) * 0.13
                val whoosh = sin(TAU * (330.0 + 1_100.0 * progress) * t) * (1.0 - progress) * 0.16
                impact + crack + whoosh
            }

            Cue.ChestReveal -> {
                val rarity = voice.value.coerceIn(0, 5)
                val base = 523.25 + rarity * 62.0
                val chord = chordArpeggioSample(
                    t = t,
                    progress = progress,
                    frequencies = if (rarity >= 4) {
                        doubleArrayOf(base, base * 1.25, base * 1.5, base * 2.0, base * 2.5)
                    } else {
                        doubleArrayOf(base, base * 1.25, base * 1.5, base * 2.0)
                    },
                    gain = 0.20 + rarity * 0.012,
                )
                val impact = sin(TAU * (116.0 + rarity * 7.0) * t) *
                    exp(-progress * 9.0) * (0.09 + rarity * 0.018)
                val sparkle = sin(TAU * (1_860.0 + rarity * 190.0) * t) *
                    exp(-progress * 4.2) * (0.045 + rarity * 0.008)
                val halo = sin(TAU * (880.0 + rarity * 85.0) * t) *
                    exp(-progress * 2.8) * if (rarity >= 3) 0.045 else 0.025
                chord + impact + sparkle + halo
            }
        }
    }

    private fun chordArpeggioSample(
        t: Double,
        progress: Double,
        frequencies: DoubleArray,
        gain: Double,
    ): Double {
        val segment = (progress * frequencies.size).toInt().coerceIn(0, frequencies.lastIndex)
        val frequency = frequencies[segment]
        val local = ((progress * frequencies.size) - segment).coerceIn(0.0, 1.0)
        val env = exp(-local * 4.0) * (1.0 - progress * 0.22)
        return (
            sin(TAU * frequency * t) +
                0.30 * sin(TAU * frequency * 2.0 * t)
            ) * env * gain
    }

    private fun durationSamples(cue: Cue): Int =
        (cueDurationSeconds(cue) * SAMPLE_RATE).toInt().coerceAtLeast(1)

    private fun cueDurationSeconds(cue: Cue): Double = when (cue) {
        Cue.Countdown -> 0.13
        Cue.Go -> 0.28
        Cue.RadiantHit -> 0.15
        Cue.PerfectHit -> 0.22
        Cue.CorruptionHit -> 0.23
        Cue.Miss -> 0.10
        Cue.Fever -> 0.52
        Cue.ComboBurst -> 0.34
        Cue.FinalTick -> 0.11
        Cue.RunComplete -> 0.62
        Cue.CapsuleOpen -> 0.72
        Cue.RewardReveal -> 0.82
        Cue.ChestCharge -> 0.62
        Cue.ChestOpen -> 0.44
        Cue.ChestReveal -> 0.84
    }

    private companion object {
        const val SAMPLE_RATE = 24_000
        const val BUFFER_SAMPLES = 768
        const val TAU = 6.283185307179586
    }
}
