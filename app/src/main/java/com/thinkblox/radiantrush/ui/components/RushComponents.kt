package com.thinkblox.radiantrush.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus


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
    val tiny = width <= 340 || fontScale >= 1.35f
    val compact = width <= 390 || fontScale >= 1.18f

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
    "Opening Wallet…" -> chooseLabel("Opening Wallet…", "Opening…", "Opening…")
    "Disconnect Wallet" -> chooseLabel("Disconnect Wallet", "Disconnect", "Disconnect")
    "Refresh Firebase Sync" -> chooseLabel("Refresh Firebase Sync", "Refresh Sync", "Refresh")
    "Open Radiant Rush" -> chooseLabel("Open Radiant Rush", "Open Rush", "Open")
    else -> text
}

fun ResponsiveUiSpec.proofLabel(text: String): String = when (text) {
    "Firestore proof" -> chooseLabel("Firestore", "Cloud", "Cloud")
    "MWA authorization" -> chooseLabel("MWA Auth", "Wallet", "Wallet")
    "MWA message signature" -> chooseLabel("Wallet Signature", "Signature", "Sign")
    "Devnet memo transaction" -> chooseLabel("Devnet Memo", "Memo TX", "Memo")
    "SKR balance check" -> chooseLabel("SKR Check", "SKR", "SKR")
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
            Text(
                text = value,
                style = if (responsive.isTiny) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                maxLines = 2,
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
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    enabled = actionEnabled && quest.status == QuestStatus.Ready,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = responsive.buttonHorizontalPadding,
                        vertical = 8.dp,
                    ),
                    colors = ButtonDefaults.buttonColors(),
                    onClick = onActionClick,
                ) {
                    AdaptiveButtonText(text = actionLabel)
                }
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
            maxLines = 2,
            softWrap = true,
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
        modifier = modifier,
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
            modifier = Modifier.padding(if (responsive.isTiny) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 8.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = if (unlocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ) {
                Icon(
                    modifier = Modifier.padding(14.dp),
                    imageVector = if (unlocked) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = if (unlocked) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = description,
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
