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
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onCompleteQuest: (QuestPreview) -> Unit,
    onConnectWallet: () -> Unit,
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
                title = "Today’s Rush",
                subtitle = "Sign proof, submit a devnet memo, and keep your streak synced.",
            )
        }

        item {
            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
            )
        }

        item {
            if (responsive.isCompact || responsive.hasLargeText) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetricCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Streak",
                        value = "${user.currentStreak} days",
                        supportingText = "Saved once per day.",
                        icon = Icons.Filled.LocalFireDepartment,
                    )
                    MetricCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Level",
                        value = "Lv. ${user.level}",
                        supportingText = "${user.xp} XP synced.",
                        icon = Icons.Filled.Bolt,
                    )
                }
            } else {
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
        }

        item {
            ProgressCard(
                title = if (uiState.isWalletConnected) "Wallet linked" else "Wallet Ready",
                progress = if (uiState.isWalletConnected) 1f else (user.xp.coerceAtMost(500) / 500f).coerceIn(0f, 1f),
                caption = if (uiState.isWalletConnected) {
                    "Public wallet saved. You can sign proof and submit a devnet memo."
                } else {
                    "Connect with MWA to unlock signed proof and memo quests."
                },
            )
        }

        item {
            SectionTitle(
                title = "Priority quests",
                body = "Phase 4 supports Firebase progress, MWA wallet authorization, message signing, and a devnet memo transaction.",
            )
        }

        items(uiState.quests.size) { index ->
            val quest = uiState.quests[index]
            QuestCard(
                quest = quest,
                actionLabel = when (quest.id) {
                    QuestIds.DAILY_CHECK_IN -> "Save Firebase Check-In"
                    QuestIds.WALLET_CONNECT -> if (uiState.isWalletConnected) "Wallet Connected" else "Connect Wallet"
                    QuestIds.SIGN_DAILY_PROOF -> if (quest.status.name == "Completed") "Signed Today" else "Sign Daily Proof"
                    QuestIds.ON_CHAIN_PROOF -> if (quest.status.name == "Completed") "Memo Submitted" else "Submit Memo Proof"
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
                text = "Use Phantom test/devnet mode for Phase 4 proof quests.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
