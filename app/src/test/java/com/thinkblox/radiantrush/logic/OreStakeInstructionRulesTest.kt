package com.thinkblox.radiantrush.logic

import java.math.BigInteger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class OreStakeInstructionRulesTest {
    @Test
    fun depositMatchesVerifiedAbiShape() {
        val data = OreStakeInstructionRules.depositData(BigInteger("258"))
        assertEquals(25, data.size)
        assertEquals(10, data[0].toInt())
        assertEquals(2, data[1].toInt() and 0xff)
        assertEquals(1, data[2].toInt() and 0xff)
        assertArrayEquals(ByteArray(16), data.copyOfRange(9, 25))

        val preservingFee = OreStakeInstructionRules.depositData(
            BigInteger.ONE,
            compoundFeeLamports = BigInteger("258"),
        )
        assertEquals(2, preservingFee[9].toInt() and 0xff)
        assertEquals(1, preservingFee[10].toInt() and 0xff)
        assertArrayEquals(ByteArray(8), preservingFee.copyOfRange(17, 25))
    }

    @Test
    fun withdrawAndClaimUseVerifiedDiscriminators() {
        val amount = BigInteger.ONE
        assertEquals(9, OreStakeInstructionRules.withdrawData(amount).size)
        assertEquals(11, OreStakeInstructionRules.withdrawData(amount)[0].toInt())
        assertEquals(12, OreStakeInstructionRules.claimData(amount)[0].toInt())
        assertArrayEquals(byteArrayOf(13), OreStakeInstructionRules.compoundData())
    }
}
