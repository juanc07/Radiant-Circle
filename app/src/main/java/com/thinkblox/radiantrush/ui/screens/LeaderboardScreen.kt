package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.LeaderboardPreview
import com.thinkblox.radiantrush.data.RunCompetitionPreview
import com.thinkblox.radiantrush.data.RunLeaderboardPreview
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.StatusPill
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

@Composable
fun LeaderboardScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
) {
    val responsive = rememberResponsiveUiSpec()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Run Weekly", "Run All-Time", "My Stats", "XP")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = responsive.screenPadding,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.screenPadding,
                top = 14.dp,
                end = responsive.screenPadding,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 12.dp else 14.dp),
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        SectionTitle(
                            title = "Radiant Run • ${uiState.runCompetition.weekKey}",
                            body = "Your best ranked score this week. Combo and PERFECT hits break ties.",
                        )
                    }
                    if (uiState.runCompetition.weeklyLeaderboard.isEmpty()) {
                        item {
                            EmptyRunRanks(
                                if (uiState.isWalletConnected) {
                                    "No ranked scores yet this week. Play a Ranked Radiant Run to get on the board."
                                } else {
                                    "Connect your wallet to join ranked competition."
                                },
                            )
                        }
                    } else {
                        items(uiState.runCompetition.weeklyLeaderboard.size) { index ->
                            RunLeaderboardRow(uiState.runCompetition.weeklyLeaderboard[index])
                        }
                    }
                }

                1 -> {
                    item {
                        SectionTitle(
                            title = "Radiant Run • All-Time",
                            body = "Your best ranked run ever. Casual runs do not change this board.",
                        )
                    }
                    if (uiState.runCompetition.allTimeLeaderboard.isEmpty()) {
                        item {
                            EmptyRunRanks(
                                if (uiState.isWalletConnected) {
                                    "No all-time score yet. Finish a Ranked Radiant Run."
                                } else {
                                    "Connect your wallet to join ranked competition."
                                },
                            )
                        }
                    } else {
                        items(uiState.runCompetition.allTimeLeaderboard.size) { index ->
                            RunLeaderboardRow(uiState.runCompetition.allTimeLeaderboard[index])
                        }
                    }
                }

                2 -> item { PersonalCompetitionCard(uiState.runCompetition, uiState.isWalletConnected) }

                else -> {
                    item {
                        SectionTitle(
                            title = "Progression XP leaderboard",
                            body = "Long-term XP rankings from quests, runs, and rewards.",
                        )
                    }
                    if (uiState.leaderboard.isEmpty()) {
                        item { EmptyRunRanks("No XP ranks yet.") }
                    } else {
                        items(uiState.leaderboard.size) { index ->
                            XpLeaderboardRow(row = uiState.leaderboard[index])
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalCompetitionCard(
    competition: RunCompetitionPreview,
    walletConnected: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("My Radiant Run stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                if (walletConnected) {
                    "${competition.rankedAttemptsRemaining}/3 Ranked runs left today"
                } else {
                    "Connect your wallet to unlock Ranked runs."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatLine("Weekly PB", competition.personalWeeklyBestScore.toString())
            StatLine("All-Time PB", competition.personalAllTimeBestScore.toString())
            StatLine("Best combo", competition.personalBestCombo.toString())
            StatLine("PERFECT hits at PB", competition.personalPerfectHits.toString())
            StatLine("Weekly ranked runs", competition.personalWeeklyRuns.toString())
            StatLine("Gameplay XP today", "${competition.dailyGameplayXpEarned}/${competition.dailyGameplayXpCap}")
            competition.lastRunMode?.let { StatLine("Last run", "$it • +${competition.lastRunPerformanceXp} XP") }
            Text(
                "SKR perks never change Ranked score or Ranked attempts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RunLeaderboardRow(row: RunLeaderboardPreview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBubble(row.rank)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Clip)
                row.walletLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Combo ${row.bestCombo} • PERFECT ${row.perfectHits} • ${row.runsPlayed} ranked run${if (row.runsPlayed == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
            }
            Text(
                row.score.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun XpLeaderboardRow(row: LeaderboardPreview) {
    val responsive = rememberResponsiveUiSpec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(responsive.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RankBubble(row.rank)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Clip)
                row.walletLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "${row.xp} XP • ${row.streak}-day streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (responsive.isTiny || responsive.hasLargeText) StatusPill(row.tier)
            }
            if (!responsive.isTiny && !responsive.hasLargeText) StatusPill(row.tier)
        }
    }
}

@Composable
private fun RankBubble(rank: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = "#$rank",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    val responsive = rememberResponsiveUiSpec()
    if (responsive.isTiny || responsive.hasLargeText) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun EmptyRunRanks(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
