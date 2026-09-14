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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
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
                title = "Welcome to Radiant Circle",
                body = "Your Solana Mobile identity, daily activity, progress, and games live together here. Radiant Rush is one part of the Circle — not the whole app.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.AccountCircle,
                "1. Make it yours",
                "Set your public name and icon in Me. This identity represents you around Radiant Circle.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.AccountBalanceWallet,
                "2. Connect when useful",
                "A wallet is optional for browsing and Daily Check-In. Connect it for wallet quests, SKR Passport, and Ranked Cup runs.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.CheckCircle,
                "3. Pick your Daily Plan",
                "Today keeps your quests, streak progress, Daily Radiant Chest, and quick actions in one place.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.Bolt,
                "4. Play inside the Circle",
                "Radiant Rush is a quick skill game inside Radiant Circle. Play Casual for fun, or connect a wallet for Ranked Cup entry.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.EmojiEvents,
                "5. Build your presence",
                "Badges, Ranks, your public profile, and SKR Passport cosmetics show the progress tied to your Circle identity.",
            )
        }
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
                    Text("Your Circle today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "${uiState.user.displayName} • Lv. ${uiState.user.level} • ${uiState.user.xp} XP",
                        softWrap = true,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "${uiState.user.currentStreak}-day streak • ${uiState.completedQuestCount}/${uiState.quests.size} Daily Plan complete",
                        softWrap = true,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "SKR Passport: ${uiState.user.skrTier}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        softWrap = true,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = uiState.user.skrEligibleBalance,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        softWrap = true,
                    )
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
