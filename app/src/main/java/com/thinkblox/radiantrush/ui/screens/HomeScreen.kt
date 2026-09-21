package com.thinkblox.radiantrush.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags

/**
 * Social-first landing screen for Radiant Circle.
 *
 * The launch foyer already introduces the brand. After entering, Home should
 * feel like "your place in the Circle", not a checklist. Daily actions live on
 * the Today tab and detailed wallet/SKR controls remain on Profile.
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
    onOpenDailyRadiance: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenCircle: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val user = uiState.user
    val avatar = PublicProfileRules.avatarFor(user.avatarId)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.HOME_SCREEN),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = if (responsive.isTiny) 10.dp else 14.dp,
            end = responsive.screenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 12.dp else 14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 30.dp,
                    topEnd = 10.dp,
                    bottomEnd = 30.dp,
                    bottomStart = 10.dp,
                ),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.82f),
                                ),
                            ),
                        )
                        .padding(if (responsive.isTiny) 16.dp else 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(if (responsive.isTiny) 58.dp else 68.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = avatar.symbol,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = "RADIANT CIRCLE // YOUR SIGNAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                            Text(
                                text = "${user.skrTier} • Lv. ${user.level}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                text = if (uiState.isWalletConnected) "Wallet signal linked" else "Your Circle is ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        if (uiState.firebaseStatus != FirebaseStatus.Ready) {
            item {
                SyncStatusCard(
                    status = uiState.firebaseStatus,
                    message = uiState.lastMessage,
                    onRetry = onRetryFirebase,
                )
            }
        }

        item {
            DailyRadianceCard(
                uiState = uiState,
                onReveal = onOpenDailyRadiance,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (responsive.isTiny) 16.dp else 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Find your Circle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "Shake to discover an adult Circle member through Shared Sparks. Approximate location only; mutual connection before chat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Clip,
                        )
                    }
                    FilledTonalButton(
                        modifier = Modifier.testTag(UiTestTags.HOME_OPEN_CIRCLE),
                        onClick = onOpenCircle,
                    ) {
                        Text("Open")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeStat(
                    modifier = Modifier.weight(1f),
                    value = "${user.currentStreak}d",
                    label = "Streak",
                )
                HomeStat(
                    modifier = Modifier.weight(1f),
                    value = "Lv. ${user.level}",
                    label = "Level",
                )
                HomeStat(
                    modifier = Modifier.weight(1f),
                    value = "${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal}",
                    label = "Collection",
                )
            }
        }

        item {
            WalletHomeCard(
                uiState = uiState,
                onConnectWallet = onConnectWallet,
                onDisconnectWallet = onDisconnectWallet,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (responsive.isTiny) 16.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Route,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Next up",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = uiState.retention.nextActionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }

                    Text(
                        text = uiState.retention.nextActionDetail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight),
                        shape = RoundedCornerShape(16.dp),
                        onClick = onOpenToday,
                    ) {
                        AdaptiveButtonText(
                            text = "View Today",
                            compactText = "Today",
                            tinyText = "Today",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyRadianceCard(
    uiState: RushUiState,
    onReveal: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val radiance = uiState.dailyRadiance
    val revealAlpha = remember(radiance.dayKey) { Animatable(1f) }

    LaunchedEffect(radiance.revealedToday) {
        if (radiance.revealedToday) {
            revealAlpha.snapTo(0.35f)
            revealAlpha.animateTo(1f, animationSpec = tween(durationMillis = 420))
        } else {
            revealAlpha.snapTo(1f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.DAILY_RADIANCE_CARD),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (responsive.isTiny) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "✦",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TODAY'S RADIANCE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (radiance.revealedToday) radiance.category else "A small spark for your day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (radiance.revealedToday) {
                Column(
                    modifier = Modifier.graphicsLayer(alpha = revealAlpha.value),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = radiance.message,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = radianceStreakLabel(
                            current = radiance.currentStreak,
                            longest = radiance.longestStreak,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f),
                    )
                    Text(
                        text = "Come back tomorrow for a new Radiance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.68f),
                    )
                }
            } else {
                Text(
                    text = if (radiance.currentStreak > 0) {
                        "Your ${radiance.currentStreak}-day Radiance streak is waiting for today's spark."
                    } else {
                        "Open one positive message each day — no score, no competition."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f),
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight)
                        .testTag(UiTestTags.DAILY_RADIANCE_REVEAL),
                    enabled = uiState.isFirebaseReady && !radiance.opening && !uiState.walletActionInProgress,
                    shape = RoundedCornerShape(16.dp),
                    onClick = onReveal,
                ) {
                    AdaptiveButtonText(
                        text = if (radiance.opening) "Opening…" else "Reveal Today's Radiance",
                        compactText = if (radiance.opening) "Opening…" else "Reveal Radiance",
                        tinyText = if (radiance.opening) "Opening…" else "Reveal",
                    )
                }
            }
        }
    }
}

private fun radianceStreakLabel(current: Int, longest: Int): String {
    val currentLabel = if (current == 1) "1-day Radiance streak" else "$current-day Radiance streak"
    val bestLabel = if (longest == 1) "Best: 1 day" else "Best: $longest days"
    return "$currentLabel • $bestLabel"
}

@Composable
private fun HomeStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WalletHomeCard(
    uiState: RushUiState,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val connected = uiState.isWalletConnected
    val canManageWallet = uiState.firebaseStatus == FirebaseStatus.Ready && !uiState.walletActionInProgress
    var showDisconnectConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (responsive.isTiny) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (connected) Icons.Filled.CheckCircle else Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (connected) "Wallet connected" else "Connect your wallet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (connected) {
                            shortWallet(uiState.user.walletAddress)
                        } else {
                            "Needed for the Weekly Cup, SKR Passport, and wallet proofs."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (connected) 1 else 2,
                        overflow = TextOverflow.Clip,
                    )
                }
            }

            if (connected) {
                // A filled, high-contrast action makes wallet management discoverable on Home.
                // Dark violet keeps the action visually distinct without implying destructive
                // data loss (the confirmation dialog still explains the reversible disconnect).
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    enabled = canManageWallet,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4B246D),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF4B246D).copy(alpha = 0.38f),
                        disabledContentColor = Color.White.copy(alpha = 0.68f),
                    ),
                    onClick = { showDisconnectConfirmation = true },
                ) {
                    AdaptiveButtonText(
                        text = if (uiState.walletActionInProgress) "Disconnecting…" else "Disconnect Wallet",
                        compactText = "Disconnect",
                        tinyText = "Disconnect",
                    )
                }
            } else {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    enabled = canManageWallet,
                    shape = RoundedCornerShape(16.dp),
                    onClick = onConnectWallet,
                ) {
                    AdaptiveButtonText(
                        text = if (uiState.walletActionInProgress) "Opening Wallet…" else "Connect Wallet",
                        compactText = "Connect Wallet",
                        tinyText = "Connect",
                    )
                }
            }
        }
    }

    if (showDisconnectConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = { Text("Disconnect wallet?") },
            text = {
                Text("Your Radiant Circle profile and earned rewards stay intact. Wallet-only features will be unavailable until you reconnect.")
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmation = false }) {
                    AdaptiveButtonText("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.walletActionInProgress,
                    onClick = {
                        showDisconnectConfirmation = false
                        onDisconnectWallet()
                    },
                ) {
                    AdaptiveButtonText("Disconnect")
                }
            },
        )
    }
}

private fun shortWallet(value: String): String {
    val clean = value.trim()
    if (clean.length <= 14) return clean
    return "${clean.take(6)}…${clean.takeLast(6)}"
}
