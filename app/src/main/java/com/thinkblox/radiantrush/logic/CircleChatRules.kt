package com.thinkblox.radiantrush.logic

object CircleChatRules {
    const val MAX_MESSAGE_LENGTH = 500
    const val MIN_SEND_INTERVAL_MILLIS = 1_000L
    const val MAX_MESSAGES_LOADED = 100L
    const val TYPING_REFRESH_INTERVAL_MILLIS = 3_000L
    const val TYPING_STALE_AFTER_MILLIS = 7_000L

    val REPORT_REASONS = listOf(
        "Spam",
        "Harassment",
        "Unsafe behavior",
        "Other",
    )

    fun sanitizeMessage(raw: String): String = raw.trim().take(MAX_MESSAGE_LENGTH)

    fun isValidMessage(raw: String): Boolean {
        val clean = raw.trim()
        return clean.isNotEmpty() && clean.length <= MAX_MESSAGE_LENGTH
    }

    fun canSendAfter(previousMessageAtMillis: Long, nowMillis: Long): Boolean =
        previousMessageAtMillis <= 0L || nowMillis - previousMessageAtMillis >= MIN_SEND_INTERVAL_MILLIS

    fun isAtOrAfterConnectionStart(eventAtMillis: Long, connectionStartedAtMillis: Long): Boolean =
        connectionStartedAtMillis <= 0L ||
            (eventAtMillis > 0L && eventAtMillis >= connectionStartedAtMillis)

    fun normalizeReportReason(raw: String): String =
        REPORT_REASONS.firstOrNull { it.equals(raw.trim(), ignoreCase = true) } ?: "Other"
}
