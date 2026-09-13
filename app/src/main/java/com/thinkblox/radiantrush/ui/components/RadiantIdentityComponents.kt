package com.thinkblox.radiantrush.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Small, scalable visual identity primitives for badges and the SKR Passport.
 * Everything is drawn in Compose so the marks stay crisp at any density and
 * there is no bitmap asset that can become blurry on larger devices.
 */
enum class RadiantBadgeGlyph {
    Launch,
    Daily,
    Wallet,
    Proof,
    Chain,
    Skr,
    Chest,
    Run,
    Combo,
    Collector,
    Streak,
    Generic,
    ;

    companion object {
        fun forTitle(title: String): RadiantBadgeGlyph {
            val key = title.lowercase()
            return when {
                "launch" in key -> Launch
                "daily saver" in key || "daily ready" in key -> Daily
                "wallet" in key -> Wallet
                "daily proof" in key -> Proof
                "on-chain" in key || "on chain" in key -> Chain
                "skr" in key || "radiant" in key && "chest" !in key -> Skr
                "chest" in key -> Chest
                "first run" in key -> Run
                "combo" in key -> Combo
                "collector" in key || "vault" in key -> Collector
                "7-day" in key || "streak" in key -> Streak
                else -> Generic
            }
        }
    }
}

@Composable
fun RadiantBadgeCrest(
    glyph: RadiantBadgeGlyph,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val halo = if (unlocked) primary else onSurface.copy(alpha = 0.18f)
    val accent = if (unlocked) secondary else onSurface.copy(alpha = 0.32f)
    val ink = if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer else onSurface.copy(alpha = 0.48f)

    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        halo.copy(alpha = if (unlocked) 0.28f else 0.10f),
                        Color.Transparent,
                    ),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val outer = side * 0.44f
            val inner = side * 0.335f
            val stroke = side * 0.045f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (unlocked) primary.copy(alpha = 0.30f) else surface,
                        if (unlocked) tertiary.copy(alpha = 0.10f) else surface,
                    ),
                    center = center,
                    radius = outer,
                ),
                radius = outer,
                center = center,
            )
            drawCircle(
                color = halo.copy(alpha = if (unlocked) 0.88f else 0.40f),
                radius = outer,
                center = center,
                style = Stroke(width = stroke),
            )

            // Signature Radiant Circle eight-ray crown around every unlocked crest.
            if (unlocked) {
                repeat(8) { index ->
                    val angle = (index * 45.0 - 90.0) * PI / 180.0
                    val start = Offset(
                        center.x + cos(angle).toFloat() * outer * 0.78f,
                        center.y + sin(angle).toFloat() * outer * 0.78f,
                    )
                    val end = Offset(
                        center.x + cos(angle).toFloat() * outer * 1.02f,
                        center.y + sin(angle).toFloat() * outer * 1.02f,
                    )
                    drawLine(
                        color = accent.copy(alpha = 0.78f),
                        start = start,
                        end = end,
                        strokeWidth = stroke * 0.72f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            when (glyph) {
                RadiantBadgeGlyph.Launch -> {
                    drawCircle(color = ink, radius = inner * 0.35f, center = center)
                    repeat(4) { index ->
                        val angle = (index * 90.0) * PI / 180.0
                        drawLine(
                            color = ink,
                            start = Offset(
                                center.x + cos(angle).toFloat() * inner * 0.52f,
                                center.y + sin(angle).toFloat() * inner * 0.52f,
                            ),
                            end = Offset(
                                center.x + cos(angle).toFloat() * inner * 0.86f,
                                center.y + sin(angle).toFloat() * inner * 0.86f,
                            ),
                            strokeWidth = stroke * 0.78f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                RadiantBadgeGlyph.Daily -> {
                    val w = inner * 1.20f
                    val h = inner * 1.08f
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(stroke * 1.6f),
                        style = Stroke(width = stroke * 0.72f),
                    )
                    drawLine(
                        color = ink,
                        start = Offset(center.x - w * 0.28f, center.y + h * 0.06f),
                        end = Offset(center.x - w * 0.05f, center.y + h * 0.27f),
                        strokeWidth = stroke * 0.8f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = ink,
                        start = Offset(center.x - w * 0.05f, center.y + h * 0.27f),
                        end = Offset(center.x + w * 0.30f, center.y - h * 0.16f),
                        strokeWidth = stroke * 0.8f,
                        cap = StrokeCap.Round,
                    )
                }

                RadiantBadgeGlyph.Wallet -> {
                    val w = inner * 1.28f
                    val h = inner * 0.86f
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(stroke * 1.6f),
                        style = Stroke(width = stroke * 0.72f),
                    )
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(center.x + w * 0.12f, center.y - h * 0.18f),
                        size = Size(w * 0.46f, h * 0.36f),
                        cornerRadius = CornerRadius(stroke),
                        style = Stroke(width = stroke * 0.68f),
                    )
                    drawCircle(color = ink, radius = stroke * 0.44f, center = Offset(center.x + w * 0.31f, center.y))
                }

                RadiantBadgeGlyph.Proof -> {
                    val shield = Path().apply {
                        moveTo(center.x, center.y - inner * 0.72f)
                        lineTo(center.x + inner * 0.62f, center.y - inner * 0.38f)
                        lineTo(center.x + inner * 0.48f, center.y + inner * 0.44f)
                        lineTo(center.x, center.y + inner * 0.74f)
                        lineTo(center.x - inner * 0.48f, center.y + inner * 0.44f)
                        lineTo(center.x - inner * 0.62f, center.y - inner * 0.38f)
                        close()
                    }
                    drawPath(shield, color = ink, style = Stroke(width = stroke * 0.72f))
                    drawLine(
                        color = ink,
                        start = Offset(center.x - inner * 0.28f, center.y + inner * 0.02f),
                        end = Offset(center.x - inner * 0.05f, center.y + inner * 0.24f),
                        strokeWidth = stroke * 0.78f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = ink,
                        start = Offset(center.x - inner * 0.05f, center.y + inner * 0.24f),
                        end = Offset(center.x + inner * 0.32f, center.y - inner * 0.22f),
                        strokeWidth = stroke * 0.78f,
                        cap = StrokeCap.Round,
                    )
                }

                RadiantBadgeGlyph.Chain -> {
                    drawCircle(
                        color = ink,
                        radius = inner * 0.40f,
                        center = Offset(center.x - inner * 0.25f, center.y),
                        style = Stroke(width = stroke * 0.72f),
                    )
                    drawCircle(
                        color = ink,
                        radius = inner * 0.40f,
                        center = Offset(center.x + inner * 0.25f, center.y),
                        style = Stroke(width = stroke * 0.72f),
                    )
                }

                RadiantBadgeGlyph.Skr -> {
                    val gem = Path().apply {
                        moveTo(center.x, center.y - inner * 0.78f)
                        lineTo(center.x + inner * 0.66f, center.y)
                        lineTo(center.x, center.y + inner * 0.78f)
                        lineTo(center.x - inner * 0.66f, center.y)
                        close()
                    }
                    drawPath(gem, color = ink, style = Stroke(width = stroke * 0.76f))
                    drawLine(
                        color = ink,
                        start = Offset(center.x - inner * 0.34f, center.y),
                        end = Offset(center.x + inner * 0.34f, center.y),
                        strokeWidth = stroke * 0.55f,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = accent, radius = stroke * 0.58f, center = center)
                }

                RadiantBadgeGlyph.Chest -> {
                    val w = inner * 1.35f
                    val h = inner * 0.88f
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(center.x - w / 2f, center.y - h * 0.22f),
                        size = Size(w, h * 0.72f),
                        cornerRadius = CornerRadius(stroke * 1.1f),
                        style = Stroke(width = stroke * 0.72f),
                    )
                    drawArc(
                        color = ink,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - w / 2f, center.y - h * 0.62f),
                        size = Size(w, h * 0.76f),
                        style = Stroke(width = stroke * 0.72f),
                    )
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(center.x - stroke * 0.55f, center.y - stroke * 0.08f),
                        size = Size(stroke * 1.10f, stroke * 1.36f),
                        cornerRadius = CornerRadius(stroke * 0.32f),
                    )
                }

                RadiantBadgeGlyph.Run -> {
                    val bolt = Path().apply {
                        moveTo(center.x + inner * 0.10f, center.y - inner * 0.78f)
                        lineTo(center.x - inner * 0.42f, center.y + inner * 0.02f)
                        lineTo(center.x - inner * 0.05f, center.y + inner * 0.02f)
                        lineTo(center.x - inner * 0.22f, center.y + inner * 0.78f)
                        lineTo(center.x + inner * 0.48f, center.y - inner * 0.16f)
                        lineTo(center.x + inner * 0.08f, center.y - inner * 0.16f)
                        close()
                    }
                    drawPath(bolt, color = ink)
                }

                RadiantBadgeGlyph.Combo -> {
                    repeat(3) { index ->
                        val y = center.y + (index - 1) * inner * 0.34f
                        drawLine(
                            color = ink.copy(alpha = 0.72f + index * 0.12f),
                            start = Offset(center.x - inner * 0.46f, y + inner * 0.16f),
                            end = Offset(center.x, y - inner * 0.16f),
                            strokeWidth = stroke * 0.72f,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = ink.copy(alpha = 0.72f + index * 0.12f),
                            start = Offset(center.x, y - inner * 0.16f),
                            end = Offset(center.x + inner * 0.46f, y + inner * 0.16f),
                            strokeWidth = stroke * 0.72f,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                RadiantBadgeGlyph.Collector -> {
                    val positions = listOf(
                        Offset(center.x, center.y - inner * 0.42f),
                        Offset(center.x - inner * 0.40f, center.y + inner * 0.28f),
                        Offset(center.x + inner * 0.40f, center.y + inner * 0.28f),
                    )
                    positions.forEachIndexed { index, position ->
                        drawCircle(
                            color = if (index == 0) accent else ink,
                            radius = inner * if (index == 0) 0.19f else 0.16f,
                            center = position,
                        )
                    }
                }

                RadiantBadgeGlyph.Streak -> {
                    val flame = Path().apply {
                        moveTo(center.x, center.y + inner * 0.72f)
                        cubicTo(
                            center.x - inner * 0.62f,
                            center.y + inner * 0.34f,
                            center.x - inner * 0.34f,
                            center.y - inner * 0.16f,
                            center.x - inner * 0.08f,
                            center.y - inner * 0.72f,
                        )
                        cubicTo(
                            center.x + inner * 0.02f,
                            center.y - inner * 0.34f,
                            center.x + inner * 0.68f,
                            center.y - inner * 0.08f,
                            center.x + inner * 0.34f,
                            center.y + inner * 0.48f,
                        )
                        cubicTo(
                            center.x + inner * 0.20f,
                            center.y + inner * 0.70f,
                            center.x + inner * 0.06f,
                            center.y + inner * 0.76f,
                            center.x,
                            center.y + inner * 0.72f,
                        )
                        close()
                    }
                    drawPath(flame, color = ink)
                    drawCircle(color = accent, radius = inner * 0.16f, center = Offset(center.x, center.y + inner * 0.28f))
                }

                RadiantBadgeGlyph.Generic -> {
                    val star = Path()
                    repeat(10) { index ->
                        val radius = if (index % 2 == 0) inner * 0.72f else inner * 0.32f
                        val angle = (index * 36.0 - 90.0) * PI / 180.0
                        val point = Offset(
                            center.x + cos(angle).toFloat() * radius,
                            center.y + sin(angle).toFloat() * radius,
                        )
                        if (index == 0) star.moveTo(point.x, point.y) else star.lineTo(point.x, point.y)
                    }
                    star.close()
                    drawPath(star, color = ink)
                }
            }
        }
    }
}

@Composable
fun PassportCrestCard(
    tier: String,
    eligibleBalance: String,
    stakeBoostActive: Boolean,
    frameLabel: String,
    auraLabel: String,
    modifier: Modifier = Modifier,
) {
    val responsive = rememberResponsiveUiSpec()
    val container = MaterialTheme.colorScheme.primaryContainer
    val content = MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        if (responsive.isCompact || responsive.hasLargeText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RadiantBadgeCrest(
                    glyph = RadiantBadgeGlyph.Skr,
                    unlocked = tier != "Explorer",
                    modifier = Modifier.size(if (responsive.isTiny) 76.dp else 88.dp),
                )
                PassportTextBlock(
                    tier = tier,
                    eligibleBalance = eligibleBalance,
                    stakeBoostActive = stakeBoostActive,
                    frameLabel = frameLabel,
                    auraLabel = auraLabel,
                    centered = true,
                    contentColor = content,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(responsive.cardPadding),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadiantBadgeCrest(
                    glyph = RadiantBadgeGlyph.Skr,
                    unlocked = tier != "Explorer",
                    modifier = Modifier.size(94.dp),
                )
                PassportTextBlock(
                    modifier = Modifier.weight(1f),
                    tier = tier,
                    eligibleBalance = eligibleBalance,
                    stakeBoostActive = stakeBoostActive,
                    frameLabel = frameLabel,
                    auraLabel = auraLabel,
                    centered = false,
                    contentColor = content,
                )
            }
        }
    }
}

@Composable
private fun PassportTextBlock(
    tier: String,
    eligibleBalance: String,
    stakeBoostActive: Boolean,
    frameLabel: String,
    auraLabel: String,
    centered: Boolean,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (centered) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = tier,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            textAlign = textAlign,
            softWrap = true,
        )
        Text(
            text = eligibleBalance,
            style = MaterialTheme.typography.titleMedium,
            color = contentColor.copy(alpha = 0.90f),
            textAlign = textAlign,
            softWrap = true,
        )
        if (stakeBoostActive) {
            Text(
                text = "Guardian Stake Boost active",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = textAlign,
                softWrap = true,
            )
        }
        Text(
            text = "$frameLabel • $auraLabel",
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.76f),
            textAlign = textAlign,
            softWrap = true,
        )
    }
}
