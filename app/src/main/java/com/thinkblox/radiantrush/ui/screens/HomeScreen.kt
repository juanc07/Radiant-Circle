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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.MetricCard
import com.thinkblox.radiantrush.ui.components.ProgressCard
import com.thinkblox.radiantrush.ui.components.QuestCard
import com.thinkblox.radiantrush.ui.components.SectionTitle

@Composable
fun HomeScreen(contentPadding: PaddingValues) {
    val user = PreviewContent.user

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
                subtitle = "Connect, prove activity, and build your streak. Phase 1 is the polished app shell before Firebase and Solana wiring.",
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
                    supportingText = "Starts after real daily proof.",
                    icon = Icons.Filled.LocalFireDepartment,
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Level",
                    value = "Lv. ${user.level}",
                    supportingText = "XP persists in Phase 2.",
                    icon = Icons.Filled.Bolt,
                )
            }
        }

        item {
            ProgressCard(
                title = "Visitor → Radiant",
                progress = 0.12f,
                caption = "SKR holder boosts and Radiant tier logic arrive in Phase 5.",
            )
        }

        item {
            SectionTitle(
                title = "Priority quests",
                body = "These cards are static previews now. They become real wallet/Firebase flows in later phases.",
            )
        }

        items(PreviewContent.quests.size) { index ->
            QuestCard(quest = PreviewContent.quests[index])
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Build target: native Android APK first, then Firebase, then Mobile Wallet Adapter and real Solana proofs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
