package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.QuestIds
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
    onConnectWallet: () -> Unit,
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
                subtitle = "Build cloud-saved streaks, then connect your Solana wallet through Mobile Wallet Adapter. On-chain proof arrives in Phase 4.",
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
                title = if (uiState.isWalletConnected) "Wallet linked" else "Visitor → Wallet Ready",
                progress = if (uiState.isWalletConnected) 1f else (user.xp.coerceAtMost(500) / 500f).coerceIn(0f, 1f),
                caption = if (uiState.isWalletConnected) {
                    "Public wallet identity is saved. Phase 4 will request signed proof and submit a memo transaction."
                } else {
                    "Connect through MWA to unlock wallet identity before SKR verification in Phase 5."
                },
            )
        }

        item {
            SectionTitle(
                title = "Priority quests",
                body = "Phase 3 supports Firebase progress plus real MWA wallet authorization. Transaction signing stays locked until Phase 4.",
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
                onActionClick = {
                    if (quest.id == QuestIds.WALLET_CONNECT) {
                        onConnectWallet()
                    } else {
                        onCompleteQuest(quest)
                    }
                },
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Current build target: real wallet connect first, then signed daily proof and memo transaction next.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
