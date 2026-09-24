package com.thinkblox.radiantrush.logic

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OreStakingRulesTest {
    @Test
    fun supportsBothProductionStakeGenerations() {
        assertEquals(
            "stakecNP3FpiExZPCgZfqRgumVzi6dNqnfrjwXyTgeH",
            OreStakingRules.ORE_STAKE_PROGRAM_ID,
        )
        assertEquals(
            "STkEAu2cEyQp5ktgUauRVq8es6mEP2w6ixw4NEd5tDJ",
            OreStakingRules.LEGACY_ORE_STAKE_PROGRAM_ID,
        )
        assertEquals(2, OreStakingRules.SUPPORTED_PROTOCOLS.size)
    }

    @Test
    fun capturedProductionCurrentStakeDecodesExactOreComBalance() {
        val data = Base64.getDecoder().decode(
            "bAAAAAAAAAD2sIXNCJIvJfML4I8QIvaEb8EjZK/ZOFTv0WKHcOaQ0+rzdcQCAAAAAAAAAAAAAAAAAAAAAAAAAGzos2oAAAAAbOizagAAAABs6LNqAAAAACDBUN6iCwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        )
        val authority = data.copyOfRange(8, 40)
        val stake = OreStakingRules.parseStake(data, authority)

        assertEquals(BigInteger("11885999082"), stake.balanceRaw)
        assertEquals("0.11885999082", OreStakingRules.formatRawOre(stake.balanceRaw))
    }

    @Test
    fun capturedProductionLegacyStakeCanCoexistWithZeroBalance() {
        val data = Base64.getDecoder().decode(
            "bAAAAAAAAAD2sIXNCJIvJfML4I8QIvaEb8EjZK/ZOFTv0WKHcOaQ0wAAAAAAAAAAWBsAAAAAAAAAAAAAAAAAAFVkA2kAAAAAdYACaQAAAACQZgNpAAAAAA/wCcyBFgAAAAAAAAAAAAAAAAAAAAAAAKkiqZ4GAAAA",
        )
        val authority = data.copyOfRange(8, 40)
        val stake = OreStakingRules.parseStake(data, authority)

        assertEquals(BigInteger.ZERO, stake.balanceRaw)
        assertEquals(BigInteger("28431688361"), stake.lifetimeRewardsRaw)
        assertEquals("0.28431688361", OreStakingRules.formatRawOre(stake.lifetimeRewardsRaw))
    }

    @Test
    fun liveRewardsMirrorLinearVestingAndRewardFactor() {
        val stake = OreStakeRead(
            balanceRaw = BigInteger.valueOf(100),
            rewardsFactorRaw = BigInteger.ZERO,
            rewardsRaw = BigInteger.ZERO,
            lifetimeRewardsRaw = BigInteger.ZERO,
        )
        val treasury = OreTreasuryRead(BigInteger.ZERO, BigInteger.valueOf(100))
        val vesting = OreVestingRead(BigInteger.valueOf(3600), BigInteger.ZERO, 0L)

        val live = OreStakingRules.calculateLiveRewards(stake, treasury, vesting, 1800L)

        assertEquals(BigInteger.valueOf(1800), live.newlyVestedIntoTreasuryRaw)
        assertEquals(BigInteger.valueOf(1800), live.unclaimedRewardsRaw)
        assertEquals(BigInteger.valueOf(1800), live.lifetimeRewardsRaw)
    }

    @Test
    fun stakeDecoderRejectsWrongAuthority() {
        val data = ByteArray(OreStakingRules.STAKE_ACCOUNT_SIZE)
        val expected = ByteArray(32) { 1 }
        assertThrows(IllegalArgumentException::class.java) {
            OreStakingRules.parseStake(data, expected)
        }
    }

    @Test
    fun stakeAuthorityCanBeReadBeforePdaVerification() {
        val data = ByteArray(OreStakingRules.STAKE_ACCOUNT_SIZE)
        val authority = ByteArray(32) { (it + 3).toByte() }
        authority.copyInto(data, 8)

        assertArrayEquals(authority, OreStakingRules.stakeAuthorityBytes(data))
    }

    @Test
    fun parseCurrentStakeLayout() {
        val data = ByteArray(OreStakingRules.STAKE_ACCOUNT_SIZE)
        val authority = ByteArray(32) { (it + 1).toByte() }
        authority.copyInto(data, 8)
        putU64(data, 40, 1234L)
        putU64(data, 48, 7000L)
        putU64(data, 56, 14000L)
        putU64(data, 64, 1_700_000_001L)
        putU64(data, 72, 1_700_000_002L)
        putU64(data, 80, 1_700_000_003L)
        putU64(data, 104, 44L)
        putU64(data, 112, 99L)

        val read = OreStakingRules.parseStake(data, authority)

        assertEquals(BigInteger.valueOf(1234), read.balanceRaw)
        assertEquals(BigInteger.valueOf(7000), read.compoundFeeLamports)
        assertEquals(BigInteger.valueOf(14000), read.compoundFeeReserveLamports)
        assertEquals(1_700_000_001L, read.lastClaimAtSeconds)
        assertEquals(1_700_000_002L, read.lastDepositAtSeconds)
        assertEquals(1_700_000_003L, read.lastWithdrawAtSeconds)
        assertEquals(BigInteger.valueOf(44), read.rewardsRaw)
        assertEquals(BigInteger.valueOf(99), read.lifetimeRewardsRaw)
    }

    private fun putU64(data: ByteArray, offset: Int, value: Long) {
        ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(value)
    }
}
