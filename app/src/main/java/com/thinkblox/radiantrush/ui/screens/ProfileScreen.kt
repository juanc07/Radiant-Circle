package com.thinkblox.radiantrush.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
) {
    val user = uiState.user

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 14.dp,
            end = 20.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            GradientHeroCard(
                title = user.displayName,
                subtitle = "${user.walletStatus}. Firebase handles cloud progress now; live wallet identity starts in Phase 3.",
            )
        }

        item {
            SyncStatusCard(
                status = uiState.firebaseStatus,
                message = uiState.lastMessage,
                onRetry = onRetryFirebase,
            )
        }

        item {
            SectionTitle(
                title = "Identity readiness",
                body = "Phase 2 links progress to Firebase Anonymous Auth. Phase 3 links the Firebase profile to a public wallet address through Mobile Wallet Adapter.",
            )
        }

        item {
            ProfileInfoRow(
                title = "Firebase Profile",
                value = "${user.xp} XP • ${user.currentStreak}-day streak • ${uiState.todayKey}",
                helper = "Saved in users/{uid}, users/{uid}/completedQuests, and leaderboard/{uid}.",
                icon = Icons.Filled.Storage,
            )
        }
        item {
            ProfileInfoRow(
                title = "Wallet",
                value = user.walletAddress,
                helper = "Phase 3: connect through Mobile Wallet Adapter.",
                icon = Icons.Filled.AccountBalanceWallet,
            )
        }
        item {
            ProfileInfoRow(
                title = "SKR Tier",
                value = user.skrTier,
                helper = "Phase 5: token balance determines XP boosts and badges.",
                icon = Icons.Filled.Token,
            )
        }
        item {
            ProfileInfoRow(
                title = "Security Rule",
                value = "No secrets in APK",
                helper = "Private keys, mint authority, and reward authority never belong in the Android app.",
                icon = Icons.Filled.Security,
            )
        }
        item {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                onClick = onRetryFirebase,
            ) {
                Text("Refresh Firebase Sync")
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    title: String,
    value: String,
    helper: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
