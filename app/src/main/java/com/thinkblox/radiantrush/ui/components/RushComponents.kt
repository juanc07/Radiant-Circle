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
    val tiny = width <= 360 || fontScale >= 1.35f
    // Treat normal phone widths (including ~400–430dp devices such as Seeker-class
    // hardware) as compact. Phone layouts should stack before text ever has to clip.
    val compact = width <= 480 || fontScale >= 1.18f

    return ResponsiveUiSpec(
        screenWidthDp = width,
        fontScale = fontScale,
        isCompact = compact,
        isTiny = tiny,
        hasLargeText = fontScale >= 1.18f,
        screenPadding = when {
            tiny -> 12.dp
            compact -> 16.dp
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
            tiny -> 12.sp
            compact -> 13.sp
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
    "Open Radiant Rush" -> chooseLabel("Open Radiant Rush", "Open Rush", "Open")
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
        text = responsive.chooseLabel(text, compactText, tinyText),
        maxLines = 1,
        overflow = TextOverflow.Clip,
        fontSize = responsive.navTextSize,
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
                    text = title,
                    style = if (responsive.isTiny) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = subtitle,
                    style = if (responsive.isTiny) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.86f),
                )
            }
            if (!responsive.isTiny) {
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
            val isSingleTokenValue = value.none { it.isWhitespace() }
            if (isSingleTokenValue) {
                // Never split or clip tier names such as “Explorer”. Start at the
                // intended display size, then step down only if the measured line
                // would overflow its actual card width/font-scale combination.
                val baseSize = when {
                    responsive.isTiny -> 20.sp
                    responsive.isCompact -> 22.sp
                    else -> 24.sp
                }
                var fittedSize by remember(value, responsive.screenWidthDp, responsive.fontScale) {
                    mutableStateOf(baseSize)
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = fittedSize),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && fittedSize.value > 12f) {
                            fittedSize = (fittedSize.value - 1f).sp
                        }
                    },
                )
            } else {
                Text(
                    text = value,
                    style = if (responsive.isTiny) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    maxLines = 3,
                    softWrap = true,
                    overflow = TextOverflow.Clip,
                )
            }
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
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (responsive.isTiny || responsive.hasLargeText) {
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
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
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
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
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
    var previousChestStatus by remember { androidx.compose.runtime.mutableStateOf(chest.status) }
    val motion = rememberInfiniteTransition(label = "chestIdle")
    val idlePulse by motion.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(780, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "chestPulse",
    )

    fun rarityRank(): Int = when (chest.lastRewardRarity?.lowercase()) {
        "uncommon" -> 1
        "rare" -> 2
        "epic" -> 3
        "legendary" -> 4
        "mythic" -> 5
        else -> 0
    }

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
                charge.snapTo(0.22f)
                lidOpen.snapTo(0f)
                burst.snapTo(0f)
                rewardLift.snapTo(0f)
                shakeOffset.snapTo(0f)
            }
            RadiantChestStatus.Opening -> {
                previousChestStatus = RadiantChestStatus.Opening
                audio.setMusicActive(true)
                audio.setIntensity(fever = false, finalRush = false)
                charge.snapTo(0.18f)
                lidOpen.snapTo(0f)
                burst.snapTo(0f)
                rewardLift.snapTo(0f)
                shakeOffset.snapTo(0f)
                audio.play(Cue.ChestCharge)
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                coroutineScope {
                    launch {
                        charge.animateTo(1f, animationSpec = tween(760, easing = FastOutSlowInEasing))
                    }
                    // A short damped shake reads as anticipation without the high-frequency
                    // micro-jitter caused by deriving position from the charge value.
                    shakeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 620
                            0f at 0
                            -3.5f at 90
                            3.0f at 180
                            -2.4f at 270
                            1.8f at 360
                            -1.0f at 450
                            0f at 620
                        },
                    )
                }
                audio.play(Cue.ChestOpen)
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                lidOpen.animateTo(1f, animationSpec = tween(340, easing = FastOutSlowInEasing))
                burst.animateTo(1f, animationSpec = tween(620, easing = FastOutSlowInEasing))
            }
            RadiantChestStatus.Claimed -> {
                shakeOffset.snapTo(0f)
                val revealNow = previousChestStatus == RadiantChestStatus.Opening
                previousChestStatus = RadiantChestStatus.Claimed
                audio.setMusicActive(false)
                lidOpen.snapTo(1f)
                if (revealNow) {
                    burst.snapTo(0f)
                    rewardLift.snapTo(0f)
                    audio.play(Cue.ChestReveal, rarityRank())
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    burst.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
                    rewardLift.animateTo(1f, animationSpec = tween(520, easing = FastOutSlowInEasing))
                } else {
                    burst.snapTo(1f)
                    rewardLift.snapTo(1f)
                }
            }
        }
    }

    val isOpening = chest.status == RadiantChestStatus.Opening
    val revealColor = when (rarityRank()) {
        1 -> Color(0xFF22C55E)
        2 -> Color(0xFF38BDF8)
        3 -> Color(0xFFA855F7)
        4 -> Color(0xFFF59E0B)
        5 -> Color(0xFFEC4899)
        else -> MaterialTheme.colorScheme.primary
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when (chest.status) {
                RadiantChestStatus.Ready -> MaterialTheme.colorScheme.primaryContainer
                RadiantChestStatus.Claimed -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 14.dp),
        ) {
            Text(
                text = chest.title,
                style = if (responsive.isTiny) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Clip,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (responsive.isTiny) 190.dp else 220.dp)
                    .graphicsLayer {
                        // Keep the card's geometry stable. Glow breathes inside the Canvas;
                        // only the deliberate damped anticipation offset moves the chest.
                        translationX = if (isOpening) shakeOffset.value else 0f
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.58f
                    val chestW = min(size.width * 0.62f, 260.dp.toPx())
                    val chestH = chestW * 0.48f
                    val bodyTop = cy - chestH * 0.12f
                    val bodyLeft = cx - chestW / 2f
                    val gold = Color(0xFFF6C453)
                    val darkGold = Color(0xFFB7791F)
                    val wood = Color(0xFF6B3F24)
                    val woodLight = Color(0xFF9A5A32)
                    val glow = if (chest.status == RadiantChestStatus.Claimed) revealColor else Color(0xFFFFD166)
                    val energy = when (chest.status) {
                        RadiantChestStatus.Ready -> 0.40f + (idlePulse - 0.97f) * 3.0f
                        RadiantChestStatus.Opening -> 0.25f + charge.value * 0.65f
                        RadiantChestStatus.Claimed -> (1f - burst.value) * 0.75f + 0.18f
                        else -> 0.08f
                    }.coerceIn(0f, 0.95f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = energy), glow.copy(alpha = energy * 0.20f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = chestW * 0.92f,
                        ),
                        radius = chestW * 0.92f,
                        center = Offset(cx, cy),
                    )

                    if (isOpening || chest.status == RadiantChestStatus.Claimed) {
                        val p = if (chest.status == RadiantChestStatus.Claimed) burst.value else burst.value
                        val alpha = (1f - p).coerceIn(0f, 1f)
                        if (p < 1f) {
                            repeat(36) { index ->
                                val angle = PI * 2.0 * index / 36.0 + index * 0.013
                                val travel = chestW * (0.22f + p * 0.82f) * (0.72f + (index % 7) * 0.055f)
                                val point = Offset(
                                    cx + (cos(angle) * travel).toFloat(),
                                    cy + (sin(angle) * travel * 0.72).toFloat(),
                                )
                                drawCircle(
                                    color = glow.copy(alpha = alpha * 0.92f),
                                    radius = (3.5f + (index % 4) * 1.7f) * (1f - p * 0.35f),
                                    center = point,
                                )
                            }
                            repeat(3) { ring ->
                                drawCircle(
                                    color = glow.copy(alpha = alpha * (0.55f - ring * 0.13f)),
                                    radius = chestW * (0.28f + p * (0.44f + ring * 0.12f)),
                                    center = Offset(cx, cy),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = (6f - ring).coerceAtLeast(2f)),
                                )
                            }
                        }
                    }

                    // Chest body
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
                    // Gold bands
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

                    // Lid rises during opening.
                    val lidLift = lidOpen.value * chestH * 0.50f
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

                    // Lock visibly pops away once the lid opens.
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

                    if (chest.status == RadiantChestStatus.Opening && charge.value > 0.62f) {
                        val beamAlpha = ((charge.value - 0.62f) / 0.38f).coerceIn(0f, 1f)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(glow.copy(alpha = beamAlpha * 0.52f), Color.Transparent),
                            ),
                            topLeft = Offset(cx - chestW * 0.18f, lidTop - chestH * 0.70f),
                            size = Size(chestW * 0.36f, chestH * 0.92f),
                            cornerRadius = CornerRadius(30f, 30f),
                        )
                    }
                }
            }

            Text(
                text = when (chest.status) {
                    RadiantChestStatus.Opening -> if (charge.value < 0.72f) "CHARGING…" else "OPENING!"
                    RadiantChestStatus.Claimed -> "${chest.lastRewardRarity ?: "Reward"} • ${chest.lastRewardTitle ?: "Daily reward"}"
                    else -> chest.subtitle
                },
                style = if (chest.status == RadiantChestStatus.Claimed) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                color = if (chest.status == RadiantChestStatus.Claimed) revealColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (chest.status == RadiantChestStatus.Claimed) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (chest.status == RadiantChestStatus.Claimed) {
                            translationY = (1f - rewardLift.value) * 28f
                            alpha = 0.35f + rewardLift.value * 0.65f
                            scaleX = 0.88f + rewardLift.value * 0.12f
                            scaleY = 0.88f + rewardLift.value * 0.12f
                        }
                    },
            )

            if (chest.status == RadiantChestStatus.Claimed) {
                Text(
                    text = chest.rewardText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = (1f - rewardLift.value) * 18f
                            alpha = 0.30f + rewardLift.value * 0.70f
                        },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = chest.progressText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
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
            maxLines = if (text.none { it.isWhitespace() }) 1 else 2,
            softWrap = text.any { it.isWhitespace() },
            overflow = TextOverflow.Clip,
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
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Clip,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 14.dp),
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
                Text(
                    text = status.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = message ?: status.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    AdaptiveButtonText("Retry")
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
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
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
                maxLines = 3,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 5,
                overflow = TextOverflow.Clip,
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
