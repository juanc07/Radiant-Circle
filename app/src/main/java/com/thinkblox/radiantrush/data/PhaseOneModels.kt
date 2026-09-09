package com.thinkblox.radiantrush.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shared app models for the Phase 1 UI shell and Phase 2 Firebase foundation.
 * Live Solana wallet state starts in Phase 3. On-chain proof starts in Phase 4.
 */
data class QuestPreview(
    val id: String,
    val title: String,
    val description: String,
    val xp: Int,
    val status: QuestStatus,
    val proofType: String,
)

enum class QuestStatus(val label: String, val icon: ImageVector) {
    Ready("Ready", Icons.Filled.RadioButtonUnchecked),
    Completed("Done", Icons.Filled.CheckCircle),
    Locked("Phase 3+", Icons.Filled.Shield),
    Syncing("Syncing", Icons.Filled.HourglassTop),
}

data class BadgePreview(
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

data class LeaderboardPreview(
    val rank: Int,
    val name: String,
    val xp: Int,
    val streak: Int,
    val tier: String,
)

data class UserPreview(
    val displayName: String,
    val walletStatus: String,
    val walletAddress: String,
    val skrTier: String,
    val xp: Int,
    val level: Int,
    val currentStreak: Int,
)

data class RushUiState(
    val firebaseStatus: FirebaseStatus = FirebaseStatus.NotConfigured,
    val user: UserPreview = PreviewContent.user,
    val quests: List<QuestPreview> = PreviewContent.quests,
    val badges: List<BadgePreview> = PreviewContent.badges,
    val leaderboard: List<LeaderboardPreview> = PreviewContent.leaderboard,
    val todayKey: String = "local-preview",
    val lastMessage: String? = null,
) {
    val isFirebaseReady: Boolean
        get() = firebaseStatus == FirebaseStatus.Ready
}

enum class FirebaseStatus(
    val label: String,
    val detail: String,
    val icon: ImageVector,
) {
    Loading(
        "Firebase syncing",
        "Signing in anonymously and loading your profile.",
        Icons.Filled.CloudSync,
    ),
    Ready(
        "Firebase connected",
        "Anonymous Auth and Firestore are available.",
        Icons.Filled.CloudSync,
    ),
    NotConfigured(
        "Firebase setup needed",
        "Add app/google-services.json, enable Anonymous Auth, and create Firestore.",
        Icons.Filled.CloudOff,
    ),
    Error(
        "Firebase error",
        "The app stayed usable, but cloud sync failed. Check setup and rules.",
        Icons.Filled.CloudOff,
    ),
}

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home("Today", Icons.Filled.Home),
    Quests("Quests", Icons.Filled.Route),
    Badges("Badges", Icons.Filled.MilitaryTech),
    Leaderboard("Ranks", Icons.Filled.EmojiEvents),
    Profile("Profile", Icons.Filled.AccountCircle),
}

object PreviewContent {
    val user = UserPreview(
        displayName = "Radiant Rookie",
        walletStatus = "Wallet not connected yet",
        walletAddress = "Mobile Wallet Adapter starts in Phase 3",
        skrTier = "Visitor",
        xp = 0,
        level = 1,
        currentStreak = 0,
    )

    val quests = listOf(
        QuestPreview(
            id = "daily-check-in",
            title = "Daily Firebase Check-In",
            description = "Save today’s quest progress to Firebase. This is cloud progress only, not an on-chain proof yet.",
            xp = 50,
            status = QuestStatus.Ready,
            proofType = "Firestore proof",
        ),
        QuestPreview(
            id = "wallet-connect",
            title = "Wallet Ready",
            description = "Phase 3 will connect through Mobile Wallet Adapter and save the public wallet address.",
            xp = 75,
            status = QuestStatus.Locked,
            proofType = "MWA authorization",
        ),
        QuestPreview(
            id = "on-chain-proof",
            title = "On-Chain Proof",
            description = "Phase 4 will submit a lightweight Solana memo transaction as proof.",
            xp = 100,
            status = QuestStatus.Locked,
            proofType = "Memo transaction",
        ),
        QuestPreview(
            id = "skr-holder",
            title = "Prove SKR Holder",
            description = "Phase 5 will read token accounts and apply the SKR XP boost.",
            xp = 75,
            status = QuestStatus.Locked,
            proofType = "SKR balance check",
        ),
    )

    val badges = listOf(
        BadgePreview("First Launch", "Open the native Android app shell.", unlocked = true),
        BadgePreview("Cloud Synced", "Create a Firebase profile and save progress.", unlocked = false),
        BadgePreview("Wallet Ready", "Connect with Mobile Wallet Adapter.", unlocked = false),
        BadgePreview("Daily Proof", "Complete the first signed daily quest.", unlocked = false),
        BadgePreview("On-Chain Spark", "Submit the first memo proof transaction.", unlocked = false),
        BadgePreview("SKR Radiant", "Hold SKR and unlock boosted status.", unlocked = false),
        BadgePreview("7-Day Rush", "Keep a seven-day streak alive.", unlocked = false),
    )

    val leaderboard = listOf(
        LeaderboardPreview(1, "You", 0, 0, "Visitor"),
    )

    fun defaultState(): RushUiState = RushUiState()
}
