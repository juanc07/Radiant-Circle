package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.StatusPill
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags

@Composable
fun DemoScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
) {
    val responsive = rememberResponsiveUiSpec()
    val walletDone = uiState.isWalletConnected
    val signedDone = uiState.quests.any { it.id == QuestIds.SIGN_DAILY_PROOF && it.status == QuestStatus.Completed }
    val memoDone = uiState.quests.any { it.id == QuestIds.ON_CHAIN_PROOF && it.status == QuestStatus.Completed }
    val skrDone = uiState.quests.any { it.id == QuestIds.SKR_HOLDER && it.status == QuestStatus.Completed }
    val completedCount = uiState.quests.count { it.status == QuestStatus.Completed }
    val progress = completedCount.toFloat() / uiState.quests.size.coerceAtLeast(1).toFloat()

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
            GradientHeroCard(
                title = "Judge Demo Mode",
                subtitle = "A safe 3-minute walkthrough: native Android UI, Firebase progress, MWA wallet proof, devnet memo, and read-only SKR Passport.",
            )
        }

        item {
            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
            )
        }

        item {
            DemoReadinessCard(
                progress = progress,
                completedCount = completedCount,
                totalCount = uiState.quests.size,
                walletDone = walletDone,
                signedDone = signedDone,
                memoDone = memoDone,
                skrDone = skrDone,
                skrTier = uiState.user.skrTier,
                skrBalance = uiState.user.skrBalance,
                streak = uiState.user.currentStreak,
                xp = uiState.user.xp,
            )
        }

        item {
            SectionTitle(
                title = "3-minute demo path",
                body = "Use this order in the recording. It shows the app is not a website wrapper and every proof stays inside safe Solana/Firebase boundaries.",
            )
        }

        item {
            DemoStepCard(
                number = "1",
                icon = Icons.Filled.CloudSync,
                title = "Open Radiant Rush",
                body = "Show Firebase connected, Today tab, and mobile-first quest cards. Mention Anonymous Auth + Firestore store progress.",
            )
        }
        item {
            DemoStepCard(
                number = "2",
                icon = Icons.Filled.AccountBalanceWallet,
                title = "Connect wallet",
                body = "Tap Wallet Ready. Phantom or another MWA wallet opens. Approve once; the app stores only the public address.",
            )
        }
        item {
            DemoStepCard(
                number = "3",
                icon = Icons.Filled.Security,
                title = "Sign Daily Proof",
                body = "Open Phantom for a message signature. Explain this proves wallet control without spending SOL.",
            )
        }
        item {
            DemoStepCard(
                number = "4",
                icon = Icons.Filled.Route,
                title = "Send Memo Proof",
                body = "Approve one devnet Memo transaction. This creates the on-chain proof signature used in Profile and Firebase.",
            )
        }
        item {
            DemoStepCard(
                number = "5",
                icon = Icons.Filled.Token,
                title = "Scan SKR Passport",
                body = "No wallet popup should appear. The app reads the connected public address on mainnet for official SKR balance and tier.",
            )
        }
        item {
            DemoStepCard(
                number = "6",
                icon = Icons.Filled.CheckCircle,
                title = "Show Profile proof",
                body = "Open Profile and show copied wallet address, signature, memo explorer link, SKR tier, XP, streak, and no-secret rule.",
            )
        }

        item {
            SectionTitle(
                title = "Judge talking points",
                body = "Keep this short and confident during the video.",
            )
        }

        item {
            TalkingPointsCard()
        }
    }
}

@Composable
private fun DemoReadinessCard(
    progress: Float,
    completedCount: Int,
    totalCount: Int,
    walletDone: Boolean,
    signedDone: Boolean,
    memoDone: Boolean,
    skrDone: Boolean,
    skrTier: String,
    skrBalance: String,
    streak: Int,
    xp: Int,
) {
    val responsive = rememberResponsiveUiSpec()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 12.dp),
        ) {
            Text(
                text = "Demo readiness",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = "$completedCount of $totalCount quests done • $xp XP • $streak-day streak",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 8.dp),
            )
            if (responsive.isCompact || responsive.hasLargeText) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(if (walletDone) "Wallet ready" else "Wallet needed")
                    StatusPill(if (signedDone) "Signed" else "Need sign")
                    StatusPill(if (memoDone) "Memo done" else "Need memo")
                    StatusPill(if (skrDone) "SKR scanned" else "Scan SKR")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(if (walletDone) "Wallet ready" else "Wallet needed")
                    StatusPill(if (signedDone) "Signed" else "Need sign")
                    StatusPill(if (memoDone) "Memo done" else "Need memo")
                    StatusPill(if (skrDone) "SKR scanned" else "Scan SKR")
                }
            }
            Text(
                text = "SKR Passport: $skrTier • $skrBalance. A zero-SKR wallet is still honest Explorer tier.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DemoStepCard(
    number: String,
    icon: ImageVector,
    title: String,
    body: String,
) {
    val responsive = rememberResponsiveUiSpec()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(if (responsive.isTiny) 38.dp else 44.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TalkingPointsCard() {
    val responsive = rememberResponsiveUiSpec()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TalkingPoint("Native Android Jetpack Compose app, not WebView.")
            TalkingPoint("Mobile Wallet Adapter connects and signs through the user wallet.")
            TalkingPoint("Devnet memo proof gives a real transaction signature for demo safety.")
            TalkingPoint("SKR is read from mainnet by public address only; no spending or fake token.")
            TalkingPoint("Firebase keeps daily XP, streaks, badges, and leaderboard sticky.")
            TalkingPoint("No seed phrase, private key, or reward authority is stored in the APK.")
        }
    }
}

@Composable
private fun TalkingPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
