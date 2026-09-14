package com.thinkblox.radiantrush.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.R
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags

/**
 * Quiet launch foyer for Radiant Circle.
 *
 * Keep the first impression deliberately sparse: brand, one short promise,
 * and one clear action. Product/dashboard information belongs behind Enter.
 */
@Composable
fun WelcomeScreen(
    onEnterDemoShell: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val logoSize = when {
        responsive.isTiny -> 152.dp
        responsive.isCompact -> 176.dp
        else -> 196.dp
    }

    val entranceAlpha = remember { Animatable(0f) }
    val entranceOffset = remember { Animatable(18f) }
    val infiniteTransition = rememberInfiniteTransition(label = "radiantEntrance")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoGlowPulse",
    )
    val logoBreath by infiniteTransition.animateFloat(
        initialValue = 0.992f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logoBreath",
    )
    val buttonBreath by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "buttonBreath",
    )

    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        )
        entranceOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.055f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                horizontal = responsive.screenPadding,
                vertical = if (responsive.isTiny) 18.dp else 28.dp,
            ),
    ) {
        // Soft ambient radiance behind the logo. It deliberately stays subtle
        // so the transparent artwork remains the visual focus.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (responsive.isTiny) (-18).dp else (-28).dp)
                .size(logoSize * 1.55f)
                .scale(glowPulse)
                .alpha(0.30f * glowPulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = entranceAlpha.value
                    translationY = entranceOffset.value
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.radiant_circle),
                    contentDescription = "Radiant Circle logo",
                    modifier = Modifier
                        .size(logoSize)
                        .scale(logoBreath),
                )

                Spacer(modifier = Modifier.size(if (responsive.isTiny) 18.dp else 24.dp))

                Text(
                    text = "Radiant Circle",
                    style = if (responsive.isTiny) {
                        MaterialTheme.typography.headlineMedium
                    } else {
                        MaterialTheme.typography.displaySmall
                    },
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "Connect. Play. Radiate.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(min = responsive.buttonHeight)
                .scale(buttonBreath)
                .testTag(UiTestTags.WELCOME_OPEN_RUSH),
            shape = RoundedCornerShape(22.dp),
            contentPadding = PaddingValues(
                horizontal = responsive.buttonHorizontalPadding,
                vertical = 10.dp,
            ),
            onClick = onEnterDemoShell,
        ) {
            AdaptiveButtonText(
                text = "Enter Radiant Circle",
                compactText = "Enter Circle",
                tinyText = "Enter",
            )
        }
    }
}
