package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.QuestCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

@Composable
fun QuestsScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onCompleteQuest: (QuestPreview) -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    var selectedQuest by remember { mutableStateOf<QuestPreview?>(null) }

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
            SectionTitle(
                title = "Quest board",
                body = "Proof quests open Phantom on Devnet. SKR Passport is read-only mainnet scanning and should complete inside the app.",
            )
        }

        item {
            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
            )
        }

        items(uiState.quests.size) { index ->
            val quest = uiState.quests[index]
            QuestCard(
                quest = quest,
                actionLabel = questActionLabel(quest, uiState),
                actionEnabled = uiState.isFirebaseReady && !uiState.walletActionInProgress && quest.status == QuestStatus.Ready,
                onClick = { selectedQuest = quest },
                onActionClick = { onCompleteQuest(quest) },
            )
        }
    }

    selectedQuest?.let { quest ->
        AlertDialog(
            onDismissRequest = { selectedQuest = null },
            title = { Text(quest.title) },
            text = {
                Text(
                    text = "Proof: ${quest.proofType}\n\n${quest.description}\n\nPhase 5 keeps memo proof on Devnet and scans SKR on mainnet without moving tokens.",
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedQuest = null }) {
                    AdaptiveButtonText("Got it")
                }
            },
        )
    }
}


private fun questActionLabel(quest: QuestPreview, uiState: RushUiState): String? {
    if (uiState.activeQuestId == quest.id || quest.status == QuestStatus.Syncing) {
        return when (quest.id) {
            QuestIds.DAILY_CHECK_IN -> "Saving Check-In…"
            QuestIds.WALLET_CONNECT -> "Opening Wallet…"
            QuestIds.SIGN_DAILY_PROOF -> "Waiting for Signature…"
            QuestIds.ON_CHAIN_PROOF -> "Opening Memo…"
            QuestIds.SKR_HOLDER -> "Scanning SKR…"
            else -> "Working…"
        }
    }

    return when (quest.id) {
        QuestIds.DAILY_CHECK_IN -> if (quest.status == QuestStatus.Completed) "Done Today" else "Save Firebase Check-In"
        QuestIds.WALLET_CONNECT -> if (uiState.isWalletConnected || quest.status == QuestStatus.Completed) "Wallet Connected" else "Connect Wallet"
        QuestIds.SIGN_DAILY_PROOF -> if (quest.status == QuestStatus.Completed) "Signed Today" else "Sign Daily Proof"
        QuestIds.ON_CHAIN_PROOF -> if (quest.status == QuestStatus.Completed) "Memo Submitted" else "Submit Memo Proof"
        QuestIds.SKR_HOLDER -> if (quest.status == QuestStatus.Completed) "SKR Checked" else "Check SKR Balance"
        else -> null
    }
}
