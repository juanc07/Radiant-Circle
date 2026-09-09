package com.thinkblox.radiantrush.solana

import android.net.Uri
import androidx.activity.ComponentActivity
import com.funkatronics.encoders.Base58
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult

/**
 * Phase 3 Mobile Wallet Adapter integration.
 *
 * This class requests wallet authorization only. It stores/returns public wallet
 * identity and never asks for or stores private keys, seed phrases, mint authority,
 * or reward authority. Message signing starts in Phase 4.
 */
class MobileWalletRepository(
    private val activity: ComponentActivity,
) {
    private val sender: ActivityResultSender = ActivityResultSender(activity)

    private val walletAdapter = MobileWalletAdapter(
        connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse("https://thinkbloxph.dev/radiant-rush"),
            iconUri = Uri.parse("favicon.ico"),
            identityName = "Radiant Rush",
        ),
    )

    suspend fun connectWallet(): WalletConnectResult = runCatching {
            when (val result = walletAdapter.connect(sender)) {
                is TransactionResult.Success -> {
                    val account = result.authResult.accounts.firstOrNull()
                    if (account == null) {
                        WalletConnectResult.Failure("Wallet approved, but no account was returned.")
                    } else {
                        WalletConnectResult.Connected(
                            publicKey = Base58.encodeToString(account.publicKey),
                            accountLabel = account.accountLabel,
                        )
                    }
                }
                is TransactionResult.NoWalletFound -> {
                    WalletConnectResult.NoWalletFound
                }
                is TransactionResult.Failure -> {
                    WalletConnectResult.Failure(result.e.message ?: "Mobile Wallet Adapter connection failed.")
                }
            }
    }.getOrElse { error ->
        WalletConnectResult.Failure(error.message ?: error::class.java.simpleName)
    }

    suspend fun disconnectWallet(): WalletDisconnectResult = runCatching {
            when (val result = walletAdapter.disconnect(sender)) {
                is TransactionResult.Success -> WalletDisconnectResult.Disconnected
                is TransactionResult.NoWalletFound -> WalletDisconnectResult.NoWalletFound
                is TransactionResult.Failure -> WalletDisconnectResult.Failure(
                    result.e.message ?: "Mobile Wallet Adapter disconnect failed.",
                )
            }
    }.getOrElse { error ->
        WalletDisconnectResult.Failure(error.message ?: error::class.java.simpleName)
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
