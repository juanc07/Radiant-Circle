package com.thinkblox.radiantrush.solana

import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import com.funkatronics.encoders.Base58
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionParams
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Solana Mobile Wallet Adapter boundary for Radiant Circle.
 *
 * The repository owns wallet authorization, message signing, and Phase 4 devnet
 * memo proof submission. It never asks for or stores seed phrases/private keys.
 */
class MobileWalletRepository(
    private val activity: ComponentActivity,
) {
    private val sender: ActivityResultSender = ActivityResultSender(activity)

    /**
     * One adapter owns the active authorization token for connect/disconnect
     * and memo submission. Memo must reuse this active authorization so Phantom
     * can go straight to transaction approval instead of showing only another
     * connection screen.
     */
    private val connectionWalletAdapter = createWalletAdapter()

    private fun createWalletAdapter(): MobileWalletAdapter = MobileWalletAdapter(
        connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse("https://thinkbloxph.dev/radiant-rush"),
            iconUri = Uri.parse("favicon.ico"),
            identityName = "Radiant Circle",
        ),
    ).apply {
        // Phase 4 is a devnet-only proof flow. Without this, MWA can default to
        // mainnet while our memo transaction uses a devnet blockhash.
        blockchain = Solana.Devnet
    }

    suspend fun connectWallet(): WalletConnectResult = runCatching {
        Log.i(TAG, "Opening wallet connection on $MWA_CHAIN_LABEL")
        when (val result = connectionWalletAdapter.connect(sender)) {
            is TransactionResult.Success -> {
                val account = result.authResult.accounts.firstOrNull()
                if (account == null) {
                    WalletConnectResult.Failure("Wallet approved, but no account was returned.")
                } else {
                    val publicKey = Base58.encodeToString(account.publicKey)
                    Log.i(TAG, "Wallet connected on $MWA_CHAIN_LABEL. accountReturned=true addressPrefix=${publicKey.take(6)}")
                    WalletConnectResult.Connected(
                        publicKey = publicKey,
                        accountLabel = account.accountLabel,
                    )
                }
            }
            is TransactionResult.NoWalletFound -> WalletConnectResult.NoWalletFound
            is TransactionResult.Failure -> {
                Log.e(TAG, "Wallet connect failed on $MWA_CHAIN_LABEL", result.e)
                WalletConnectResult.Failure(
                    result.e.message ?: "Mobile Wallet Adapter connection failed.",
                )
            }
        }
    }.getOrElse { error ->
        Log.e(TAG, "Wallet connect crashed before MWA returned a result", error)
        WalletConnectResult.Failure(error.message ?: error::class.java.simpleName)
    }

    suspend fun disconnectWallet(): WalletDisconnectResult = runCatching {
        Log.i(TAG, "Disconnecting wallet on $MWA_CHAIN_LABEL")
        when (val result = connectionWalletAdapter.disconnect(sender)) {
            is TransactionResult.Success -> WalletDisconnectResult.Disconnected
            is TransactionResult.NoWalletFound -> WalletDisconnectResult.NoWalletFound
            is TransactionResult.Failure -> {
                Log.e(TAG, "Wallet disconnect failed on $MWA_CHAIN_LABEL", result.e)
                WalletDisconnectResult.Failure(
                    result.e.message ?: "Mobile Wallet Adapter disconnect failed.",
                )
            }
        }
    }.getOrElse { error ->
        Log.e(TAG, "Wallet disconnect crashed before MWA returned a result", error)
        WalletDisconnectResult.Failure(error.message ?: error::class.java.simpleName)
    }

    suspend fun signDailyProof(todayKey: String): WalletSignedProofResult = runCatching {
        Log.i(TAG, "Starting daily proof signing on $MWA_CHAIN_LABEL for date=$todayKey")
        var requestedWalletAddress = ""
        var requestedMessage = ""
        val proofWalletAdapter = createWalletAdapter()
        Log.i(TAG, "Daily proof using fresh MWA authorization session on $MWA_CHAIN_LABEL")
        val result = proofWalletAdapter.transact(sender) { authResult ->
            val account = authResult.accounts.first()
            requestedWalletAddress = Base58.encodeToString(account.publicKey)
            requestedMessage = buildDailyProofMessage(todayKey, requestedWalletAddress)
            signMessagesDetached(
                arrayOf(requestedMessage.encodeToByteArray()),
                arrayOf(account.publicKey),
            )
        }

        when (result) {
            is TransactionResult.Success -> {
                val signatureBytes = result.successPayload?.messages?.firstOrNull()?.signatures?.firstOrNull()
                Log.i(TAG, "Daily proof MWA success. signatureReturned=${signatureBytes != null}")
                if (signatureBytes == null) {
                    WalletSignedProofResult.Failure("Wallet approved the request, but did not return a message signature. Try again after updating the wallet app.")
                } else {
                    WalletSignedProofResult.Signed(
                        walletAddress = requestedWalletAddress,
                        message = requestedMessage,
                        signature = Base58.encodeToString(signatureBytes),
                    )
                }
            }
            is TransactionResult.NoWalletFound -> WalletSignedProofResult.NoWalletFound
            is TransactionResult.Failure -> {
                Log.e(TAG, "Daily proof signing failed on $MWA_CHAIN_LABEL", result.e)
                WalletSignedProofResult.Failure(
                    result.e.message ?: "Daily proof signing failed.",
                )
            }
        }
    }.getOrElse { error ->
        Log.e(TAG, "Daily proof signing crashed before MWA returned a result", error)
        WalletSignedProofResult.Failure(error.message ?: error::class.java.simpleName)
    }

    suspend fun sendDailyMemoProof(todayKey: String): WalletMemoProofResult {
        val firstResult = sendDailyMemoProofOnce(
            todayKey = todayKey,
            attemptLabel = "primary",
            clearAuthBeforeAttempt = false,
        )

        if (firstResult is WalletMemoProofResult.Failure && isWalletAuthorizationError(firstResult.message)) {
            Log.w(
                TAG,
                "Memo proof authorization failed on first attempt. Refreshing wallet authorization and retrying once.",
            )
            connectionWalletAdapter.authToken = null
            delay(AUTH_RETRY_DELAY_MS)

            val retryResult = sendDailyMemoProofOnce(
                todayKey = todayKey,
                attemptLabel = "auth-refresh-retry",
                clearAuthBeforeAttempt = true,
            )

            if (retryResult is WalletMemoProofResult.Failure && isWalletAuthorizationError(retryResult.message)) {
                return WalletMemoProofResult.Failure(
                    "Phantom rejected wallet authorization twice. Force close Phantom, reopen Devnet wallet, tap Connect Wallet, then tap Send Memo once.",
                )
            }

            return retryResult
        }

        return firstResult
    }

    private suspend fun sendDailyMemoProofOnce(
        todayKey: String,
        attemptLabel: String,
        clearAuthBeforeAttempt: Boolean,
    ): WalletMemoProofResult = runCatching {
        if (clearAuthBeforeAttempt) {
            connectionWalletAdapter.authToken = null
            Log.i(TAG, "Cleared cached wallet authorization before memo attempt=$attemptLabel")
        }

        Log.i(TAG, "Starting devnet memo proof on $MWA_CHAIN_LABEL for date=$todayKey attempt=$attemptLabel")

        // Fetch the blockhash before opening Phantom. If devnet RPC/DNS is down,
        // fail in Radiant Circle instead of bouncing the user into Phantom and then
        // silently returning with no transaction prompt.
        val chainContext = fetchLatestDevnetChainContext()
        Log.i(
            TAG,
            "Devnet chain context ready. minContextSlot=${chainContext.minContextSlot} blockhashPrefix=${chainContext.blockhash.take(8)} attempt=$attemptLabel",
        )

        var requestedWalletAddress = ""
        var requestedMemoText = ""
        Log.i(TAG, "Memo proof using active MWA session on $MWA_CHAIN_LABEL attempt=$attemptLabel")
        val result = connectionWalletAdapter.transact(sender) { authResult ->
            val accountPublicKey = authResult.accounts.first().publicKey
            val account = SolanaPublicKey(accountPublicKey)
            requestedWalletAddress = Base58.encodeToString(accountPublicKey)
            requestedMemoText = buildDailyMemoText(todayKey, requestedWalletAddress)
            val memoTx = buildMemoTransaction(account, requestedMemoText, chainContext.blockhash)
            signAndSendTransactions(
                transactions = arrayOf(memoTx.serialize()),
                params = TransactionParams(
                    minContextSlot = chainContext.minContextSlot,
                    commitment = RPC_COMMITMENT,
                    skipPreflight = false,
                    maxRetries = SIGN_AND_SEND_MAX_RETRIES,
                    waitForCommitmentToSendNextTransaction = true,
                ),
            )
        }

        when (result) {
            is TransactionResult.Success -> {
                val signatureBytes = result.successPayload?.signatures?.firstOrNull()
                Log.i(TAG, "Memo proof MWA success. signatureReturned=${signatureBytes != null} attempt=$attemptLabel")
                if (signatureBytes == null) {
                    WalletMemoProofResult.Failure("Wallet returned from the memo request but no transaction signature was provided. Reopen Phantom, stay on Devnet, then try once more.")
                } else {
                    val signature = Base58.encodeToString(signatureBytes)
                    WalletMemoProofResult.Submitted(
                        walletAddress = requestedWalletAddress,
                        memoText = requestedMemoText,
                        transactionSignature = signature,
                        explorerUrl = "$DEVNET_EXPLORER_PREFIX$signature?cluster=devnet",
                    )
                }
            }
            is TransactionResult.NoWalletFound -> WalletMemoProofResult.NoWalletFound
            is TransactionResult.Failure -> {
                val rawMessage = result.e.message ?: result.message ?: "Memo transaction failed."
                if (isWalletAuthorizationError(rawMessage)) {
                    Log.w(TAG, "Memo transaction authorization failed on $MWA_CHAIN_LABEL attempt=$attemptLabel", result.e)
                } else {
                    Log.e(TAG, "Memo transaction failed on $MWA_CHAIN_LABEL attempt=$attemptLabel", result.e)
                }
                val userMessage = when {
                    isWalletAuthorizationError(rawMessage) ->
                        "Wallet authorization expired: $rawMessage"
                    rawMessage.contains("not submitted", ignoreCase = true) ->
                        "Phantom did not submit the memo transaction. Confirm Devnet has SOL for gas, then try once more."
                    else -> rawMessage
                }
                WalletMemoProofResult.Failure(userMessage)
            }
        }
    }.getOrElse { error ->
        Log.e(TAG, "Memo proof crashed before MWA returned a result attempt=$attemptLabel", error)
        val message = when (error) {
            is java.net.UnknownHostException -> "Radiant Circle could not reach devnet RPC. Check phone internet/DNS, then try Send Memo again."
            is java.net.SocketTimeoutException -> "Devnet RPC timed out before Phantom opened. Try again in a moment."
            else -> error.message ?: error::class.java.simpleName
        }
        WalletMemoProofResult.Failure(message)
    }

    private fun isWalletAuthorizationError(message: String): Boolean =
        message.contains("authorization request failed", ignoreCase = true) ||
            message.contains("authorization expired", ignoreCase = true) ||
            message.contains("auth token", ignoreCase = true) ||
            message.contains("reauthorize", ignoreCase = true) ||
            message.contains("rejected wallet authorization", ignoreCase = true)

    private fun buildMemoTransaction(
        account: SolanaPublicKey,
        memoText: String,
        blockhash: String,
    ): Transaction {
        val memoInstruction = TransactionInstruction(
            SolanaPublicKey.from(MEMO_PROGRAM_ID),
            listOf(AccountMeta(account, true, true)),
            memoText.encodeToByteArray(),
        )

        val message = Message.Builder()
            .addInstruction(memoInstruction)
            .setRecentBlockhash(blockhash)
            .build()

        return Transaction(message)
    }

    private suspend fun fetchLatestDevnetChainContext(): DevnetChainContext = withContext(Dispatchers.IO) {
        val requestBody =
            """{"jsonrpc":"2.0","id":1,"method":"getLatestBlockhash","params":[{"commitment":"confirmed"}]}"""

        val responseBody = postDevnetRpc(requestBody)
        val blockhash = BLOCKHASH_REGEX.find(responseBody)
            ?.groupValues
            ?.getOrNull(1)
            ?: throw IllegalStateException("Could not parse devnet blockhash from RPC response.")

        val contextSlot = SLOT_REGEX.find(responseBody)
            ?.groupValues
            ?.getOrNull(1)
            ?.toLongOrNull()
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()

        DevnetChainContext(
            blockhash = blockhash,
            minContextSlot = contextSlot,
        )
    }

    private fun postDevnetRpc(requestBody: String): String {
        val connection = (URL(DEVNET_RPC_URL).openConnection() as HttpURLConnection).apply {
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
                throw IllegalStateException("Devnet RPC HTTP $responseCode: ${responseBody.take(180)}")
            }

            responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun buildDailyProofMessage(todayKey: String, walletAddress: String): String = buildString {
        appendLine("Radiant Circle Daily Proof")
        appendLine("Wallet: $walletAddress")
        appendLine("Date: $todayKey")
        appendLine("Quest: sign-daily-proof")
        append("Network: off-chain MWA signature")
    }

    private fun buildDailyMemoText(todayKey: String, walletAddress: String): String =
        "radiant-rush:daily-memo-proof:$todayKey:$walletAddress"

    private data class DevnetChainContext(
        val blockhash: String,
        val minContextSlot: Int?,
    )

    private companion object {
        const val DEVNET_RPC_URL = "https://api.devnet.solana.com"
        const val DEVNET_EXPLORER_PREFIX = "https://explorer.solana.com/tx/"
        const val MWA_CHAIN_LABEL = "solana:devnet"
        const val MEMO_PROGRAM_ID = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
        const val RPC_TIMEOUT_MS = 15_000
        const val RPC_COMMITMENT = "confirmed"
        const val SIGN_AND_SEND_MAX_RETRIES = 3
        const val AUTH_RETRY_DELAY_MS = 750L
        const val TAG = "RadiantRushWallet"
        val BLOCKHASH_REGEX = "\"blockhash\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val SLOT_REGEX = "\"slot\"\\s*:\\s*(\\d+)".toRegex()
    }
}

sealed interface WalletConnectResult {
    data class Connected(
        val publicKey: String,
        val accountLabel: String?,
    ) : WalletConnectResult

    data object NoWalletFound : WalletConnectResult

    data class Failure(
        val message: String,
    ) : WalletConnectResult
}

sealed interface WalletDisconnectResult {
    data object Disconnected : WalletDisconnectResult
    data object NoWalletFound : WalletDisconnectResult
    data class Failure(val message: String) : WalletDisconnectResult
}

sealed interface WalletSignedProofResult {
    data class Signed(
        val walletAddress: String,
        val message: String,
        val signature: String,
    ) : WalletSignedProofResult

    data object NoWalletFound : WalletSignedProofResult
    data class Failure(val message: String) : WalletSignedProofResult
}

sealed interface WalletMemoProofResult {
    data class Submitted(
        val walletAddress: String,
        val memoText: String,
        val transactionSignature: String,
        val explorerUrl: String,
    ) : WalletMemoProofResult

    data object NoWalletFound : WalletMemoProofResult
    data class Failure(val message: String) : WalletMemoProofResult
}
