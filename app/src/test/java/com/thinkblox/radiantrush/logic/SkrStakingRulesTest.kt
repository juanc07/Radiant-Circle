package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkrStakingRulesTest {
    @Test
    fun officialShareMathUsesOneE9ScaleAndSixSkrDecimals() {
        val shares = BigInteger("100000000")
        val sharePrice = BigInteger("1250000000")
        val raw = SkrStakingRules.activeRawFromShares(shares, sharePrice)

        assertEquals(BigInteger("125000000"), raw)
        assertEquals(BigDecimal("125.000000"), SkrStakingRules.rawSkrToUi(raw))
    }

    @Test
    fun decodesOfficialStakeConfigOffsets() {
        val data = ByteArray(SkrStakingRules.STAKE_CONFIG_MIN_SIZE)
        byteArrayOf(-18, -105, 43, 3, 11, -105, 63, -80).copyInto(data, 0)
        writeUnsignedLittleEndian(data, 113, BigInteger("172800"), 8)
        writeUnsignedLittleEndian(data, 137, BigInteger("1234567890"), 16)

        val parsed = SkrStakingRules.parseStakeConfig(data)

        assertEquals(172800L, parsed.cooldownSeconds)
        assertEquals(BigInteger("1234567890"), parsed.sharePrice)
    }

    @Test
    fun decodesOfficialUserStakeOffsets() {
        val data = ByteArray(SkrStakingRules.USER_STAKE_MIN_SIZE)
        byteArrayOf(102, 53, -93, 107, 9, -118, 87, -103).copyInto(data, 0)
        writeUnsignedLittleEndian(data, 105, BigInteger("987654321"), 16)
        writeUnsignedLittleEndian(data, 153, BigInteger("25000000"), 8)
        writeSignedLongLittleEndian(data, 161, 1_700_000_000L)

        val parsed = SkrStakingRules.parseUserStakeOrNull(data)!!

        assertEquals(BigInteger("987654321"), parsed.shares)
        assertEquals(BigInteger("25000000"), parsed.unstakingRaw)
        assertEquals(1_700_000_000L, parsed.unstakeTimestampSeconds)
    }

    @Test
    fun rejectsNonUserStakeDiscriminator() {
        assertNull(SkrStakingRules.parseUserStakeOrNull(ByteArray(SkrStakingRules.USER_STAKE_MIN_SIZE)))
    }

    private fun writeUnsignedLittleEndian(target: ByteArray, offset: Int, value: BigInteger, length: Int) {
        val bytes = value.toByteArray().let { if (it.size > 1 && it[0].toInt() == 0) it.copyOfRange(1, it.size) else it }
        bytes.reversedArray().copyInto(target, offset, endIndex = minOf(bytes.size, length))
    }

    private fun writeSignedLongLittleEndian(target: ByteArray, offset: Int, value: Long) {
        repeat(8) { index ->
            target[offset + index] = ((value ushr (index * 8)) and 0xffL).toByte()
        }
    }
}
