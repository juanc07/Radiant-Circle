package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkrTierRulesTest {
    @Test
    fun zeroBalanceIsExplorer() {
        val tier = SkrTierRules.tierForBalance(BigDecimal.ZERO)

        assertEquals("Explorer", tier.label)
        assertEquals("1.00x", tier.multiplierLabel)
        assertEquals(1.00, tier.multiplierValue, 0.0)
        assertFalse(tier.hasSkr)
    }

    @Test
    fun positiveBalanceIsScout() {
        val tier = SkrTierRules.tierForBalance(BigDecimal("0.000001"))

        assertEquals("Radiant Scout", tier.label)
        assertEquals("1.05x", tier.multiplierLabel)
        assertTrue(tier.hasSkr)
    }

    @Test
    fun tierThresholdsAreStable() {
        assertEquals("Radiant Holder", SkrTierRules.tierForBalance(BigDecimal("100")).label)
        assertEquals("Radiant Elite", SkrTierRules.tierForBalance(BigDecimal("1000")).label)
        assertEquals("Radiant Legend", SkrTierRules.tierForBalance(BigDecimal("10000")).label)
    }

    @Test
    fun formatBalanceKeepsDemoTextShort() {
        assertEquals("0", SkrTierRules.formatBalance(BigDecimal.ZERO))
        assertEquals("10.1", SkrTierRules.formatBalance(BigDecimal("10.100000")))
        assertEquals("123.4567", SkrTierRules.formatBalance(BigDecimal("123.456789")))
    }

    @Test
    fun officialMintIsConfigured() {
        assertEquals("SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3", SkrTierRules.OFFICIAL_SKR_MINT)
        assertEquals("mainnet-beta", SkrTierRules.MAINNET_NETWORK_LABEL)
    }
}
