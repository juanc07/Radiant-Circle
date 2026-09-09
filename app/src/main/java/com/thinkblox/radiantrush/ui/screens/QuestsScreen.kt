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
                body = "Only the daily Firebase check-in can complete in Phase 2. Wallet, on-chain, and SKR quests stay locked until their real implementations exist.",
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
                actionLabel = if (quest.id == "daily-check-in") "Save Firebase Check-In" else null,
                actionEnabled = uiState.isFirebaseReady,
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
                    text = "Proof type: ${quest.proofType}\n\n${quest.description}\n\nPhase 2 uses Firebase Auth + Firestore for app progress. It does not claim wallet or on-chain completion.",
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
