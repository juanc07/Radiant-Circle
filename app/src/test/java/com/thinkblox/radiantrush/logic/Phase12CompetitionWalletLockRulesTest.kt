package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12CompetitionWalletLockRulesTest {
    @Test
    fun sameWalletMatchesImmutableCupLock() {
        assertTrue(
            Phase12CompetitionWalletLockRules.walletMatchesLock(
                "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
                " J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg ",
            ),
        )
    }

    @Test
    fun differentOrMissingWalletFailsClosed() {
        assertFalse(
            Phase12CompetitionWalletLockRules.walletMatchesLock(
                "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
                "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS",
            ),
        )
        assertFalse(Phase12CompetitionWalletLockRules.walletMatchesLock(null, "wallet"))
    }
    @Test
    fun trustedLockShapeRequiresExactAccountWeekAndAuthority() {
        assertTrue(
            Phase12CompetitionWalletLockRules.isTrustedLockShape(
                schemaVersion = 1,
                weekKey = "2026-W39",
                ownerUid = "firebase-user-a",
                lockAuthority = Phase12CompetitionWalletLockRules.LOCK_AUTHORITY,
                expectedWeekKey = "2026-W39",
                expectedOwnerUid = "firebase-user-a",
            ),
        )
        assertFalse(
            Phase12CompetitionWalletLockRules.isTrustedLockShape(
                schemaVersion = 1,
                weekKey = "2026-W39",
                ownerUid = "firebase-user-a",
                lockAuthority = "forged",
                expectedWeekKey = "2026-W39",
                expectedOwnerUid = "firebase-user-a",
            ),
        )
    }

    @Test
    fun rankedEntryIsAllowedBeforeFirstLockButBlockedForDifferentWalletAfterLock() {
        assertTrue(
            Phase12CompetitionWalletLockRules.allowsRankedEntry(
                lockExists = false,
                trustedLockShape = false,
                lockedWallet = null,
                candidateWallet = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
            ),
        )
        assertFalse(
            Phase12CompetitionWalletLockRules.allowsRankedEntry(
                lockExists = true,
                trustedLockShape = true,
                lockedWallet = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
                candidateWallet = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS",
            ),
        )
    }

}
