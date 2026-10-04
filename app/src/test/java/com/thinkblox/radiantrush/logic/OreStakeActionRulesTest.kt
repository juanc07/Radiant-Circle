package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.OreStakeAction
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OreStakeActionRulesTest {
    @Test
    fun parsesOreWithElevenDecimals() {
        assertEquals(BigInteger("100000000000"), OreStakeActionRules.parseOreInputToRaw("1"))
        assertEquals(BigInteger("12345678901"), OreStakeActionRules.parseOreInputToRaw("0.12345678901"))
    }

    @Test
    fun rejectsZeroAndTooManyDecimals() {
        assertThrows(IllegalArgumentException::class.java) { OreStakeActionRules.parseOreInputToRaw("0") }
        assertThrows(IllegalArgumentException::class.java) { OreStakeActionRules.parseOreInputToRaw("0.000000000001") }
    }

    @Test
    fun validatesAgainstCurrentActionBalance() {
        assertNull(
            OreStakeActionRules.validateAvailable(
                OreStakeAction.Stake,
                BigInteger("100"),
                liquidRaw = "100",
                currentStakedRaw = "50",
                currentClaimableRaw = "25",
            ),
        )
        assertEquals(
            "Amount is higher than your current-program stake.",
            OreStakeActionRules.validateAvailable(
                OreStakeAction.Withdraw,
                BigInteger("51"),
                liquidRaw = "100",
                currentStakedRaw = "50",
                currentClaimableRaw = "25",
            ),
        )
    }

    @Test
    fun verifiesStakeWithdrawAndClaimByObservedStateChange() {
        assertTrue(OreStakeActionRules.verifiedAfterRefresh(OreStakeAction.Stake, BigInteger("20"), "100", "50", "80", "70"))
        assertTrue(OreStakeActionRules.verifiedAfterRefresh(OreStakeAction.Withdraw, BigInteger("20"), "100", "50", "120", "30"))
        assertFalse(OreStakeActionRules.verifiedAfterRefresh(OreStakeAction.Withdraw, BigInteger("20"), "100", "50", "100", "30"))
        assertTrue(OreStakeActionRules.verifiedAfterRefresh(OreStakeAction.Claim, BigInteger("20"), "100", "50", "120", "50"))
        assertFalse(OreStakeActionRules.verifiedAfterRefresh(OreStakeAction.Claim, BigInteger("20"), "100", "50", "119", "50"))
    }
}
