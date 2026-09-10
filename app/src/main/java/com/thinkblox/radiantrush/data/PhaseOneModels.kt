package com.thinkblox.radiantrush.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Shared app models for the native Android shell, Firebase foundation, Mobile
 * Wallet Adapter connection flow, proof quests, SKR Passport, and the Phase 9
 * reward loop.
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
    Blocked("Wallet", Icons.Filled.AccountBalanceWallet),
    Locked("Phase 9+", Icons.Filled.Lock),
    Syncing("Syncing", Icons.Filled.HourglassTop),
}

data class RadiantChestPreview(
    val status: RadiantChestStatus = RadiantChestStatus.Locked,
    val title: String = "Daily Radiant Chest",
    val subtitle: String = "Complete every daily proof to unlock a no-loss reward reveal.",
    val progressText: String = "0/5 daily proofs ready",
    val rewardText: String = "Reward: locked",
    val buttonLabel: String = "Unlock Chest",
    val lastRewardRarity: String? = null,
    val lastRewardTitle: String? = null,
    val lastRewardXp: Int = 0,
)

enum class RadiantChestStatus(val label: String) {
    Locked("Locked"),
    Ready("Ready"),
    Opening("Opening"),
    Claimed("Claimed"),
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
    val lastSignedMessageSignature: String? = null,
    val lastOnChainTxSignature: String? = null,
    val lastOnChainExplorerUrl: String? = null,
    val skrBalance: String = "Not checked",
    val skrMultiplier: String = "1.00x",
    val skrNetwork: String = "mainnet-beta",
    val skrMint: String = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3",
    val lastSkrChecked: String? = null,
    val hasSkr: Boolean = false,
    val lastChestClaimDate: String? = null,
    val lastChestRewardTitle: String? = null,
    val lastChestRewardRarity: String? = null,
    val lastChestRewardXp: Int = 0,
    val totalChestXp: Int = 0,
)

data class RushUiState(
    val firebaseStatus: FirebaseStatus = FirebaseStatus.NotConfigured,
    val user: UserPreview = PreviewContent.user,
    val quests: List<QuestPreview> = PreviewContent.quests,
    val radiantChest: RadiantChestPreview = PreviewContent.radiantChest,
    val badges: List<BadgePreview> = PreviewContent.badges,
    val leaderboard: List<LeaderboardPreview> = PreviewContent.leaderboard,
    val todayKey: String = "local-preview",
    val lastMessage: String? = null,
    val walletActionInProgress: Boolean = false,
    val activeQuestId: String? = null,
) {
    val isFirebaseReady: Boolean
        get() = firebaseStatus == FirebaseStatus.Ready

    val isWalletConnected: Boolean
        get() = user.walletStatus == "Wallet connected"

    val completedQuestCount: Int
        get() = quests.count { it.status == QuestStatus.Completed }

    val dailyQuestProgressLabel: String
        get() = "$completedQuestCount/${quests.size} daily proofs"
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
    Demo("Demo", Icons.Filled.CheckCircle),
}

object QuestIds {
    const val DAILY_CHECK_IN = "daily-check-in"
    const val WALLET_CONNECT = "wallet-connect"
    const val SIGN_DAILY_PROOF = "sign-daily-proof"
    const val ON_CHAIN_PROOF = "on-chain-proof"
    const val SKR_HOLDER = "skr-holder"
    const val DAILY_RADIANT_CHEST = "daily-radiant-chest"
}

object PreviewContent {
    val user = UserPreview(
        displayName = "Radiant Rookie",
        walletStatus = "Wallet not connected yet",
        walletAddress = "Connect with Mobile Wallet Adapter in Phase 3",
        skrTier = "Explorer",
        xp = 0,
        level = 1,
        currentStreak = 0,
    )

    val quests = listOf(
        QuestPreview(
            id = QuestIds.DAILY_CHECK_IN,
            title = "Daily Check-In",
            description = "Save today’s quest progress to Firebase. This is cloud progress only, not on-chain proof.",
            xp = 50,
            status = QuestStatus.Ready,
            proofType = "Firestore proof",
        ),
        QuestPreview(
            id = QuestIds.WALLET_CONNECT,
            title = "Wallet Ready",
            description = "Connect a Solana wallet with Mobile Wallet Adapter and save the public address.",
            xp = 75,
            status = QuestStatus.Ready,
            proofType = "MWA authorization",
        ),
        QuestPreview(
            id = QuestIds.SIGN_DAILY_PROOF,
            title = "Sign Daily Proof",
            description = "Ask your wallet to sign today’s proof message. This proves wallet control without spending SOL.",
            xp = 75,
            status = QuestStatus.Blocked,
            proofType = "MWA message signature",
        ),
        QuestPreview(
            id = QuestIds.ON_CHAIN_PROOF,
            title = "On-Chain Memo Proof",
            description = "Open your wallet and approve one lightweight devnet Memo transaction as today’s on-chain proof.",
            xp = 100,
            status = QuestStatus.Blocked,
            proofType = "Devnet memo transaction",
        ),
        QuestPreview(
            id = QuestIds.SKR_HOLDER,
            title = "Scan SKR Passport",
            description = "Read your official SKR SPL token balance on mainnet. No wallet popup; public address only.",
            xp = 50,
            status = QuestStatus.Blocked,
            proofType = "Mainnet SKR balance",
        ),
    )

    val radiantChest = RadiantChestPreview()

    val badges = listOf(
        BadgePreview("First Launch", "Open the native Android app.", unlocked = true),
        BadgePreview("Cloud Synced", "Save Firebase progress.", unlocked = false),
        BadgePreview("Wallet Ready", "Connect with MWA.", unlocked = false),
        BadgePreview("Daily Proof", "Sign the daily proof message.", unlocked = false),
        BadgePreview("On-Chain Spark", "Submit first memo proof.", unlocked = false),
        BadgePreview("SKR Radiant", "Hold real mainnet SKR and unlock boosted status.", unlocked = false),
        BadgePreview("Radiant Chest", "Complete all daily proofs and claim a no-loss reward reveal.", unlocked = false),
        BadgePreview("7-Day Rush", "Keep a seven-day streak alive.", unlocked = false),
    )

    val leaderboard = listOf(
        LeaderboardPreview(1, "You", 0, 0, "Explorer"),
    )

    fun defaultState(): RushUiState = RushUiState()
}
