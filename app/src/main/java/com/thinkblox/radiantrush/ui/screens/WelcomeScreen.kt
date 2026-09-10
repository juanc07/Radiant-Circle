package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.StatusPill
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

@Composable
fun WelcomeScreen(
    uiState: RushUiState,
    onEnterDemoShell: () -> Unit,
    onRetryFirebase: () -> Unit,
    onConnectWallet: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = responsive.screenPadding, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 14.dp else 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        modifier = Modifier.padding(11.dp),
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Column {
                    Text(
                        text = "Radiant Rush",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Phase 10 Game Layer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            GradientHeroCard(
                title = "Connect. Quest. Prove.",
                subtitle = "Real Solana proof quests now feed a native 20-second arcade run, capsule reveal, and persistent collectible Vault.",
                trailing = {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(78.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                        ) {
                            Icon(
                                modifier = Modifier.padding(18.dp),
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                            )
                        }
                    }
                },
            )

            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
                onRetry = onRetryFirebase,
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "What works in Phase 10",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    PhaseBullet("Real proof quests earn XP plus free Rush Tickets")
                    PhaseBullet("Radiant Run is a native 20-second Compose skill game")
                    PhaseBullet("Combo + FEVER scoring turns proof progress into gameplay")
                    PhaseBullet("Capsules reveal six persistent collectibles; duplicates become shards")
                    PhaseBullet("MWA signing/memo and read-only mainnet SKR stay isolated and safe")
                    PhaseBullet("Demo tab gives a 3-minute judge walkthrough")
                    if (responsive.isTiny || responsive.hasLargeText) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill("Firebase")
                            StatusPill("MWA")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill("Firebase: live")
                            StatusPill("MWA: proof")
                            StatusPill("Native game")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight)
                    .testTag(UiTestTags.WELCOME_OPEN_RUSH),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = responsive.buttonHorizontalPadding, vertical = 8.dp),
                onClick = onEnterDemoShell,
            ) {
                AdaptiveButtonText("Open Radiant Rush")
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight)
                    .testTag(UiTestTags.WELCOME_CONNECT_WALLET),
                shape = RoundedCornerShape(18.dp),
                enabled = uiState.isFirebaseReady && !uiState.walletActionInProgress && !uiState.isWalletConnected,
                onClick = onConnectWallet,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.size(8.dp))
                AdaptiveButtonText(
                    text = when {
                        uiState.walletActionInProgress -> "Opening Wallet…"
                        uiState.isWalletConnected -> "Wallet Connected"
                        else -> "Connect Wallet"
                    },
                )
            }
            Text(
                text = "Use Phantom Devnet for signing and memo. SKR scan is read-only mainnet. Rush Tickets/capsules are free app progression and never spend XP, SKR, or SOL.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PhaseBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
