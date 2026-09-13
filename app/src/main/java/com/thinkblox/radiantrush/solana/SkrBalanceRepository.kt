package com.thinkblox.radiantrush.solana

import android.util.Log
import com.thinkblox.radiantrush.logic.SkrPassportRules
import com.thinkblox.radiantrush.logic.SkrTierRules
import com.thinkblox.radiantrush.logic.SkrStakingRules
import java.math.BigDecimal
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only SKR Passport scanner used by Phase 11C.1.
 *
 * Liquid SKR is read from SPL token accounts. Active staked SKR is read from
 * the official Solana Mobile staking program's public accounts. Both reads use
 * only the connected public wallet address on Mainnet; neither path requests a
 * signature, transaction, seed phrase, private key, or token movement.
 */
class SkrBalanceRepository {
    suspend fun fetchSkrBalance(walletAddress: String): SkrBalanceResult = withContext(Dispatchers.IO) {
        runCatching {
            val cleanWallet = walletAddress.trim()
            require(cleanWallet.isNotBlank()) { "Connect a wallet before checking SKR." }

            Log.i(TAG, "Checking mainnet SKR Passport for walletPrefix=${cleanWallet.take(6)}")
            val liquidResponse = postMainnetRpc(buildGetTokenAccountsRequest(cleanWallet))
            val liquid = parseLiquidBalanceResponse(liquidResponse)

            // Staking is additive to the liquid scan, but a temporary staking-RPC
            // failure must not destroy an otherwise valid liquid Passport refresh.
            val staking = runCatching { fetchVerifiedStaking(cleanWallet) }
                .onFailure { Log.w(TAG, "Official SKR staking read unavailable; liquid scan remains valid", it) }
                .getOrElse { error ->
                    SkrStakingSnapshot.unverified(
                        "Staking read unavailable — ${friendlyRpcFailure(error)}",
                    )
                }

            val eligibleBalance = SkrPassportRules.eligibleBalance(
                liquidBalance = liquid.uiAmount,
                activeStakedBalance = staking.activeStakedUiAmount,
                stakedVerified = staking.verified,
            )
            val tier = SkrTierRules.tierForBalance(eligibleBalance)
            val perks = SkrPassportRules.perksForBalances(
                liquidBalance = liquid.uiAmount,
                activeStakedBalance = staking.activeStakedUiAmount,
                stakedVerified = staking.verified,
            )
            val formattedLiquid = SkrTierRules.formatBalance(liquid.uiAmount)
            val formattedStaked = SkrTierRules.formatBalance(staking.activeStakedUiAmount)
            val formattedUnstaking = SkrTierRules.formatBalance(staking.unstakingUiAmount)
            val formattedEligible = SkrTierRules.formatBalance(eligibleBalance)

            val snapshot = SkrBalanceSnapshot(
                walletAddress = cleanWallet,
                mint = SkrTierRules.OFFICIAL_SKR_MINT,
                network = SkrTierRules.MAINNET_NETWORK_LABEL,
                balanceRawAmount = liquid.rawAmount.toString(),
                balanceUiAmount = formattedLiquid,
                balanceDisplay = "$formattedLiquid SKR",
                decimals = liquid.decimals,
                tokenAccountCount = liquid.tokenAccountCount,
                tierLabel = tier.label,
                xpMultiplierLabel = tier.multiplierLabel,
                xpMultiplierValue = tier.multiplierValue,
                hasSkr = tier.hasSkr,
                passportVersion = SkrPassportRules.PASSPORT_VERSION,
                dailyCasualTicketBonus = perks.dailyCasualTicketBonus,
                chestBonusXp = perks.chestBonusXp,
                chestBonusTickets = perks.chestBonusTickets,
                frameLabel = perks.frameLabel,
                auraLabel = perks.auraLabel,
                holderCollectibleLabel = perks.holderCollectibleLabel,
                eligibleSkrUiAmount = formattedEligible,
                eligibleSkrDisplay = "$formattedEligible SKR",
                stakedSkrUiAmount = formattedStaked,
                stakedSkrDisplay = "$formattedStaked SKR",
                unstakingSkrUiAmount = formattedUnstaking,
                unstakingSkrDisplay = "$formattedUnstaking SKR",
                stakedSkrStatus = staking.status,
                stakedSkrVerified = staking.verified,
                stakeBoostActive = perks.stakeBoostActive,
                stakeBoostLabel = perks.stakeBoostLabel,
                unstakingReady = staking.unstakingReady,
                unstakeReadyAtClientMs = staking.unstakeReadyAtClientMs,
                stakingProgramId = SkrStakingRules.STAKING_PROGRAM_ID,
                stakingAccountCount = staking.accountCount,
                stakingRpcSlot = staking.rpcSlot,
                rpcSlot = liquid.rpcSlot,
                checkedAtClientMs = System.currentTimeMillis(),
            )

            Log.i(
                TAG,
                "SKR Passport complete. liquid=$formattedLiquid staked=$formattedStaked " +
                    "eligible=$formattedEligible stakingVerified=${staking.verified} tier=${tier.label}",
            )
            SkrBalanceResult.Success(snapshot)
        }.getOrElse { error ->
            Log.e(TAG, "SKR Passport check failed", error)
            SkrBalanceResult.Failure(friendlyRpcFailure(error))
        }
    }

    private fun buildGetTokenAccountsRequest(walletAddress: String): String =
        """{"jsonrpc":"2.0","id":1,"method":"getTokenAccountsByOwner","params":["$walletAddress",{"mint":"${SkrTierRules.OFFICIAL_SKR_MINT}"},{"encoding":"jsonParsed","commitment":"confirmed"}]}"""

    private fun buildGetStakeConfigRequest(): String =
        """{"jsonrpc":"2.0","id":2,"method":"getAccountInfo","params":["${SkrStakingRules.STAKE_CONFIG_ADDRESS}",{"encoding":"base64","commitment":"confirmed"}]}"""

    /**
     * Finds every UserStake owned by this wallet. Filtering on offset 41 is
     * derived from the official Anchor IDL layout:
     * discriminator(8) + bump(1) + stake_config(32) = user pubkey at byte 41.
     * This avoids assuming the wallet will always use only one Guardian pool.
     */
    private fun buildGetUserStakeAccountsRequest(walletAddress: String): String =
        """{"jsonrpc":"2.0","id":3,"method":"getProgramAccounts","params":["${SkrStakingRules.STAKING_PROGRAM_ID}",{"encoding":"base64","commitment":"confirmed","withContext":true,"filters":[{"memcmp":{"offset":${SkrStakingRules.USER_PUBKEY_OFFSET},"bytes":"$walletAddress"}}]}]}"""

    private fun parseLiquidBalanceResponse(responseBody: String): LiquidBalanceSnapshot {
        val root = JSONObject(responseBody)
        throwIfRpcError(root, "Mainnet SKR RPC")
        val result = root.optJSONObject("result")
            ?: throw IllegalStateException("Mainnet SKR RPC response did not include a result.")
        val slot = result.optJSONObject("context")?.optLong("slot", -1L)?.takeIf { it >= 0L }
        val accounts = result.optJSONArray("value")

        var rawAmount = BigInteger.ZERO
        var uiAmount = BigDecimal.ZERO
        var decimals = SkrTierRules.DEFAULT_SKR_DECIMALS
        var tokenAccountCount = 0

        if (accounts != null) {
            for (index in 0 until accounts.length()) {
                val tokenAmount = accounts.optJSONObject(index)
                    ?.optJSONObject("account")
                    ?.optJSONObject("data")
                    ?.optJSONObject("parsed")
                    ?.optJSONObject("info")
                    ?.optJSONObject("tokenAmount")
                    ?: continue

                rawAmount += tokenAmount.optString("amount", "0").toBigIntegerOrNull() ?: BigInteger.ZERO
                uiAmount += tokenAmount.optString("uiAmountString", "0").toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (tokenAccountCount == 0) {
                    decimals = tokenAmount.optInt("decimals", SkrTierRules.DEFAULT_SKR_DECIMALS)
                }
                tokenAccountCount += 1
            }
        }

        return LiquidBalanceSnapshot(rawAmount, uiAmount, decimals, tokenAccountCount, slot)
    }

    private fun fetchVerifiedStaking(walletAddress: String): SkrStakingSnapshot {
        val configResponse = postMainnetRpc(buildGetStakeConfigRequest())
        val configRoot = JSONObject(configResponse)
        throwIfRpcError(configRoot, "SKR staking config RPC")
        val configResult = configRoot.optJSONObject("result")
            ?: throw IllegalStateException("SKR staking config response did not include a result.")
        val configSlot = configResult.optJSONObject("context")?.optLong("slot", -1L)?.takeIf { it >= 0L }
        val configValue = configResult.optJSONObject("value")
            ?: throw IllegalStateException("Official SKR StakeConfig account was not found.")
        val configData = decodeBase64AccountData(configValue)
        val config = SkrStakingRules.parseStakeConfig(configData)
        val cooldownSeconds = config.cooldownSeconds
        val sharePrice = config.sharePrice

        val stakeAccountsResponse = postMainnetRpc(buildGetUserStakeAccountsRequest(walletAddress))
        val stakeRoot = JSONObject(stakeAccountsResponse)
        throwIfRpcError(stakeRoot, "SKR UserStake RPC")
        val resultAny = stakeRoot.opt("result")
        val stakeResultObject = resultAny as? JSONObject
        val stakeArray = when (resultAny) {
            is JSONObject -> resultAny.optJSONArray("value")
            is JSONArray -> resultAny
            else -> null
        } ?: JSONArray()
        val stakeSlot = stakeResultObject?.optJSONObject("context")
            ?.optLong("slot", -1L)
            ?.takeIf { it >= 0L }
            ?: configSlot

        var totalShares = BigInteger.ZERO
        var totalUnstakingRaw = BigInteger.ZERO
        var accountCount = 0
        var latestReadyAtSeconds: Long? = null

        for (index in 0 until stakeArray.length()) {
            val accountObject = stakeArray.optJSONObject(index)?.optJSONObject("account") ?: continue
            val data = decodeBase64AccountData(accountObject)
            val parsed = SkrStakingRules.parseUserStakeOrNull(data) ?: continue

            totalShares += parsed.shares
            val unstakingRaw = parsed.unstakingRaw
            totalUnstakingRaw += unstakingRaw
            if (unstakingRaw > BigInteger.ZERO) {
                val readyAt = parsed.unstakeTimestampSeconds + cooldownSeconds
                latestReadyAtSeconds = maxOf(latestReadyAtSeconds ?: readyAt, readyAt)
            }
            accountCount += 1
        }

        // Official staking math: token raw amount = shares * sharePrice / 1e9.
        val activeRaw = SkrStakingRules.activeRawFromShares(totalShares, sharePrice)
        val activeUi = SkrStakingRules.rawSkrToUi(activeRaw)
        val unstakingUi = SkrStakingRules.rawSkrToUi(totalUnstakingRaw)
        val nowSeconds = System.currentTimeMillis() / 1000L
        val unstakingReady = totalUnstakingRaw > BigInteger.ZERO &&
            latestReadyAtSeconds != null &&
            nowSeconds >= latestReadyAtSeconds
        val readyAtMs = latestReadyAtSeconds?.times(1000L)
        val status = when {
            activeUi > BigDecimal.ZERO && totalUnstakingRaw > BigInteger.ZERO -> {
                val cooldown = if (unstakingReady) "unstaking amount ready to withdraw" else "unstaking cooldown active"
                "Verified on-chain • active stake earning rewards • $cooldown"
            }
            activeUi > BigDecimal.ZERO -> "Verified on-chain • active stake earning rewards"
            totalUnstakingRaw > BigInteger.ZERO && unstakingReady -> "Verified on-chain • unstaked amount ready to withdraw"
            totalUnstakingRaw > BigInteger.ZERO -> "Verified on-chain • unstaking cooldown active; cooldown SKR does not earn Stake Boost"
            else -> "Verified on-chain • no active SKR stake found"
        }

        return SkrStakingSnapshot(
            activeStakedUiAmount = activeUi,
            unstakingUiAmount = unstakingUi,
            verified = true,
            status = status,
            unstakingReady = unstakingReady,
            unstakeReadyAtClientMs = readyAtMs,
            accountCount = accountCount,
            rpcSlot = stakeSlot,
        )
    }

    private fun decodeBase64AccountData(accountObject: JSONObject): ByteArray {
        val data = accountObject.optJSONArray("data")
            ?: throw IllegalStateException("Solana account response did not include base64 data.")
        val encoded = data.optString(0)
        require(encoded.isNotBlank()) { "Solana account base64 payload was empty." }
        return Base64.getDecoder().decode(encoded)
    }

    private fun throwIfRpcError(root: JSONObject, label: String) {
        val rpcError = root.optJSONObject("error") ?: return
        throw IllegalStateException("$label error: ${rpcError.optString("message", "Unknown RPC error")}")
    }

    private fun postMainnetRpc(requestBody: String): String {
        val connection = (URL(MAINNET_RPC_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = RPC_TIMEOUT_MS
            readTimeout = RPC_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            connection.outputStream.use { outputStream ->
                outputStream.write(requestBody.encodeToByteArray())
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            if (responseCode !in 200..299) {
                throw IllegalStateException("Mainnet RPC HTTP $responseCode: ${responseBody.take(180)}")
            }
            responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyRpcFailure(error: Throwable): String = when (error) {
        is java.net.UnknownHostException -> "Radiant Circle could not reach Solana mainnet RPC. Check phone internet/DNS, then refresh SKR Passport."
        is java.net.SocketTimeoutException -> "Solana mainnet RPC timed out. Refresh SKR Passport again in a moment."
        is IllegalArgumentException -> error.message ?: "Wallet address or on-chain SKR data was not valid."
        else -> error.message ?: error::class.java.simpleName
    }

    private data class LiquidBalanceSnapshot(
        val rawAmount: BigInteger,
        val uiAmount: BigDecimal,
        val decimals: Int,
        val tokenAccountCount: Int,
        val rpcSlot: Long?,
    )

    private data class SkrStakingSnapshot(
        val activeStakedUiAmount: BigDecimal,
        val unstakingUiAmount: BigDecimal,
        val verified: Boolean,
        val status: String,
        val unstakingReady: Boolean,
        val unstakeReadyAtClientMs: Long?,
        val accountCount: Int,
        val rpcSlot: Long?,
    ) {
        companion object {
            fun unverified(status: String) = SkrStakingSnapshot(
                activeStakedUiAmount = BigDecimal.ZERO,
                unstakingUiAmount = BigDecimal.ZERO,
                verified = false,
                status = status,
                unstakingReady = false,
                unstakeReadyAtClientMs = null,
                accountCount = 0,
                rpcSlot = null,
            )
        }
    }

    private companion object {
        const val MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com"
        const val RPC_TIMEOUT_MS = 15_000
        const val TAG = "RadiantRushSKR"

    }
}

data class SkrBalanceSnapshot(
    val walletAddress: String,
    val mint: String,
    val network: String,
    val balanceRawAmount: String,
    val balanceUiAmount: String,
    val balanceDisplay: String,
    val decimals: Int,
    val tokenAccountCount: Int,
    val tierLabel: String,
    val xpMultiplierLabel: String,
    val xpMultiplierValue: Double,
    val hasSkr: Boolean,
    val passportVersion: Int,
    val dailyCasualTicketBonus: Int,
    val chestBonusXp: Int,
    val chestBonusTickets: Int,
    val frameLabel: String,
    val auraLabel: String,
    val holderCollectibleLabel: String,
    val eligibleSkrUiAmount: String,
    val eligibleSkrDisplay: String,
    val stakedSkrUiAmount: String,
    val stakedSkrDisplay: String,
    val unstakingSkrUiAmount: String,
    val unstakingSkrDisplay: String,
    val stakedSkrStatus: String,
    val stakedSkrVerified: Boolean,
    val stakeBoostActive: Boolean,
    val stakeBoostLabel: String,
    val unstakingReady: Boolean,
    val unstakeReadyAtClientMs: Long?,
    val stakingProgramId: String,
    val stakingAccountCount: Int,
    val stakingRpcSlot: Long?,
    val rpcSlot: Long?,
    val checkedAtClientMs: Long,
)

sealed interface SkrBalanceResult {
    data class Success(val snapshot: SkrBalanceSnapshot) : SkrBalanceResult
    data class Failure(val message: String) : SkrBalanceResult
}
