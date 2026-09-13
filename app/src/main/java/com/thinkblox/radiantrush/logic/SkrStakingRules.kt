package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android/RPC-free decoder/math for the official Solana Mobile SKR staking
 * Anchor accounts used by the read-only Passport scan.
 *
 * Layout constants mirror the official `skr-staking/program/idl.json`.
 */
object SkrStakingRules {
    const val STAKING_PROGRAM_ID = "SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ"
    const val STAKE_CONFIG_ADDRESS = "4HQy82s9CHTv1GsYKnANHMiHfhcqesYkK6sB3RDSYyqw"
    const val USER_PUBKEY_OFFSET = 41
    const val USER_STAKE_MIN_SIZE = 169
    const val STAKE_CONFIG_MIN_SIZE = 153

    val SHARE_PRICE_SCALE: BigInteger = BigInteger("1000000000")

    private const val USER_STAKE_SHARES_OFFSET = 105
    private const val USER_STAKE_UNSTAKING_AMOUNT_OFFSET = 153
    private const val USER_STAKE_UNSTAKE_TIMESTAMP_OFFSET = 161
    private const val STAKE_CONFIG_COOLDOWN_OFFSET = 113
    private const val STAKE_CONFIG_SHARE_PRICE_OFFSET = 137

    private val USER_STAKE_DISCRIMINATOR = byteArrayOf(102, 53, -93, 107, 9, -118, 87, -103)
    private val STAKE_CONFIG_DISCRIMINATOR = byteArrayOf(-18, -105, 43, 3, 11, -105, 63, -80)

    fun parseStakeConfig(data: ByteArray): StakeConfigRead {
        require(matchesDiscriminator(data, STAKE_CONFIG_DISCRIMINATOR)) {
            "StakeConfig discriminator did not match the official SKR staking IDL."
        }
        require(data.size >= STAKE_CONFIG_MIN_SIZE) { "Official SKR StakeConfig account data was too short." }

        val cooldownSeconds = littleEndianUnsigned(data, STAKE_CONFIG_COOLDOWN_OFFSET, 8).toLong()
        val sharePrice = littleEndianUnsigned(data, STAKE_CONFIG_SHARE_PRICE_OFFSET, 16)
        require(sharePrice > BigInteger.ZERO) { "Official SKR StakeConfig share price was zero." }
        return StakeConfigRead(sharePrice = sharePrice, cooldownSeconds = cooldownSeconds)
    }

    fun parseUserStakeOrNull(data: ByteArray): UserStakeRead? {
        if (!matchesDiscriminator(data, USER_STAKE_DISCRIMINATOR) || data.size < USER_STAKE_MIN_SIZE) return null
        return UserStakeRead(
            shares = littleEndianUnsigned(data, USER_STAKE_SHARES_OFFSET, 16),
            unstakingRaw = littleEndianUnsigned(data, USER_STAKE_UNSTAKING_AMOUNT_OFFSET, 8),
            unstakeTimestampSeconds = littleEndianSignedLong(data, USER_STAKE_UNSTAKE_TIMESTAMP_OFFSET),
        )
    }

    /** Official staking math: raw token amount = shares * sharePrice / 1e9. */
    fun activeRawFromShares(shares: BigInteger, sharePrice: BigInteger): BigInteger =
        shares.coerceAtLeast(BigInteger.ZERO)
            .multiply(sharePrice.coerceAtLeast(BigInteger.ZERO))
            .divide(SHARE_PRICE_SCALE)

    fun rawSkrToUi(raw: BigInteger): BigDecimal =
        BigDecimal(raw.coerceAtLeast(BigInteger.ZERO)).movePointLeft(SkrTierRules.DEFAULT_SKR_DECIMALS)

    private fun matchesDiscriminator(data: ByteArray, expected: ByteArray): Boolean =
        data.size >= expected.size && expected.indices.all { data[it] == expected[it] }

    private fun littleEndianUnsigned(data: ByteArray, offset: Int, length: Int): BigInteger {
        require(offset >= 0 && length > 0 && offset + length <= data.size) { "Invalid on-chain integer range." }
        return BigInteger(1, data.copyOfRange(offset, offset + length).reversedArray())
    }

    private fun littleEndianSignedLong(data: ByteArray, offset: Int): Long {
        require(offset >= 0 && offset + 8 <= data.size) { "Invalid on-chain timestamp range." }
        return ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
    }
}

data class StakeConfigRead(
    val sharePrice: BigInteger,
    val cooldownSeconds: Long,
)

data class UserStakeRead(
    val shares: BigInteger,
    val unstakingRaw: BigInteger,
    val unstakeTimestampSeconds: Long,
)
