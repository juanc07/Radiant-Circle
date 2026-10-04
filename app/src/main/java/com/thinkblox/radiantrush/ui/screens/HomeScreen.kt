package com.thinkblox.radiantrush.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.audio.ProceduralGameAudioEngine
import com.thinkblox.radiantrush.audio.ProceduralGameAudioEngine.Cue
import com.thinkblox.radiantrush.data.CircleUiState
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.logic.DailyRadiancePresentationRules
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    circleState: CircleUiState,
    onRetryFirebase: () -> Unit,
    onOpenDailyRadiance: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenCircle: () -> Unit,
    onOpenCompete: () -> Unit,
    onOpenProfile: () -> Unit,
    onPlayRadiantRun: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val context = LocalContext.current
    val buildLabel = remember(context) {
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "unknown"
            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            "v$versionName ($versionCode)"
        }.getOrDefault("vunknown")
    }
    val user = uiState.user
    val avatar = PublicProfileRules.avatarFor(user.avatarId)
    val completed = uiState.completedQuestCount
    val total = uiState.quests.size.coerceAtLeast(1)
    val progress = (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val cup = uiState.runCompetition.weeklyCup

    Box(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfile)
                    .testTag(UiTestTags.HOME_PROFILE_ENTRY),
                shape = RoundedCornerShape(28.dp),
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
                            modifier = Modifier.size(if (responsive.isTiny) 56.dp else 64.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = avatar.symbol, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = "TODAY // RADIANT CIRCLE",
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
                                text = "${user.currentStreak}-day streak • Lv. ${user.level}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                text = "Tap to open your profile",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
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
            CirclePulseCard(
                circleState = circleState,
                onOpenCircle = onOpenCircle,
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Route,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = "$completed of $total complete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = if (completed >= total) {
                            "Daily plan complete. Your next activity can be social or competitive."
                        } else {
                            uiState.retention.nextActionTitle
                        },
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
                            text = if (completed >= total) "Review Daily Plan" else "Continue Daily Plan",
                            compactText = if (completed >= total) "Review Plan" else "Continue",
                            tinyText = "Plan",
                        )
                    }
                }
            }
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
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Weekly Radiant Cup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                text = cup.sponsoredPrizeLabel
                                    ?.takeIf { cup.sponsoredPrizeActive }
                                    ?: cup.cupStatusLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = cup.personalRank?.let { "Your rank #$it" } ?: "Play Ranked to join",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (cup.participantCount > 0) {
                            Text(
                                text = "${cup.participantCount} players",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight),
                        enabled = uiState.isFirebaseReady && uiState.radiantRun.canPlay && !uiState.walletActionInProgress,
                        shape = RoundedCornerShape(16.dp),
                        onClick = onPlayRadiantRun,
                    ) {
                        AdaptiveButtonText("Play Radiant Rush", compactText = "Play Rush", tinyText = "Play")
                    }
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight),
                        shape = RoundedCornerShape(16.dp),
                        onClick = onOpenCompete,
                    ) {
                        AdaptiveButtonText("Weekly Cup Details", compactText = "Cup Details", tinyText = "Cup")
                    }
                }
            }
        }
    }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = responsive.screenPadding,
                    bottom = 8.dp,
                ),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            tonalElevation = 3.dp,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                text = buildLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CirclePulseCard(
    circleState: CircleUiState,
    onOpenCircle: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val connections = circleState.connections
    val pending = circleState.incomingRequests.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (responsive.isTiny) 16.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Circle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = when {
                            pending > 0 -> "$pending new Spark${if (pending == 1) "" else "s"} waiting"
                            connections.isNotEmpty() -> "${connections.size} connection${if (connections.size == 1) "" else "s"} in your Circle"
                            else -> "Discover someone through Shared Sparks"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (connections.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    connections.take(4).forEach { spark ->
                        val friendAvatar = PublicProfileRules.avatarFor(spark.member.avatarId)
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = friendAvatar.symbol, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    if (connections.size > 4) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "+${connections.size - 4}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight)
                    .testTag(UiTestTags.HOME_OPEN_CIRCLE),
                shape = RoundedCornerShape(16.dp),
                onClick = onOpenCircle,
            ) {
                AdaptiveButtonText(
                    text = if (connections.isEmpty()) "Discover Your Circle" else "Open Circle",
                    compactText = if (connections.isEmpty()) "Discover" else "Open Circle",
                    tinyText = "Circle",
                )
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
    val haptics = LocalHapticFeedback.current
    val audio = remember { ProceduralGameAudioEngine() }
    val anticipation = remember(radiance.dayKey) { Animatable(0f) }
    val reveal = remember(radiance.dayKey) { Animatable(if (radiance.revealedToday) 1f else 0f) }
    val sparkBurst = remember(radiance.dayKey) { Animatable(1f) }
    var armedForReveal by remember(radiance.dayKey) { mutableStateOf(false) }

    DisposableEffect(audio) {
        onDispose { audio.release() }
    }

    LaunchedEffect(radiance.opening, radiance.revealedToday) {
        when {
            radiance.opening -> {
                armedForReveal = true
                reveal.snapTo(0f)
                sparkBurst.snapTo(1f)
                anticipation.snapTo(0f)
                audio.play(Cue.RadianceCharge)
                anticipation.animateTo(
                    1f,
                    animationSpec = tween(
                        durationMillis = DailyRadiancePresentationRules.CHARGE_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }

            radiance.revealedToday && armedForReveal -> {
                armedForReveal = false
                audio.play(Cue.RadianceReveal)
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                reveal.snapTo(0f)
                sparkBurst.snapTo(0f)
                coroutineScope {
                    launch {
                        reveal.animateTo(
                            1f,
                            animationSpec = tween(
                                durationMillis = DailyRadiancePresentationRules.REVEAL_MS,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                    launch {
                        sparkBurst.animateTo(
                            1f,
                            animationSpec = tween(
                                durationMillis = DailyRadiancePresentationRules.SPARK_BURST_MS,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                }
                anticipation.snapTo(0f)
            }

            radiance.revealedToday -> {
                // Returning to Today later should show the settled result without replaying SFX.
                anticipation.snapTo(0f)
                reveal.snapTo(1f)
                sparkBurst.snapTo(1f)
            }

            else -> {
                armedForReveal = false
                anticipation.snapTo(0f)
                reveal.snapTo(0f)
                sparkBurst.snapTo(1f)
            }
        }
    }

    val revealScale = if (radiance.revealedToday) {
        0.965f + reveal.value * 0.035f
    } else {
        1f + anticipation.value * 0.012f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = revealScale, scaleY = revealScale)
            .testTag(UiTestTags.DAILY_RADIANCE_CARD),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (radiance.opening || (radiance.revealedToday && sparkBurst.value < 1f)) {
                val glowColor = MaterialTheme.colorScheme.tertiary
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cx = size.width * 0.82f
                    val cy = size.height * 0.24f
                    if (radiance.opening) {
                        val p = anticipation.value
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = 0.18f * p),
                                    glowColor.copy(alpha = 0.06f * p),
                                    Color.Transparent,
                                ),
                                center = androidx.compose.ui.geometry.Offset(cx, cy),
                                radius = size.minDimension * (0.20f + 0.12f * p),
                            ),
                            radius = size.minDimension * (0.20f + 0.12f * p),
                            center = androidx.compose.ui.geometry.Offset(cx, cy),
                        )
                    } else {
                        val p = sparkBurst.value
                        val alpha = (1f - p).coerceIn(0f, 1f)
                        repeat(10) { index ->
                            val angle = PI * 2.0 * index / 10.0
                            val travel = size.minDimension * (0.05f + p * 0.22f)
                            val point = androidx.compose.ui.geometry.Offset(
                                cx + (cos(angle) * travel).toFloat(),
                                cy + (sin(angle) * travel).toFloat(),
                            )
                            drawCircle(
                                color = glowColor.copy(alpha = alpha * 0.72f),
                                radius = 2.2f + (index % 3) * 0.9f,
                                center = point,
                            )
                        }
                    }
                }
            }

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
                        modifier = Modifier
                            .size(42.dp)
                            .graphicsLayer(
                                scaleX = 1f + anticipation.value * 0.12f,
                                scaleY = 1f + anticipation.value * 0.12f,
                            ),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (radiance.opening) "✧" else "✦",
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
                            text = when {
                                radiance.opening -> "Gathering today's spark…"
                                radiance.revealedToday -> radiance.category
                                else -> "A small spark for your day"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (radiance.revealedToday) {
                    Column(
                        modifier = Modifier.graphicsLayer(
                            alpha = reveal.value,
                            translationY = (1f - reveal.value) * 10f,
                        ),
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
                        text = if (radiance.opening) {
                            "A quiet moment — your message is almost here."
                        } else if (radiance.currentStreak > 0) {
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
