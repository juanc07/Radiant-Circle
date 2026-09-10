package com.thinkblox.radiantrush.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.logic.RadiantGameRules
import com.thinkblox.radiantrush.logic.RadiantRunResult
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import kotlinx.coroutines.delay
import kotlin.math.min
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

@Composable
fun RadiantRunScreen(
    uiState: RushUiState,
    onSubmitResult: (RadiantRunResult) -> Unit,
    onExit: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val haptics = LocalHapticFeedback.current
    var runCountBeforeSubmit by remember { mutableIntStateOf(uiState.radiantRun.totalRuns) }

    var phase by remember { mutableStateOf(RunPhase.Briefing) }
    var countdown by remember { mutableIntStateOf(3) }
    var timeLeft by remember { mutableIntStateOf(RadiantGameRules.RUN_SECONDS) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var radiantHits by remember { mutableIntStateOf(0) }
    var corruptedHits by remember { mutableIntStateOf(0) }
    var targetSerial by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf<RunTarget?>(null) }
    var popText by remember { mutableStateOf("READY") }
    var finalResult by remember { mutableStateOf<RadiantRunResult?>(null) }

    val fever = combo >= 5
    val rewardScale by animateFloatAsState(
        targetValue = if (phase == RunPhase.Reward) 1f else 0.82f,
        label = "rewardScale",
    )

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
        targetSerial = 0
        target = null
        popText = "GET READY"
        finalResult = null
        runCountBeforeSubmit = uiState.radiantRun.totalRuns
        phase = RunPhase.Countdown
    }

    fun submitFinalResult() {
        val result = finalResult ?: RadiantRunResult(
            score = score,
            maxCombo = maxCombo,
            radiantHits = radiantHits,
            corruptedHits = corruptedHits,
        ).also { finalResult = it }
        phase = RunPhase.Saving
        onSubmitResult(result)
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
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(650)
            }
            popText = "RUSH!"
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
                if (timeLeft <= 0) {
                    target = null
                    finalResult = RadiantRunResult(
                        score = score,
                        maxCombo = maxCombo,
                        radiantHits = radiantHits,
                        corruptedHits = corruptedHits,
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
                if (target?.corrupted == false && combo > 0) {
                    combo = 0
                    popText = "COMBO LOST"
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
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("RADIANT RUN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(
                    "Native Compose arcade mode • no image assets required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(uiState.radiantRun.rushTickets.toString(), fontWeight = FontWeight.Bold)
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
                onTap = { tap, width, height ->
                    val active = target
                    if (active != null) {
                        val center = Offset(width * active.x, height * active.y)
                        val radius = min(width, height) * 0.085f
                        val dx = tap.x - center.x
                        val dy = tap.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance <= radius * 1.3f) {
                            if (active.corrupted) {
                                score = (score - 140).coerceAtLeast(0)
                                combo = 0
                                corruptedHits += 1
                                popText = "CORRUPTED -140"
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                combo += 1
                                maxCombo = maxOf(maxCombo, combo)
                                radiantHits += 1
                                val comboGain = combo * 10
                                val feverGain = if (combo >= 5) 65 else 0
                                val gain = 100 + comboGain + feverGain
                                score += gain
                                popText = if (combo >= 5) "FEVER +$gain" else "+$gain"
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            target = spawnTarget()
                        } else {
                            combo = 0
                            popText = "MISS"
                        }
                    }
                },
            )
            RunPhase.Saving -> SavingPanel(
                result = finalResult ?: RadiantRunResult(score, maxCombo, radiantHits, corruptedHits),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("A real 20-second skill run", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(
                "Tap glowing Radiant energy before it moves. Ignore red Corruption. Chain 5 hits to enter FEVER and score faster.",
                style = MaterialTheme.typography.bodyLarge,
            )
            RunStatRow("Best score", uiState.radiantRun.bestScore.toString())
            RunStatRow("Runs finished", uiState.radiantRun.totalRuns.toString())
            RunStatRow("Collection", "${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal}")
            RunStatRow("Entry", "1 free Rush Ticket")
            Text(
                "Finishing saves one capsule reveal, XP, best score, and collectible progress to Firebase. Rush Tickets cannot be bought with SOL/SKR in this phase.",
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
    onTap: (Offset, Float, Float) -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GameMetric(Modifier.weight(1f), "TIME", "$timeLeft")
        GameMetric(Modifier.weight(1f), "SCORE", score.toString())
        GameMetric(Modifier.weight(1f), "COMBO", "x$combo")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                // Cheap native "graphics": animated-looking grid + radial glow targets.
                val gridColor = onSurface.copy(alpha = 0.06f)
                val step = size.width / 6f
                var x = step
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = step
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }

                target?.let { active ->
                    val center = Offset(size.width * active.x, size.height * active.y)
                    val radius = min(size.width, size.height) * 0.085f
                    val mainColor = if (active.corrupted) error else if (fever) tertiary else primary
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(mainColor.copy(alpha = 0.75f), mainColor.copy(alpha = 0.18f), Color.Transparent),
                            center = center,
                            radius = radius * 2.1f,
                        ),
                        radius = radius * 2.1f,
                        center = center,
                    )
                    drawCircle(mainColor, radius = radius, center = center)
                    drawCircle(Color.White.copy(alpha = 0.78f), radius = radius * 0.22f, center = center + Offset(-radius * 0.2f, -radius * 0.2f))
                    drawCircle(mainColor.copy(alpha = 0.75f), radius = radius * 1.28f, center = center, style = Stroke(width = 4f))
                    if (active.corrupted) {
                        drawLine(Color.White.copy(alpha = 0.9f), center + Offset(-radius * 0.35f, -radius * 0.35f), center + Offset(radius * 0.35f, radius * 0.35f), strokeWidth = 6f)
                        drawLine(Color.White.copy(alpha = 0.9f), center + Offset(radius * 0.35f, -radius * 0.35f), center + Offset(-radius * 0.35f, radius * 0.35f), strokeWidth = 6f)
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                shape = RoundedCornerShape(100.dp),
                color = if (fever) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            ) {
                Text(
                    text = if (fever) "⚡ FEVER x$combo • $popText" else popText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("● RADIANT = TAP", style = MaterialTheme.typography.labelSmall, color = primary)
                Text("✕ CORRUPT = IGNORE", style = MaterialTheme.typography.labelSmall, color = error)
                Text("BEST COMBO $maxCombo", style = MaterialTheme.typography.labelSmall)
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
            modifier = Modifier.padding(responsive.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (saving) CircularProgressIndicator()
            Text("RUN COMPLETE", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("${result.score} points • x${result.maxCombo} best combo", style = MaterialTheme.typography.titleLarge)
            Text(
                if (saving) "Saving score and opening your capsule…" else (message ?: "Save did not finish."),
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                RadiantGameRules.capsuleTierForScore(uiState.radiantRun.lastScore).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Box(
                modifier = Modifier
                    .size((118 * rewardScale).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = reward?.symbol ?: "✦",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                uiState.radiantRun.lastRewardRarity ?: "Reward",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                uiState.radiantRun.lastRewardTitle ?: "Radiant Collectible",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                if (duplicate) {
                    "Duplicate converted to +${uiState.radiantRun.lastRewardShards} Radiant Shards • +${uiState.radiantRun.lastRewardXp} XP"
                } else {
                    "NEW DISCOVERY • +${uiState.radiantRun.lastRewardXp} XP"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            reward?.let {
                Text(it.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            Text(
                "Score ${uiState.radiantRun.lastScore} • Best ${uiState.radiantRun.bestScore} • ${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal} collected",
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
                    if (uiState.radiantRun.canPlay) "Run Again • 1 Ticket" else "No Tickets Left",
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
