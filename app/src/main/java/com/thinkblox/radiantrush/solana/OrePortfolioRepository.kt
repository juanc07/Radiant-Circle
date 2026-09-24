package com.thinkblox.radiantrush.solana

import android.util.Log
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.thinkblox.radiantrush.data.OrePortfolioSnapshot
import com.thinkblox.radiantrush.data.OreProtocolPositionProof
import com.thinkblox.radiantrush.logic.OreStakeProtocol
import com.thinkblox.radiantrush.logic.OreStakeRead
import com.thinkblox.radiantrush.logic.OreStakingRules
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 14D.1R only: read-only ORE portfolio scanner on Solana mainnet-beta.
 *
 * Production ore.com reads both the current and legacy ORE staking generations. We do the
 * same deterministically from the connected wallet's direct PDAs. There is no transaction
 * history guessing, manual staking address, custody, or token action in this repository.
 */
class OrePortfolioRepository {
    suspend fun fetchPortfolio(walletAddress: String): OrePortfolioResult = withContext(Dispatchers.IO) {
        runCatching {
            val cleanWallet = walletAddress.trim()
            require(cleanWallet.isNotBlank()) { "Connect a wallet before checking ORE." }
            val connectedWallet = SolanaPublicKey.from(cleanWallet)

            Log.i(TAG, "Reading ORE portfolio walletPrefix=${cleanWallet.take(6)} across supported stake generations")

            val liquid = fetchLiquidOre(cleanWallet)
            val specs = OreStakingRules.SUPPORTED_PROTOCOLS.map { protocol ->
                deriveProtocolSpec(protocol, connectedWallet)
            }

            // Match the production read pattern: get both stake generations and both sets of
            // Treasury/Vesting state in one consistent confirmed-slot account batch, plus Clock.
            val accountAddresses = buildList {
                specs.forEach { spec ->
                    add(spec.stakeAddress)
                    add(spec.treasuryAddress)
                    add(spec.vestingAddress)
                }
                add(CLOCK_SYSVAR_ADDRESS)
            }
            val batch = fetchMultipleAccounts(accountAddresses)
            val clockAccount = batch.values.lastOrNull()
                ?: throw IllegalStateException("Solana Clock sysvar was not found.")
            val clockData = clockAccount.data
            require(clockData.size >= 40) { "Solana Clock sysvar data was too short." }
            val clockUnixTimestampSeconds = littleEndianLong(clockData, 32)

            val protocolReads = specs.mapIndexed { index, spec ->
                val base = index * 3
                readProtocolPosition(
                    spec = spec,
                    connectedWallet = connectedWallet,
                    stakeAccount = batch.values.getOrNull(base),
                    treasuryAccount = batch.values.getOrNull(base + 1),
                    vestingAccount = batch.values.getOrNull(base + 2),
                    clockUnixTimestampSeconds = clockUnixTimestampSeconds,
                )
            }

            val positions = protocolReads.mapNotNull { it.position }
            val primary = positions
                .filter { it.stake.balanceRaw > BigInteger.ZERO }
                .maxWithOrNull(compareBy<VerifiedPosition> { it.stake.balanceRaw }.thenBy { it.liveUnclaimedRaw })
                ?: positions.maxWithOrNull(compareBy<VerifiedPosition> { it.liveLifetimeRaw }.thenBy { it.stake.rewardsRaw })

            val stakedRaw = positions.fold(BigInteger.ZERO) { total, it -> total + it.stake.balanceRaw }
            val unclaimedRaw = positions.fold(BigInteger.ZERO) { total, it -> total + it.liveUnclaimedRaw }
            val lifetimeRaw = positions.fold(BigInteger.ZERO) { total, it -> total + it.liveLifetimeRaw }
            val storedRewardsRaw = positions.fold(BigInteger.ZERO) { total, it -> total + it.stake.rewardsRaw }
            val pendingAccrualRaw = positions.fold(BigInteger.ZERO) { total, it -> total + it.pendingAccrualRaw }

            val fallbackSpec = specs.first()
            val sourceLabel = when {
                positions.count { it.stake.balanceRaw > BigInteger.ZERO } > 1 -> "Connected wallet • multiple active ORE stake generations"
                positions.any { it.stake.balanceRaw > BigInteger.ZERO } -> "Connected wallet • active ORE stake verified"
                positions.isNotEmpty() -> "Connected wallet • historical ORE stake verified"
                else -> "Connected wallet • no ORE stake account found"
            }

            val proofs = protocolReads.map { read ->
                val position = read.position
                OreProtocolPositionProof(
                    label = read.spec.protocol.label,
                    programId = read.spec.protocol.programId,
                    stakeAddress = read.spec.stakeAddress,
                    treasuryAddress = read.spec.treasuryAddress,
                    vestingAddress = read.spec.vestingAddress,
                    accountFound = position != null,
                    active = position?.stake?.balanceRaw?.signum() == 1,
                    stakedDisplay = "${OreStakingRules.formatRawOre(position?.stake?.balanceRaw ?: BigInteger.ZERO)} ORE",
                    yieldDisplay = "${OreStakingRules.formatRawOre(position?.liveUnclaimedRaw ?: BigInteger.ZERO)} ORE",
                    lifetimeRewardsDisplay = "${OreStakingRules.formatRawOre(position?.liveLifetimeRaw ?: BigInteger.ZERO)} ORE",
                )
            }

            val snapshot = OrePortfolioSnapshot(
                walletAddress = cleanWallet,
                stakingAuthorityAddress = cleanWallet,
                usesExternalStakingAuthority = false,
                stakingAuthoritySourceLabel = sourceLabel,
                stakingAuthorityAutoDetected = false,
                signedOreTransactionsScanned = 0,
                autoDiscoveryAttempted = false,
                network = OreStakingRules.MAINNET_NETWORK_LABEL,
                mint = OreStakingRules.ORE_MINT,
                stakingProgramId = primary?.spec?.protocol?.programId ?: fallbackSpec.protocol.programId,
                stakeAddress = primary?.spec?.stakeAddress ?: fallbackSpec.stakeAddress,
                treasuryAddress = primary?.spec?.treasuryAddress ?: fallbackSpec.treasuryAddress,
                vestingAddress = primary?.spec?.vestingAddress ?: fallbackSpec.vestingAddress,
                protocolPositions = proofs,
                liquidRaw = liquid.rawAmount.toString(),
                liquidDisplay = "${OreStakingRules.formatRawOre(liquid.rawAmount)} ORE",
                stakingAuthorityLiquidRaw = liquid.rawAmount.toString(),
                stakingAuthorityLiquidDisplay = "${OreStakingRules.formatRawOre(liquid.rawAmount)} ORE",
                stakingAuthorityLiquidTokenAccountCount = liquid.tokenAccountCount,
                stakedRaw = stakedRaw.toString(),
                stakedDisplay = "${OreStakingRules.formatRawOre(stakedRaw)} ORE",
                unclaimedRewardsRaw = unclaimedRaw.toString(),
                unclaimedRewardsDisplay = "${OreStakingRules.formatRawOre(unclaimedRaw)} ORE",
                lifetimeRewardsRaw = lifetimeRaw.toString(),
                lifetimeRewardsDisplay = "${OreStakingRules.formatRawOre(lifetimeRaw)} ORE",
                storedRewardsRaw = storedRewardsRaw.toString(),
                storedRewardsDisplay = "${OreStakingRules.formatRawOre(storedRewardsRaw)} ORE",
                pendingAccrualRaw = pendingAccrualRaw.toString(),
                pendingAccrualDisplay = "${OreStakingRules.formatRawOre(pendingAccrualRaw)} ORE",
                lastClaimAtSeconds = primary?.stake?.lastClaimAtSeconds?.takeIf { it > 0L },
                lastDepositAtSeconds = primary?.stake?.lastDepositAtSeconds?.takeIf { it > 0L },
                lastWithdrawAtSeconds = primary?.stake?.lastWithdrawAtSeconds?.takeIf { it > 0L },
                stakeAccountDataSize = primary?.account?.data?.size,
                stakeOwnerVerified = positions.isNotEmpty(),
                stakeAuthorityVerified = positions.isNotEmpty(),
                hasStakeAccount = positions.isNotEmpty(),
                liquidTokenAccountCount = liquid.tokenAccountCount,
                rpcSlot = listOfNotNull(liquid.rpcSlot, batch.rpcSlot).maxOrNull(),
                checkedAtClientMs = System.currentTimeMillis(),
            )
            OrePortfolioResult.Success(snapshot)
        }.getOrElse { error ->
            Log.e(TAG, "ORE portfolio read failed", error)
            OrePortfolioResult.Failure(friendlyRpcFailure(error))
        }
    }

    private suspend fun deriveProtocolSpec(
        protocol: OreStakeProtocol,
        connectedWallet: SolanaPublicKey,
    ): ProtocolReadSpec {
        val programId = SolanaPublicKey.from(protocol.programId)
        return ProtocolReadSpec(
            protocol = protocol,
            programId = programId,
            stakeAddress = derivePda(listOf(OreStakingRules.STAKE_SEED, connectedWallet.bytes), programId),
            treasuryAddress = derivePda(listOf(OreStakingRules.TREASURY_SEED), programId),
            vestingAddress = derivePda(listOf(OreStakingRules.VESTING_SEED), programId),
        )
    }

    private fun readProtocolPosition(
        spec: ProtocolReadSpec,
        connectedWallet: SolanaPublicKey,
        stakeAccount: ProgramAccountRead?,
        treasuryAccount: ProgramAccountRead?,
        vestingAccount: ProgramAccountRead?,
        clockUnixTimestampSeconds: Long,
    ): ProtocolPositionRead {
        if (stakeAccount == null) return ProtocolPositionRead(spec, null)

        require(stakeAccount.owner == spec.protocol.programId) {
            "${spec.protocol.label} Stake account owner did not match its official program."
        }
        require(stakeAccount.data.size == OreStakingRules.STAKE_ACCOUNT_SIZE) {
            "${spec.protocol.label} Stake account layout changed."
        }
        val stake = OreStakingRules.parseStake(stakeAccount.data, connectedWallet.bytes)

        val treasury = treasuryAccount
            ?: throw IllegalStateException("${spec.protocol.label} Treasury account was not found.")
        val vesting = vestingAccount
            ?: throw IllegalStateException("${spec.protocol.label} Vesting account was not found.")
        require(treasury.owner == spec.protocol.programId) {
            "${spec.protocol.label} Treasury owner did not match its official program."
        }
        require(vesting.owner == spec.protocol.programId) {
            "${spec.protocol.label} Vesting owner did not match its official program."
        }

        val treasuryRead = OreStakingRules.parseTreasury(treasury.data)
        val vestingRead = OreStakingRules.parseVesting(vesting.data)
        val live = OreStakingRules.calculateLiveRewards(
            stake = stake,
            treasury = treasuryRead,
            vesting = vestingRead,
            clockUnixTimestampSeconds = clockUnixTimestampSeconds,
        )

        return ProtocolPositionRead(
            spec = spec,
            position = VerifiedPosition(
                spec = spec,
                account = stakeAccount,
                stake = stake,
                liveUnclaimedRaw = live.unclaimedRewardsRaw,
                liveLifetimeRaw = live.lifetimeRewardsRaw,
                pendingAccrualRaw = live.newlyAccruedSinceStakeUpdateRaw,
            ),
        )
    }

    private suspend fun derivePda(seeds: List<ByteArray>, programId: SolanaPublicKey): String =
        ProgramDerivedAddress.find(seeds, programId)
            .getOrElse { throw IllegalStateException("Could not derive official ORE staking PDA.", it) }
            .base58()

    private fun fetchLiquidOre(walletAddress: String): LiquidOreRead {
        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "getTokenAccountsByOwner")
            .put(
                "params",
                JSONArray()
                    .put(walletAddress)
                    .put(JSONObject().put("mint", OreStakingRules.ORE_MINT))
                    .put(JSONObject().put("encoding", "jsonParsed").put("commitment", "confirmed")),
            )
        val root = postMainnetRpc(request)
        throwIfRpcError(root, "ORE token RPC")
        val result = root.optJSONObject("result")
            ?: throw IllegalStateException("ORE token RPC response did not include a result.")
        val slot = result.optJSONObject("context")?.optLong("slot", -1L)?.takeIf { it >= 0L }
        val accounts = result.optJSONArray("value") ?: JSONArray()
        var total = BigInteger.ZERO
        var count = 0
        for (index in 0 until accounts.length()) {
            val amount = accounts.optJSONObject(index)
                ?.optJSONObject("account")
                ?.optJSONObject("data")
                ?.optJSONObject("parsed")
                ?.optJSONObject("info")
                ?.optJSONObject("tokenAmount")
                ?.optString("amount", "0")
                ?.toBigIntegerOrNull()
                ?: continue
            total += amount
            count += 1
        }
        return LiquidOreRead(total, count, slot)
    }

    private fun fetchMultipleAccounts(addresses: List<String>): MultipleAccountsRead {
        require(addresses.isNotEmpty()) { "ORE account batch was empty." }
        val addressArray = JSONArray()
        addresses.forEach(addressArray::put)
        val request = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 2)
            .put("method", "getMultipleAccounts")
            .put(
                "params",
                JSONArray()
                    .put(addressArray)
                    .put(JSONObject().put("encoding", "base64").put("commitment", "confirmed")),
            )
        val root = postMainnetRpc(request)
        throwIfRpcError(root, "ORE production-state RPC")
        val result = root.optJSONObject("result")
            ?: throw IllegalStateException("ORE production-state RPC response did not include a result.")
        val slot = result.optJSONObject("context")?.optLong("slot", -1L)?.takeIf { it >= 0L }
        val values = result.optJSONArray("value") ?: JSONArray()
        val accounts = addresses.indices.map { index ->
            val value = values.optJSONObject(index) ?: return@map null
            ProgramAccountRead(
                owner = value.optString("owner"),
                data = decodeBase64AccountData(value),
            )
        }
        return MultipleAccountsRead(accounts, slot)
    }

    private fun decodeBase64AccountData(accountObject: JSONObject): ByteArray {
        val data = accountObject.optJSONArray("data")
            ?: throw IllegalStateException("Solana account response did not include base64 data.")
        val encoded = data.optString(0)
        require(encoded.isNotBlank()) { "Solana account base64 payload was empty." }
        return Base64.getDecoder().decode(encoded)
    }

    private fun littleEndianLong(data: ByteArray, offset: Int): Long {
        require(offset >= 0 && offset + 8 <= data.size) { "Invalid Solana Clock layout." }
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((data[offset + i].toLong() and 0xffL) shl (8 * i))
        }
        return result
    }

    private fun postMainnetRpc(request: JSONObject): JSONObject = JSONObject(postRpcBody(request.toString()))

    private fun postRpcBody(body: String): String {
        val connection = (URL(MAINNET_RPC_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = RPC_TIMEOUT_MS
            readTimeout = RPC_TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.use { it.write(body.encodeToByteArray()) }
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

    private fun throwIfRpcError(root: JSONObject, label: String) {
        val rpcError = root.optJSONObject("error") ?: return
        throw IllegalStateException("$label error: ${rpcError.optString("message", "Unknown RPC error")}")
    }

    private fun friendlyRpcFailure(error: Throwable): String = when (error) {
        is java.net.UnknownHostException -> "Radiant Circle could not reach Solana mainnet RPC. Check phone internet/DNS, then refresh ORE."
        is java.net.SocketTimeoutException -> "Solana mainnet RPC timed out. Refresh ORE again in a moment."
        is IllegalArgumentException -> error.message ?: "The wallet address or ORE account data was invalid."
        else -> error.message ?: error::class.java.simpleName
    }

    private data class LiquidOreRead(
        val rawAmount: BigInteger,
        val tokenAccountCount: Int,
        val rpcSlot: Long?,
    )

    private data class ProgramAccountRead(
        val owner: String,
        val data: ByteArray,
    )

    private data class MultipleAccountsRead(
        val values: List<ProgramAccountRead?>,
        val rpcSlot: Long?,
    )

    private data class ProtocolReadSpec(
        val protocol: OreStakeProtocol,
        val programId: SolanaPublicKey,
        val stakeAddress: String,
        val treasuryAddress: String,
        val vestingAddress: String,
    )

    private data class VerifiedPosition(
        val spec: ProtocolReadSpec,
        val account: ProgramAccountRead,
        val stake: OreStakeRead,
        val liveUnclaimedRaw: BigInteger,
        val liveLifetimeRaw: BigInteger,
        val pendingAccrualRaw: BigInteger,
    )

    private data class ProtocolPositionRead(
        val spec: ProtocolReadSpec,
        val position: VerifiedPosition?,
    )

    private companion object {
        const val MAINNET_RPC_URL = "https://api.mainnet-beta.solana.com"
        const val CLOCK_SYSVAR_ADDRESS = "SysvarC1ock11111111111111111111111111111111"
        const val RPC_TIMEOUT_MS = 15_000
        const val TAG = "RadiantRushORE"
    }
}

sealed interface OrePortfolioResult {
    data class Success(val snapshot: OrePortfolioSnapshot) : OrePortfolioResult
    data class Failure(val message: String) : OrePortfolioResult
}
