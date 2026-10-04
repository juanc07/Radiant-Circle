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
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import com.thinkblox.radiantrush.data.OreStakeAction
import com.thinkblox.radiantrush.logic.OreStakeInstructionRules
import com.thinkblox.radiantrush.logic.OreStakingRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.math.BigInteger
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

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

    private fun createMainnetWalletAdapter(): MobileWalletAdapter = MobileWalletAdapter(
        connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse("https://thinkbloxph.dev/radiant-rush"),
            iconUri = Uri.parse("favicon.ico"),
            identityName = "Radiant Circle",
        ),
    ).apply {
        // ORE staking moves real assets. Keep this authorization completely separate
        // from the devnet Daily Proof adapter and explicitly bind it to Mainnet.
        blockchain = Solana.Mainnet
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

    suspend fun sendOreStakeAction(
        expectedWalletAddress: String,
        action: OreStakeAction,
        amountRaw: String,
        compoundFeeLamportsRaw: String = "0",
    ): OreStakeTransactionResult = runCatching {
        val expectedAddress = expectedWalletAddress.trim()
        require(expectedAddress.isNotBlank()) { "Connect the same wallet shown in ORE before continuing." }
        val amount = amountRaw.toBigIntegerOrNull()
            ?: throw IllegalArgumentException("ORE amount could not be decoded.")
        require(amount > BigInteger.ZERO && amount <= U64_MAX) { "ORE amount is outside the protocol u64 range." }
        val compoundFeeLamports = compoundFeeLamportsRaw.toBigIntegerOrNull()
            ?: throw IllegalArgumentException("ORE compound fee state could not be decoded.")
        require(compoundFeeLamports >= BigInteger.ZERO && compoundFeeLamports <= U64_MAX) { "ORE compound fee state is outside the protocol u64 range." }

        val expectedWallet = SolanaPublicKey.from(expectedAddress)
        val addresses = deriveOreStakeAddresses(expectedWallet)
        if (action == OreStakeAction.Stake) {
            val senderAtaBalance = fetchMainnetTokenAccountBalance(addresses.walletTokenAccount)
            require(senderAtaBalance >= amount) {
                "Your ORE associated token account has less than the requested stake amount. Refresh ORE and use a smaller amount."
            }
        }
        val chainContext = fetchLatestMainnetChainContext()
        val mainnetAdapter = createMainnetWalletAdapter()
        var authorizedWallet = ""

        Log.i(TAG, "Opening Mainnet ORE ${action.name} approval. walletPrefix=${expectedAddress.take(6)}")
        val result = mainnetAdapter.transact(sender) { authResult ->
            val accountPublicKey = authResult.accounts.firstOrNull()?.publicKey
                ?: throw IllegalStateException("Wallet approved, but returned no account.")
            authorizedWallet = Base58.encodeToString(accountPublicKey)
            require(authorizedWallet == expectedAddress) {
                "The wallet selected in Phantom does not match the wallet shown in Radiant Circle ORE. No transaction was submitted."
            }

            val transaction = buildOreStakeTransaction(
                signer = SolanaPublicKey(accountPublicKey),
                action = action,
                amount = amount,
                compoundFeeLamports = compoundFeeLamports,
                blockhash = chainContext.blockhash,
                addresses = addresses,
            )
            signAndSendTransactions(
                transactions = arrayOf(transaction.serialize()),
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
                if (signatureBytes == null) {
                    OreStakeTransactionResult.Failure("Wallet returned without a Mainnet transaction signature. No ORE action was verified.")
                } else {
                    val signature = Base58.encodeToString(signatureBytes)
                    val confirmed = waitForMainnetConfirmation(signature)
                    OreStakeTransactionResult.Submitted(
                        walletAddress = authorizedWallet,
                        action = action,
                        amountRaw = amount.toString(),
                        transactionSignature = signature,
                        explorerUrl = "$MAINNET_EXPLORER_PREFIX$signature",
                        rpcConfirmed = confirmed,
                    )
                }
            }
            is TransactionResult.NoWalletFound -> OreStakeTransactionResult.NoWalletFound
            is TransactionResult.Failure -> {
                val message = result.e.message ?: result.message ?: "Mainnet ORE transaction failed."
                Log.e(TAG, "Mainnet ORE ${action.name} failed", result.e)
                OreStakeTransactionResult.Failure(mainnetWalletMessage(message))
            }
        }
    }.getOrElse { error ->
        Log.e(TAG, "Mainnet ORE ${action.name} crashed before verification", error)
        OreStakeTransactionResult.Failure(
            when (error) {
                is java.net.UnknownHostException -> "Radiant Circle could not reach Solana Mainnet RPC. Check internet/DNS before retrying."
                is java.net.SocketTimeoutException -> "Solana Mainnet RPC timed out. Refresh ORE before retrying the action."
                else -> mainnetWalletMessage(error.message ?: error::class.java.simpleName)
            },
        )
    }

    private fun mainnetWalletMessage(message: String): String {
        val lower = message.lowercase()
        return if (
            lower.contains("devnet") ||
            lower.contains("testnet") ||
            lower.contains("chain not supported") ||
            lower.contains("unsupported chain") ||
            lower.contains("wrong network") ||
            lower.contains("network mismatch")
        ) {
            "Solana Mainnet is required for ORE staking. Switch your wallet to Mainnet, return to Radiant Circle, and retry. No ORE action was verified."
        } else {
            message
        }
    }

    private suspend fun deriveOreStakeAddresses(wallet: SolanaPublicKey): OreStakeAddresses {
        val stakeProgram = SolanaPublicKey.from(OreStakingRules.ORE_STAKE_PROGRAM_ID)
        val mint = SolanaPublicKey.from(OreStakingRules.ORE_MINT)
        val tokenProgram = SolanaPublicKey.from(TOKEN_PROGRAM_ID)
        val associatedTokenProgram = SolanaPublicKey.from(ASSOCIATED_TOKEN_PROGRAM_ID)
        val stake = derivePdaPublicKey(listOf(OreStakingRules.STAKE_SEED, wallet.bytes), stakeProgram)
        val treasury = derivePdaPublicKey(listOf(OreStakingRules.TREASURY_SEED), stakeProgram)
        val vesting = derivePdaPublicKey(listOf(OreStakingRules.VESTING_SEED), stakeProgram)
        return OreStakeAddresses(
            program = stakeProgram,
            mint = mint,
            walletTokenAccount = derivePdaPublicKey(listOf(wallet.bytes, tokenProgram.bytes, mint.bytes), associatedTokenProgram),
            stake = stake,
            stakeTokenAccount = derivePdaPublicKey(listOf(stake.bytes, tokenProgram.bytes, mint.bytes), associatedTokenProgram),
            treasury = treasury,
            treasuryTokenAccount = derivePdaPublicKey(listOf(treasury.bytes, tokenProgram.bytes, mint.bytes), associatedTokenProgram),
            vesting = vesting,
            systemProgram = SolanaPublicKey.from(SYSTEM_PROGRAM_ID),
            tokenProgram = tokenProgram,
            associatedTokenProgram = associatedTokenProgram,
        )
    }

    private suspend fun derivePdaPublicKey(seeds: List<ByteArray>, programId: SolanaPublicKey): SolanaPublicKey {
        val address = ProgramDerivedAddress.find(seeds, programId)
            .getOrElse { throw IllegalStateException("Could not derive the verified ORE protocol address.", it) }
        return SolanaPublicKey.from(address.base58())
    }

    private fun buildOreStakeTransaction(
        signer: SolanaPublicKey,
        action: OreStakeAction,
        amount: BigInteger,
        compoundFeeLamports: BigInteger,
        blockhash: String,
        addresses: OreStakeAddresses,
    ): Transaction {
        fun meta(key: SolanaPublicKey, signerFlag: Boolean = false, writable: Boolean = false) =
            AccountMeta(key, signerFlag, writable)

        val instruction = when (action) {
            OreStakeAction.Stake -> TransactionInstruction(
                addresses.program,
                listOf(
                    meta(signer, signerFlag = true, writable = true),
                    meta(signer, signerFlag = true, writable = true),
                    meta(addresses.mint),
                    meta(addresses.walletTokenAccount, writable = true),
                    meta(addresses.stake, writable = true),
                    meta(addresses.stakeTokenAccount, writable = true),
                    meta(addresses.treasury, writable = true),
                    meta(addresses.vesting, writable = true),
                    meta(addresses.systemProgram),
                    meta(addresses.tokenProgram),
                    meta(addresses.associatedTokenProgram),
                    meta(addresses.program),
                ),
                OreStakeInstructionRules.depositData(
                    amount = amount,
                    compoundFeeLamports = compoundFeeLamports,
                    compoundFeeDepositLamports = BigInteger.ZERO,
                ),
            )

            OreStakeAction.Withdraw -> TransactionInstruction(
                addresses.program,
                listOf(
                    meta(signer, signerFlag = true, writable = true),
                    meta(addresses.mint),
                    meta(addresses.walletTokenAccount, writable = true),
                    meta(addresses.stake, writable = true),
                    meta(addresses.stakeTokenAccount, writable = true),
                    meta(addresses.treasury, writable = true),
                    meta(addresses.vesting, writable = true),
                    meta(addresses.systemProgram),
                    meta(addresses.tokenProgram),
                    meta(addresses.associatedTokenProgram),
                    meta(addresses.program),
                ),
                OreStakeInstructionRules.withdrawData(amount),
            )

            OreStakeAction.Claim -> TransactionInstruction(
                addresses.program,
                listOf(
                    meta(signer, signerFlag = true, writable = true),
                    meta(addresses.mint),
                    meta(addresses.walletTokenAccount, writable = true),
                    meta(addresses.stake, writable = true),
                    meta(addresses.treasury, writable = true),
                    meta(addresses.treasuryTokenAccount, writable = true),
                    meta(addresses.vesting, writable = true),
                    meta(addresses.systemProgram),
                    meta(addresses.tokenProgram),
                    meta(addresses.associatedTokenProgram),
                    meta(addresses.program),
                ),
                OreStakeInstructionRules.claimData(amount),
            )
        }

        val message = Message.Builder()
            .addInstruction(instruction)
            .setRecentBlockhash(blockhash)
            .build()
        return Transaction(message)
    }

    private suspend fun fetchMainnetTokenAccountBalance(tokenAccount: SolanaPublicKey): BigInteger = withContext(Dispatchers.IO) {
        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 40)
            .put("method", "getTokenAccountBalance")
            .put(
                "params",
                JSONArray()
                    .put(Base58.encodeToString(tokenAccount.bytes))
                    .put(JSONObject().put("commitment", RPC_COMMITMENT)),
            )
        val root = postMainnetRpc(request)
        val rpcError = root.optJSONObject("error")
        if (rpcError != null) {
            val message = rpcError.optString("message", "")
            if (message.contains("could not find account", ignoreCase = true) ||
                message.contains("Invalid param", ignoreCase = true)
            ) {
                return@withContext BigInteger.ZERO
            }
            throw IllegalStateException("Mainnet token-balance RPC error: $message")
        }
        root.optJSONObject("result")
            ?.optJSONObject("value")
            ?.optString("amount")
            ?.toBigIntegerOrNull()
            ?: BigInteger.ZERO
    }

    private suspend fun fetchLatestMainnetChainContext(): MainnetChainContext = withContext(Dispatchers.IO) {
        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 41)
            .put("method", "getLatestBlockhash")
            .put("params", JSONArray().put(JSONObject().put("commitment", RPC_COMMITMENT)))
        val root = postMainnetRpc(request)
        val error = root.optJSONObject("error")
        if (error != null) throw IllegalStateException("Mainnet blockhash RPC error: ${error.optString("message")}")
        val result = root.getJSONObject("result")
        val blockhash = result.getJSONObject("value").getString("blockhash")
        val slot = result.getJSONObject("context").optLong("slot", -1L)
            .takeIf { it >= 0L }
            ?.coerceAtMost(Int.MAX_VALUE.toLong())
            ?.toInt()
        MainnetChainContext(blockhash, slot)
    }

    private suspend fun waitForMainnetConfirmation(signature: String): Boolean = withContext(Dispatchers.IO) {
        repeat(MAINNET_CONFIRMATION_ATTEMPTS) { attempt ->
            val request = JSONObject()
                .put("jsonrpc", "2.0")
                .put("id", 42 + attempt)
                .put("method", "getSignatureStatuses")
                .put(
                    "params",
                    JSONArray()
                        .put(JSONArray().put(signature))
                        .put(JSONObject().put("searchTransactionHistory", true)),
                )
            val root = postMainnetRpc(request)
            root.optJSONObject("error")?.let { rpcError ->
                throw IllegalStateException("Mainnet confirmation RPC error: ${rpcError.optString("message")}")
            }
            val status = root.optJSONObject("result")
                ?.optJSONArray("value")
                ?.optJSONObject(0)
            if (status != null) {
                if (!status.isNull("err")) {
                    throw IllegalStateException("Solana rejected the ORE transaction: ${status.opt("err")}")
                }
                val confirmationStatus = status.optString("confirmationStatus")
                if (confirmationStatus == "confirmed" || confirmationStatus == "finalized") return@withContext true
            }
            delay(MAINNET_CONFIRMATION_DELAY_MS)
        }
        false
    }

    private fun postMainnetRpc(request: JSONObject): JSONObject {
        val connection = (URL(MAINNET_RPC_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = RPC_TIMEOUT_MS
            readTimeout = RPC_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(request.toString().encodeToByteArray()) }
            val responseCode = connection.responseCode
            val body = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (responseCode !in 200..299) {
                throw IllegalStateException("Mainnet RPC HTTP $responseCode: ${body.take(180)}")
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private data class OreStakeAddresses(
        val program: SolanaPublicKey,
        val mint: SolanaPublicKey,
        val walletTokenAccount: SolanaPublicKey,
        val stake: SolanaPublicKey,
        val stakeTokenAccount: SolanaPublicKey,
        val treasury: SolanaPublicKey,
        val treasuryTokenAccount: SolanaPublicKey,
        val vesting: SolanaPublicKey,
        val systemProgram: SolanaPublicKey,
        val tokenProgram: SolanaPublicKey,
        val associatedTokenProgram: SolanaPublicKey,
    )

    private data class MainnetChainContext(
        val blockhash: String,
        val minContextSlot: Int?,
    )

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
        const val MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com"
        const val DEVNET_EXPLORER_PREFIX = "https://explorer.solana.com/tx/"
        const val MAINNET_EXPLORER_PREFIX = "https://explorer.solana.com/tx/"
        const val MWA_CHAIN_LABEL = "solana:devnet"
        const val MEMO_PROGRAM_ID = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"
        const val RPC_TIMEOUT_MS = 15_000
        const val RPC_COMMITMENT = "confirmed"
        const val SIGN_AND_SEND_MAX_RETRIES = 3
        const val AUTH_RETRY_DELAY_MS = 750L
        const val MAINNET_CONFIRMATION_ATTEMPTS = 12
        const val MAINNET_CONFIRMATION_DELAY_MS = 1_000L
        const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
        const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        const val ASSOCIATED_TOKEN_PROGRAM_ID = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
        val U64_MAX: BigInteger = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)
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


sealed interface OreStakeTransactionResult {
    data class Submitted(
        val walletAddress: String,
        val action: OreStakeAction,
        val amountRaw: String,
        val transactionSignature: String,
        val explorerUrl: String,
        val rpcConfirmed: Boolean,
    ) : OreStakeTransactionResult

    data object NoWalletFound : OreStakeTransactionResult
    data class Failure(val message: String) : OreStakeTransactionResult
}
