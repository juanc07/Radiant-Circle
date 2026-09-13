package com.thinkblox.radiantrush.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinkblox.radiantrush.audio.ProceduralGameAudioEngine
import com.thinkblox.radiantrush.audio.ProceduralGameAudioEngine.Cue
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RadiantChestPreview
import com.thinkblox.radiantrush.data.RadiantChestStatus
import com.thinkblox.radiantrush.logic.RadiantChestPresentationRules
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


/**
 * Mobile-first sizing rules shared by screens and cards.
 *
 * The app must not rely on long fixed button labels or fixed desktop-like
 * spacing. These values respond to physical screen width and Android font scale
 * so small phones and larger accessibility text keep actions readable.
 */
data class ResponsiveUiSpec(
    val screenWidthDp: Int,
    val fontScale: Float,
    val isCompact: Boolean,
    val isTiny: Boolean,
    val hasLargeText: Boolean,
    val screenPadding: Dp,
    val cardPadding: Dp,
    val buttonHeight: Dp,
    val buttonHorizontalPadding: Dp,
    val buttonTextSize: TextUnit,
    val navTextSize: TextUnit,
)

@Composable
fun rememberResponsiveUiSpec(): ResponsiveUiSpec {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val width = configuration.screenWidthDp
    val fontScale = density.fontScale
    // Bias aggressively toward stacked phone layouts. The app has many dynamic
    // strings (wallet state, streaks, tiers, leaderboard identity), so a layout
    // should become compact before text is forced into clipping or awkward wraps.
    val tiny = width <= 380 || fontScale >= 1.30f
    val compact = width <= 600 || fontScale >= 1.15f

    return ResponsiveUiSpec(
        screenWidthDp = width,
        fontScale = fontScale,
        isCompact = compact,
        isTiny = tiny,
        hasLargeText = fontScale >= 1.15f,
        screenPadding = when {
            tiny -> 12.dp
            compact -> 14.dp
            else -> 20.dp
        },
        cardPadding = when {
            tiny -> 14.dp
            compact -> 16.dp
            else -> 18.dp
        },
        buttonHeight = when {
            tiny -> 62.dp
            compact -> 58.dp
            else -> 54.dp
        },
        buttonHorizontalPadding = when {
            tiny -> 10.dp
            compact -> 14.dp
            else -> 18.dp
        },
        buttonTextSize = when {
            tiny -> 13.sp
            compact -> 14.sp
            else -> 14.sp
        },
        navTextSize = when {
            tiny -> 9.sp
            compact -> 10.sp
            else -> 11.sp
        },
    )
}

fun ResponsiveUiSpec.chooseLabel(
    full: String,
    compact: String = full,
    tiny: String = compact,
): String = when {
    isTiny -> tiny
    isCompact || hasLargeText -> compact
    else -> full
}

fun ResponsiveUiSpec.actionLabel(text: String): String = when (text) {
    "Save Firebase Check-In" -> chooseLabel("Save Check-In", "Check In", "Check In")
    "Wallet Connected" -> chooseLabel("Wallet Connected", "Connected", "Done")
    "Connect Wallet" -> chooseLabel("Connect Wallet", "Connect", "Connect")
    "Signed Today" -> chooseLabel("Signed Today", "Signed", "Done")
    "Sign Daily Proof" -> chooseLabel("Sign Daily Proof", "Sign Proof", "Sign")
    "Memo Submitted" -> chooseLabel("Memo Submitted", "Memo Done", "Done")
    "Submit Memo Proof" -> chooseLabel("Submit Memo Proof", "Send Memo", "Memo")
    "Saving Check-In…" -> chooseLabel("Saving Check-In…", "Saving…", "Saving…")
    "Saving Wallet…" -> chooseLabel("Saving Wallet…", "Saving…", "Saving…")
    "Waiting for Signature…" -> chooseLabel("Waiting for Signature…", "Waiting…", "Wait…")
    "Opening Memo…" -> chooseLabel("Opening Memo…", "Opening…", "Opening…")
    "Saving Memo…" -> chooseLabel("Saving Memo…", "Saving…", "Saving…")
    "Opening Wallet…" -> chooseLabel("Opening Wallet…", "Opening…", "Opening…")
    "Disconnect Wallet" -> chooseLabel("Disconnect Wallet", "Disconnect", "Disconnect")
    "Refresh Firebase Sync" -> chooseLabel("Refresh", "Refresh", "Refresh")
    "Check SKR Balance" -> chooseLabel("Scan SKR Passport", "Scan SKR", "Scan")
    "SKR Checked" -> chooseLabel("SKR Checked", "SKR Done", "Done")
    "Scanning SKR…" -> chooseLabel("Scanning SKR…", "Scanning…", "Scan…")
    "Scanning SKR + stake…" -> chooseLabel("Scanning SKR + stake…", "Scanning SKR…", "Scanning…")
    "Checking SKR…" -> chooseLabel("Checking SKR…", "Checking…", "Checking…")
    "Open Radiant Circle" -> chooseLabel("Open Radiant Circle", "Open Circle", "Open")
    "Open Chest" -> chooseLabel("Open Daily Chest", "Open Chest", "Open")
    "Opening…" -> chooseLabel("Opening Chest…", "Opening…", "Open…")
    "Claimed Today" -> chooseLabel("Claimed Today", "Claimed", "Done")
    "Locked" -> chooseLabel("Locked", "Locked", "Lock")
    else -> text
}

fun ResponsiveUiSpec.proofLabel(text: String): String = when (text) {
    "Firestore proof" -> chooseLabel("Check-In", "Check-In", "Check-In")
    "MWA authorization" -> chooseLabel("Wallet Access", "Wallet", "Wallet")
    "MWA message signature" -> chooseLabel("Wallet Signature", "Signature", "Sign")
    "Devnet memo transaction" -> chooseLabel("Memo Proof", "Memo", "Memo")
    "SKR balance check" -> chooseLabel("SKR Check", "SKR", "SKR")
    "Mainnet SKR balance" -> chooseLabel("SKR Balance", "SKR", "SKR")
    else -> text
}

@Composable
fun AdaptiveButtonText(
    text: String,
    modifier: Modifier = Modifier,
    compactText: String? = null,
    tinyText: String? = null,
) {
    val responsive = rememberResponsiveUiSpec()
    val safeText = when {
        tinyText != null || compactText != null -> responsive.chooseLabel(
            full = text,
            compact = compactText ?: text,
            tiny = tinyText ?: compactText ?: text,
        )
        else -> responsive.actionLabel(text)
    }

    Text(
        modifier = modifier,
        text = safeText,
        maxLines = 2,
        softWrap = true,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        fontSize = responsive.buttonTextSize,
        fontWeight = FontWeight.SemiBold,
        lineHeight = responsive.buttonTextSize * 1.12f,
    )
}

@Composable
fun AdaptiveNavLabel(
    text: String,
    compactText: String,
    tinyText: String,
) {
    val responsive = rememberResponsiveUiSpec()
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = responsive.chooseLabel(text, compactText, tinyText),
        maxLines = 2,
        softWrap = true,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
        fontSize = responsive.navTextSize,
        lineHeight = responsive.navTextSize * 1.08f,
    )
}

@Composable
fun GradientHeroCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val responsive = rememberResponsiveUiSpec()

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        if (responsive.isCompact || responsive.hasLargeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = if (responsive.isTiny) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    softWrap = true,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = subtitle,
                    style = if (responsive.isTiny) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                    softWrap = true,
                )
                trailing?.invoke()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        softWrap = true,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                        softWrap = true,
                    )
                }
                trailing?.invoke()
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    supportingText: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val responsive = rememberResponsiveUiSpec()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 8.dp else 10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    modifier = Modifier
                        .padding(9.dp)
                        .size(20.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value,
                style = when {
                    responsive.isTiny -> MaterialTheme.typography.titleLarge
                    responsive.isCompact -> MaterialTheme.typography.headlineSmall
                    else -> MaterialTheme.typography.headlineSmall
                },
                softWrap = true,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun QuestCard(
    quest: QuestPreview,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionEnabled: Boolean = false,
    onClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    val responsive = rememberResponsiveUiSpec()

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(if (responsive.isTiny) 38.dp else 44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        modifier = Modifier.padding(if (responsive.isTiny) 8.dp else 10.dp),
                        imageVector = quest.status.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = quest.title,
                        style = if (responsive.isTiny) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        softWrap = true,
                    )
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (responsive.isCompact || responsive.hasLargeText) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusPill(text = "+${quest.xp} XP")
                        StatusPill(text = responsive.proofLabel(quest.proofType))
                    }
                    Text(
                        text = quest.status.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        softWrap = true,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(text = "+${quest.xp} XP")
                    StatusPill(text = responsive.proofLabel(quest.proofType))
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = quest.status.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        softWrap = true,
                    )
                }
            }
            if (actionLabel != null) {
                var lastAcceptedClickMs by remember(quest.id) { mutableLongStateOf(0L) }
                val buttonEnabled = actionEnabled && quest.status == QuestStatus.Ready
                val visibleLabel = when (quest.status) {
                    QuestStatus.Completed -> responsive.chooseLabel("Done Today", "Done", "Done")
                    QuestStatus.Syncing -> actionLabel
                    QuestStatus.Blocked -> responsive.chooseLabel("Connect Wallet First", "Connect First", "Wallet")
                    QuestStatus.Locked -> quest.status.label
                    QuestStatus.Ready -> actionLabel
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    enabled = buttonEnabled,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = responsive.buttonHorizontalPadding,
                        vertical = 8.dp,
                    ),
                    colors = ButtonDefaults.buttonColors(),
                    onClick = {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastAcceptedClickMs >= 900L) {
                            lastAcceptedClickMs = now
                            onActionClick()
                        }
                    },
                ) {
                    if (quest.status == QuestStatus.Syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    AdaptiveButtonText(text = visibleLabel)
                }
            }
        }
    }
}


@Composable
fun RadiantChestCard(
    chest: RadiantChestPreview,
    actionEnabled: Boolean,
    onClaim: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val responsive = rememberResponsiveUiSpec()
    val haptics = LocalHapticFeedback.current
    val audio = remember { ProceduralGameAudioEngine() }
    val charge = remember { Animatable(0f) }
    val lidOpen = remember { Animatable(if (chest.status == RadiantChestStatus.Claimed) 1f else 0f) }
    val burst = remember { Animatable(if (chest.status == RadiantChestStatus.Claimed) 1f else 0f) }
    val rewardLift = remember { Animatable(if (chest.status == RadiantChestStatus.Claimed) 1f else 0f) }
    val shakeOffset = remember { Animatable(0f) }
    var previousChestStatus by remember { mutableStateOf(chest.status) }
    val rarityRank = RadiantChestPresentationRules.rarityRank(chest.lastRewardRarity)
    val particleCount = RadiantChestPresentationRules.particleCount(chest.lastRewardRarity)
    val shockwaveCount = RadiantChestPresentationRules.shockwaveCount(chest.lastRewardRarity)
    val motion = rememberInfiniteTransition(label = "chestIdle")
    val idlePulse by motion.animateFloat(
        initialValue = 0.975f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chestPulse",
    )

    DisposableEffect(audio) {
        onDispose { audio.release() }
    }

    LaunchedEffect(chest.status) {
        when (chest.status) {
            RadiantChestStatus.Locked -> {
                previousChestStatus = RadiantChestStatus.Locked
                audio.setMusicActive(false)
                charge.snapTo(0f)
                lidOpen.snapTo(0f)
                burst.snapTo(0f)
                rewardLift.snapTo(0f)
                shakeOffset.snapTo(0f)
            }

            RadiantChestStatus.Ready -> {
                previousChestStatus = RadiantChestStatus.Ready
                audio.setMusicActive(false)
                charge.snapTo(0.18f)
                lidOpen.snapTo(0f)
                burst.snapTo(0f)
                rewardLift.snapTo(0f)
                shakeOffset.snapTo(0f)
            }

            RadiantChestStatus.Opening -> {
                previousChestStatus = RadiantChestStatus.Opening
                audio.setMusicActive(true)
                audio.setIntensity(fever = false, finalRush = false)
                charge.snapTo(0.12f)
                lidOpen.snapTo(0f)
                burst.snapTo(0f)
                rewardLift.snapTo(0f)
                shakeOffset.snapTo(0f)

                audio.play(Cue.ChestCharge)
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

                coroutineScope {
                    launch {
                        charge.animateTo(
                            1f,
                            animationSpec = tween(460, easing = FastOutSlowInEasing),
                        )
                    }
                    launch {
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 410
                                0f at 0
                                -2.2f at 80
                                2.0f at 150
                                -1.5f at 220
                                1.0f at 290
                                -0.5f at 350
                                0f at 410
                            },
                        )
                    }
                }

                audio.play(Cue.ChestOpen)
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                lidOpen.animateTo(
                    0.55f,
                    animationSpec = tween(170, easing = FastOutSlowInEasing),
                )
            }

            RadiantChestStatus.Claimed -> {
                shakeOffset.snapTo(0f)
                val revealNow = previousChestStatus == RadiantChestStatus.Opening
                previousChestStatus = RadiantChestStatus.Claimed
                audio.setMusicActive(false)

                if (revealNow) {
                    burst.snapTo(0f)
                    rewardLift.snapTo(0f)
                    audio.play(Cue.ChestReveal, rarityRank)
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)

                    coroutineScope {
                        launch {
                            lidOpen.animateTo(
                                1f,
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                            )
                        }
                        launch {
                            burst.animateTo(
                                1f,
                                animationSpec = tween(760, easing = FastOutSlowInEasing),
                            )
                        }
                        launch {
                            rewardLift.animateTo(
                                1f,
                                animationSpec = tween(440, easing = FastOutSlowInEasing),
                            )
                        }
                    }
                } else {
                    lidOpen.snapTo(1f)
                    burst.snapTo(1f)
                    rewardLift.snapTo(1f)
                }
            }
        }
    }

    val isOpening = chest.status == RadiantChestStatus.Opening
    val revealColor = when (rarityRank) {
        1 -> Color(0xFFA7E3C1)
        2 -> Color(0xFF9ECBF4)
        3 -> Color(0xFFCAB5F4)
        4 -> Color(0xFFFFD68A)
        5 -> Color(0xFFFFB5D2)
        else -> Color(0xFFAEDCF5)
    }
    val idleGlow = Color(0xFFFFD5A6)

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when (chest.status) {
                RadiantChestStatus.Ready -> MaterialTheme.colorScheme.primaryContainer
                RadiantChestStatus.Claimed -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 14.dp),
        ) {
            Text(
                text = chest.title,
                modifier = Modifier.fillMaxWidth(),
                style = if (responsive.isTiny) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Surface(
                shape = RoundedCornerShape(50.dp),
                color = when (chest.status) {
                    RadiantChestStatus.Ready -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    RadiantChestStatus.Opening -> idleGlow.copy(alpha = 0.22f)
                    RadiantChestStatus.Claimed -> revealColor.copy(alpha = 0.18f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                },
            ) {
                Text(
                    text = when (chest.status) {
                        RadiantChestStatus.Locked -> "DAILY REWARD • LOCKED"
                        RadiantChestStatus.Ready -> "DAILY REWARD • READY"
                        RadiantChestStatus.Opening -> "DAILY REWARD • OPENING"
                        RadiantChestStatus.Claimed -> "DAILY REWARD • SECURED"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (responsive.isTiny) 198.dp else 232.dp)
                    .graphicsLayer {
                        // Only the chest art moves, never the surrounding layout.
                        translationX = if (isOpening) shakeOffset.value else 0f
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.60f
                    val chestW = min(size.width * 0.62f, 260.dp.toPx())
                    val chestH = chestW * 0.48f
                    val bodyTop = cy - chestH * 0.12f
                    val bodyLeft = cx - chestW / 2f
                    val gold = Color(0xFFF3C96A)
                    val darkGold = Color(0xFFB9822E)
                    val wood = Color(0xFF6B3F2A)
                    val woodLight = Color(0xFF9C6040)
                    val glow = if (chest.status == RadiantChestStatus.Claimed) revealColor else idleGlow
                    val energy = when (chest.status) {
                        RadiantChestStatus.Ready -> 0.34f + (idlePulse - 0.975f) * 3.2f
                        RadiantChestStatus.Opening -> 0.25f + charge.value * 0.64f
                        RadiantChestStatus.Claimed -> 0.20f + (1f - burst.value) * 0.60f
                        else -> 0.07f
                    }.coerceIn(0f, 0.90f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glow.copy(alpha = energy),
                                glow.copy(alpha = energy * 0.24f),
                                Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = chestW * 0.95f,
                        ),
                        radius = chestW * 0.95f,
                        center = Offset(cx, cy),
                    )

                    // Stable ambient motes make the ready chest feel alive without moving layout.
                    if (chest.status != RadiantChestStatus.Locked) {
                        repeat(12) { index ->
                            val angle = PI * 2.0 * index / 12.0 + (idlePulse - 1f) * 2.4f
                            val orbit = chestW * (0.63f + (index % 3) * 0.07f)
                            val point = Offset(
                                cx + (cos(angle) * orbit).toFloat(),
                                cy + (sin(angle) * orbit * 0.62).toFloat(),
                            )
                            drawCircle(
                                color = glow.copy(alpha = 0.18f + (index % 4) * 0.035f),
                                radius = 2.4f + (index % 3) * 0.8f,
                                center = point,
                            )
                        }
                    }

                    if (chest.status == RadiantChestStatus.Claimed) {
                        val p = burst.value
                        val alpha = (1f - p).coerceIn(0f, 1f)

                        if (p < 1f) {
                            // Soft radial rays: higher rarities produce a denser, brighter reveal.
                            repeat(12 + rarityRank * 2) { index ->
                                val angle = PI * 2.0 * index / (12 + rarityRank * 2)
                                val inner = chestW * (0.30f + p * 0.12f)
                                val outer = chestW * (0.58f + p * 0.55f)
                                val start = Offset(
                                    cx + (cos(angle) * inner).toFloat(),
                                    cy + (sin(angle) * inner * 0.74).toFloat(),
                                )
                                val finish = Offset(
                                    cx + (cos(angle) * outer).toFloat(),
                                    cy + (sin(angle) * outer * 0.74).toFloat(),
                                )
                                drawLine(
                                    color = glow.copy(alpha = alpha * (0.22f + rarityRank * 0.035f)),
                                    start = start,
                                    end = finish,
                                    strokeWidth = 3f + rarityRank * 0.45f,
                                )
                            }

                            repeat(particleCount) { index ->
                                val angle = PI * 2.0 * index / particleCount + index * 0.011
                                val travel = chestW * (0.20f + p * 0.84f) *
                                    (0.72f + (index % 7) * 0.055f)
                                val point = Offset(
                                    cx + (cos(angle) * travel).toFloat(),
                                    cy + (sin(angle) * travel * 0.72).toFloat(),
                                )
                                drawCircle(
                                    color = glow.copy(alpha = alpha * 0.92f),
                                    radius = (3.2f + (index % 4) * 1.5f) * (1f - p * 0.30f),
                                    center = point,
                                )
                            }

                            repeat(shockwaveCount) { ring ->
                                drawCircle(
                                    color = glow.copy(
                                        alpha = alpha * (0.52f - ring * 0.085f).coerceAtLeast(0.16f),
                                    ),
                                    radius = chestW * (0.26f + p * (0.43f + ring * 0.10f)),
                                    center = Offset(cx, cy),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = (6f - ring * 0.7f).coerceAtLeast(2.5f),
                                    ),
                                )
                            }
                        }
                    }

                    // Chest body.
                    drawRoundRect(
                        color = wood,
                        topLeft = Offset(bodyLeft, bodyTop),
                        size = Size(chestW, chestH * 0.72f),
                        cornerRadius = CornerRadius(18f, 18f),
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(woodLight, wood)),
                        topLeft = Offset(bodyLeft + chestW * 0.04f, bodyTop + chestH * 0.07f),
                        size = Size(chestW * 0.92f, chestH * 0.50f),
                        cornerRadius = CornerRadius(14f, 14f),
                    )
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(bodyLeft + chestW * 0.12f, bodyTop),
                        size = Size(chestW * 0.09f, chestH * 0.72f),
                        cornerRadius = CornerRadius(8f, 8f),
                    )
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(bodyLeft + chestW * 0.79f, bodyTop),
                        size = Size(chestW * 0.09f, chestH * 0.72f),
                        cornerRadius = CornerRadius(8f, 8f),
                    )

                    val lidLift = lidOpen.value * chestH * 0.52f
                    val lidTop = bodyTop - chestH * 0.36f - lidLift
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(woodLight, wood)),
                        topLeft = Offset(bodyLeft, lidTop),
                        size = Size(chestW, chestH * 0.42f),
                        cornerRadius = CornerRadius(22f, 22f),
                    )
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(bodyLeft, lidTop + chestH * 0.29f),
                        size = Size(chestW, chestH * 0.10f),
                        cornerRadius = CornerRadius(6f, 6f),
                    )

                    if (lidOpen.value < 0.72f) {
                        val lockW = chestW * 0.17f
                        val lockH = chestH * 0.28f
                        drawRoundRect(
                            color = darkGold,
                            topLeft = Offset(cx - lockW / 2f, bodyTop + chestH * 0.15f),
                            size = Size(lockW, lockH),
                            cornerRadius = CornerRadius(10f, 10f),
                        )
                        drawCircle(
                            color = Color(0xFF3F2B1D),
                            radius = lockW * 0.10f,
                            center = Offset(cx, bodyTop + chestH * 0.26f),
                        )
                    }

                    if (chest.status == RadiantChestStatus.Opening && charge.value > 0.48f) {
                        val beamAlpha = ((charge.value - 0.48f) / 0.52f).coerceIn(0f, 1f)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    glow.copy(alpha = beamAlpha * 0.58f),
                                    glow.copy(alpha = beamAlpha * 0.16f),
                                    Color.Transparent,
                                ),
                            ),
                            topLeft = Offset(cx - chestW * 0.19f, lidTop - chestH * 0.92f),
                            size = Size(chestW * 0.38f, chestH * 1.14f),
                            cornerRadius = CornerRadius(32f, 32f),
                        )
                    }

                    // The revealed reward stays visible after the burst finishes.
                    if (chest.status == RadiantChestStatus.Claimed) {
                        val lift = rewardLift.value
                        val orbY = bodyTop - chestH * (0.33f + lift * 0.47f)
                        val orbRadius = chestW * (0.075f + rarityRank * 0.004f)

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.92f),
                                    glow.copy(alpha = 0.76f),
                                    glow.copy(alpha = 0.12f),
                                    Color.Transparent,
                                ),
                                center = Offset(cx, orbY),
                                radius = orbRadius * 3.2f,
                            ),
                            radius = orbRadius * 3.2f,
                            center = Offset(cx, orbY),
                        )
                        drawCircle(
                            color = glow,
                            radius = orbRadius,
                            center = Offset(cx, orbY),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.86f),
                            radius = orbRadius * 0.38f,
                            center = Offset(cx - orbRadius * 0.24f, orbY - orbRadius * 0.24f),
                        )
                    }
                }
            }

            when (chest.status) {
                RadiantChestStatus.Claimed -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = (1f - rewardLift.value) * 22f
                                alpha = 0.35f + rewardLift.value * 0.65f
                                scaleX = 0.94f + rewardLift.value * 0.06f
                                scaleY = 0.94f + rewardLift.value * 0.06f
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = revealColor.copy(alpha = 0.15f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (responsive.isTiny) 12.dp else 16.dp,
                                    vertical = if (responsive.isTiny) 12.dp else 16.dp,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = (chest.lastRewardRarity ?: "Reward").uppercase(),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelLarge,
                                color = revealColor,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = chest.lastRewardTitle ?: "Daily Radiant Reward",
                                modifier = Modifier.fillMaxWidth(),
                                style = if (responsive.isTiny) {
                                    MaterialTheme.typography.titleLarge
                                } else {
                                    MaterialTheme.typography.headlineSmall
                                },
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "+${chest.lastRewardXp} XP",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = RadiantChestPresentationRules.revealTagline(chest.lastRewardRarity),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            if (responsive.isCompact) {
                                if (chest.lastRewardStandardTickets > 0) {
                                    ChestRewardChip(
                                        text = "+${chest.lastRewardStandardTickets} Rush Ticket${if (chest.lastRewardStandardTickets == 1) "" else "s"}",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (chest.lastRewardSkrCasualTickets > 0) {
                                    ChestRewardChip(
                                        text = "+${chest.lastRewardSkrCasualTickets} SKR Casual Ticket${if (chest.lastRewardSkrCasualTickets == 1) "" else "s"}",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (chest.lastRewardSkrBonusXp > 0) {
                                    ChestRewardChip(
                                        text = "SKR Boost +${chest.lastRewardSkrBonusXp} XP",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (chest.lastRewardStandardTickets > 0) {
                                        ChestRewardChip(
                                            text = "+${chest.lastRewardStandardTickets} Rush Ticket${if (chest.lastRewardStandardTickets == 1) "" else "s"}",
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (chest.lastRewardSkrCasualTickets > 0) {
                                        ChestRewardChip(
                                            text = "+${chest.lastRewardSkrCasualTickets} SKR Casual",
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                if (chest.lastRewardSkrBonusXp > 0) {
                                    ChestRewardChip(
                                        text = "SKR Boost +${chest.lastRewardSkrBonusXp} XP",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }

                RadiantChestStatus.Opening -> {
                    Text(
                        text = if (charge.value < 0.66f) "RADIANCE BUILDING…" else "OPENING!",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Your reward is being secured.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                else -> {
                    Text(
                        text = chest.subtitle,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = chest.progressText,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (chest.status == RadiantChestStatus.Ready && chest.rewardText.isNotBlank()) {
                        Text(
                            text = chest.rewardText,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight),
                enabled = actionEnabled,
                shape = RoundedCornerShape(18.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = responsive.buttonHorizontalPadding,
                    vertical = 8.dp,
                ),
                onClick = onClaim,
            ) {
                if (isOpening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                AdaptiveButtonText(chest.buttonLabel)
            }
        }
    }
}

@Composable
private fun ChestRewardChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StatusPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    val responsive = rememberResponsiveUiSpec()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = if (responsive.isTiny) 10.dp else 12.dp,
                vertical = if (responsive.isTiny) 6.dp else 7.dp,
            ),
            text = text,
            maxLines = 3,
            softWrap = true,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun SectionTitle(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            softWrap = true,
        )
        if (body != null) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SyncStatusCard(
    status: FirebaseStatus,
    message: String?,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val responsive = rememberResponsiveUiSpec()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                FirebaseStatus.Ready -> MaterialTheme.colorScheme.primaryContainer
                FirebaseStatus.Loading -> MaterialTheme.colorScheme.surfaceVariant
                FirebaseStatus.NotConfigured, FirebaseStatus.Error -> MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        if (responsive.isCompact || responsive.hasLargeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = status.icon,
                        contentDescription = null,
                        tint = when (status) {
                            FirebaseStatus.NotConfigured, FirebaseStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = status.label,
                        style = MaterialTheme.typography.titleMedium,
                        softWrap = true,
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = message ?: status.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true,
                )
                if (onRetry != null) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRetry,
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        AdaptiveButtonText("Retry")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = status.icon,
                    contentDescription = null,
                    tint = when (status) {
                        FirebaseStatus.NotConfigured, FirebaseStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = status.label, style = MaterialTheme.typography.titleMedium, softWrap = true)
                    Text(
                        text = message ?: status.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                    )
                }
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        AdaptiveButtonText("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressCard(
    title: String,
    progress: Float,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val responsive = rememberResponsiveUiSpec()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    softWrap = true,
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(100.dp)),
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BadgeMedallion(
    title: String,
    description: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val responsive = rememberResponsiveUiSpec()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (responsive.isTiny) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 8.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RadiantBadgeCrest(
                glyph = RadiantBadgeGlyph.forTitle(title),
                unlocked = unlocked,
                modifier = Modifier.size(if (responsive.isTiny) 68.dp else 76.dp),
            )
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                softWrap = true,
            )
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                softWrap = true,
            )
        }
    }
}

@Composable
fun GlowOrb(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .backgroundBrush(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}

private fun Modifier.backgroundBrush(brush: Brush): Modifier = this.background(brush)
