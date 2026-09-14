package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RadiantChestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.logic.WalletFeatureAccessRules
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.QuestCard
import com.thinkblox.radiantrush.ui.components.RadiantChestCard
import com.thinkblox.radiantrush.ui.components.RadiantRunLauncherCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

/** Daily action hub. The social/profile-oriented Home stays intentionally light. */
@Composable
fun QuestsScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
    onCompleteQuest: (QuestPreview) -> Unit,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
    onClaimRadiantChest: () -> Unit,
    onPlayRadiantRun: () -> Unit,
    returnToRushRequest: Int = 0,
    onReturnToRushHandled: () -> Unit = {},
) {
    val responsive = rememberResponsiveUiSpec()
    var selectedQuest by remember { mutableStateOf<QuestPreview?>(null) }
    var showDisconnectConfirmation by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val syncItemCount = if (uiState.firebaseStatus != FirebaseStatus.Ready) 1 else 0
    val radiantRushLauncherIndex = syncItemCount + uiState.quests.size + 3

    LaunchedEffect(returnToRushRequest, radiantRushLauncherIndex) {
        if (returnToRushRequest > 0) {
            listState.scrollToItem(radiantRushLauncherIndex)
            onReturnToRushHandled()
        }
    }

    val radiantRushWalletAccess = WalletFeatureAccessRules.radiantRushAccess(
        walletConnected = uiState.isWalletConnected,
        cupStatusCode = uiState.runCompetition.weeklyCup.cupStatusCode,
        startsAtEpochMillis = uiState.runCompetition.weeklyCup.seasonStartsAtEpochMillis,
        endsAtEpochMillis = uiState.runCompetition.weeklyCup.seasonEndsAtEpochMillis,
    )

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = 14.dp,
            end = responsive.screenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 12.dp else 14.dp),
    ) {
        if (uiState.firebaseStatus != FirebaseStatus.Ready) {
            item {
                SyncStatusCard(
                    status = uiState.firebaseStatus,
                    message = uiState.lastMessage,
                    onRetry = onRetryFirebase,
                )
            }
        }

        item {
            DailyProgressCard(uiState)
        }

        items(uiState.quests.size) { index ->
            val quest = uiState.quests[index]
            QuestCard(
                quest = quest,
                actionLabel = questActionLabel(quest, uiState),
                actionEnabled = uiState.isFirebaseReady &&
                    !uiState.walletActionInProgress &&
                    (quest.status == QuestStatus.Ready ||
                        quest.id == QuestIds.WALLET_CONNECT ||
                        (quest.id == QuestIds.SKR_HOLDER && quest.status == QuestStatus.Completed)),
                allowCompletedAction = quest.id == QuestIds.WALLET_CONNECT || quest.id == QuestIds.SKR_HOLDER,
                onClick = { selectedQuest = quest },
                onActionClick = {
                    if (quest.id == QuestIds.WALLET_CONNECT) {
                        if (uiState.isWalletConnected) {
                            showDisconnectConfirmation = true
                        } else {
                            onConnectWallet()
                        }
                    } else {
                        onCompleteQuest(quest)
                    }
                },
            )
        }

        item {
            RadiantChestCard(
                chest = uiState.radiantChest,
                actionEnabled = uiState.isFirebaseReady &&
                    !uiState.walletActionInProgress &&
                    uiState.radiantChest.status == RadiantChestStatus.Ready,
                onClaim = onClaimRadiantChest,
            )
        }

        item {
            SectionTitle(
                title = "Play when you’re ready",
                body = "Radiant Rush is a quick skill run. A connected wallet is required only for Ranked Cup entry.",
            )
        }

        item {
            RadiantRunLauncherCard(
                run = uiState.radiantRun,
                enabled = uiState.isFirebaseReady && !uiState.walletActionInProgress,
                walletConnected = uiState.isWalletConnected,
                cupRunning = radiantRushWalletAccess.cupRunning,
                onConnectWallet = onConnectWallet,
                onPlay = onPlayRadiantRun,
            )
        }
    }


    if (showDisconnectConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = {
                Text(
                    text = "Disconnect wallet?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    text = "Your Radiant Circle profile and earned rewards stay intact. Wallet-only features will be unavailable until you reconnect.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmation = false }) {
                    AdaptiveButtonText("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.walletActionInProgress,
                    onClick = {
                        showDisconnectConfirmation = false
                        onDisconnectWallet()
                    },
                ) {
                    AdaptiveButtonText("Disconnect")
                }
            },
        )
    }

    selectedQuest?.let { quest ->
        AlertDialog(
            onDismissRequest = { selectedQuest = null },
            title = {
                Text(
                    text = quest.title,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    text = "${quest.description}\n\nReward: +${quest.xp} XP",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = { selectedQuest = null }) {
                        AdaptiveButtonText("Got it")
                    }
                }
            },
        )
    }
}

@Composable
private fun DailyProgressCard(uiState: RushUiState) {
    val responsive = rememberResponsiveUiSpec()
    val total = uiState.quests.size.coerceAtLeast(1)
    val completed = uiState.completedQuestCount.coerceIn(0, total)
    val progress = completed.toFloat() / total.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (responsive.isTiny) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$completed of $total complete",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (completed == total) {
                    "All set."
                } else {
                    "Pick one task below — no need to do everything at once."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun questActionLabel(quest: QuestPreview, uiState: RushUiState): String? {
    if (uiState.activeQuestId == quest.id) {
        return when (quest.id) {
            QuestIds.DAILY_CHECK_IN -> "Saving Check-In…"
            QuestIds.WALLET_CONNECT -> if (uiState.isWalletConnected) "Disconnecting…" else "Opening Wallet…"
            QuestIds.SIGN_DAILY_PROOF -> "Waiting for Signature…"
            QuestIds.ON_CHAIN_PROOF -> "Opening Memo…"
            QuestIds.SKR_HOLDER -> "Scanning SKR + stake…"
            else -> "Working…"
        }
    }

    // Profile-first bootstrap intentionally arrives before the slower completion-history read.
    // During that short window an unknown daily action is disabled and described as a status
    // check, never as if the player had just pressed it.
    if (quest.status == QuestStatus.Syncing) {
        return "Checking status…"
    }

    return when (quest.id) {
        QuestIds.DAILY_CHECK_IN -> if (quest.status == QuestStatus.Completed) "Done" else "Check In"
        QuestIds.WALLET_CONNECT -> if (uiState.isWalletConnected) "Disconnect Wallet" else "Connect Wallet"
        QuestIds.SIGN_DAILY_PROOF -> if (quest.status == QuestStatus.Completed) "Signed" else "Sign Daily Proof"
        QuestIds.ON_CHAIN_PROOF -> if (quest.status == QuestStatus.Completed) "Memo Submitted" else "Submit Memo Proof"
        QuestIds.SKR_HOLDER -> if (quest.status == QuestStatus.Completed) "Refresh SKR Passport" else "Check SKR Passport"
        else -> null
    }
}
