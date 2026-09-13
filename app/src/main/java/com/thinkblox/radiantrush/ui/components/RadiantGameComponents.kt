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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
                        text = if (run.canPlay) "Play • 1 Ticket" else "Earn a Rush Ticket",
                        compactText = if (run.canPlay) "Play • 1 Ticket" else "Earn Ticket",
                        tinyText = if (run.canPlay) "Play • 1" else "No Ticket",
                    )
                }

                Text(
                    text = if (run.skrCasualRushTickets > 0) {
                        "${run.rushTickets} Ranked-ready • ${run.skrCasualRushTickets} SKR Casual"
                    } else {
                        "Earn more Rush Tickets from quests and your Daily Chest."
                    },
                    style = MaterialTheme.typography.bodySmall,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Radiant Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${run.collectionOwned}/${run.collectionTotal} discovered • ${run.radiantShards} shards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${(run.collectionProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            LinearProgressIndicator(
                progress = { run.collectionProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(collection, key = { it.id }) { item ->
                    CollectibleMiniCard(item)
                }
            }
        }
    }
}

@Composable
private fun CollectibleMiniCard(item: RadiantCollectiblePreview) {
    val container = if (item.discovered) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    Surface(
        modifier = Modifier
            .width(132.dp)
            .heightIn(min = 154.dp),
        shape = RoundedCornerShape(20.dp),
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
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
        }
    }
}
