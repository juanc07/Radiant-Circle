package com.thinkblox.radiantrush.data

/** Read-only Phase 14D.1R ORE portfolio state. No transaction state belongs here. */
sealed interface OrePortfolioUiState {
    data object NoWallet : OrePortfolioUiState

    data class Loading(
        val walletAddress: String,
    ) : OrePortfolioUiState

    data class Ready(
        val snapshot: OrePortfolioSnapshot,
        val accruedToday: OreAccruedToday,
    ) : OrePortfolioUiState

    data class Error(
        val walletAddress: String?,
        val message: String,
    ) : OrePortfolioUiState
}

data class OrePortfolioSnapshot(
    val walletAddress: String,
    val stakingAuthorityAddress: String,
    val usesExternalStakingAuthority: Boolean,
    val stakingAuthoritySourceLabel: String,
    val stakingAuthorityAutoDetected: Boolean,
    val signedOreTransactionsScanned: Int,
    val autoDiscoveryAttempted: Boolean,
    val network: String,
    val mint: String,
    val stakingProgramId: String,
    val stakeAddress: String,
    val treasuryAddress: String,
    val vestingAddress: String,
    val protocolPositions: List<OreProtocolPositionProof>,
    val liquidRaw: String,
    val liquidDisplay: String,
    val stakingAuthorityLiquidRaw: String,
    val stakingAuthorityLiquidDisplay: String,
    val stakingAuthorityLiquidTokenAccountCount: Int,
    val stakedRaw: String,
    val stakedDisplay: String,
    val unclaimedRewardsRaw: String,
    val unclaimedRewardsDisplay: String,
    val lifetimeRewardsRaw: String,
    val lifetimeRewardsDisplay: String,
    val storedRewardsRaw: String,
    val storedRewardsDisplay: String,
    val pendingAccrualRaw: String,
    val pendingAccrualDisplay: String,
    val lastClaimAtSeconds: Long?,
    val lastDepositAtSeconds: Long?,
    val lastWithdrawAtSeconds: Long?,
    val stakeAccountDataSize: Int?,
    val stakeOwnerVerified: Boolean,
    val stakeAuthorityVerified: Boolean,
    val hasStakeAccount: Boolean,
    val liquidTokenAccountCount: Int,
    val rpcSlot: Long?,
    val checkedAtClientMs: Long,
)

data class OreProtocolPositionProof(
    val label: String,
    val programId: String,
    val stakeAddress: String,
    val treasuryAddress: String,
    val vestingAddress: String,
    val accountFound: Boolean,
    val active: Boolean,
    val stakedDisplay: String,
    val yieldDisplay: String,
    val lifetimeRewardsDisplay: String,
)

data class OreAccruedToday(
    val rawAmount: String,
    val displayAmount: String,
    val baselineDate: String,
    val baselineCapturedAtClientMs: Long,
    val isExactMidnightBaseline: Boolean,
    val helperText: String,
)
