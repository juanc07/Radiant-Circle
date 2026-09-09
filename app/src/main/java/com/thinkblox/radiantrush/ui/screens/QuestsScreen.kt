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
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.QuestCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard

@Composable
fun QuestsScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onCompleteQuest: (QuestPreview) -> Unit,
) {
    var selectedQuest by remember { mutableStateOf<QuestPreview?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 14.dp,
            end = 20.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionTitle(
                title = "Quest board",
                body = "Phase 3 adds real Mobile Wallet Adapter authorization. On-chain memo proof and SKR balance checks remain locked until their real implementations exist.",
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
                actionLabel = when (quest.id) {
                    QuestIds.DAILY_CHECK_IN -> "Save Firebase Check-In"
                    QuestIds.WALLET_CONNECT -> if (uiState.isWalletConnected) "Wallet Connected" else "Connect Wallet"
                    else -> null
                },
                actionEnabled = uiState.isFirebaseReady && !uiState.walletActionInProgress,
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
                    text = "Proof type: ${quest.proofType}\n\n${quest.description}\n\nPhase 3 can authorize a wallet and save its public address. It does not request message signing, send a transaction, or claim SKR verification yet.",
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedQuest = null }) {
                    Text("Got it")
                }
            },
        )
    }
}
