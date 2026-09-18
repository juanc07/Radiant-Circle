package com.thinkblox.radiantrush.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.thinkblox.radiantrush.R

/**
 * Small, non-blocking chat cues.
 *
 * SoundPool loads samples asynchronously. If the user sends immediately after opening chat,
 * queue that cue until the sample is ready instead of silently dropping the first sound.
 */
class ChatSoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var sendId: Int = 0
    private var receiveId: Int = 0
    private val loadedIds = mutableSetOf<Int>()
    private var pendingSend = false
    private var pendingReceive = false
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (released || status != 0) return@setOnLoadCompleteListener
            loadedIds += sampleId

            when (sampleId) {
                sendId -> if (pendingSend) {
                    pendingSend = false
                    playSendInternal()
                }

                receiveId -> if (pendingReceive) {
                    pendingReceive = false
                    playReceiveInternal()
                }
            }
        }

        sendId = soundPool.load(context.applicationContext, R.raw.circle_chat_send, 1)
        receiveId = soundPool.load(context.applicationContext, R.raw.circle_chat_receive, 1)
    }

    fun playSend() {
        // Intentionally silent. The optimistic message bubble is enough send feedback,
        // while avoiding a repetitive/loud cue on every outgoing message.
    }

    fun playReceive() {
        if (released) return
        if (loadedIds.contains(receiveId)) {
            playReceiveInternal()
        } else {
            pendingReceive = true
        }
    }

    private fun playSendInternal() {
        soundPool.play(sendId, 0.95f, 0.95f, 1, 0, 1f)
    }

    private fun playReceiveInternal() {
        soundPool.play(receiveId, 0.68f, 0.68f, 1, 0, 1f)
    }

    fun release() {
        released = true
        pendingSend = false
        pendingReceive = false
        loadedIds.clear()
        soundPool.release()
    }
}
