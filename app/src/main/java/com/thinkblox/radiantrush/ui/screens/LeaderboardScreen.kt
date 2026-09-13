package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.LeaderboardPreview
import com.thinkblox.radiantrush.data.RunCompetitionPreview
import com.thinkblox.radiantrush.data.RunLeaderboardPreview
import com.thinkblox.radiantrush.data.WeeklyCupPreview
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.StatusPill
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import kotlinx.coroutines.delay

@Composable
fun LeaderboardScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
) {
    val responsive = rememberResponsiveUiSpec()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Weekly Cup", "Run All-Time", "My Stats", "XP")

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
                            softWrap = false,
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
                        WeeklyCupHeaderCard(
                            cup = uiState.runCompetition.weeklyCup,
                            walletConnected = uiState.isWalletConnected,
                        )
                    }
                    if (
                        uiState.runCompetition.weeklyCup.sponsoredPrizeActive ||
                        !uiState.runCompetition.weeklyCup.trustedSponsorConfig
                    ) {
                        item {
                            WeeklyCupSponsorCard(uiState.runCompetition.weeklyCup)
                        }
                    }
                    if (uiState.runCompetition.weeklyLeaderboard.isEmpty()) {
                        item {
                            EmptyRunRanks(
                                if (uiState.isWalletConnected) {
                                    "No Ranked scores yet this week. Finish a Ranked Radiant Rush to join the Cup."
                                } else {
                                    "Connect your wallet to join the Weekly Radiant Cup."
                                },
                            )
                        }
                    } else {
                        item {
                            SectionTitle(
                                title = "Cup podium",
                                body = "Raw Ranked score wins. Combo and PERFECT hits break ties.",
                            )
                        }
                        item {
                            CupPodium(uiState.runCompetition.weeklyLeaderboard.take(3))
                        }
                        if (uiState.runCompetition.weeklyLeaderboard.size > 3) {
                            item {
                                SectionTitle(
                                    title = "Standings",
                                    body = "Keep improving your weekly personal best before the season closes.",
                                )
                            }
                            items(uiState.runCompetition.weeklyLeaderboard.size - 3) { offset ->
                                RunLeaderboardRow(uiState.runCompetition.weeklyLeaderboard[offset + 3])
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        SectionTitle(
                            title = "Radiant Rush • All-Time",
                            body = "Your best ranked run ever. Casual runs do not change this board.",
                        )
                    }
                    if (uiState.runCompetition.allTimeLeaderboard.isEmpty()) {
                        item {
                            EmptyRunRanks(
                                if (uiState.isWalletConnected) {
                                    "No all-time score yet. Finish a Ranked Radiant Rush."
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
private fun WeeklyCupHeaderCard(
    cup: WeeklyCupPreview,
    walletConnected: Boolean,
) {
    val responsive = rememberResponsiveUiSpec()
    val countdown = rememberCupCountdown(cup.seasonEndsAtEpochMillis)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "WEEKLY RADIANT CUP",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = cup.seasonKey.ifBlank { "Current season" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            if (cup.trustedSponsorConfig || cup.cupStatusCode != "UNCONFIGURED") {
                StatusPill(cup.cupStatusLabel)
            }
            Text(
                text = countdown,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when {
                    cup.participantCount >= 100 -> "100+ ranked wallets"
                    cup.participantCount == 1 -> "1 ranked wallet"
                    else -> "${cup.participantCount} ranked wallets"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))

            if (walletConnected) {
                Text(
                    text = cup.personalRank?.let { "Your rank • #$it" } ?: "Your rank • Unranked",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Weekly PB • ${cup.personalBestScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Projected weekly reward",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = cup.projectedRewardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = cup.projectedRewardDetail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Text(
                    text = "Connect your wallet and finish a Ranked run to enter this week’s Cup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                )
            }

            cup.previousRewardTitle?.let { reward ->
                Text(
                    text = buildString {
                        append("Last season • ")
                        cup.previousSeasonRank?.let { append("#$it • ") }
                        append(reward)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WeeklyCupSponsorCard(cup: WeeklyCupPreview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = if (cup.trustedSponsorConfig) "Weekly prize" else "Sponsor prize",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            when {
                cup.sponsoredPrizeActive -> {
                    Text(
                        text = cup.sponsoredPrizeLabel ?: cup.sponsoredPrizeStatus,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    cup.sponsorName?.let {
                        Text(
                            text = "Presented by $it",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (cup.trustedSponsorConfig) {
                        val publicFundingLabel = when (cup.fundingVerificationStatus) {
                            "VERIFIED" -> "Funding confirmed"
                            "REJECTED" -> "Prize unavailable"
                            else -> "Funding pending"
                        }
                        StatusPill(publicFundingLabel)
                        cup.placementAllocationLabel?.let { allocation ->
                            Text(
                                text = allocation,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = cup.sponsoredPrizeStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun CupPodium(rows: List<RunLeaderboardPreview>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { row ->
            val title = when (row.rank) {
                1 -> "CHAMPION"
                2 -> "2ND PLACE"
                else -> "3RD PLACE"
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    when {
                        row.rank == 1 -> MaterialTheme.colorScheme.tertiaryContainer
                        row.isCurrentUser -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = "#${row.rank} • $title",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = PublicProfileRules.avatarFor(row.avatarId).symbol,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    if (row.isCurrentUser) {
                        CurrentUserPill()
                    }
                    row.walletLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = row.score.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Combo ${row.bestCombo} • PERFECT ${row.perfectHits}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCupCountdown(endEpochMillis: Long): String {
    var now by remember(endEpochMillis) { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(endEpochMillis) {
        while (endEpochMillis > 0L && now < endEpochMillis) {
            now = System.currentTimeMillis()
            if (now < endEpochMillis) delay(30_000L)
        }
    }

    val remaining = (endEpochMillis - now).coerceAtLeast(0L)
    if (endEpochMillis <= 0L) return "Season time unavailable"
    if (remaining <= 0L) return "Season closing"

    val totalMinutes = remaining / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val minutes = totalMinutes % 60L

    return when {
        days > 0L -> "Ends in ${days}d ${hours}h ${minutes}m"
        hours > 0L -> "Ends in ${hours}h ${minutes}m"
        else -> "Ends in ${minutes}m"
    }
}

@Composable
private fun PersonalCompetitionCard(
    competition: RunCompetitionPreview,
    walletConnected: Boolean,
) {
    val responsive = rememberResponsiveUiSpec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("My Radiant Rush stats", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
    val responsive = rememberResponsiveUiSpec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            if (row.isCurrentUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        if (responsive.isCompact || responsive.hasLargeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RankBubble(row.rank)
                    PlayerAvatarBubble(row.avatarId)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = row.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            softWrap = true,
                        )
                        if (row.isCurrentUser) CurrentUserPill()
                    }
                }
                row.walletLabel?.let {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        softWrap = true,
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = row.score.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Combo ${row.bestCombo} • PERFECT ${row.perfectHits} • ${row.runsPlayed} ranked run${if (row.runsPlayed == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(responsive.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RankBubble(row.rank)
                PlayerAvatarBubble(row.avatarId)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = row.name,
                            style = MaterialTheme.typography.titleMedium,
                            softWrap = true,
                        )
                        if (row.isCurrentUser) CurrentUserPill()
                    }
                    row.walletLabel?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, softWrap = true)
                    }
                    Text(
                        text = "Combo ${row.bestCombo} • PERFECT ${row.perfectHits} • ${row.runsPlayed} ranked run${if (row.runsPlayed == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                    )
                }
                Text(
                    text = row.score.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun XpLeaderboardRow(row: LeaderboardPreview) {
    val responsive = rememberResponsiveUiSpec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            if (row.isCurrentUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        if (responsive.isCompact || responsive.hasLargeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RankBubble(row.rank)
                    PlayerAvatarBubble(row.avatarId)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = row.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            softWrap = true,
                        )
                        if (row.isCurrentUser) CurrentUserPill()
                    }
                }
                row.walletLabel?.let {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        softWrap = true,
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "${row.xp} XP • ${row.streak}-day streak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true,
                )
                StatusPill(row.tier)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(responsive.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RankBubble(row.rank)
                PlayerAvatarBubble(row.avatarId)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = row.name,
                            style = MaterialTheme.typography.titleMedium,
                            softWrap = true,
                        )
                        if (row.isCurrentUser) CurrentUserPill()
                    }
                    row.walletLabel?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, softWrap = true)
                    }
                    Text(
                        "${row.xp} XP • ${row.streak}-day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                    )
                }
                StatusPill(row.tier)
            }
        }
    }
}

@Composable
private fun PlayerAvatarBubble(avatarId: String) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = PublicProfileRules.avatarFor(avatarId).symbol,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun CurrentUserPill() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = "YOU",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
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
    if (responsive.isCompact || responsive.hasLargeText) {
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
                softWrap = true,
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
                softWrap = true,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                softWrap = true,
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
