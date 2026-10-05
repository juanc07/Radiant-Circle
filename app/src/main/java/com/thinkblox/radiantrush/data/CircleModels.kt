package com.thinkblox.radiantrush.data

enum class CircleDiscoveryStatus {
    Idle,
    Locating,
    Searching,
    MatchFound,
    Empty,
    Error,
}

data class SharedSparkPreview(
    val label: String,
    val value: String,
    val symbol: String,
)

data class CircleProfilePreview(
    val uid: String = "",
    val displayName: String = "Radiant Rookie",
    val avatarId: String = "fox",
    val motto: String = "",
    val favoriteFood: String = "",
    val music: String = "",
    val games: String = "",
    val hobbies: String = "",
    val books: String = "",
    val pets: String = "",
    val currentlyInto: String = "",
    val weekendVibe: String = "",
    val talkAbout: String = "",
)

data class CircleMemberPreview(
    val uid: String,
    val displayName: String,
    val avatarId: String,
    val radianceStreak: Int = 0,
    val level: Int = 1,
    val distanceLabel: String = "Radiant Circle",
    val sharedSparks: List<SharedSparkPreview> = emptyList(),
)

data class CircleMemberProfilePreview(
    val member: CircleMemberPreview,
    val profile: CircleProfilePreview,
    val sharedSparks: List<SharedSparkPreview> = emptyList(),
)

data class CircleSparkPreview(
    val edgeId: String,
    val member: CircleMemberPreview,
    val incoming: Boolean,
    val status: String,
    val lastMessagePreview: String = "",
    val lastMessageAtEpochMillis: Long = 0L,
    val hasUnread: Boolean = false,
)

data class CircleSocialSnapshot(
    val incomingRequests: List<CircleSparkPreview> = emptyList(),
    val connections: List<CircleSparkPreview> = emptyList(),
)

data class CircleDiscoveryResult(
    val member: CircleMemberPreview? = null,
    val message: String,
)

data class CircleActionResult(
    val success: Boolean,
    val message: String,
)

data class CircleUiState(
    val discoveryStatus: CircleDiscoveryStatus = CircleDiscoveryStatus.Idle,
    val discoveredMember: CircleMemberPreview? = null,
    val incomingRequests: List<CircleSparkPreview> = emptyList(),
    val connections: List<CircleSparkPreview> = emptyList(),
    val myProfile: CircleProfilePreview = CircleProfilePreview(),
    val myProfileLoading: Boolean = false,
    val selectedMemberProfile: CircleMemberProfilePreview? = null,
    val profileLoading: Boolean = false,
    val chatMember: CircleMemberPreview? = null,
    val chatMessages: List<CircleChatMessagePreview> = emptyList(),
    val chatLoading: Boolean = false,
    val chatSending: Boolean = false,
    val chatStatusMessage: String? = null,
    val chatSentSequence: Int = 0,
    val chatPeerLastReadAtEpochMillis: Long = 0L,
    val chatPeerTyping: Boolean = false,
    val aiSparkStarter: String = "",
    val aiSparkLoading: Boolean = false,
    val aiSparkError: String? = null,
    val message: String = "Shake your phone to discover someone in the Circle.",
    val actionInProgress: Boolean = false,
)

data class ApproximateCircleLocation(
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
)
