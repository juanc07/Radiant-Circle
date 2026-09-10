package com.thinkblox.radiantrush.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
) {
    val user = uiState.user
    val responsive = rememberResponsiveUiSpec()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = 14.dp,
            end = responsive.screenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 12.dp else 16.dp),
    ) {
        item {
            GradientHeroCard(
                title = user.displayName,
                subtitle = if (uiState.isWalletConnected) {
                    "${user.walletStatus}. Public Solana address is linked."
                } else {
                    "${user.walletStatus}. Connect with Mobile Wallet Adapter."
                },
            )
        }

        item {
            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
                onRetry = onRetryFirebase,
            )
        }

        item {
            SectionTitle(
                title = "Identity readiness",
                body = "Phase 10 links real Solana proofs to a native skill game: quests earn Rush Tickets, runs earn collectible capsules, and Firebase keeps score/progression synced.",
            )
        }

        item {
            ProfileInfoRow(
                title = "Firebase Profile",
                value = "${user.xp} XP • ${user.currentStreak}-day streak • ${uiState.todayKey}",
                helper = "Firebase UID stays internal; public ranks collapse duplicate rows by connected wallet identity.",
                icon = Icons.Filled.Storage,
            )
        }
        item {
            ProfileInfoRow(
                title = "Radiant Run",
                value = "Best ${user.bestRunScore} • ${user.totalRuns} runs",
                helper = "Last ${user.lastRunScore} • x${user.lastRunMaxCombo} combo • ${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal} collectibles.",
                icon = Icons.Filled.Bolt,
            )
        }
        item {
            ProfileInfoRow(
                title = "Rush Tickets / Shards",
                value = "${user.rushTickets} tickets • ${user.radiantShards} shards",
                helper = "Free in-app progression only. Tickets are earned from proof quests/chests; SOL, SKR, and XP are not wagers.",
                icon = Icons.Filled.ConfirmationNumber,
            )
        }
        if (!user.lastRunRewardTitle.isNullOrBlank()) {
            item {
                ProfileInfoRow(
                    title = "Last Capsule",
                    value = "${user.lastRunRewardRarity ?: "Reward"} • ${user.lastRunRewardTitle}",
                    helper = "+${user.lastRunRewardXp} XP" + if (user.lastRunRewardShards > 0) " • duplicate +${user.lastRunRewardShards} shards" else " • new discovery",
                    icon = Icons.Filled.EmojiEvents,
                )
            }
        }
        item {
            ProfileInfoRow(
                title = "Daily Radiant Chest",
                value = if (user.lastChestRewardXp > 0) {
                    "+${user.lastChestRewardXp} XP • ${user.lastChestRewardRarity ?: "Reward"}"
                } else {
                    uiState.radiantChest.status.label
                },
                helper = if (user.lastChestRewardXp > 0) {
                    "${user.lastChestRewardTitle ?: "Daily reward"} claimed ${user.lastChestClaimDate ?: "today"}. Total chest XP: ${user.totalChestXp}."
                } else {
                    "Complete all daily proofs to unlock a no-loss chest reveal. XP is never spent as a wager."
                },
                icon = Icons.Filled.EmojiEvents,
            )
        }
        item {
            ProfileInfoRow(
                title = "Wallet",
                value = user.walletAddress,
                helper = if (uiState.isWalletConnected) {
                    "Public address only. No seed phrase or private key is stored."
                } else {
                    "Tap Connect Wallet to authorize through an MWA-compatible Solana wallet."
                },
                icon = Icons.Filled.AccountBalanceWallet,
                copyValue = user.walletAddress.takeIf { uiState.isWalletConnected },
                copyLabel = "Copy Address",
            )
        }
        item {
            ProfileInfoRow(
                title = "SKR Tier",
                value = user.skrTier,
                helper = "${user.skrMultiplier} XP boost from the latest mainnet SKR scan.",
                icon = Icons.Filled.Token,
            )
        }
        item {
            ProfileInfoRow(
                title = "SKR Balance",
                value = user.skrBalance,
                helper = "${user.skrNetwork} • checked ${user.lastSkrChecked ?: "not yet"}. Read-only scan; copy uses the official SKR mint.",
                icon = Icons.Filled.Token,
                copyValue = user.skrMint,
                copyLabel = "Copy Mint",
            )
        }
        if (!user.lastSignedMessageSignature.isNullOrBlank()) {
            item {
                ProfileInfoRow(
                    title = "Last Signed Proof",
                    value = shortenForProfile(user.lastSignedMessageSignature.orEmpty()),
                    helper = "MWA message signature saved in today’s completedQuests document.",
                    icon = Icons.Filled.Security,
                    copyValue = user.lastSignedMessageSignature,
                    copyLabel = "Copy Signature",
                )
            }
        }
        if (!user.lastOnChainTxSignature.isNullOrBlank()) {
            item {
                ProfileInfoRow(
                    title = "Last Memo Transaction",
                    value = shortenForProfile(user.lastOnChainTxSignature.orEmpty()),
                    helper = user.lastOnChainExplorerUrl ?: "Saved devnet transaction signature.",
                    icon = Icons.Filled.Token,
                    copyValue = user.lastOnChainExplorerUrl ?: user.lastOnChainTxSignature,
                    copyLabel = if (!user.lastOnChainExplorerUrl.isNullOrBlank()) "Copy Explorer" else "Copy TX",
                )
            }
        }
        item {
            ProfileInfoRow(
                title = "Security Rule",
                value = "No secrets in APK",
                helper = "Private keys, mint authority, and reward authority never belong in the Android app.",
                icon = Icons.Filled.Security,
            )
        }
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight),
                enabled = uiState.isFirebaseReady && !uiState.walletActionInProgress && !uiState.isWalletConnected,
                shape = RoundedCornerShape(16.dp),
                onClick = onConnectWallet,
            ) {
                AdaptiveButtonText(if (uiState.walletActionInProgress) "Opening Wallet…" else "Connect Wallet")
            }
        }
        if (uiState.isWalletConnected) {
            item {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    enabled = !uiState.walletActionInProgress,
                    shape = RoundedCornerShape(16.dp),
                    onClick = onDisconnectWallet,
                ) {
                    AdaptiveButtonText("Disconnect Wallet")
                }
            }
        }
        item {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight),
                enabled = !uiState.walletActionInProgress,
                shape = RoundedCornerShape(16.dp),
                onClick = onRetryFirebase,
            ) {
                AdaptiveButtonText("Refresh Firebase Sync")
            }
        }
    }
}

private fun shortenForProfile(value: String): String {
    val clean = value.trim()
    return if (clean.length <= 18) clean else "${clean.take(8)}…${clean.takeLast(8)}"
}

@Composable
private fun ProfileInfoRow(
    title: String,
    value: String,
    helper: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    copyValue: String? = null,
    copyLabel: String = "Copy",
) {
    val responsive = rememberResponsiveUiSpec()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.size(2.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!copyValue.isNullOrBlank()) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (responsive.isTiny) 46.dp else 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            clipboardManager.setText(AnnotatedString(copyValue))
                            Toast.makeText(context, "$copyLabel copied", Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        AdaptiveButtonText(
                            text = copyLabel,
                            compactText = copyLabel.replace("Copy ", "Copy "),
                            tinyText = when {
                                copyLabel.contains("Signature") -> "Copy Sign"
                                copyLabel.contains("Explorer") -> "Copy Link"
                                copyLabel.contains("Address") -> "Copy Addr"
                                copyLabel.contains("Mint") -> "Copy Mint"
                                else -> "Copy"
                            },
                        )
                    }
                }
            }
        }
    }
}
