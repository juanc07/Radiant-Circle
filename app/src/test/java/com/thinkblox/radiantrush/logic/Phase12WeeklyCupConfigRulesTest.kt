package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12WeeklyCupConfigRulesTest {
    private fun validState(
        status: String = "OPEN",
        fundingStatus: String = "NOT_VERIFIED",
    ) = Phase12WeeklyCupConfigRules.presentation(
        expectedWeekKey = "2026-W37",
        schemaVersion = 2,
        weekKey = "2026-W37",
        status = status,
        sponsorName = "ThinkBloxPH",
        sponsorNote = "Trusted results only after Cup close.",
        prizeAssetSymbol = "SKR",
        prizeMint = SkrTierRules.OFFICIAL_SKR_MINT,
        prizeDecimals = SkrTierRules.DEFAULT_SKR_DECIMALS,
        prizeAmountAtomic = "1000000000",
        placementAllocationsBps = mapOf(1 to 5000, 2 to 3000, 3 to 2000),
        startsAtEpochMillis = 1_789_344_000_000L,
        endsAtEpochMillis = 1_789_948_800_000L,
        fundingWalletAddress = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
        fundingVerificationStatus = fundingStatus,
        configurationAuthority = Phase12WeeklyCupConfigRules.CONFIGURATION_AUTHORITY,
        trustedResultsRequired = true,
    )

    @Test
    fun trustedOpenConfigIsPublishedButPayoutStaysDisabled() {
        val state = validState()

        assertTrue(state.recognized)
        assertTrue(state.published)
        assertEquals("Cup open", state.statusLabel)
        assertEquals("1000 SKR", state.prizeLabel)
        assertEquals("Prize split • #1 50% • #2 30% • #3 20%", state.placementAllocationLabel)
        assertEquals("Funding not verified", state.fundingStatusLabel)
        assertFalse(state.payoutEnabled)
    }

    @Test
    fun draftConfigIsRecognizedButNotPublished() {
        val state = validState(status = "DRAFT")

        assertTrue(state.recognized)
        assertFalse(state.published)
        assertEquals("Cup draft", state.statusLabel)
    }

    @Test
    fun wrongMintIsRejectedInsteadOfBeingShownAsSkr() {
        val state = Phase12WeeklyCupConfigRules.presentation(
            expectedWeekKey = "2026-W37",
            schemaVersion = 2,
            weekKey = "2026-W37",
            status = "OPEN",
            sponsorName = "Sponsor",
            sponsorNote = null,
            prizeAssetSymbol = "SKR",
            prizeMint = "not-the-official-skr-mint",
            prizeDecimals = 6,
            prizeAmountAtomic = "1000000",
            placementAllocationsBps = mapOf(1 to 10_000),
            startsAtEpochMillis = 1L,
            endsAtEpochMillis = 2L,
            fundingWalletAddress = null,
            fundingVerificationStatus = "NOT_CONFIGURED",
            configurationAuthority = Phase12WeeklyCupConfigRules.CONFIGURATION_AUTHORITY,
            trustedResultsRequired = true,
        )

        assertFalse(state.recognized)
        assertFalse(state.published)
        assertFalse(state.payoutEnabled)
    }

    @Test
    fun malformedPrizeSplitIsRejected() {
        val state = Phase12WeeklyCupConfigRules.presentation(
            expectedWeekKey = "2026-W37",
            schemaVersion = 2,
            weekKey = "2026-W37",
            status = "ANNOUNCED",
            sponsorName = "Sponsor",
            sponsorNote = null,
            prizeAssetSymbol = "SKR",
            prizeMint = SkrTierRules.OFFICIAL_SKR_MINT,
            prizeDecimals = 6,
            prizeAmountAtomic = "1000000",
            placementAllocationsBps = mapOf(1 to 5000, 2 to 3000),
            startsAtEpochMillis = 1L,
            endsAtEpochMillis = 2L,
            fundingWalletAddress = null,
            fundingVerificationStatus = "NOT_CONFIGURED",
            configurationAuthority = Phase12WeeklyCupConfigRules.CONFIGURATION_AUTHORITY,
            trustedResultsRequired = true,
        )

        assertFalse(state.recognized)
        assertFalse(state.payoutEnabled)
    }

    @Test
    fun futureVerifiedFundingStillCannotEnableAndroidPayout() {
        val state = validState(fundingStatus = "VERIFIED")

        assertTrue(state.recognized)
        assertEquals("Funding verified on-chain", state.fundingStatusLabel)
        assertFalse(state.payoutEnabled)
    }
}
