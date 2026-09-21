package com.thinkblox.radiantrush.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.ApproximateCircleLocation
import com.thinkblox.radiantrush.data.CircleDiscoverySafetyPreferences
import com.thinkblox.radiantrush.data.CircleDiscoveryStatus
import com.thinkblox.radiantrush.data.CircleMemberPreview
import com.thinkblox.radiantrush.data.CircleMemberProfilePreview
import com.thinkblox.radiantrush.data.CircleSparkPreview
import com.thinkblox.radiantrush.data.CircleUiState
import com.thinkblox.radiantrush.location.ApproximateLocationProvider
import com.thinkblox.radiantrush.logic.CircleChatRules
import com.thinkblox.radiantrush.logic.CircleDiscoverySafetyRules
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.ui.components.ShakeToDiscoverEffect
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.audio.ChatSoundPlayer
import com.thinkblox.radiantrush.ui.audio.CircleInteractionSoundPlayer
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay

private fun playerFacingCircleMessage(raw: String): String {
    val text = raw.trim()
    if (text.isBlank()) return text
    val lower = text.lowercase()
    return when {
        "permission_denied" in lower ||
            "missing or insufficient permissions" in lower ||
            "firebasefirestore" in lower ->
            "Circle couldn't complete that right now. Refresh and try again."
        else -> text
    }
}

@Composable
fun CircleScreen(
    contentPadding: PaddingValues,
    state: CircleUiState,
    onStartDiscovery: (ApproximateCircleLocation) -> Unit,
    onDisableDiscovery: () -> Unit,
    onSendSpark: (CircleMemberPreview) -> Unit,
    onRespondToSpark: (CircleSparkPreview, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenProfile: (CircleMemberPreview) -> Unit,
    onCloseProfile: () -> Unit,
    onOpenChat: (CircleMemberPreview) -> Unit,
    onCloseChat: () -> Unit,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onRemoveConnection: (CircleMemberPreview) -> Unit,
    onBlockMember: (CircleMemberPreview) -> Unit,
    onReportMember: (CircleMemberPreview, String) -> Unit,
) {
    val context = LocalContext.current
    val responsive = rememberResponsiveUiSpec()
    val locationProvider = remember(context) {
        ApproximateLocationProvider(context.applicationContext)
    }
    var locating by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    val discoverySafetyPreferences = remember(context) {
        CircleDiscoverySafetyPreferences(context.applicationContext)
    }
    var discoverySafety by remember { mutableStateOf(discoverySafetyPreferences.snapshot()) }
    var showDiscoveryEligibilityDialog by remember { mutableStateOf(false) }
    var showDiscoveryPrivacyDialog by remember { mutableStateOf(false) }
    var enablingDiscoveryAwaitingPermission by remember { mutableStateOf(false) }

    val activeChatMember = state.chatMember
    if (activeChatMember != null) {
        CircleChatView(
            contentPadding = contentPadding,
            member = activeChatMember,
            messages = state.chatMessages,
            loading = state.chatLoading,
            sending = state.chatSending,
            statusMessage = state.chatStatusMessage,
            sentSequence = state.chatSentSequence,
            peerLastReadAtEpochMillis = state.chatPeerLastReadAtEpochMillis,
            peerTyping = state.chatPeerTyping,
            onBack = onCloseChat,
            onSendMessage = onSendMessage,
            onTypingChanged = onTypingChanged,
            onBlock = { onBlockMember(activeChatMember) },
            onReport = { reason -> onReportMember(activeChatMember, reason) },
        )
        return
    }

    if (state.profileLoading || state.selectedMemberProfile != null) {
        CircleMemberProfileView(
            contentPadding = contentPadding,
            profile = state.selectedMemberProfile,
            loading = state.profileLoading,
            onBack = onCloseProfile,
            onMessage = { member -> onOpenChat(member) },
            onRemove = onRemoveConnection,
            onBlock = onBlockMember,
            onReport = onReportMember,
            actionInProgress = state.actionInProgress,
        )
        return
    }

    fun locateAndDiscover() {
        if (locating || state.actionInProgress || state.discoveryStatus == CircleDiscoveryStatus.Searching) return
        locating = true
        localMessage = "Finding your approximate area…"
        locationProvider.requestCurrentLocation { result ->
            locating = false
            when (result) {
                is ApproximateLocationProvider.Result.Success -> {
                    localMessage = null
                    onStartDiscovery(result.location)
                }
                is ApproximateLocationProvider.Result.Failure -> {
                    localMessage = result.message
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            enablingDiscoveryAwaitingPermission = false
            locateAndDiscover()
        } else {
            if (enablingDiscoveryAwaitingPermission) {
                discoverySafety = discoverySafetyPreferences.setDiscoveryEnabled(false)
                enablingDiscoveryAwaitingPermission = false
                onDisableDiscovery()
            }
            localMessage = "Discovery stayed off because approximate location wasn't allowed."
        }
    }

    fun beginDiscovery() {
        if (!CircleDiscoverySafetyRules.canDiscover(
                adultConfirmed = discoverySafety.adultConfirmed,
                discoveryEnabled = discoverySafety.discoveryEnabled,
            )
        ) {
            showDiscoveryEligibilityDialog = true
            return
        }
        if (locationProvider.hasPermission()) {
            locateAndDiscover()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun activateDiscovery() {
        if (locationProvider.hasPermission()) {
            locateAndDiscover()
        } else {
            enablingDiscoveryAwaitingPermission = true
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun setDiscoveryVisibility(enabled: Boolean) {
        if (enabled && !discoverySafety.adultConfirmed) {
            showDiscoveryEligibilityDialog = true
            return
        }
        discoverySafety = discoverySafetyPreferences.setDiscoveryEnabled(enabled)
        if (enabled) {
            localMessage = "Discovery is on. You stay discoverable until you turn it off."
            activateDiscovery()
        } else {
            localMessage = "You are hidden from Shake to Discover."
            onDisableDiscovery()
        }
    }

    val interactionSoundPlayer = remember(context) {
        CircleInteractionSoundPlayer(context.applicationContext)
    }
    DisposableEffect(interactionSoundPlayer) {
        onDispose { interactionSoundPlayer.release() }
    }

    ShakeToDiscoverEffect(
        enabled = !locating && !state.actionInProgress && state.discoveryStatus != CircleDiscoveryStatus.Searching,
        onShake = {
            interactionSoundPlayer.playShake()
            beginDiscovery()
        },
    )

    if (showDiscoveryEligibilityDialog) {
        AlertDialog(
            onDismissRequest = { showDiscoveryEligibilityDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ready to discover?",
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "A safer way to meet through Shared Sparks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiscoverySafetyPoint(
                        symbol = "◎",
                        title = "Approximate only",
                        detail = "Location is used only when you choose to discover.",
                    )
                    DiscoverySafetyPoint(
                        symbol = "◉",
                        title = "You control visibility",
                        detail = "Stay discoverable until you switch discovery off.",
                    )
                    DiscoverySafetyPoint(
                        symbol = "✓",
                        title = "Mutual chat",
                        detail = "Messaging unlocks only after both people connect.",
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "18+ only",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "By enabling discovery, you confirm that you're 18 or older.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        discoverySafety = discoverySafetyPreferences.confirmAdultAndEnable()
                        localMessage = "Discovery is on. You stay discoverable until you turn it off."
                        showDiscoveryEligibilityDialog = false
                        activateDiscovery()
                    },
                ) {
                    Text("Enable discovery")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscoveryEligibilityDialog = false }) {
                    Text("Not now")
                }
            },
        )
    }

    if (showDiscoveryPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showDiscoveryPrivacyDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Privacy & safety",
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "You stay in control of discovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiscoverySafetyPoint(
                        symbol = "◎",
                        title = "Location",
                        detail = "Approximate and foreground-only. Exact coordinates aren't shown or stored in discovery.",
                    )
                    DiscoverySafetyPoint(
                        symbol = "◉",
                        title = "Visibility",
                        detail = "When enabled, you stay discoverable until you turn it off. Your last approximate area is refreshed when you actively use discovery; there is no background location tracking.",
                    )
                    DiscoverySafetyPoint(
                        symbol = "♡",
                        title = "Connections",
                        detail = "Shared Sparks first. Private chat requires mutual acceptance.",
                    )
                    HorizontalDivider()
                    Text(
                        text = "Safety tools",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Block • Report • Remove connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "No harassment, exploitation, threats, impersonation, or spam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiscoveryPrivacyDialog = false }) {
                    Text("Got it")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.CIRCLE_SCREEN),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = contentPadding.calculateTopPadding() + if (responsive.isTiny) 10.dp else 14.dp,
            end = responsive.screenPadding,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 12.dp else 14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (responsive.isTiny) 18.dp else 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(if (responsive.isTiny) 64.dp else 76.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✨",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                        }
                    }
                    Text(
                        text = "Shake to Discover",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Shake to meet someone nearby through Shared Sparks.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.CIRCLE_SHAKE_PROMPT),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "📱 〰️ ✨",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = when {
                                    locating -> "Finding your approximate area…"
                                    state.discoveryStatus == CircleDiscoveryStatus.Searching -> "Searching the Circle…"
                                    !discoverySafety.adultConfirmed -> "Review safety before discovering"
                                    !discoverySafety.discoveryEnabled -> "Turn on discovery to use Shake"
                                    else -> "Shake your phone to discover"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                softWrap = true,
                            )
                            if (!locating && state.discoveryStatus != CircleDiscoveryStatus.Searching) {
                                Text(
                                    text = if (discoverySafety.discoveryEnabled) {
                                        "A firm, natural shake is enough."
                                    } else {
                                        "Discovery stays off until you explicitly enable it."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Text(
                        text = "18+ • Approximate only • Visible until off • Mutual chat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            DiscoverySafetyCard(
                adultConfirmed = discoverySafety.adultConfirmed,
                discoveryEnabled = discoverySafety.discoveryEnabled,
                onDiscoveryEnabledChange = ::setDiscoveryVisibility,
                onReviewEligibility = { showDiscoveryEligibilityDialog = true },
                onOpenPrivacy = { showDiscoveryPrivacyDialog = true },
            )
        }

        val message = playerFacingCircleMessage(localMessage ?: state.message)
        if (message.isNotBlank()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        state.discoveredMember?.let { member ->
            item {
                DiscoveredMemberCard(
                    member = member,
                    enabled = !state.actionInProgress,
                    onSendSpark = { onSendSpark(member) },
                )
            }
        }

        if (state.incomingRequests.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Sparks waiting for you",
                    subtitle = "Accept a Spark to add someone to your Circle.",
                )
            }
            items(
                count = state.incomingRequests.size,
                key = { index -> state.incomingRequests[index].edgeId },
            ) { index ->
                val request = state.incomingRequests[index]
                SparkRequestCard(
                    request = request,
                    enabled = !state.actionInProgress,
                    onAccept = { onRespondToSpark(request, true) },
                    onIgnore = { onRespondToSpark(request, false) },
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Circle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (state.connections.isEmpty()) {
                            "Accepted Sparks will appear here."
                        } else {
                            "${state.connections.size} connection${if (state.connections.size == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(
                    onClick = onRefresh,
                    enabled = !state.actionInProgress,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh Circle",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (state.connections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                ) {
                    Text(
                        modifier = Modifier.padding(18.dp),
                        text = "Your first accepted Spark will start your Circle. No wallet address or exact location is shown to other members.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(
                count = state.connections.size,
                key = { index -> state.connections[index].edgeId },
            ) { index ->
                val connection = state.connections[index]
                val member = connection.member
                CircleConnectionCard(
                    connection = connection,
                    onClick = { onOpenProfile(member) },
                )
            }
        }
    }
}

@Composable
private fun DiscoverySafetyPoint(
    symbol: String,
    title: String,
    detail: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiscoverySafetyCard(
    adultConfirmed: Boolean,
    discoveryEnabled: Boolean,
    onDiscoveryEnabledChange: (Boolean) -> Unit,
    onReviewEligibility: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Appear in Shake Discovery",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            !adultConfirmed -> "18+ • Off until you choose to enable"
                            discoveryEnabled -> "On • Visible until you turn this off"
                            else -> "Off • You're hidden from discovery"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = CircleDiscoverySafetyRules.canDiscover(
                        adultConfirmed = adultConfirmed,
                        discoveryEnabled = discoveryEnabled,
                    ),
                    onCheckedChange = { enabled ->
                        if (enabled && !adultConfirmed) onReviewEligibility()
                        else onDiscoveryEnabledChange(enabled)
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Approximate only • Mutual chat",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenPrivacy) {
                    Text("Privacy & safety")
                }
            }
        }
    }
}

@Composable
private fun DiscoveredMemberCard(
    member: CircleMemberPreview,
    enabled: Boolean,
    onSendSpark: () -> Unit,
) {
    val avatar = PublicProfileRules.avatarFor(member.avatarId)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = avatar.symbol, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                    Text(
                        text = member.distanceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Radiance ${member.radianceStreak}d • Lv. ${member.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (member.sharedSparks.isNotEmpty()) {
                SharedSparksSection(
                    title = "You already have things in common",
                    sparks = member.sharedSparks,
                )
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSendSpark,
                enabled = enabled,
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "Send Spark")
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Shake again to discover someone else.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SparkRequestCard(
    request: CircleSparkPreview,
    enabled: Boolean,
    onAccept: () -> Unit,
    onIgnore: () -> Unit,
) {
    val avatar = PublicProfileRules.avatarFor(request.member.avatarId)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = avatar.symbol, style = MaterialTheme.typography.headlineSmall)
                Text(
                    modifier = Modifier.weight(1f),
                    text = "${request.member.displayName} sent you a Spark",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Clip,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onAccept,
                    enabled = enabled,
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onIgnore,
                    enabled = enabled,
                ) {
                    Text("Pass")
                }
            }
        }
    }
}

@Composable
private fun CircleConnectionCard(
    connection: CircleSparkPreview,
    onClick: () -> Unit,
) {
    val member = connection.member
    val avatar = PublicProfileRules.avatarFor(member.avatarId)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = avatar.symbol, style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = when {
                        connection.hasUnread -> "New message"
                        connection.lastMessagePreview.isNotBlank() -> connection.lastMessagePreview
                        else -> "Tap to view profile"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connection.hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (connection.hasUnread) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {}
            } else {
                Text(text = "✨", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun CircleMemberProfileView(
    contentPadding: PaddingValues,
    profile: CircleMemberProfilePreview?,
    loading: Boolean,
    onBack: () -> Unit,
    onMessage: (CircleMemberPreview) -> Unit,
    onRemove: (CircleMemberPreview) -> Unit,
    onBlock: (CircleMemberPreview) -> Unit,
    onReport: (CircleMemberPreview, String) -> Unit,
    actionInProgress: Boolean,
) {
    val responsive = rememberResponsiveUiSpec()
    var showRemoveConfirm by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showAllAbout by remember(profile?.member?.uid) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = responsive.screenPadding,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading || profile == null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        modifier = Modifier.padding(20.dp),
                        text = "Opening Circle profile…",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            return@LazyColumn
        }

        item {
            val avatar = PublicProfileRules.avatarFor(profile.profile.avatarId)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(if (responsive.isTiny) 68.dp else 76.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = avatar.symbol, style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = profile.profile.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Clip,
                        )
                        Text(
                            text = "✨ In Your Circle",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        if (profile.profile.motto.isNotBlank()) {
                            Text(
                                text = "“${profile.profile.motto}”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                maxLines = 3,
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onMessage(profile.member) },
                enabled = !actionInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "Message")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                CompactProfileAction(
                    icon = Icons.Filled.PersonRemove,
                    label = "Remove",
                    contentDescription = "Remove from Circle",
                    enabled = !actionInProgress,
                    onClick = { showRemoveConfirm = true },
                )
                CompactProfileAction(
                    icon = Icons.Filled.Block,
                    label = "Block",
                    contentDescription = "Block member",
                    enabled = !actionInProgress,
                    onClick = { showBlockConfirm = true },
                )
                CompactProfileAction(
                    icon = Icons.Filled.Flag,
                    label = "Report",
                    contentDescription = "Report safety issue",
                    enabled = !actionInProgress,
                    onClick = { showReportDialog = true },
                )
            }
        }

        if (profile.sharedSparks.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Shared Sparks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(count = profile.sharedSparks.size) { index ->
                                val spark = profile.sharedSparks[index]
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                ) {
                                    Text(
                                        text = "${spark.symbol} ${spark.value}",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val details = listOf(
            Triple("🍜", "Favorite food", profile.profile.favoriteFood),
            Triple("🎵", "Music", profile.profile.music),
            Triple("🎮", "Games", profile.profile.games),
            Triple("🎨", "Hobbies", profile.profile.hobbies),
            Triple("📚", "Books", profile.profile.books),
            Triple("🐾", "Pets", profile.profile.pets),
            Triple("✨", "Currently into", profile.profile.currentlyInto),
            Triple("🌤", "Weekend vibe", profile.profile.weekendVibe),
            Triple("💬", "Talks for hours about", profile.profile.talkAbout),
        ).filter { it.third.isNotBlank() }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Only what ${profile.profile.displayName} chose to share.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )

                    if (details.isEmpty()) {
                        Text(
                            text = "No public interests yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        val visibleDetails = if (showAllAbout) details else details.take(4)
                        visibleDetails.forEachIndexed { index, detail ->
                            if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 7.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = detail.first, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = detail.second,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.widthIn(min = 92.dp, max = 132.dp),
                                )
                                Text(
                                    text = detail.third,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (details.size > 4) {
                            TextButton(
                                onClick = { showAllAbout = !showAllAbout },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(if (showAllAbout) "Show less" else "Show ${details.size - 4} more")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Private account details, wallet addresses, and exact location are never shown here.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showRemoveConfirm && profile != null) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove from Your Circle?") },
            text = { Text("Messaging stops immediately and this connection leaves Your Circle.") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemove(profile.member)
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showRemoveConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showBlockConfirm && profile != null) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block this person?") },
            text = { Text("Blocking ends this Circle connection and prevents further private chat.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockConfirm = false
                    onBlock(profile.member)
                }) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showReportDialog && profile != null) {
        ReportReasonDialog(
            displayName = profile.profile.displayName,
            onDismiss = { showReportDialog = false },
            onReasonSelected = { reason ->
                showReportDialog = false
                onReport(profile.member, reason)
            },
        )
    }
}

@Composable
private fun CompactProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(46.dp),
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IncomingTypingIndicator(
    avatarSymbol: String,
) {
    var dotPhase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            dotPhase = (dotPhase + 1) % 3
            delay(280L)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Surface(
            modifier = Modifier
                .padding(end = 7.dp, bottom = 1.dp)
                .size(30.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = avatarSymbol, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Surface(
            modifier = Modifier.widthIn(max = 92.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 6.dp,
                bottomEnd = 20.dp,
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    val active = index == dotPhase
                    Surface(
                        modifier = Modifier.size(if (active) 8.dp else 6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (active) 0.9f else 0.35f),
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun CircleChatView(

    contentPadding: PaddingValues,
    member: CircleMemberPreview,
    messages: List<com.thinkblox.radiantrush.data.CircleChatMessagePreview>,
    loading: Boolean,
    sending: Boolean,
    statusMessage: String?,
    sentSequence: Int,
    peerLastReadAtEpochMillis: Long,
    peerTyping: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onTypingChanged: (Boolean) -> Unit,
    onBlock: () -> Unit,
    onReport: (String) -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val context = LocalContext.current
    val soundPlayer = remember(member.uid) { ChatSoundPlayer(context) }
    val listState = rememberLazyListState()
    var draft by remember(member.uid) { mutableStateOf("") }
    var pendingText by remember(member.uid) { mutableStateOf<String?>(null) }
    var pendingStartedAt by remember(member.uid) { mutableStateOf(0L) }
    var lastSeenSentSequence by remember(member.uid) { mutableStateOf(sentSequence) }
    var lastObservedMessageId by remember(member.uid) { mutableStateOf<String?>(null) }
    var messageBaselineReady by remember(member.uid) { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val serverEchoExists = pendingText?.let { pending ->
        messages.any { message ->
            message.isMine &&
                message.text == pending &&
                (message.sentAtEpochMillis == 0L || message.sentAtEpochMillis >= pendingStartedAt - 5_000L)
        }
    } ?: false

    val displayedMessages = if (!pendingText.isNullOrBlank() && !serverEchoExists) {
        messages + com.thinkblox.radiantrush.data.CircleChatMessagePreview(
            id = "local-pending-$pendingStartedAt",
            senderUid = "",
            text = pendingText.orEmpty(),
            sentAtEpochMillis = pendingStartedAt,
            isMine = true,
            hasPendingWrites = true,
        )
    } else {
        messages
    }

    fun sendDraft() {
        val clean = CircleChatRules.sanitizeMessage(draft)
        if (!sending && CircleChatRules.isValidMessage(clean)) {
            pendingText = clean
            pendingStartedAt = System.currentTimeMillis()
            draft = ""
            onTypingChanged(false)
            soundPlayer.playSend()
            onSendMessage(clean)
        }
    }

    DisposableEffect(member.uid) {
        onDispose {
            onTypingChanged(false)
            soundPlayer.release()
        }
    }

    LaunchedEffect(member.uid, draft.isNotBlank()) {
        if (draft.isBlank()) {
            onTypingChanged(false)
        } else {
            while (true) {
                onTypingChanged(true)
                delay(CircleChatRules.TYPING_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    LaunchedEffect(loading, messages.lastOrNull()?.id) {
        if (!loading) {
            val latest = messages.lastOrNull()
            if (!messageBaselineReady) {
                messageBaselineReady = true
                lastObservedMessageId = latest?.id
            } else if (latest != null && latest.id != lastObservedMessageId) {
                if (!latest.isMine) soundPlayer.playReceive()
                lastObservedMessageId = latest.id
            }
        }
    }

    val chatListExtraItems = if (!loading && peerTyping) 1 else 0

    LaunchedEffect(displayedMessages.size, peerTyping, loading) {
        val targetIndex = displayedMessages.lastIndex + chatListExtraItems
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(sentSequence) {
        if (sentSequence > lastSeenSentSequence) {
            pendingText = null
            pendingStartedAt = 0L
            lastSeenSentSequence = sentSequence
        }
    }

    LaunchedEffect(sending, statusMessage) {
        if (!sending && pendingText != null && !statusMessage.isNullOrBlank() && !serverEchoExists) {
            if (draft.isBlank()) draft = pendingText.orEmpty()
            pendingText = null
            pendingStartedAt = 0L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.CIRCLE_CHAT_SCREEN)
            .padding(
                start = responsive.screenPadding,
                top = contentPadding.calculateTopPadding(),
                end = responsive.screenPadding,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Private Circle chat",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(
                onClick = { showReportDialog = true },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Filled.Flag, contentDescription = "Report safety issue", modifier = Modifier.size(18.dp))
            }
            FilledTonalIconButton(
                onClick = { showBlockConfirm = true },
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(40.dp),
            ) {
                Icon(Icons.Filled.Block, contentDescription = "Block member", modifier = Modifier.size(18.dp))
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = playerFacingCircleMessage(statusMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 8.dp),
        ) {
            if (loading) {
                item {
                    Text(
                        "Opening conversation…",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (displayedMessages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 44.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        }
                        Text(
                            text = "Say hello to ${member.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Only accepted Circle members can message each other.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(count = displayedMessages.size, key = { index -> displayedMessages[index].id }) { index ->
                    val message = displayedMessages[index]
                    val avatar = PublicProfileRules.avatarFor(member.avatarId)
                    val nextMessage = displayedMessages.getOrNull(index + 1)
                    val showIncomingAvatar = !message.isMine && (nextMessage == null || nextMessage.isMine)
                    val isLatestMine = message.isMine && displayedMessages.drop(index + 1).none { it.isMine }
                    val deliveryLabel = when {
                        !isLatestMine -> null
                        message.hasPendingWrites || message.id.startsWith("local-pending-") -> "Sending…"
                        message.sentAtEpochMillis > 0L && peerLastReadAtEpochMillis >= message.sentAtEpochMillis -> "Read"
                        else -> "Sent"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        if (!message.isMine) {
                            if (showIncomingAvatar) {
                                Surface(
                                    modifier = Modifier
                                        .padding(end = 7.dp, bottom = 1.dp)
                                        .size(30.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = avatar.symbol, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.padding(end = 7.dp).size(30.dp))
                            }
                        }
                        Column(horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start) {
                            Surface(
                                modifier = Modifier.widthIn(max = 296.dp),
                                shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp,
                                    bottomStart = if (message.isMine) 20.dp else 6.dp,
                                    bottomEnd = if (message.isMine) 6.dp else 20.dp,
                                ),
                                color = if (message.isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                                    Text(message.text, style = MaterialTheme.typography.bodyMedium, softWrap = true)
                                    if (message.sentAtEpochMillis > 0L && !message.hasPendingWrites) {
                                        Text(
                                            text = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.sentAtEpochMillis)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.End),
                                        )
                                    }
                                }
                            }
                            if (deliveryLabel != null) {
                                Text(
                                    text = deliveryLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp, end = 4.dp),
                                )
                            }
                        }
                    }
                }
                if (peerTyping) {
                    item(key = "peer-typing-indicator") {
                        IncomingTypingIndicator(
                            avatarSymbol = PublicProfileRules.avatarFor(member.avatarId).symbol,
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 2.dp, bottom = 2.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp, top = 3.dp, end = 5.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextField(
                    value = draft,
                    onValueChange = { value ->
                        val nextDraft = value.take(CircleChatRules.MAX_MESSAGE_LENGTH)
                        val wasTyping = draft.isNotBlank()
                        val isTyping = nextDraft.isNotBlank()
                        draft = nextDraft
                        if (wasTyping != isTyping) {
                            onTypingChanged(isTyping)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp, max = 92.dp),
                    enabled = true,
                    placeholder = { Text("Message…") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendDraft() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                FilledIconButton(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterVertically)
                        .testTag(UiTestTags.CIRCLE_CHAT_SEND),
                    onClick = ::sendDraft,
                    enabled = !sending && CircleChatRules.isValidMessage(draft),
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send message", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block ${member.displayName}?") },
            text = { Text("This ends the Circle connection and stops private chat immediately.") },
            confirmButton = {
                TextButton(onClick = {
                    showBlockConfirm = false
                    onBlock()
                }) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showReportDialog) {
        ReportReasonDialog(
            displayName = member.displayName,
            onDismiss = { showReportDialog = false },
            onReasonSelected = { reason ->
                showReportDialog = false
                onReport(reason)
            },
        )
    }
}

@Composable
private fun ReportReasonDialog(
    displayName: String,
    onDismiss: () -> Unit,
    onReasonSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report $displayName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Choose the reason that best matches the safety issue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                CircleChatRules.REPORT_REASONS.forEach { reason ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReasonSelected(reason) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SharedSparksSection(
    title: String,
    sparks: List<com.thinkblox.radiantrush.data.SharedSparkPreview>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
            sparks.forEach { spark ->
                Text(
                    text = "${spark.symbol} ${spark.label}: ${spark.value}",
                    style = MaterialTheme.typography.bodyMedium,
                    softWrap = true,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
