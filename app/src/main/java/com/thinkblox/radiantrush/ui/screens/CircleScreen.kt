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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.ApproximateCircleLocation
import com.thinkblox.radiantrush.data.CircleDiscoveryStatus
import com.thinkblox.radiantrush.data.CircleMemberPreview
import com.thinkblox.radiantrush.data.CircleMemberProfilePreview
import com.thinkblox.radiantrush.data.CircleSparkPreview
import com.thinkblox.radiantrush.data.CircleUiState
import com.thinkblox.radiantrush.location.ApproximateLocationProvider
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.ui.components.ShakeToDiscoverEffect
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags

@Composable
fun CircleScreen(
    contentPadding: PaddingValues,
    state: CircleUiState,
    onStartDiscovery: (ApproximateCircleLocation) -> Unit,
    onSendSpark: (CircleMemberPreview) -> Unit,
    onRespondToSpark: (CircleSparkPreview, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onOpenProfile: (CircleMemberPreview) -> Unit,
    onCloseProfile: () -> Unit,
) {
    val context = LocalContext.current
    val responsive = rememberResponsiveUiSpec()
    val locationProvider = remember(context) {
        ApproximateLocationProvider(context.applicationContext)
    }
    var locating by remember { mutableStateOf(false) }
    var localMessage by remember { mutableStateOf<String?>(null) }

    if (state.profileLoading || state.selectedMemberProfile != null) {
        CircleMemberProfileView(
            contentPadding = contentPadding,
            profile = state.selectedMemberProfile,
            loading = state.profileLoading,
            onBack = onCloseProfile,
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
            locateAndDiscover()
        } else {
            localMessage = "Approximate location stays optional. Allow it when you want to discover people around your area."
        }
    }

    fun beginDiscovery() {
        if (locationProvider.hasPermission()) {
            locateAndDiscover()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    ShakeToDiscoverEffect(
        enabled = !locating && !state.actionInProgress && state.discoveryStatus != CircleDiscoveryStatus.Searching,
        onShake = ::beginDiscovery,
    )

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
                        text = "Find another active Radiant Circle member around your area. If no one is nearby, the Circle quietly widens the search.",
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
                                    else -> "Shake your phone to discover"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                softWrap = true,
                            )
                            if (!locating && state.discoveryStatus != CircleDiscoveryStatus.Searching) {
                                Text(
                                    text = "A firm, natural shake is enough.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Approximate location only • active for about 2 minutes • exact coordinates are not shared",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        val message = localMessage ?: state.message
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
                val member = state.connections[index].member
                CircleConnectionCard(
                    member = member,
                    onClick = { onOpenProfile(member) },
                )
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
    member: CircleMemberPreview,
    onClick: () -> Unit,
) {
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
                    text = "Tap to view profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(text = "✨", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CircleMemberProfileView(
    contentPadding: PaddingValues,
    profile: CircleMemberProfilePreview?,
    loading: Boolean,
    onBack: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = responsive.screenPadding,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(modifier = Modifier.padding(start = 8.dp), text = "Back to Your Circle")
            }
        }

        if (loading || profile == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(if (responsive.isTiny) 78.dp else 92.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = avatar.symbol, style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                    Text(
                        text = profile.profile.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        softWrap = true,
                    )
                    if (profile.profile.motto.isNotBlank()) {
                        Text(
                            text = "“${profile.profile.motto}”",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            softWrap = true,
                        )
                    }
                    Text(
                        text = "✨ In Your Circle",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (profile.sharedSparks.isNotEmpty()) {
            item {
                SharedSparksSection(
                    title = "Shared Sparks",
                    sparks = profile.sharedSparks,
                )
            }
        }

        item {
            SectionHeader(
                title = "About ${profile.profile.displayName}",
                subtitle = "Only the details they chose to share are shown here.",
            )
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
            Triple("💬", "Can talk for hours about", profile.profile.talkAbout),
        ).filter { it.third.isNotBlank() }

        if (details.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        modifier = Modifier.padding(18.dp),
                        text = "They haven't shared interests yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(count = details.size) { index ->
                val detail = details[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(text = detail.first, style = MaterialTheme.typography.titleLarge)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = detail.second,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = detail.third,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                softWrap = true,
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Private account details, wallet addresses, and exact location are never shown on Circle profiles.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                softWrap = true,
            )
        }
    }
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
