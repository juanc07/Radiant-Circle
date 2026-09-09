package com.thinkblox.radiantrush.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Phase 1 data is intentionally static UI-preview content.
 * Firebase persistence starts in Phase 2. Live Solana data starts in Phase 3+.
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
    Completed("Preview", Icons.Filled.CheckCircle),
    Locked("Phase 3+", Icons.Filled.Shield),
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
        walletStatus = "Not connected yet",
        walletAddress = "MWA connection starts in Phase 3",
        skrTier = "Visitor",
        xp = 0,
        level = 1,
        currentStreak = 0,
    )

    val quests = listOf(
        QuestPreview(
            id = "daily-check-in",
            title = "Daily Check-In",
            description = "Phase 3 will use Mobile Wallet Adapter to sign a daily proof message.",
            xp = 50,
            status = QuestStatus.Ready,
            proofType = "Signed message",
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
        BadgePreview("Wallet Ready", "Connect with Mobile Wallet Adapter.", unlocked = false),
        BadgePreview("Daily Proof", "Complete the first signed daily quest.", unlocked = false),
        BadgePreview("On-Chain Spark", "Submit the first memo proof transaction.", unlocked = false),
        BadgePreview("SKR Radiant", "Hold SKR and unlock boosted status.", unlocked = false),
        BadgePreview("7-Day Rush", "Keep a seven-day streak alive.", unlocked = false),
    )

    val leaderboard = listOf(
        LeaderboardPreview(1, "Sample Radiant", 9600, 18, "Legend"),
        LeaderboardPreview(2, "Quest Runner", 7420, 12, "Core"),
        LeaderboardPreview(3, "Mobile Native", 5880, 9, "Radiant"),
        LeaderboardPreview(4, "You", 0, 0, "Visitor"),
    )
}
