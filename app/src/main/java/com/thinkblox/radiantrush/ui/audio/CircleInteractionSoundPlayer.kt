package com.thinkblox.radiantrush.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.thinkblox.radiantrush.R

/** Short foreground interaction cue for Shake to Discover. */
class CircleInteractionSoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var shakeId: Int = 0
    private var loaded = false
    private var pendingShake = false
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released || status != 0 || sampleId != shakeId) return@setOnLoadCompleteListener
            loaded = true
            if (pendingShake) {
                pendingShake = false
                playShakeInternal()
            }
        }
        shakeId = soundPool.load(context.applicationContext, R.raw.circle_shake, 1)
    }

    fun playShake() {
        if (released) return
        if (loaded) playShakeInternal() else pendingShake = true
    }

    private fun playShakeInternal() {
        soundPool.play(shakeId, 0.92f, 0.92f, 1, 0, 1f)
    }

    fun release() {
        released = true
        pendingShake = false
        soundPool.release()
    }
}
