package com.thinkblox.radiantrush.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.audio.ProceduralGameAudioEngine
import com.thinkblox.radiantrush.audio.ProceduralGameAudioEngine.Cue
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.logic.RadiantGameRules
import com.thinkblox.radiantrush.logic.RadiantRunResult
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private enum class RunPhase {
    Briefing,
    Countdown,
    Playing,
    Saving,
    Reward,
}

private data class RunTarget(
    val x: Float,
    val y: Float,
    val corrupted: Boolean,
    val serial: Int,
)

private enum class HitKind {
    Radiant,
    Perfect,
    Corruption,
    Miss,
}

private data class HitFeedback(
    val x: Float,
    val y: Float,
    val kind: HitKind,
    val serial: Int,
)

private fun rewardRarityRank(rarity: String?): Int = when (rarity?.lowercase()) {
    "uncommon" -> 1
    "rare" -> 2
    "epic" -> 3
    "legendary" -> 4
    "mythic" -> 5
    else -> 0
}

@Composable
fun RadiantRunScreen(
    uiState: RushUiState,
    onSubmitResult: (RadiantRunResult) -> Unit,
    onExit: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val haptics = LocalHapticFeedback.current
    val audio = remember { ProceduralGameAudioEngine() }
    var audioEnabled by rememberSaveable { mutableStateOf(true) }
    var runCountBeforeSubmit by remember { mutableIntStateOf(uiState.radiantRun.totalRuns) }

    var phase by remember { mutableStateOf(RunPhase.Briefing) }
    var countdown by remember { mutableIntStateOf(3) }
    var timeLeft by remember { mutableIntStateOf(RadiantGameRules.RUN_SECONDS) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var radiantHits by remember { mutableIntStateOf(0) }
    var corruptedHits by remember { mutableIntStateOf(0) }
    var perfectHits by remember { mutableIntStateOf(0) }
    var targetSerial by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<RunTarget?>(null) }
    var popText by remember { mutableStateOf("READY") }
    var finalResult by remember { mutableStateOf<RadiantRunResult?>(null) }
    var feedbackSerial by remember { mutableIntStateOf(0) }
    var hitFeedback by remember { mutableStateOf<HitFeedback?>(null) }

    val fever = combo >= 5
    val rewardScale by animateFloatAsState(
        targetValue = if (phase == RunPhase.Reward) 1f else 0.82f,
        label = "rewardScale",
    )

    fun emitFeedback(x: Float, y: Float, kind: HitKind) {
        feedbackSerial += 1
        hitFeedback = HitFeedback(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f), kind, feedbackSerial)
    }

    fun spawnTarget(): RunTarget {
        targetSerial += 1
        val random = Random(targetSerial * 7_919 + score * 31 + timeLeft * 97)
        return RunTarget(
            x = 0.14f + random.nextFloat() * 0.72f,
            y = 0.16f + random.nextFloat() * 0.68f,
            corrupted = random.nextInt(100) < 22,
            serial = targetSerial,
        )
    }

    fun beginRun() {
        if (!uiState.radiantRun.canPlay || uiState.walletActionInProgress) return
        countdown = 3
        timeLeft = RadiantGameRules.RUN_SECONDS
        score = 0
        combo = 0
        maxCombo = 0
        radiantHits = 0
        corruptedHits = 0
        perfectHits = 0
        targetSerial = 0
        target = null
        popText = "GET READY"
        finalResult = null
        hitFeedback = null
        runCountBeforeSubmit = uiState.radiantRun.totalRuns
        phase = RunPhase.Countdown
    }

    fun submitFinalResult() {
        val result = finalResult ?: RadiantRunResult(
            score = score,
            maxCombo = maxCombo,
            radiantHits = radiantHits,
            corruptedHits = corruptedHits,
            perfectHits = perfectHits,
        ).also { finalResult = it }
        phase = RunPhase.Saving
        onSubmitResult(result)
    }

    DisposableEffect(audio) {
        onDispose { audio.release() }
    }

    LaunchedEffect(audioEnabled, phase, fever, timeLeft) {
        audio.setEnabled(audioEnabled)
        audio.setMusicActive(
            phase == RunPhase.Countdown || phase == RunPhase.Playing || phase == RunPhase.Reward,
        )
        audio.setIntensity(
            fever = phase == RunPhase.Playing && fever,
            finalRush = phase == RunPhase.Playing && timeLeft in 1..5,
        )
    }

    BackHandler {
        when (phase) {
            RunPhase.Playing, RunPhase.Countdown -> {
                target = null
                phase = RunPhase.Briefing
                popText = "RUN CANCELLED"
            }
            else -> onExit()
        }
    }

    LaunchedEffect(phase) {
        if (phase == RunPhase.Countdown) {
            for (value in 3 downTo 1) {
                countdown = value
                popText = value.toString()
                audio.play(Cue.Countdown, value)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(650)
            }
            popText = "RUSH!"
            audio.play(Cue.Go)
            target = spawnTarget()
            phase = RunPhase.Playing
        }
    }

    LaunchedEffect(phase) {
        if (phase == RunPhase.Playing) {
            while (timeLeft > 0 && phase == RunPhase.Playing) {
                delay(1_000)
                if (phase != RunPhase.Playing) break
                timeLeft -= 1
                if (timeLeft in 1..5) {
                    audio.play(Cue.FinalTick, timeLeft)
                }
                if (timeLeft <= 0) {
                    target = null
                    audio.play(Cue.RunComplete)
                    finalResult = RadiantRunResult(
                        score = score,
                        maxCombo = maxCombo,
                        radiantHits = radiantHits,
                        corruptedHits = corruptedHits,
                        perfectHits = perfectHits,
                    )
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    submitFinalResult()
                }
            }
        }
    }

    LaunchedEffect(phase, targetSerial) {
        if (phase == RunPhase.Playing) {
            val delayMs = if (fever) 430L else 620L
            delay(delayMs)
            if (phase == RunPhase.Playing && target?.serial == targetSerial) {
                // Let corrupted targets pass safely, but missing a Radiant target breaks combo.
                val expired = target
                if (expired?.corrupted == false) {
                    if (combo > 0) {
                        combo = 0
                        popText = "COMBO LOST"
                    }
                    emitFeedback(expired.x, expired.y, HitKind.Miss)
                    audio.play(Cue.Miss)
                }
                target = spawnTarget()
            }
        }
    }

    LaunchedEffect(phase, uiState.radiantRun.totalRuns, uiState.walletActionInProgress) {
        if (
            phase == RunPhase.Saving &&
            uiState.radiantRun.totalRuns > runCountBeforeSubmit &&
            !uiState.walletActionInProgress
        ) {
            phase = RunPhase.Reward
            popText = "CAPSULE OPEN!"
            audio.play(Cue.CapsuleOpen)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(phase, uiState.radiantRun.lastRewardRarity) {
        if (phase == RunPhase.Reward) {
            delay(360)
            val rarityRank = rewardRarityRank(uiState.radiantRun.lastRewardRarity)
            audio.play(Cue.RewardReveal, rarityRank)
        }
    }

    val pageBrush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.background,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBrush)
            .padding(
                start = responsive.screenPadding,
                end = responsive.screenPadding,
                top = 8.dp,
                bottom = 18.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (phase == RunPhase.Playing || phase == RunPhase.Countdown) {
                    target = null
                    phase = RunPhase.Briefing
                } else {
                    onExit()
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("RADIANT RUN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "20-second skill run",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { audioEnabled = !audioEnabled }) {
                Icon(
                    imageVector = if (audioEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    contentDescription = if (audioEnabled) "Mute game audio" else "Enable game audio",
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(uiState.radiantRun.totalPlayableTickets.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }

        when (phase) {
            RunPhase.Briefing -> BriefingPanel(
                uiState = uiState,
                onPlay = ::beginRun,
                onExit = onExit,
            )
            RunPhase.Countdown -> CountdownPanel(countdown)
            RunPhase.Playing -> GamePanel(
                modifier = Modifier.weight(1f),
                score = score,
                combo = combo,
                maxCombo = maxCombo,
                timeLeft = timeLeft,
                fever = fever,
                popText = popText,
                target = target,
                hitFeedback = hitFeedback,
                onTap = { tap, width, height ->
                    val active = target
                    if (active != null) {
                        val center = Offset(width * active.x, height * active.y)
                        val radius = min(width, height) * 0.085f
                        val dx = tap.x - center.x
                        val dy = tap.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance <= radius * 1.45f) {
                            if (active.corrupted) {
                                score = (score - 140).coerceAtLeast(0)
                                combo = 0
                                corruptedHits += 1
                                popText = "CORRUPTED -140"
                                emitFeedback(active.x, active.y, HitKind.Corruption)
                                audio.play(Cue.CorruptionHit)
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                val perfect = distance <= radius * 0.50f
                                combo += 1
                                maxCombo = maxOf(maxCombo, combo)
                                radiantHits += 1
                                if (perfect) perfectHits += 1
                                val comboGain = combo * 10
                                val feverGain = if (combo >= 5) 65 else 0
                                val perfectGain = if (perfect) 50 else 0
                                val gain = 100 + comboGain + feverGain + perfectGain
                                score += gain
                                popText = when {
                                    perfect -> "PERFECT +$gain"
                                    combo >= 5 -> "FEVER +$gain"
                                    else -> "+$gain"
                                }
                                emitFeedback(
                                    active.x,
                                    active.y,
                                    if (perfect) HitKind.Perfect else HitKind.Radiant,
                                )
                                audio.play(if (perfect) Cue.PerfectHit else Cue.RadiantHit, combo)
                                if (combo == 5) {
                                    audio.play(Cue.Fever)
                                } else if (combo >= 10 && combo % 5 == 0) {
                                    audio.play(Cue.ComboBurst, combo)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            target = spawnTarget()
                        } else {
                            combo = 0
                            popText = "MISS"
                            emitFeedback(
                                x = (tap.x / width).coerceIn(0f, 1f),
                                y = (tap.y / height).coerceIn(0f, 1f),
                                kind = HitKind.Miss,
                            )
                            audio.play(Cue.Miss)
                        }
                    }
                },
            )
            RunPhase.Saving -> SavingPanel(
                result = finalResult ?: RadiantRunResult(score, maxCombo, radiantHits, corruptedHits, perfectHits),
                message = uiState.lastMessage,
                saving = uiState.walletActionInProgress,
                onRetry = ::submitFinalResult,
                onExit = onExit,
            )
            RunPhase.Reward -> RewardPanel(
                uiState = uiState,
                rewardScale = rewardScale,
                onPlayAgain = {
                    if (uiState.radiantRun.canPlay) beginRun() else onExit()
                },
                onExit = onExit,
            )
        }
    }
}

@Composable
private fun BriefingPanel(
    uiState: RushUiState,
    onPlay: () -> Unit,
    onExit: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val rankedWithStandardTicket = uiState.isWalletConnected &&
        uiState.runCompetition.rankedAttemptsRemaining > 0 &&
        uiState.radiantRun.rushTickets > 0
    val usesSkrCasualTicket = !rankedWithStandardTicket && uiState.radiantRun.skrCasualRushTickets > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("20-second skill run", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Tap glowing Radiant targets. Aim for the center for PERFECT. Avoid red Corruption. Chain 5 hits for FEVER.",
                style = MaterialTheme.typography.bodyLarge,
            )
            RunStatRow("Best score", uiState.radiantRun.bestScore.toString())
            RunStatRow("Runs finished", uiState.radiantRun.totalRuns.toString())
            RunStatRow("Collection", "${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal}")
            RunStatRow(
                "Tickets",
                "${uiState.radiantRun.rushTickets} standard • ${uiState.radiantRun.skrCasualRushTickets} SKR casual",
            )
            RunStatRow(
                "Entry",
                when {
                    rankedWithStandardTicket -> "1 standard Rush Ticket"
                    usesSkrCasualTicket -> "1 SKR casual ticket"
                    else -> "1 standard Rush Ticket"
                },
            )
            RunStatRow(
                "Competition",
                when {
                    !uiState.isWalletConnected ->
                        "CASUAL • connect wallet to publish ranked score"
                    rankedWithStandardTicket ->
                        "RANKED • ${uiState.runCompetition.rankedAttemptsRemaining}/3 wallet attempts left"
                    uiState.runCompetition.rankedAttemptsRemaining > 0 && usesSkrCasualTicket ->
                        "CASUAL • SKR bonus tickets cannot fund ranked attempts"
                    else ->
                        "CASUAL • 3/3 ranked wallet attempts used today"
                },
            )
            RunStatRow(
                "Gameplay XP",
                "${uiState.runCompetition.dailyGameplayXpEarned}/${uiState.runCompetition.dailyGameplayXpCap} today",
            )
            Text(
                "SKR bonus tickets are Casual-only. Ranked runs stay skill-based.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                modifier = Modifier.fillMaxWidth().heightIn(min = responsive.buttonHeight),
                enabled = uiState.isFirebaseReady && uiState.radiantRun.canPlay && !uiState.walletActionInProgress,
                shape = RoundedCornerShape(18.dp),
                onClick = onPlay,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                AdaptiveButtonText(
                    if (uiState.radiantRun.canPlay) "Start Radiant Run" else "Earn a Rush Ticket",
                    compactText = if (uiState.radiantRun.canPlay) "Start Run" else "Earn Ticket",
                    tinyText = if (uiState.radiantRun.canPlay) "Start" else "No Ticket",
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = responsive.buttonHeight),
                shape = RoundedCornerShape(18.dp),
                onClick = onExit,
            ) {
                AdaptiveButtonText("Back to Today", compactText = "Back", tinyText = "Back")
            }
        }
    }
}

@Composable
private fun CountdownPanel(countdown: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = countdown.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Tap light. Avoid red.", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GamePanel(
    modifier: Modifier = Modifier,
    score: Int,
    combo: Int,
    maxCombo: Int,
    timeLeft: Int,
    fever: Boolean,
    popText: String,
    target: RunTarget?,
    hitFeedback: HitFeedback?,
    onTap: (Offset, Float, Float) -> Unit,
 ) {
    val responsive = rememberResponsiveUiSpec()
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val onSurface = MaterialTheme.colorScheme.onSurface
    val pastelRadiant = Color(0xFF9FFFE0)
    val pastelPerfect = Color(0xFFFFE7A3)
    val pastelCorruption = Color(0xFFFFA8B5)
    val pastelMiss = Color(0xFFCDBBFF)
    val motion = rememberInfiniteTransition(label = "radiantRunMotion")
    val targetPulse by motion.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "targetPulse",
    )
    val gridPhase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (fever) 800 else 1_450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gridPhase",
    )
    val burstProgress = remember { Animatable(1f) }
    LaunchedEffect(hitFeedback?.serial) {
        if (hitFeedback != null) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (hitFeedback.kind == HitKind.Perfect) 620 else 460,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GameMetric(Modifier.weight(1f), "TIME", "$timeLeft")
        GameMetric(Modifier.weight(1f), "SCORE", score.toString())
        GameMetric(Modifier.weight(1f), "COMBO", "x$combo")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val successful = hitFeedback?.kind == HitKind.Radiant || hitFeedback?.kind == HitKind.Perfect
                val strength = when {
                    !successful || combo < 10 -> 0f
                    combo >= 20 -> 11f
                    combo >= 15 -> 8f
                    else -> 5f
                }
                if (strength > 0f) {
                    val decay = 1f - burstProgress.value
                    translationX = sin(burstProgress.value.toDouble() * PI * 8.0).toFloat() * strength * decay
                    translationY = cos(burstProgress.value.toDouble() * PI * 6.0).toFloat() * strength * 0.45f * decay
                }
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = if (fever) {
                                listOf(tertiary.copy(alpha = 0.25f), primary.copy(alpha = 0.08f), Color.Transparent)
                            } else {
                                listOf(primary.copy(alpha = 0.12f), Color.Transparent)
                            },
                        ),
                    )
                    .pointerInput(target?.serial) {
                        detectTapGestures { tap -> onTap(tap, size.width.toFloat(), size.height.toFloat()) }
                    },
            ) {
                // Native Canvas juice: moving grid, pulsing targets, hit particles and impact flashes.
                val gridColor = onSurface.copy(alpha = if (fever) 0.09f else 0.055f)
                val step = size.width / 6f
                val shift = step * gridPhase
                var x = -step + shift
                while (x < size.width + step) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = -step + shift
                while (y < size.height + step) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                if (fever) {
                    // FEVER is a screen-state glow, not a target. Keep it rectangular so
                    // players never mistake decorative cyan circles for something tappable.
                    drawRect(
                        color = tertiary.copy(alpha = 0.16f),
                        style = Stroke(width = 5f),
                    )
                }

                target?.let { active ->
                    val center = Offset(size.width * active.x, size.height * active.y)
                    val baseRadius = min(size.width, size.height) * 0.085f
                    val radius = baseRadius * targetPulse
                    val mainColor = if (active.corrupted) error else if (fever) tertiary else primary
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(mainColor.copy(alpha = 0.75f), mainColor.copy(alpha = 0.18f), Color.Transparent),
                            center = center,
                            radius = radius * 2.25f,
                        ),
                        radius = radius * 2.25f,
                        center = center,
                    )
                    drawCircle(mainColor, radius = radius, center = center)
                    drawCircle(
                        Color.White.copy(alpha = 0.78f),
                        radius = radius * 0.22f,
                        center = center + Offset(-radius * 0.2f, -radius * 0.2f),
                    )
                    drawCircle(
                        mainColor.copy(alpha = 0.75f),
                        radius = radius * 1.28f,
                        center = center,
                        style = Stroke(width = 4f),
                    )
                    if (!active.corrupted) {
                        // Small inner ring communicates the PERFECT center-hit zone without text.
                        drawCircle(
                            Color.White.copy(alpha = 0.45f),
                            radius = radius * 0.50f,
                            center = center,
                            style = Stroke(width = 2.5f),
                        )
                    } else {
                        drawLine(
                            Color.White.copy(alpha = 0.9f),
                            center + Offset(-radius * 0.35f, -radius * 0.35f),
                            center + Offset(radius * 0.35f, radius * 0.35f),
                            strokeWidth = 6f,
                        )
                        drawLine(
                            Color.White.copy(alpha = 0.9f),
                            center + Offset(radius * 0.35f, -radius * 0.35f),
                            center + Offset(-radius * 0.35f, radius * 0.35f),
                            strokeWidth = 6f,
                        )
                    }
                }

                hitFeedback?.let { hit ->
                    val progress = burstProgress.value.coerceIn(0f, 1f)
                    if (progress < 1f) {
                        val center = Offset(size.width * hit.x, size.height * hit.y)
                        val alpha = (1f - progress).coerceIn(0f, 1f)
                        val base = min(size.width, size.height) * 0.085f
                        val particleColor = when (hit.kind) {
                            HitKind.Corruption -> error
                            HitKind.Perfect -> tertiary
                            HitKind.Miss -> onSurface.copy(alpha = 0.55f)
                            HitKind.Radiant -> primary
                        }

                        if (hit.kind != HitKind.Miss) {
                            val epicCombo = combo >= 10 && (hit.kind == HitKind.Radiant || hit.kind == HitKind.Perfect)
                            val count = when {
                                epicCombo && hit.kind == HitKind.Perfect -> 38
                                epicCombo -> 28
                                hit.kind == HitKind.Perfect -> 22
                                else -> 14
                            }
                            repeat(count) { index ->
                                val angle = (PI * 2.0 * index.toDouble() / count.toDouble()) +
                                    ((hit.serial % 7) * 0.08)
                                val variance = 0.78f + ((index * 37 + hit.serial * 11) % 31) / 100f
                                val travel = base * (0.65f + progress * 3.2f) * variance
                                val point = center + Offset(
                                    (cos(angle) * travel).toFloat(),
                                    (sin(angle) * travel).toFloat(),
                                )
                                drawCircle(
                                    color = particleColor.copy(alpha = alpha * 0.90f),
                                    radius = base * (0.055f + (index % 3) * 0.018f) * (1f - progress * 0.45f),
                                    center = point,
                                )
                            }

                            if (epicCombo) {
                                repeat(if (combo >= 15) 3 else 2) { ring ->
                                    drawCircle(
                                        color = particleColor.copy(alpha = alpha * (0.42f - ring * 0.10f)),
                                        radius = base * (1.15f + progress * (2.9f + ring * 0.75f)),
                                        center = center,
                                        style = Stroke(width = (5f - ring).coerceAtLeast(2f)),
                                    )
                                }
                            }
                        }

                        drawCircle(
                            color = particleColor.copy(alpha = alpha * 0.85f),
                            radius = base * (0.75f + progress * 2.35f),
                            center = center,
                            style = Stroke(width = if (hit.kind == HitKind.Perfect) 6f else 4f),
                        )

                        when (hit.kind) {
                            HitKind.Corruption -> drawRect(error.copy(alpha = alpha * 0.16f))
                            HitKind.Perfect -> drawRect(Color.White.copy(alpha = alpha * 0.08f))
                            else -> Unit
                        }
                    }
                }
            }

            val feedbackTextColor = when (hitFeedback?.kind) {
                HitKind.Radiant -> pastelRadiant
                HitKind.Perfect -> pastelPerfect
                HitKind.Corruption -> pastelCorruption
                HitKind.Miss -> pastelMiss
                null -> if (fever) pastelPerfect else MaterialTheme.colorScheme.onSurface
            }
            val feedbackSurfaceColor = when (hitFeedback?.kind) {
                HitKind.Radiant -> Color(0xFF153D36).copy(alpha = 0.92f)
                HitKind.Perfect -> Color(0xFF493C1D).copy(alpha = 0.94f)
                HitKind.Corruption -> Color(0xFF4B2028).copy(alpha = 0.94f)
                HitKind.Miss -> Color(0xFF30264A).copy(alpha = 0.94f)
                null -> MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .graphicsLayer {
                        val impact = 1f - burstProgress.value
                        scaleX = 1f + impact * if (combo >= 10) 0.13f else 0.08f
                        scaleY = 1f + impact * if (combo >= 10) 0.13f else 0.08f
                    },
                shape = RoundedCornerShape(100.dp),
                color = feedbackSurfaceColor,
            ) {
                Text(
                    text = if (fever) "⚡ FEVER x$combo • $popText" else popText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = feedbackTextColor,
                    fontWeight = FontWeight.ExtraBold,
                )
            }

            if (responsive.isCompact || responsive.hasLargeText) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("● RADIANT = HIT", style = MaterialTheme.typography.labelSmall, color = primary)
                    Text("◎ CENTER = PERFECT", style = MaterialTheme.typography.labelSmall, color = tertiary)
                    Text("✕ RED = AVOID", style = MaterialTheme.typography.labelSmall, color = error)
                }
            } else {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("● RADIANT = HIT", style = MaterialTheme.typography.labelSmall, color = primary)
                    Text("◎ CENTER = PERFECT", style = MaterialTheme.typography.labelSmall, color = tertiary)
                    Text("✕ RED = AVOID", style = MaterialTheme.typography.labelSmall, color = error)
                }
            }
        }
    }
}

@Composable
private fun SavingPanel(
    result: RadiantRunResult,
    message: String?,
    saving: Boolean,
    onRetry: () -> Unit,
    onExit: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (saving) CircularProgressIndicator()
            Text(
                text = "RUN COMPLETE",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${result.score} points • x${result.maxCombo} best combo",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (saving) "Saving score and opening your capsule…" else (message ?: "Save did not finish."),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!saving) {
                Button(
                    modifier = Modifier.fillMaxWidth().heightIn(min = responsive.buttonHeight),
                    onClick = onRetry,
                ) { AdaptiveButtonText("Retry Reward Save", compactText = "Retry Save", tinyText = "Retry") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().heightIn(min = responsive.buttonHeight),
                    onClick = onExit,
                ) { AdaptiveButtonText("Back to Today", compactText = "Back", tinyText = "Back") }
            }
        }
    }
}

@Composable
private fun RewardPanel(
    uiState: RushUiState,
    rewardScale: Float,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val reward = uiState.collection.firstOrNull { it.title == uiState.radiantRun.lastRewardTitle }
    val duplicate = uiState.radiantRun.lastRewardShards > 0
    val rarityRank = rewardRarityRank(uiState.radiantRun.lastRewardRarity)
    val rewardBurst = remember { Animatable(1f) }
    LaunchedEffect(uiState.radiantRun.lastRewardTitle, rarityRank) {
        rewardBurst.snapTo(0f)
        rewardBurst.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = if (rarityRank >= 3) 1_150 else 760,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    val rewardPastel = when (rarityRank) {
        1 -> Color(0xFFB7F0FF)
        2 -> Color(0xFFB9C8FF)
        3 -> Color(0xFFE5B8FF)
        4 -> Color(0xFFFFD59E)
        5 -> Color(0xFFFFB4E8)
        else -> Color(0xFFB8FFE2)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = RadiantGameRules.capsuleTierForScore(uiState.radiantRun.lastScore).uppercase(),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .size((170 * rewardScale).dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progress = rewardBurst.value.coerceIn(0f, 1f)
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val particleCount = 10 + rarityRank * 7
                    val maxTravel = min(size.width, size.height) * (0.28f + rarityRank * 0.045f)
                    repeat(particleCount) { index ->
                        val angle = PI * 2.0 * index / particleCount + (rarityRank * 0.17)
                        val variance = 0.72f + ((index * 29 + rarityRank * 13) % 35) / 100f
                        val travel = maxTravel * progress * variance
                        val point = center + Offset(
                            (cos(angle) * travel).toFloat(),
                            (sin(angle) * travel).toFloat(),
                        )
                        drawCircle(
                            color = rewardPastel.copy(alpha = alpha * 0.95f),
                            radius = (3.2f + (index % 4) * 1.4f) * (1f - progress * 0.45f),
                            center = point,
                        )
                    }
                    val shockwaves = if (rarityRank >= 3) 3 else 1
                    repeat(shockwaves) { ring ->
                        drawCircle(
                            color = rewardPastel.copy(alpha = alpha * (0.70f - ring * 0.16f)),
                            radius = min(size.width, size.height) * (0.16f + progress * (0.23f + ring * 0.09f)),
                            center = center,
                            style = Stroke(width = (5f - ring).coerceAtLeast(2f)),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(rewardPastel.copy(alpha = 0.72f), Color.Transparent),
                            ),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = reward?.symbol ?: "✦",
                        style = MaterialTheme.typography.displayLarge,
                        color = rewardPastel,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            Text(
                text = uiState.radiantRun.lastRewardRarity ?: "Reward",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = rewardPastel,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = uiState.radiantRun.lastRewardTitle ?: "Radiant Collectible",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (duplicate) {
                    "Duplicate converted to +${uiState.radiantRun.lastRewardShards} Radiant Shards • +${uiState.radiantRun.lastRewardXp} XP"
                } else {
                    "NEW DISCOVERY • +${uiState.radiantRun.lastRewardXp} XP"
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            reward?.let {
                Text(
                    text = it.description,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = "${uiState.runCompetition.lastRunMode ?: "Casual"} • +${uiState.runCompetition.lastRunPerformanceXp} performance XP • ${uiState.runCompetition.rankedAttemptsRemaining}/3 ranked attempts left",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Score ${uiState.radiantRun.lastScore} • Best ${uiState.radiantRun.bestScore} • ${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal} collected",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                modifier = Modifier.fillMaxWidth().heightIn(min = responsive.buttonHeight),
                enabled = uiState.radiantRun.canPlay,
                onClick = onPlayAgain,
            ) {
                AdaptiveButtonText(
                    if (uiState.radiantRun.canPlay) {
                        if (uiState.radiantRun.skrCasualRushTickets > 0 &&
                            (!uiState.isWalletConnected || uiState.runCompetition.rankedAttemptsRemaining == 0 || uiState.radiantRun.rushTickets == 0)
                        ) {
                            "Run Again • Casual"
                        } else {
                            "Run Again • 1 Ticket"
                        }
                    } else {
                        "No Tickets Left"
                    },
                    compactText = if (uiState.radiantRun.canPlay) "Run Again" else "No Tickets",
                    tinyText = if (uiState.radiantRun.canPlay) "Again" else "Done",
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = responsive.buttonHeight),
                onClick = onExit,
            ) { AdaptiveButtonText("Back to Today", compactText = "Back", tinyText = "Back") }
        }
    }
}

@Composable
private fun RunStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GameMetric(modifier: Modifier, label: String, value: String) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
