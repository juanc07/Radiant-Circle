package com.thinkblox.radiantrush.solana

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only SKR balance scanner for Phase 5.
 *
 * SKR is a mainnet SPL token. Radiant Rush keeps Phase 4 proof transactions on
 * devnet, then reads SKR from mainnet by public wallet address only. This class
 * never requests signing, token transfers, private keys, or seed phrases.
 */
class SkrBalanceRepository {
    suspend fun fetchSkrBalance(walletAddress: String): SkrBalanceResult = withContext(Dispatchers.IO) {
        runCatching {
            val cleanWallet = walletAddress.trim()
            require(cleanWallet.isNotBlank()) { "Connect a wallet before checking SKR." }

            Log.i(TAG, "Checking mainnet SKR balance for walletPrefix=${cleanWallet.take(6)}")
            val requestBody = buildGetTokenAccountsRequest(cleanWallet)
            val responseBody = postMainnetRpc(requestBody)
            val snapshot = parseSkrBalanceResponse(cleanWallet, responseBody)
            Log.i(
                TAG,
                "SKR balance check complete. accountCount=${snapshot.tokenAccountCount} balance=${snapshot.balanceUiAmount} tier=${snapshot.tierLabel}",
            )
            SkrBalanceResult.Success(snapshot)
        }.getOrElse { error ->
            Log.e(TAG, "SKR balance check failed", error)
            val message = when (error) {
                is java.net.UnknownHostException -> "Radiant Rush could not reach Solana mainnet RPC. Check phone internet/DNS, then try Check SKR again."
                is java.net.SocketTimeoutException -> "Solana mainnet RPC timed out. Try Check SKR again in a moment."
                is IllegalArgumentException -> error.message ?: "Wallet address was not valid for SKR check."
                else -> error.message ?: error::class.java.simpleName
            }
            SkrBalanceResult.Failure(message)
        }
    }

    private fun buildGetTokenAccountsRequest(walletAddress: String): String =
        """{"jsonrpc":"2.0","id":1,"method":"getTokenAccountsByOwner","params":["$walletAddress",{"mint":"$SKR_MINT"},{"encoding":"jsonParsed","commitment":"confirmed"}]}"""

    private fun parseSkrBalanceResponse(
        walletAddress: String,
        responseBody: String,
    ): SkrBalanceSnapshot {
        val root = JSONObject(responseBody)
        val rpcError = root.optJSONObject("error")
        if (rpcError != null) {
            throw IllegalStateException("Mainnet SKR RPC error: ${rpcError.optString("message", "Unknown RPC error")}")
        }

        val result = root.optJSONObject("result")
            ?: throw IllegalStateException("Mainnet SKR RPC response did not include a result.")
        val slot = result.optJSONObject("context")
            ?.optLong("slot", -1L)
            ?.takeIf { it >= 0L }
        val accounts = result.optJSONArray("value")

        var rawAmount = BigInteger.ZERO
        var uiAmount = BigDecimal.ZERO
        var decimals = DEFAULT_SKR_DECIMALS
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
                    decimals = tokenAmount.optInt("decimals", DEFAULT_SKR_DECIMALS)
                }
                tokenAccountCount += 1
            }
        }

        val tier = tierFor(uiAmount)

        return SkrBalanceSnapshot(
            walletAddress = walletAddress,
            mint = SKR_MINT,
            network = MAINNET_NETWORK_LABEL,
            balanceRawAmount = rawAmount.toString(),
            balanceUiAmount = formatDecimal(uiAmount),
            balanceDisplay = "${formatDecimal(uiAmount)} SKR",
            decimals = decimals,
            tokenAccountCount = tokenAccountCount,
            tierLabel = tier.label,
            xpMultiplierLabel = tier.multiplierLabel,
            xpMultiplierValue = tier.multiplierValue,
            hasSkr = uiAmount > BigDecimal.ZERO,
            rpcSlot = slot,
            checkedAtClientMs = System.currentTimeMillis(),
        )
    }

    private fun tierFor(balance: BigDecimal): SkrTier = when {
        balance >= BigDecimal("10000") -> SkrTier("Radiant Legend", "1.35x", 1.35)
        balance >= BigDecimal("1000") -> SkrTier("Radiant Elite", "1.20x", 1.20)
        balance >= BigDecimal("100") -> SkrTier("Radiant Holder", "1.10x", 1.10)
        balance > BigDecimal.ZERO -> SkrTier("Radiant Scout", "1.05x", 1.05)
        else -> SkrTier("Explorer", "1.00x", 1.00)
    }

    private fun formatDecimal(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val normalized = value.stripTrailingZeros()
        return if (normalized.scale() <= 4) {
            normalized.toPlainString()
        } else {
            normalized.setScale(4, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        }
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

    private data class SkrTier(
        val label: String,
        val multiplierLabel: String,
        val multiplierValue: Double,
    )

    private companion object {
        const val MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com"
        const val MAINNET_NETWORK_LABEL = "mainnet-beta"
        const val SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
        const val DEFAULT_SKR_DECIMALS = 6
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
    val rpcSlot: Long?,
    val checkedAtClientMs: Long,
)

sealed interface SkrBalanceResult {
    data class Success(val snapshot: SkrBalanceSnapshot) : SkrBalanceResult
    data class Failure(val message: String) : SkrBalanceResult
}
