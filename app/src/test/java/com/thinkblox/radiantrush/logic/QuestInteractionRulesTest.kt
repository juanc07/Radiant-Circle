package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestInteractionRulesTest {
    @Test
    fun walletProofQuestsOpenExternalWallet() {
        assertTrue(QuestInteractionRules.opensExternalWallet(QuestInteractionRules.WALLET_CONNECT))
        assertTrue(QuestInteractionRules.opensExternalWallet(QuestInteractionRules.SIGN_DAILY_PROOF))
        assertTrue(QuestInteractionRules.opensExternalWallet(QuestInteractionRules.ON_CHAIN_PROOF))
    }

    @Test
    fun skrPassportIsReadOnlyAndDoesNotOpenWallet() {
        assertTrue(QuestInteractionRules.requiresConnectedWallet(QuestInteractionRules.SKR_HOLDER))
        assertTrue(QuestInteractionRules.isReadOnlyRpcQuest(QuestInteractionRules.SKR_HOLDER))
        assertFalse(QuestInteractionRules.opensExternalWallet(QuestInteractionRules.SKR_HOLDER))
    }

    @Test
    fun firebaseCheckInDoesNotNeedWallet() {
        assertFalse(QuestInteractionRules.requiresConnectedWallet(QuestInteractionRules.DAILY_CHECK_IN))
        assertFalse(QuestInteractionRules.opensExternalWallet(QuestInteractionRules.DAILY_CHECK_IN))
        assertFalse(QuestInteractionRules.isReadOnlyRpcQuest(QuestInteractionRules.DAILY_CHECK_IN))
    }

    @Test
    fun dailyRadiantChestDoesNotOpenWalletOrSpendXp() {
        assertFalse(QuestInteractionRules.requiresConnectedWallet(QuestInteractionRules.DAILY_RADIANT_CHEST))
        assertFalse(QuestInteractionRules.opensExternalWallet(QuestInteractionRules.DAILY_RADIANT_CHEST))
        assertFalse(QuestInteractionRules.spendsUserXp(QuestInteractionRules.DAILY_RADIANT_CHEST))
    }
}
