package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.MetricCard
import com.thinkblox.radiantrush.ui.components.ProgressCard
import com.thinkblox.radiantrush.ui.components.QuestCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onCompleteQuest: (QuestPreview) -> Unit,
) {
    val user = uiState.user

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
            GradientHeroCard(
                title = "Today’s Rush",
                subtitle = "Firebase now saves your cloud profile, daily check-in, XP, streak, badges, and leaderboard preview. Wallet actions still start in Phase 3.",
            )
        }

        item {
            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Streak",
                    value = "${user.currentStreak} days",
                    supportingText = "Saved by Firebase once per day.",
                    icon = Icons.Filled.LocalFireDepartment,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Level",
                    value = "Lv. ${user.level}",
                    supportingText = "${user.xp} XP synced.",
                    icon = Icons.Filled.Bolt,
                )
            }
        }

        item {
            ProgressCard(
                title = "Visitor → Radiant",
                progress = (user.xp.coerceAtMost(500) / 500f).coerceIn(0f, 1f),
                caption = "SKR holder boosts and Radiant tier logic arrive in Phase 5.",
            )
        }

        item {
            SectionTitle(
                title = "Priority quests",
                body = "Phase 2 saves Firebase progress only. No wallet request or Solana transaction is simulated here.",
            )
        }

        items(uiState.quests.size) { index ->
            val quest = uiState.quests[index]
            QuestCard(
                quest = quest,
                actionLabel = if (quest.id == "daily-check-in") "Save Firebase Check-In" else null,
                actionEnabled = uiState.isFirebaseReady,
                onActionClick = { onCompleteQuest(quest) },
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Build target: Firebase profile/progress first, then Mobile Wallet Adapter and real Solana proofs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
