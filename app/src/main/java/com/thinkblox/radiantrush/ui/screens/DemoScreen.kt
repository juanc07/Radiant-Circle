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
                body = "Start with your daily ritual, discover people through shared interests, chat with accepted Circle members, play Radiant Rush, and connect Solana when you want on-chain features.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.AccountCircle,
                "1. Protect your Circle identity",
                "Set your public name and avatar in You. Continue with Google to make your Radiant Circle account easier to restore after reinstalling. Your social identity stays separate from whichever wallet you connect.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.CheckCircle,
                "2. Begin with Daily Radiance",
                "Open your Daily Radiance from Home and build its own streak. Today also keeps your Daily Plan, Daily Radiant Chest, quests, and quick actions together.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.Bolt,
                "3. Shake to discover",
                "Open Circle and physically shake your phone. Discovery uses approximate location only, stays active for about two minutes, and quietly widens the search when nobody is nearby.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.AccountCircle,
                "4. Connect through Shared Sparks",
                "See optional public interests you already share, then Send Spark. When the other person accepts, they join Your Circle and you can view each other's public social profile.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.CheckCircle,
                "5. Chat with accepted Circle members",
                "Private 1-to-1 chat unlocks only after a mutual Circle connection. Chat includes unread and read state, typing presence, profile identity, and safety actions for report, block, or remove.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.Bolt,
                "6. Play Radiant Rush and fill your Vault",
                "Radiant Rush is the quick skill game inside Radiant Circle. Runs open score-based capsules that reveal collectibles for your 12-item Radiant Vault. Duplicate collectibles become Radiant Shards.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.AccountBalanceWallet,
                "7. Connect Solana when useful",
                "A wallet is optional for the social experience. Connect with Mobile Wallet Adapter for wallet proofs, SKR Passport, on-chain activity, and Ranked Weekly Cup entry. The first wallet used for Ranked is kept as that Cup's competition wallet.",
            )
        }
        item {
            GuideStep(
                Icons.Filled.EmojiEvents,
                "8. Build your presence",
                "Badges, Ranks, Radiance streaks, your public profile, Vault collection, and SKR Passport show the progress tied to your Radiant Circle identity.",
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
                    Text("Your Circle right now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "${uiState.user.displayName} • Lv. ${uiState.user.level} • ${uiState.user.xp} XP",
                        softWrap = true,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Radiance ${uiState.dailyRadiance.currentStreak}d • ${uiState.completedQuestCount}/${uiState.quests.size} Daily Plan complete",
                        softWrap = true,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Radiant Vault: ${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal} • ${uiState.radiantRun.radiantShards} shards",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
