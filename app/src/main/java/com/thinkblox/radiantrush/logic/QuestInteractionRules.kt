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
        DAILY_CHECK_IN -> "Runs inside Radiant Rush and saves Firebase progress."
        WALLET_CONNECT -> "Opens an MWA-compatible wallet for authorization."
        SIGN_DAILY_PROOF -> "Opens an MWA-compatible wallet for message signing."
        ON_CHAIN_PROOF -> "Opens an MWA-compatible wallet for devnet memo approval."
        SKR_HOLDER -> "Runs inside Radiant Rush with read-only mainnet RPC; no wallet popup."
        DAILY_RADIANT_CHEST -> "Runs inside Radiant Rush as a no-loss XP reward reveal after all daily proofs."
        else -> "Unknown quest interaction."
    }
}
