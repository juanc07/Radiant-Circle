package com.thinkblox.radiantrush.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
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
 * Wallet Adapter connection flow, proof quests, SKR Passport, the Phase 9
 * reward loop, and the Phase 10 native Compose game layer.
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
    Locked("Later", Icons.Filled.Lock),
    Syncing("Syncing", Icons.Filled.HourglassTop),
}

data class AccountIdentityPreview(
    val isTemporary: Boolean = true,
    val providerLabel: String = "This device",
    val email: String? = null,
)

data class AccountLinkResult(
    val success: Boolean,
    val restoredExistingAccount: Boolean = false,
    val message: String,
)

data class DailyRadiancePreview(
    val dayKey: String = "local-preview",
    val messageId: String = "",
    val category: String = "",
    val message: String = "",
    val revealedToday: Boolean = false,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val opening: Boolean = false,
)

data class RadiantChestPreview(
    val status: RadiantChestStatus = RadiantChestStatus.Locked,
    val title: String = "Daily Radiant Chest",
    val subtitle: String = "Complete today’s quests to unlock your reward.",
    val progressText: String = "0/5 quests complete",
    val rewardText: String = "Reward: locked",
    val buttonLabel: String = "Unlock Chest",
    val lastRewardRarity: String? = null,
    val lastRewardTitle: String? = null,
    val lastRewardXp: Int = 0,
    val lastRewardStandardTickets: Int = 0,
    val lastRewardSkrCasualTickets: Int = 0,
    val lastRewardSkrBonusXp: Int = 0,
)

enum class RadiantChestStatus(val label: String) {
    Locked("Locked"),
    Ready("Ready"),
    Opening("Opening"),
    Claimed("Claimed"),
}

data class RadiantCollectiblePreview(
    val id: String,
    val title: String,
    val rarity: String,
    val symbol: String,
    val description: String,
    val power: Int,
    val count: Int,
) {
    val discovered: Boolean
        get() = count > 0
}

data class RadiantRunPreview(
    val rushTickets: Int = 3,
    val skrCasualRushTickets: Int = 0,
    val bestScore: Int = 0,
    val totalRuns: Int = 0,
    val lastScore: Int = 0,
    val lastMaxCombo: Int = 0,
    val lastRewardTitle: String? = null,
    val lastRewardRarity: String? = null,
    val lastRewardXp: Int = 0,
    val lastRewardShards: Int = 0,
    val radiantShards: Int = 0,
    val collectionOwned: Int = 0,
    val collectionTotal: Int = 6,
) {
    val totalPlayableTickets: Int
        get() = (rushTickets + skrCasualRushTickets).coerceAtLeast(0)

    val canPlay: Boolean
        get() = totalPlayableTickets > 0

    val collectionProgress: Float
        get() = if (collectionTotal <= 0) 0f else collectionOwned.toFloat() / collectionTotal.toFloat()
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
    val walletLabel: String? = null,
    val avatarId: String = "fox",
    val isCurrentUser: Boolean = false,
)

data class RunLeaderboardPreview(
    val rank: Int,
    val name: String,
    val walletLabel: String?,
    val score: Int,
    val bestCombo: Int,
    val perfectHits: Int,
    val runsPlayed: Int,
    val avatarId: String = "fox",
    val isCurrentUser: Boolean = false,
)

data class WeeklyCupWinnerPreview(
    val placement: Int,
    val walletLabel: String,
    val score: Int,
    val prizeLabel: String,
)

data class WeeklyCupPreview(
    val title: String = "Weekly Radiant Cup",
    val seasonKey: String = "",
    val seasonStartsAtEpochMillis: Long = 0L,
    val seasonEndsAtEpochMillis: Long = 0L,
    val participantCount: Int = 0,
    val personalRank: Int? = null,
    val personalBestScore: Int = 0,
    val projectedRewardTitle: String = "Cup Finisher Mark",
    val projectedRewardDetail: String = "Finish a Ranked run to join this week’s Cup.",
    val previousSeasonKey: String? = null,
    val previousSeasonRank: Int? = null,
    val previousRewardTitle: String? = null,
    val sponsorName: String? = null,
    val sponsoredPrizeLabel: String? = null,
    val sponsoredPrizeStatus: String = "No sponsored prize this week",
    val sponsorNote: String? = null,
    val sponsoredPrizeActive: Boolean = false,
    val trustedSponsorConfig: Boolean = false,
    val cupStatusCode: String = "UNCONFIGURED",
    val cupStatusLabel: String = "No trusted Cup config",
    val fundingVerificationStatus: String = "NOT_CONFIGURED",
    val fundingVerificationLabel: String = "Funding wallet not configured",
    val placementAllocationLabel: String? = null,
    val trustedResultsRequired: Boolean = true,
    val payoutEnabled: Boolean = false,
    val finalResultWeekKey: String? = null,
    val finalResultFundingStatus: String? = null,
    val finalResultFundingLabel: String? = null,
    val finalWinners: List<WeeklyCupWinnerPreview> = emptyList(),
)

data class RetentionGoalPreview(
    val id: String,
    val title: String,
    val detail: String,
    val progress: Int,
    val target: Int,
) {
    val completed: Boolean
        get() = progress >= target

    val progressLabel: String
        get() = "${progress.coerceIn(0, target.coerceAtLeast(0))}/${target.coerceAtLeast(0)}"
}

data class RetentionPreview(
    val dailyGoals: List<RetentionGoalPreview> = emptyList(),
    val weeklyGoals: List<RetentionGoalPreview> = emptyList(),
    val nextActionTitle: String = "Start today’s Rush",
    val nextActionDetail: String = "Complete a goal to build momentum.",
    val dailyCompleted: Int = 0,
    val weeklyCompleted: Int = 0,
    val streakNextTarget: Int = 3,
    val streakRewardTitle: String = "Spark Flame",
    val collectionNextTarget: Int = 3,
    val collectionRewardTitle: String = "Collector Crest",
)

data class RunCompetitionPreview(
    val weeklyLeaderboard: List<RunLeaderboardPreview> = emptyList(),
    val allTimeLeaderboard: List<RunLeaderboardPreview> = emptyList(),
    val weeklyCup: WeeklyCupPreview = WeeklyCupPreview(),
    val weekKey: String = "",
    val rankedAttemptsUsedToday: Int = 0,
    val rankedAttemptsRemaining: Int = 3,
    val dailyGameplayXpEarned: Int = 0,
    val dailyGameplayXpCap: Int = 300,
    val personalWeeklyBestScore: Int = 0,
    val personalAllTimeBestScore: Int = 0,
    val personalBestCombo: Int = 0,
    val personalPerfectHits: Int = 0,
    val personalWeeklyRuns: Int = 0,
    val lastRunMode: String? = null,
    val lastRunPerformanceXp: Int = 0,
) {
    val gameplayXpRemaining: Int
        get() = (dailyGameplayXpCap - dailyGameplayXpEarned).coerceAtLeast(0)
}

data class UserPreview(
    val displayName: String,
    val avatarId: String = "fox",
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
    val skrPassportVersion: Int = 2,
    val skrDailyBonusTickets: Int = 0,
    val skrDailyBonusTicketsGrantedToday: Int = 0,
    val skrChestBonusXp: Int = 0,
    val skrChestBonusTickets: Int = 0,
    val skrFrameLabel: String = "Explorer Frame",
    val skrAuraLabel: String = "No holder aura",
    val skrHolderCollectibleLabel: String = "No holder collectible",
    val skrEligibleBalance: String = "Not checked",
    val skrStakedBalance: String = "Not checked",
    val skrUnstakingBalance: String = "0 SKR",
    val skrStakedStatus: String = "Staking read not verified",
    val skrStakedVerified: Boolean = false,
    val skrStakeBoostActive: Boolean = false,
    val skrStakeBoostLabel: String = "Stake Boost inactive",
    val skrUnstakingReady: Boolean = false,
    val skrStakingProgramId: String = "SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ",
    val lastChestClaimDate: String? = null,
    val lastChestRewardTitle: String? = null,
    val lastChestRewardRarity: String? = null,
    val lastChestRewardXp: Int = 0,
    val lastChestRewardTickets: Int = 0,
    val lastChestSkrBonusXp: Int = 0,
    val lastChestSkrBonusTickets: Int = 0,
    val totalChestXp: Int = 0,
    val rushTickets: Int = 3,
    val skrCasualRushTickets: Int = 0,
    val bestRunScore: Int = 0,
    val totalRuns: Int = 0,
    val lastRunScore: Int = 0,
    val lastRunMaxCombo: Int = 0,
    val lastRunRewardTitle: String? = null,
    val lastRunRewardRarity: String? = null,
    val lastRunRewardXp: Int = 0,
    val lastRunRewardShards: Int = 0,
    val radiantShards: Int = 0,
    val collectionOwned: Int = 0,
    val dailyActivityKey: String? = null,
    val dailyRunsToday: Int = 0,
    val dailyPerfectHitsToday: Int = 0,
    val dailyBestScoreToday: Int = 0,
    val dailyBestComboToday: Int = 0,
    val weeklyActivityKey: String? = null,
    val weeklyRunsCompleted: Int = 0,
    val weeklyChestsOpened: Int = 0,
)

data class RushUiState(
    val firebaseStatus: FirebaseStatus = FirebaseStatus.NotConfigured,
    val accountIdentity: AccountIdentityPreview = AccountIdentityPreview(),
    val user: UserPreview = PreviewContent.user,
    val quests: List<QuestPreview> = PreviewContent.quests,
    val dailyRadiance: DailyRadiancePreview = PreviewContent.dailyRadiance,
    val radiantChest: RadiantChestPreview = PreviewContent.radiantChest,
    val radiantRun: RadiantRunPreview = PreviewContent.radiantRun,
    val collection: List<RadiantCollectiblePreview> = PreviewContent.collection,
    val badges: List<BadgePreview> = PreviewContent.badges,
    val leaderboard: List<LeaderboardPreview> = PreviewContent.leaderboard,
    val runCompetition: RunCompetitionPreview = RunCompetitionPreview(),
    val retention: RetentionPreview = RetentionPreview(),
    val todayKey: String = "local-preview",
    val lastMessage: String? = null,
    val walletActionInProgress: Boolean = false,
    val activeQuestId: String? = null,
    val backgroundSyncInProgress: Boolean = false,
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
        "Syncing",
        "Loading your progress…",
        Icons.Filled.CloudSync,
    ),
    Ready(
        "Cloud ready",
        "Your progress is up to date.",
        Icons.Filled.CloudSync,
    ),
    NotConfigured(
        "Setup needed",
        "Cloud progress is not available yet.",
        Icons.Filled.CloudOff,
    ),
    Error(
        "Sync problem",
        "Could not refresh cloud progress.",
        Icons.Filled.CloudOff,
    ),
}

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Filled.Home),
    Quests("Today", Icons.Filled.Route),
    Circle("Circle", Icons.Filled.Groups),
    Badges("Badges", Icons.Filled.MilitaryTech),
    Leaderboard("Ranks", Icons.Filled.EmojiEvents),
    Profile("You", Icons.Filled.AccountCircle),
    Demo("Guide", Icons.Filled.CheckCircle),
}

object QuestIds {
    const val DAILY_CHECK_IN = "daily-check-in"
    const val WALLET_CONNECT = "wallet-connect"
    const val SIGN_DAILY_PROOF = "sign-daily-proof"
    const val ON_CHAIN_PROOF = "on-chain-proof"
    const val SKR_HOLDER = "skr-holder"
    const val DAILY_RADIANT_CHEST = "daily-radiant-chest"
    const val RADIANT_RUN = "radiant-run"
}

object PreviewContent {
    val user = UserPreview(
        displayName = "Radiant Rookie",
        walletStatus = "Wallet not connected yet",
        walletAddress = "Connect with Mobile Wallet Adapter",
        skrTier = "Explorer",
        xp = 0,
        level = 1,
        currentStreak = 0,
    )

    val quests = listOf(
        QuestPreview(
            id = QuestIds.DAILY_CHECK_IN,
            title = "Daily Check-In",
            description = "Check in for today and keep your streak moving.",
            xp = 50,
            status = QuestStatus.Ready,
            proofType = "Daily check-in",
        ),
        QuestPreview(
            id = QuestIds.WALLET_CONNECT,
            title = "Wallet Ready",
            description = "Connect your Solana wallet for today’s wallet quests.",
            xp = 75,
            status = QuestStatus.Ready,
            proofType = "Wallet connection",
        ),
        QuestPreview(
            id = QuestIds.SIGN_DAILY_PROOF,
            title = "Sign Daily Proof",
            description = "Sign today’s challenge with your connected wallet.",
            xp = 75,
            status = QuestStatus.Blocked,
            proofType = "Wallet signature",
        ),
        QuestPreview(
            id = QuestIds.ON_CHAIN_PROOF,
            title = "On-Chain Memo Proof",
            description = "Approve today’s memo quest in your wallet.",
            xp = 100,
            status = QuestStatus.Blocked,
            proofType = "Memo quest",
        ),
        QuestPreview(
            id = QuestIds.SKR_HOLDER,
            title = "Scan SKR Passport v2",
            description = "Refresh your SKR Passport, staking status, and daily perks.",
            xp = 50,
            status = QuestStatus.Blocked,
            proofType = "SKR Passport",
        ),
    )

    val dailyRadiance = DailyRadiancePreview()
    val radiantChest = RadiantChestPreview()
    val radiantRun = RadiantRunPreview()
    val collection = emptyList<RadiantCollectiblePreview>()

    val badges = listOf(
        BadgePreview("First Launch", "Start Radiant Circle.", unlocked = true),
        BadgePreview("Daily Ready", "Finish your daily check-in.", unlocked = false),
        BadgePreview("Wallet Ready", "Connect your Solana wallet.", unlocked = false),
        BadgePreview("Daily Proof", "Sign the daily proof message.", unlocked = false),
        BadgePreview("On-Chain Spark", "Submit first memo proof.", unlocked = false),
        BadgePreview("SKR Radiant", "Unlock SKR Passport perks.", unlocked = false),
        BadgePreview("Radiant Chest", "Open your Daily Radiant Chest.", unlocked = false),
        BadgePreview("First Run", "Finish your first Radiant Rush.", unlocked = false),
        BadgePreview("Collector", "Discover three Radiant collectibles.", unlocked = false),
        BadgePreview("3-Day Streak Spark", "Keep a three-day streak alive.", unlocked = false),
        BadgePreview("7-Day Rush", "Keep a seven-day streak alive.", unlocked = false),
        BadgePreview("14-Day Streak Aurora", "Keep a fourteen-day streak alive.", unlocked = false),
        BadgePreview("Vault Complete", "Discover the full Radiant collection.", unlocked = false),
    )

    val leaderboard = listOf(
        LeaderboardPreview(1, "You", 0, 0, "Explorer", isCurrentUser = true),
    )

    fun defaultState(): RushUiState = RushUiState()
}
