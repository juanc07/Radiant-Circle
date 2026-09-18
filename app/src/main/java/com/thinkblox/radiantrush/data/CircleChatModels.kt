package com.thinkblox.radiantrush.data

data class CircleChatMessagePreview(
    val id: String,
    val senderUid: String,
    val text: String,
    val sentAtEpochMillis: Long,
    val isMine: Boolean,
    val hasPendingWrites: Boolean = false,
)

data class CircleChatMetaPreview(
    val peerLastReadAtEpochMillis: Long = 0L,
    val peerTyping: Boolean = false,
)
