package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.RadiantChestStatus
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.data.RetentionGoalPreview
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.MetricCard
import com.thinkblox.radiantrush.ui.components.ProgressCard
import com.thinkblox.radiantrush.ui.components.RadiantChestCard
import com.thinkblox.radiantrush.ui.components.RadiantRunLauncherCard
import com.thinkblox.radiantrush.ui.components.RadiantVaultStrip
import com.thinkblox.radiantrush.ui.components.QuestCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onCompleteQuest: (QuestPreview) -> Unit,
    onConnectWallet: () -> Unit,
    onClaimRadiantChest: () -> Unit,
    onPlayRadiantRun: () -> Unit,
) {
    val user = uiState.user
    val responsive = rememberResponsiveUiSpec()
    val skrPassportSupportingText = if (user.hasSkr) {
        buildString {
            append("${user.skrEligibleBalance} • +${user.skrDailyBonusTickets} Casual/day")
            if (user.skrStakeBoostActive) append(" • Stake Boost")
        }
    } else {
        "${user.skrBalance} • Refresh to check perks"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.HOME_SCREEN),
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
                subtitle = "Finish today’s quests, earn Rush Tickets, play Radiant Rush, and grow your collection.",
            )
        }

        item {
            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
            )
        }

        item {
            RetentionMomentumCard(uiState)
        }

        item {
            RetentionGoalsCard(
                title = "Today’s goals",
                completed = uiState.retention.dailyCompleted,
                goals = uiState.retention.dailyGoals,
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
                        supportingText = "Keep it going.",
                        icon = Icons.Filled.LocalFireDepartment,
                    )
                    MetricCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Level",
                        value = "Lv. ${user.level}",
                        supportingText = "${user.xp} XP total.",
                        icon = Icons.Filled.Bolt,
                    )
                    MetricCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = "SKR Passport",
                        value = user.skrTier,
                        supportingText = skrPassportSupportingText,
                        icon = Icons.Filled.Token,
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
                        supportingText = "Keep it going.",
                        icon = Icons.Filled.LocalFireDepartment,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "Level",
                        value = "Lv. ${user.level}",
                        supportingText = "${user.xp} XP total.",
                        icon = Icons.Filled.Bolt,
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = "SKR Passport",
                        value = user.skrTier,
                        supportingText = skrPassportSupportingText,
                        icon = Icons.Filled.Token,
                    )
                }
            }
        }

        item {
            ProgressCard(
                title = if (uiState.isWalletConnected) "Wallet linked" else "Wallet Ready",
                progress = if (uiState.isWalletConnected) 1f else (user.xp.coerceAtMost(500) / 500f).coerceIn(0f, 1f),
                caption = if (uiState.isWalletConnected) {
                    "Wallet ready for today’s quests."
                } else {
                    "Connect your wallet to unlock today’s Solana quests."
                },
            )
        }

        item {
            RadiantRunLauncherCard(
                run = uiState.radiantRun,
                enabled = uiState.isFirebaseReady && !uiState.walletActionInProgress,
                onPlay = onPlayRadiantRun,
            )
        }

        if (uiState.collection.isNotEmpty()) {
            item {
                RadiantVaultStrip(
                    collection = uiState.collection,
                    run = uiState.radiantRun,
                )
            }
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
            RetentionGoalsCard(
                title = "This week",
                completed = uiState.retention.weeklyCompleted,
                goals = uiState.retention.weeklyGoals,
            )
        }

        item {
            SectionTitle(
                title = "Priority quests",
                body = "Finish today’s quests to unlock your chest and earn more Rush Tickets.",
            )
        }

        items(uiState.quests.size) { index ->
            val quest = uiState.quests[index]
            QuestCard(
                quest = quest,
                actionLabel = questActionLabel(quest, uiState),
                actionEnabled = uiState.isFirebaseReady &&
                    !uiState.walletActionInProgress &&
                    (quest.status == QuestStatus.Ready || (quest.id == QuestIds.SKR_HOLDER && quest.status == QuestStatus.Completed)),
                onClick = {
                    if (uiState.isFirebaseReady &&
                        !uiState.walletActionInProgress &&
                        (quest.status == QuestStatus.Ready || (quest.id == QuestIds.SKR_HOLDER && quest.status == QuestStatus.Completed))
                    ) {
                        if (quest.id == QuestIds.WALLET_CONNECT) {
                            onConnectWallet()
                        } else {
                            onCompleteQuest(quest)
                        }
                    }
                },
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
                text = "SKR perks boost casual play and chest rewards. Ranked competition stays skill-based.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


@Composable
private fun RetentionMomentumCard(uiState: RushUiState) {
    val retention = uiState.retention
    val responsive = rememberResponsiveUiSpec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "What’s next",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = retention.nextActionTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = retention.nextActionDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
            )
            Text(
                text = "Streak ${uiState.user.currentStreak}d • next ${retention.streakRewardTitle} at ${retention.streakNextTarget}d",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
            )
            Text(
                text = "Collection ${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal} • ${retention.collectionRewardTitle} at ${retention.collectionNextTarget}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
            )
        }
    }
}

@Composable
private fun RetentionGoalsCard(
    title: String,
    completed: Int,
    goals: List<RetentionGoalPreview>,
) {
    if (goals.isEmpty()) return
    val responsive = rememberResponsiveUiSpec()
    val overall = (completed.toFloat() / goals.size.toFloat()).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (responsive.isCompact || responsive.hasLargeText) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        softWrap = true,
                    )
                    Text(
                        text = "$completed/${goals.size} complete",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(
                        "$completed/${goals.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { overall },
                modifier = Modifier.fillMaxWidth(),
            )
            goals.forEach { goal ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = if (goal.completed) "✓" else "○",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (goal.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = goal.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            softWrap = true,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = goal.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            softWrap = true,
                        )
                        if (responsive.isCompact || responsive.hasLargeText) {
                            Text(
                                text = goal.progressLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                softWrap = true,
                            )
                        }
                    }
                    if (!responsive.isCompact && !responsive.hasLargeText) {
                        Text(
                            text = goal.progressLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun questActionLabel(quest: QuestPreview, uiState: RushUiState): String? {
    if (uiState.activeQuestId == quest.id || quest.status == QuestStatus.Syncing) {
        return when (quest.id) {
            QuestIds.DAILY_CHECK_IN -> "Saving Check-In…"
            QuestIds.WALLET_CONNECT -> "Opening Wallet…"
            QuestIds.SIGN_DAILY_PROOF -> "Waiting for Signature…"
            QuestIds.ON_CHAIN_PROOF -> "Opening Memo…"
            QuestIds.SKR_HOLDER -> "Scanning SKR + stake…"
            else -> "Working…"
        }
    }

    return when (quest.id) {
        QuestIds.DAILY_CHECK_IN -> if (quest.status == QuestStatus.Completed) "Done Today" else "Check In"
        QuestIds.WALLET_CONNECT -> if (uiState.isWalletConnected || quest.status == QuestStatus.Completed) "Wallet Connected" else "Connect Wallet"
        QuestIds.SIGN_DAILY_PROOF -> if (quest.status == QuestStatus.Completed) "Signed Today" else "Sign Daily Proof"
        QuestIds.ON_CHAIN_PROOF -> if (quest.status == QuestStatus.Completed) "Memo Submitted" else "Submit Memo Proof"
        QuestIds.SKR_HOLDER -> if (quest.status == QuestStatus.Completed) "Refresh SKR Passport" else "Check SKR Passport"
        else -> null
    }
}
