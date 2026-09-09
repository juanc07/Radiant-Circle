package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.ui.components.QuestCard
import com.thinkblox.radiantrush.ui.components.SectionTitle

@Composable
fun QuestsScreen(contentPadding: PaddingValues) {
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
                body = "The UX contract is ready. Phase 2 will save progress to Firebase. Phase 3/4 will replace preview actions with real wallet signing and transactions.",
            )
        }

        items(PreviewContent.quests.size) { index ->
            QuestCard(
                quest = PreviewContent.quests[index],
                onClick = { selectedQuest = PreviewContent.quests[index] },
            )
        }
    }

    selectedQuest?.let { quest ->
        AlertDialog(
            onDismissRequest = { selectedQuest = null },
            title = { Text(quest.title) },
            text = {
                Text(
                    text = "Proof type: ${quest.proofType}\n\n${quest.description}\n\nThis is a Phase 1 preview only. No wallet request or transaction is simulated.",
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
