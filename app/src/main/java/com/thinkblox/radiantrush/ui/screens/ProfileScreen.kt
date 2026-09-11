package com.thinkblox.radiantrush.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
            val heroSubtitle = if (uiState.isWalletConnected) {
                buildString {
                    append("${user.walletStatus}. Public Solana address is linked.")
                    if (user.hasSkr) append(" ${user.skrAuraLabel} active.")
                }
            } else {
                "${user.walletStatus}. Connect your Solana wallet."
            }
            if (user.hasSkr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(30.dp),
                        )
                        .padding(2.dp),
                ) {
                    GradientHeroCard(
                        title = "✦ ${user.displayName} ✦",
                        subtitle = heroSubtitle,
                    )
                }
            } else {
                GradientHeroCard(
                    title = user.displayName,
                    subtitle = heroSubtitle,
                )
            }
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
                title = "Player profile",
                body = "Your progress, Radiant Run records, wallet, and SKR Passport.",
            )
        }

        item {
            ProfileInfoRow(
                title = "Progress",
                value = "${user.xp} XP • ${user.currentStreak}-day streak • ${uiState.todayKey}",
                helper = "Your XP and daily streak.",
                icon = Icons.Filled.Storage,
            )
        }
        item {
            ProfileInfoRow(
                title = "Radiant Run",
                value = "Best ${user.bestRunScore} • ${user.totalRuns} runs",
                helper = "Last ${user.lastRunScore} • x${user.lastRunMaxCombo} combo • ${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal} collectibles",
                icon = Icons.Filled.Bolt,
            )
        }
        item {
            ProfileInfoRow(
                title = "Rush Tickets / Shards",
                value = "${user.rushTickets} standard • ${user.skrCasualRushTickets} SKR casual • ${user.radiantShards} shards",
                helper = "Standard tickets can enter Ranked runs. SKR bonus tickets are Casual-only.",
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
                    buildString {
                        append("${user.lastChestRewardTitle ?: "Daily reward"} claimed ${user.lastChestClaimDate ?: "today"}. Total chest XP: ${user.totalChestXp}.")
                        if (user.lastChestRewardTickets > 0) append(" +${user.lastChestRewardTickets} Rush Tickets.")
                        if (user.lastChestSkrBonusXp > 0 || user.lastChestSkrBonusTickets > 0) {
                            append(" SKR perk included +${user.lastChestSkrBonusXp} XP")
                            if (user.lastChestSkrBonusTickets > 0) {
                                append(" and +${user.lastChestSkrBonusTickets} ticket${if (user.lastChestSkrBonusTickets == 1) "" else "s"}")
                            }
                            append(".")
                        }
                    }
                } else {
                    "Complete today’s quests to unlock your chest."
                },
                icon = Icons.Filled.EmojiEvents,
            )
        }
        item {
            ProfileInfoRow(
                title = "Wallet",
                value = user.walletAddress,
                helper = if (uiState.isWalletConnected) {
                    "Connected and ready for wallet quests."
                } else {
                    "Connect your Solana wallet to continue."
                },
                icon = Icons.Filled.AccountBalanceWallet,
                copyValue = user.walletAddress.takeIf { uiState.isWalletConnected },
                copyLabel = "Copy Address",
            )
        }
        item {
            SectionTitle(
                title = "SKR Passport",
                body = "Liquid + active staked SKR unlock daily perks, chest boosts, and cosmetics.",
            )
        }
        item {
            ProfileInfoRow(
                title = "Passport Eligible SKR",
                value = "${user.skrTier} • ${user.skrEligibleBalance}",
                helper = "Liquid + active stake. Unstaking SKR is shown separately.",
                icon = Icons.Filled.Token,
                copyValue = user.skrMint,
                copyLabel = "Copy Mint",
            )
        }
        item {
            ProfileInfoRow(
                title = "Liquid SKR",
                value = user.skrBalance,
                helper = "Last refreshed: ${user.lastSkrChecked ?: "Not yet"}",
                icon = Icons.Filled.Token,
            )
        }
        item {
            ProfileInfoRow(
                title = "Daily Holder Perks",
                value = if (user.hasSkr) {
                    buildString {
                        append("+${user.skrDailyBonusTickets} bonus ticket${if (user.skrDailyBonusTickets == 1) "" else "s"}/day")
                        append(" • Chest +${user.skrChestBonusXp} XP")
                        if (user.skrChestBonusTickets > 0) append(" +${user.skrChestBonusTickets} ticket${if (user.skrChestBonusTickets == 1) "" else "s"}")
                    }
                } else {
                    "Explorer • no holder perks yet"
                },
                helper = if (user.hasSkr) {
                    buildString {
                        append("${user.skrDailyBonusTicketsGrantedToday}/${user.skrDailyBonusTickets} daily bonus tickets claimed.")
                        if (user.skrStakeBoostActive) append(" ${user.skrStakeBoostLabel} active.")
                    }
                } else {
                    "Hold or actively stake SKR to unlock Passport perks."
                },
                icon = Icons.Filled.ConfirmationNumber,
            )
        }
        item {
            ProfileInfoRow(
                title = "Passport Cosmetics",
                value = "${user.skrFrameLabel} • ${user.skrAuraLabel}",
                helper = "Badge: ${user.skrHolderCollectibleLabel}",
                icon = Icons.Filled.EmojiEvents,
            )
        }
        item {
            ProfileInfoRow(
                title = "Active Staked SKR",
                value = if (user.skrStakedVerified) user.skrStakedBalance else "Read unavailable",
                helper = buildString {
                    append(user.skrStakedStatus)
                    if (user.skrStakeBoostActive) append(" • ${user.skrStakeBoostLabel}: +1 Casual ticket/day, +25 Chest XP")
                    else if (user.skrStakedVerified) append(" • Stake Boost unlocks with active stake")
                },
                icon = Icons.Filled.Security,
                copyValue = user.skrStakingProgramId,
                copyLabel = "Copy Staking Program",
            )
        }
        if (user.skrStakedVerified && user.skrUnstakingBalance != "0 SKR") {
            item {
                ProfileInfoRow(
                    title = "Unstaking SKR",
                    value = user.skrUnstakingBalance,
                    helper = if (user.skrUnstakingReady) {
                        "Cooldown complete. Ready to withdraw."
                    } else {
                        "Cooldown in progress."
                    },
                    icon = Icons.Filled.Security,
                )
            }
        }
        if (!user.lastSignedMessageSignature.isNullOrBlank()) {
            item {
                ProfileInfoRow(
                    title = "Last Signed Proof",
                    value = shortenForProfile(user.lastSignedMessageSignature.orEmpty()),
                    helper = "Today’s signed wallet proof.",
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
                    helper = user.lastOnChainExplorerUrl ?: "Today’s memo transaction.",
                    icon = Icons.Filled.Token,
                    copyValue = user.lastOnChainExplorerUrl ?: user.lastOnChainTxSignature,
                    copyLabel = if (!user.lastOnChainExplorerUrl.isNullOrBlank()) "Copy Explorer" else "Copy TX",
                )
            }
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
                AdaptiveButtonText("Refresh Profile")
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

    fun copy() {
        if (!copyValue.isNullOrBlank()) {
            clipboardManager.setText(AnnotatedString(copyValue))
            Toast.makeText(context, "$copyLabel copied", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        if (responsive.isCompact || responsive.hasLargeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    softWrap = true,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true,
                )
                if (!copyValue.isNullOrBlank()) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (responsive.isTiny) 46.dp else 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = ::copy,
                    ) {
                        AdaptiveButtonText(
                            text = copyLabel,
                            compactText = when {
                                copyLabel.contains("Signature") -> "Copy Sign"
                                copyLabel.contains("Explorer") -> "Copy Link"
                                copyLabel.contains("Address") -> "Copy Addr"
                                copyLabel.contains("Program") -> "Copy Program"
                                else -> copyLabel
                            },
                            tinyText = "Copy",
                        )
                    }
                }
            }
        } else {
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(text = value, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = helper,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!copyValue.isNullOrBlank()) {
                        OutlinedButton(
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            onClick = ::copy,
                        ) {
                            AdaptiveButtonText(copyLabel)
                        }
                    }
                }
            }
        }
    }
}
