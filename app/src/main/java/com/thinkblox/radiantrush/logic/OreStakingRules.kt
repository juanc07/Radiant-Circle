package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure decoder/math for the production Regolith ORE staking generations that ore.com
 * currently reads side-by-side.
 *
 * Both supported programs expose the same 120-byte Stake and 32-byte Treasury/Vesting
 * layouts used by ore-stake-api 0.2.x / 0.3.x. Steel prepends an 8-byte account header.
 * Numeric is Steel's signed Q80.48 fixed-point value.
 */
object OreStakingRules {
    const val ORE_MINT = "oreoU2P8bN6jkk3jbaiVxYnG1dCXcYxwhwyK9jSybcp"

    /** Current post-migration program (ore-stake-api 0.3.x). */
    const val ORE_STAKE_PROGRAM_ID = "stakecNP3FpiExZPCgZfqRgumVzi6dNqnfrjwXyTgeH"

    /** Legacy production program still read by ore.com / ore-stake-api 0.2.x clients. */
    const val LEGACY_ORE_STAKE_PROGRAM_ID = "STkEAu2cEyQp5ktgUauRVq8es6mEP2w6ixw4NEd5tDJ"

    val SUPPORTED_PROTOCOLS = listOf(
        OreStakeProtocol(
            key = "current",
            label = "Current ORE stake",
            programId = ORE_STAKE_PROGRAM_ID,
        ),
        OreStakeProtocol(
            key = "legacy",
            label = "Legacy ORE stake",
            programId = LEGACY_ORE_STAKE_PROGRAM_ID,
        ),
    )

    const val MAINNET_NETWORK_LABEL = "Solana mainnet-beta"
    const val TOKEN_DECIMALS = 11
    const val STEEL_ACCOUNT_HEADER_SIZE = 8
    const val STAKE_ACCOUNT_SIZE = 120
    const val TREASURY_ACCOUNT_SIZE = 32
    const val VESTING_ACCOUNT_SIZE = 32
    const val VESTING_DURATION_SECONDS = 3_600L

    val STAKE_SEED: ByteArray = "stake".encodeToByteArray()
    val TREASURY_SEED: ByteArray = "treasury".encodeToByteArray()
    val VESTING_SEED: ByteArray = "vesting".encodeToByteArray()

    private const val STAKE_AUTHORITY_OFFSET = 8
    private const val STAKE_BALANCE_OFFSET = 40
    private const val STAKE_COMPOUND_FEE_OFFSET = 48
    private const val STAKE_COMPOUND_FEE_RESERVE_OFFSET = 56
    private const val STAKE_LAST_CLAIM_AT_OFFSET = 64
    private const val STAKE_LAST_DEPOSIT_AT_OFFSET = 72
    private const val STAKE_LAST_WITHDRAW_AT_OFFSET = 80
    private const val STAKE_REWARDS_FACTOR_OFFSET = 88
    private const val STAKE_REWARDS_OFFSET = 104
    private const val STAKE_LIFETIME_REWARDS_OFFSET = 112

    private const val TREASURY_REWARDS_FACTOR_OFFSET = 8
    private const val TREASURY_TOTAL_STAKED_OFFSET = 24

    private const val VESTING_INITIAL_AMOUNT_OFFSET = 8
    private const val VESTING_VESTED_AMOUNT_OFFSET = 16
    private const val VESTING_START_TIME_OFFSET = 24

    private val FIXED_SCALE = BigInteger.ONE.shiftLeft(48)
    private val U64_MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)

    fun stakeAuthorityBytes(data: ByteArray): ByteArray {
        require(data.size == STAKE_ACCOUNT_SIZE) {
            "ORE Stake account layout changed (expected $STAKE_ACCOUNT_SIZE bytes, got ${data.size})."
        }
        return data.copyOfRange(STAKE_AUTHORITY_OFFSET, STAKE_AUTHORITY_OFFSET + 32)
    }

    fun parseStake(data: ByteArray, expectedAuthority: ByteArray): OreStakeRead {
        require(expectedAuthority.size == 32) { "ORE staking authority must be a 32-byte Solana public key." }
        require(data.size == STAKE_ACCOUNT_SIZE) {
            "ORE Stake account layout changed (expected $STAKE_ACCOUNT_SIZE bytes, got ${data.size})."
        }
        require(data.copyOfRange(STAKE_AUTHORITY_OFFSET, STAKE_AUTHORITY_OFFSET + 32).contentEquals(expectedAuthority)) {
            "ORE Stake authority did not match the connected wallet."
        }

        return OreStakeRead(
            balanceRaw = littleEndianUnsigned(data, STAKE_BALANCE_OFFSET, 8),
            compoundFeeLamports = littleEndianUnsigned(data, STAKE_COMPOUND_FEE_OFFSET, 8),
            compoundFeeReserveLamports = littleEndianUnsigned(data, STAKE_COMPOUND_FEE_RESERVE_OFFSET, 8),
            lastClaimAtSeconds = littleEndianSignedLong(data, STAKE_LAST_CLAIM_AT_OFFSET),
            lastDepositAtSeconds = littleEndianSignedLong(data, STAKE_LAST_DEPOSIT_AT_OFFSET),
            lastWithdrawAtSeconds = littleEndianSignedLong(data, STAKE_LAST_WITHDRAW_AT_OFFSET),
            rewardsFactorRaw = littleEndianSigned(data, STAKE_REWARDS_FACTOR_OFFSET, 16),
            rewardsRaw = littleEndianUnsigned(data, STAKE_REWARDS_OFFSET, 8),
            lifetimeRewardsRaw = littleEndianUnsigned(data, STAKE_LIFETIME_REWARDS_OFFSET, 8),
        )
    }

    fun parseTreasury(data: ByteArray): OreTreasuryRead {
        require(data.size == TREASURY_ACCOUNT_SIZE) {
            "ORE Treasury account layout changed (expected $TREASURY_ACCOUNT_SIZE bytes, got ${data.size})."
        }
        return OreTreasuryRead(
            rewardsFactorRaw = littleEndianSigned(data, TREASURY_REWARDS_FACTOR_OFFSET, 16),
            totalStakedRaw = littleEndianUnsigned(data, TREASURY_TOTAL_STAKED_OFFSET, 8),
        )
    }

    fun parseVesting(data: ByteArray): OreVestingRead {
        require(data.size == VESTING_ACCOUNT_SIZE) {
            "ORE Vesting account layout changed (expected $VESTING_ACCOUNT_SIZE bytes, got ${data.size})."
        }
        return OreVestingRead(
            initialAmountRaw = littleEndianUnsigned(data, VESTING_INITIAL_AMOUNT_OFFSET, 8),
            vestedAmountRaw = littleEndianUnsigned(data, VESTING_VESTED_AMOUNT_OFFSET, 8),
            startTimeSeconds = littleEndianSignedLong(data, VESTING_START_TIME_OFFSET),
        )
    }

    /** Mirrors Vesting::vest followed by Stake::update_rewards without writing chain state. */
    fun calculateLiveRewards(
        stake: OreStakeRead,
        treasury: OreTreasuryRead,
        vesting: OreVestingRead,
        clockUnixTimestampSeconds: Long,
    ): OreLiveRewards {
        var liveTreasuryFactor = treasury.rewardsFactorRaw

        val newlyVested = newlyVestedAmount(vesting, clockUnixTimestampSeconds)
        if (newlyVested > BigInteger.ZERO && treasury.totalStakedRaw > BigInteger.ZERO) {
            liveTreasuryFactor += fixedFromFraction(newlyVested, treasury.totalStakedRaw)
        }

        val factorDelta = (liveTreasuryFactor - stake.rewardsFactorRaw).coerceAtLeast(BigInteger.ZERO)
        val newlyAccrued = fixedMultiplyByUnsignedAndTruncate(factorDelta, stake.balanceRaw)
        val liveUnclaimed = checkedU64(stake.rewardsRaw + newlyAccrued, "ORE unclaimed rewards")
        val liveLifetime = checkedU64(stake.lifetimeRewardsRaw + newlyAccrued, "ORE lifetime rewards")

        return OreLiveRewards(
            unclaimedRewardsRaw = liveUnclaimed,
            lifetimeRewardsRaw = liveLifetime,
            newlyAccruedSinceStakeUpdateRaw = newlyAccrued,
            newlyVestedIntoTreasuryRaw = newlyVested,
        )
    }

    fun formatRawOre(raw: BigInteger): String {
        val value = BigDecimal(raw.coerceAtLeast(BigInteger.ZERO)).movePointLeft(TOKEN_DECIMALS)
        val normalized = value.stripTrailingZeros()
        return if (normalized.scale() < 0) normalized.setScale(0).toPlainString() else normalized.toPlainString()
    }

    fun accruedTodayRaw(currentLifetimeRaw: BigInteger, baselineLifetimeRaw: BigInteger): BigInteger =
        (currentLifetimeRaw - baselineLifetimeRaw).coerceAtLeast(BigInteger.ZERO)

    private fun newlyVestedAmount(vesting: OreVestingRead, clockSeconds: Long): BigInteger {
        if (clockSeconds < vesting.startTimeSeconds) return BigInteger.ZERO
        val elapsed = BigInteger.valueOf(clockSeconds - vesting.startTimeSeconds)
        val target = vesting.initialAmountRaw
            .multiply(elapsed)
            .divide(BigInteger.valueOf(VESTING_DURATION_SECONDS))
            .min(vesting.initialAmountRaw)
        return (target - vesting.vestedAmountRaw).coerceAtLeast(BigInteger.ZERO)
    }

    private fun fixedFromFraction(numerator: BigInteger, denominator: BigInteger): BigInteger {
        require(denominator > BigInteger.ZERO) { "ORE reward-factor denominator was zero." }
        return numerator.multiply(FIXED_SCALE).divide(denominator)
    }

    private fun fixedMultiplyByUnsignedAndTruncate(factorRaw: BigInteger, amount: BigInteger): BigInteger =
        factorRaw.coerceAtLeast(BigInteger.ZERO).multiply(amount.coerceAtLeast(BigInteger.ZERO)).divide(FIXED_SCALE)

    private fun littleEndianUnsigned(data: ByteArray, offset: Int, length: Int): BigInteger {
        require(offset >= 0 && length > 0 && offset + length <= data.size) { "Invalid ORE on-chain integer range." }
        return BigInteger(1, data.copyOfRange(offset, offset + length).reversedArray())
    }

    private fun littleEndianSigned(data: ByteArray, offset: Int, length: Int): BigInteger {
        require(offset >= 0 && length > 0 && offset + length <= data.size) { "Invalid ORE on-chain numeric range." }
        return BigInteger(data.copyOfRange(offset, offset + length).reversedArray())
    }

    private fun littleEndianSignedLong(data: ByteArray, offset: Int): Long {
        require(offset >= 0 && offset + 8 <= data.size) { "Invalid ORE on-chain timestamp range." }
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun checkedU64(value: BigInteger, label: String): BigInteger {
        require(value >= BigInteger.ZERO && value <= U64_MAX) { "$label exceeded the protocol u64 range." }
        return value
    }
}

data class OreStakeProtocol(
    val key: String,
    val label: String,
    val programId: String,
)

data class OreStakeRead(
    val balanceRaw: BigInteger,
    val compoundFeeLamports: BigInteger = BigInteger.ZERO,
    val compoundFeeReserveLamports: BigInteger = BigInteger.ZERO,
    val lastClaimAtSeconds: Long = 0L,
    val lastDepositAtSeconds: Long = 0L,
    val lastWithdrawAtSeconds: Long = 0L,
    val rewardsFactorRaw: BigInteger,
    val rewardsRaw: BigInteger,
    val lifetimeRewardsRaw: BigInteger,
)

data class OreTreasuryRead(
    val rewardsFactorRaw: BigInteger,
    val totalStakedRaw: BigInteger,
)

data class OreVestingRead(
    val initialAmountRaw: BigInteger,
    val vestedAmountRaw: BigInteger,
    val startTimeSeconds: Long,
)

data class OreLiveRewards(
    val unclaimedRewardsRaw: BigInteger,
    val lifetimeRewardsRaw: BigInteger,
    val newlyAccruedSinceStakeUpdateRaw: BigInteger,
    val newlyVestedIntoTreasuryRaw: BigInteger,
)
