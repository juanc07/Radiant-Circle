package com.thinkblox.radiantrush.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.RadiantCollectiblePreview
import com.thinkblox.radiantrush.data.RadiantRunPreview
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import androidx.compose.ui.platform.testTag

@Composable
fun RadiantRunLauncherCard(
    run: RadiantRunPreview,
    enabled: Boolean,
    walletConnected: Boolean,
    cupRunning: Boolean,
    onConnectWallet: () -> Unit,
    onPlay: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                )
                .padding(responsive.cardPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(modifier = Modifier.size(if (responsive.isTiny) 56.dp else 68.dp)) {
                        val r = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primary.copy(alpha = 0.95f), tertiary.copy(alpha = 0.45f), Color.Transparent),
                                center = center,
                                radius = r,
                            ),
                            radius = r,
                        )
                        drawCircle(color = primary, radius = r * 0.34f)
                        drawCircle(color = Color.White.copy(alpha = 0.75f), radius = r * 0.12f, center = center + Offset(-r * 0.12f, -r * 0.12f))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Radiant Rush",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "20 seconds. Hit green. Avoid red. Build FEVER.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RunMiniMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.ConfirmationNumber,
                        label = "Tickets",
                        value = run.totalPlayableTickets.toString(),
                    )
                    RunMiniMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Bolt,
                        label = "Best",
                        value = run.bestScore.toString(),
                    )
                    RunMiniMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Bolt,
                        label = "Vault",
                        value = "${run.collectionOwned}/${run.collectionTotal}",
                    )
                }

                if (cupRunning && !walletConnected) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = "Cup live • wallet needed for Ranked",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "You can still play Casual, but it won’t count in the Cup.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight),
                        enabled = enabled,
                        shape = RoundedCornerShape(18.dp),
                        onClick = onConnectWallet,
                    ) {
                        Icon(imageVector = Icons.Filled.AccountBalanceWallet, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        AdaptiveButtonText(
                            text = "Connect Wallet for Cup",
                            compactText = "Connect Wallet",
                            tinyText = "Connect",
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight)
                            .testTag(UiTestTags.RADIANT_RUN_PLAY),
                        enabled = enabled && run.canPlay,
                        shape = RoundedCornerShape(18.dp),
                        onClick = onPlay,
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        AdaptiveButtonText(
                            text = if (run.canPlay) "Play Casual • Won’t Count" else "Earn a Rush Ticket",
                            compactText = if (run.canPlay) "Play Casual" else "Earn Ticket",
                            tinyText = if (run.canPlay) "Casual" else "No Ticket",
                        )
                    }
                } else {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight)
                            .testTag(UiTestTags.RADIANT_RUN_PLAY),
                        enabled = enabled && run.canPlay,
                        shape = RoundedCornerShape(18.dp),
                        onClick = onPlay,
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        AdaptiveButtonText(
                            text = when {
                                !run.canPlay -> "Earn a Rush Ticket"
                                !walletConnected -> "Play Casual • 1 Ticket"
                                else -> "Play • 1 Ticket"
                            },
                            compactText = when {
                                !run.canPlay -> "Earn Ticket"
                                !walletConnected -> "Play Casual"
                                else -> "Play • 1 Ticket"
                            },
                            tinyText = when {
                                !run.canPlay -> "No Ticket"
                                !walletConnected -> "Casual"
                                else -> "Play • 1"
                            },
                        )
                    }
                }

                Text(
                    text = when {
                        !walletConnected && !cupRunning ->
                            "Casual now • connect a wallet anytime to unlock Ranked"
                        run.skrCasualRushTickets > 0 ->
                            "${run.rushTickets} standard • ${run.skrCasualRushTickets} SKR Casual"
                        else ->
                            "Rush Tickets come from quests and the Daily Chest"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RunMiniMetric(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RadiantVaultStrip(
    collection: List<RadiantCollectiblePreview>,
    run: RadiantRunPreview,
) {
    val responsive = rememberResponsiveUiSpec()
    val signatureShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 10.dp,
        bottomEnd = 28.dp,
        bottomStart = 10.dp,
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = signatureShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                        ),
                    ),
                )
                .padding(responsive.cardPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "RADIANT VAULT // COLLECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Keep what your runs reveal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            text = "${run.collectionOwned}/${run.collectionTotal} discovered • ${run.radiantShards} shards",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    ) {
                        Text(
                            text = "${(run.collectionProgress * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { run.collectionProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!run.lastRewardTitle.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 6.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 6.dp,
                        ),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "LATEST REVEAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = "${run.lastCapsuleTier ?: "Rush Capsule"} → ${run.lastRewardTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = "${run.lastRewardRarity ?: "Collectible"} • stays in your Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    collection.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowItems.forEach { item ->
                                CollectibleMiniCard(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectibleMiniCard(
    item: RadiantCollectiblePreview,
    modifier: Modifier = Modifier,
) {
    val container = if (item.discovered) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    }
    val tileShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 6.dp,
        bottomEnd = 18.dp,
        bottomStart = 6.dp,
    )
    Surface(
        modifier = modifier
            .heightIn(min = 138.dp),
        shape = tileShape,
        color = container,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.discovered) {
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                ),
                            )
                        } else {
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            )
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (item.discovered) item.symbol else "?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = if (item.discovered) item.title else "Unknown",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                softWrap = true,
            )
            Text(
                text = if (item.discovered) "${item.rarity} • x${item.count}" else item.rarity,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (item.discovered) {
                Text(
                    text = "PWR ${item.power}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
