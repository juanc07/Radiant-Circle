package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags

@Composable
fun DemoScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
) {
    val responsive = rememberResponsiveUiSpec()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.DEMO_SCREEN),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = 14.dp,
            end = responsive.screenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 12.dp else 16.dp),
    ) {
        item {
            SectionTitle(
                title = "How to play",
                body = "Build your daily streak, earn tickets, play Radiant Run, and grow your Passport.",
            )
        }
        item { GuideStep(Icons.Filled.AccountBalanceWallet, "1. Connect", "Connect your Solana wallet to unlock wallet quests.") }
        item { GuideStep(Icons.Filled.CheckCircle, "2. Finish quests", "Complete today’s quests to earn XP and Rush Tickets.") }
        item { GuideStep(Icons.Filled.Bolt, "3. Play Radiant Run", "Use a ticket, hit Radiant targets, avoid red Corruption, and build your combo.") }
        item { GuideStep(Icons.Filled.EmojiEvents, "4. Open your chest", "Finish all daily quests to unlock your Daily Radiant Chest.") }
        item { GuideStep(Icons.Filled.Token, "5. Grow your SKR Passport", "Liquid and active staked SKR unlock casual-play perks, chest boosts, and cosmetics.") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.padding(responsive.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Your progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${uiState.completedQuestCount}/${uiState.quests.size} quests • ${uiState.user.xp} XP • ${uiState.user.currentStreak}-day streak")
                    Text("SKR Passport: ${uiState.user.skrTier} • ${uiState.user.skrEligibleBalance}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun GuideStep(icon: ImageVector, title: String, body: String) {
    val responsive = rememberResponsiveUiSpec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(responsive.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
