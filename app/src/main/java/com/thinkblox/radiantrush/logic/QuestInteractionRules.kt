package com.thinkblox.radiantrush.logic

/**
 * Pure quest/reward interaction rules for automated tests and demo documentation.
 *
 * Wallet-connect, signature, and memo quests can open a wallet app. SKR Passport
 * is intentionally read-only and must not open Phantom because it only checks
 * the connected public address on mainnet RPC. The Daily Radiant Chest is also
 * in-app only and no-loss: it never spends XP, SOL, SKR, or any token.
 */
object QuestInteractionRules {
    const val DAILY_CHECK_IN = "daily-check-in"
    const val WALLET_CONNECT = "wallet-connect"
    const val SIGN_DAILY_PROOF = "sign-daily-proof"
    const val ON_CHAIN_PROOF = "on-chain-proof"
    const val SKR_HOLDER = "skr-holder"
    const val DAILY_RADIANT_CHEST = "daily-radiant-chest"

    fun requiresConnectedWallet(questId: String): Boolean = when (questId) {
        SIGN_DAILY_PROOF, ON_CHAIN_PROOF, SKR_HOLDER -> true
        else -> false
    }

    fun opensExternalWallet(questId: String): Boolean = when (questId) {
        WALLET_CONNECT, SIGN_DAILY_PROOF, ON_CHAIN_PROOF -> true
        else -> false
    }

    fun isReadOnlyRpcQuest(questId: String): Boolean = questId == SKR_HOLDER

    fun spendsUserXp(questId: String): Boolean = false

    fun expectedUserInteraction(questId: String): String = when (questId) {
        DAILY_CHECK_IN -> "Checks in for today."
        WALLET_CONNECT -> "Opens your Solana wallet to connect."
        SIGN_DAILY_PROOF -> "Opens your wallet to sign today’s challenge."
        ON_CHAIN_PROOF -> "Opens your wallet to approve today’s memo quest."
        SKR_HOLDER -> "Refreshes your SKR Passport and staking status."
        DAILY_RADIANT_CHEST -> "Opens your Daily Radiant Chest after all quests are complete."
        else -> "Unknown quest interaction."
    }
}
