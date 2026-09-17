package com.thinkblox.radiantrush.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thinkblox.radiantrush.data.CircleProfilePreview
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.logic.PublicProfileRules
import com.thinkblox.radiantrush.logic.SharedSparkRules
import com.thinkblox.radiantrush.ui.components.AdaptiveButtonText
import com.thinkblox.radiantrush.ui.components.PassportCrestCard
import com.thinkblox.radiantrush.ui.components.GradientHeroCard
import com.thinkblox.radiantrush.ui.components.SectionTitle
import com.thinkblox.radiantrush.ui.components.SyncStatusCard
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
    onSavePublicProfile: (String, String) -> Unit,
    circleProfile: CircleProfilePreview,
    circleProfileLoading: Boolean,
    circleActionInProgress: Boolean,
    onSaveCircleProfile: (CircleProfilePreview) -> Unit,
    accountActionInProgress: Boolean,
    accountActionMessage: String?,
    onProtectAccount: () -> Unit,
) {
    val user = uiState.user
    val responsive = rememberResponsiveUiSpec()
    val profileAvatar = PublicProfileRules.avatarFor(user.avatarId)
    var showDisconnectConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = responsive.screenPadding,
            top = 14.dp,
            end = responsive.screenPadding,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 12.dp else 16.dp),
    ) {
        item {
            val heroSubtitle = if (uiState.isWalletConnected) {
                buildString {
                    append("${user.walletStatus}. Public Solana address is linked.")
                    if (user.hasSkr) append(" ${user.skrAuraLabel} active.")
                }
            } else {
                "${user.walletStatus}. Connect your Solana wallet."
            }
            if (user.hasSkr) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(30.dp),
                        )
                        .padding(2.dp),
                ) {
                    GradientHeroCard(
                        title = "✦ ${profileAvatar.symbol} ${user.displayName} ✦",
                        subtitle = heroSubtitle,
                    )
                }
            } else {
                GradientHeroCard(
                    title = "${profileAvatar.symbol} ${user.displayName}",
                    subtitle = heroSubtitle,
                )
            }
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
                title = "Your Circle identity",
                body = "Your public profile, wallet, Passport, progress, and game activity in one place.",
            )
        }

        item {
            PublicIdentityEditor(
                displayName = user.displayName,
                avatarId = user.avatarId,
                enabled = uiState.isFirebaseReady && !uiState.walletActionInProgress,
                onSave = onSavePublicProfile,
            )
        }

        item {
            SocialProfileEditor(
                profile = circleProfile.copy(
                    displayName = user.displayName,
                    avatarId = user.avatarId,
                ),
                enabled = uiState.isFirebaseReady && !circleActionInProgress && !circleProfileLoading,
                loading = circleProfileLoading,
                onSave = onSaveCircleProfile,
            )
        }

        item {
            AccountProtectionCard(
                isTemporary = uiState.accountIdentity.isTemporary,
                providerLabel = uiState.accountIdentity.providerLabel,
                email = uiState.accountIdentity.email,
                actionInProgress = accountActionInProgress,
                walletActionInProgress = uiState.walletActionInProgress,
                message = accountActionMessage,
                onProtectAccount = onProtectAccount,
            )
        }

        item {
            ProfileInfoRow(
                title = "Progress",
                value = "${user.xp} XP • ${user.currentStreak}-day streak • ${uiState.todayKey}",
                helper = "Your XP and daily streak.",
                icon = Icons.Filled.Storage,
            )
        }
        item {
            ProfileInfoRow(
                title = "Radiant Rush",
                value = "Best ${user.bestRunScore} • ${user.totalRuns} runs",
                helper = "Last ${user.lastRunScore} • x${user.lastRunMaxCombo} combo • ${uiState.radiantRun.collectionOwned}/${uiState.radiantRun.collectionTotal} collectibles",
                icon = Icons.Filled.Bolt,
            )
        }
        item {
            ProfileInfoRow(
                title = "Rush Tickets / Shards",
                value = "${user.rushTickets} standard • ${user.skrCasualRushTickets} SKR casual • ${user.radiantShards} shards",
                helper = "Standard tickets can enter Ranked runs. SKR bonus tickets are Casual-only.",
                icon = Icons.Filled.ConfirmationNumber,
            )
        }
        if (!user.lastRunRewardTitle.isNullOrBlank()) {
            item {
                ProfileInfoRow(
                    title = "Last Capsule",
                    value = buildString {
                        append(uiState.radiantRun.lastCapsuleTier ?: "Rush Capsule")
                        append(" → ")
                        append(user.lastRunRewardTitle)
                    },
                    helper = "${user.lastRunRewardRarity ?: "Reward"} • +${user.lastRunRewardXp} XP" +
                        if (user.lastRunRewardShards > 0) {
                            " • duplicate +${user.lastRunRewardShards} shards"
                        } else {
                            " • added to Radiant Vault"
                        },
                    icon = Icons.Filled.EmojiEvents,
                )
            }
        }
        item {
            ProfileInfoRow(
                title = "Daily Radiant Chest",
                value = if (user.lastChestRewardXp > 0) {
                    "+${user.lastChestRewardXp} XP • ${user.lastChestRewardRarity ?: "Reward"}"
                } else {
                    uiState.radiantChest.status.label
                },
                helper = if (user.lastChestRewardXp > 0) {
                    buildString {
                        append("${user.lastChestRewardTitle ?: "Daily reward"} claimed ${user.lastChestClaimDate ?: "today"}. Total chest XP: ${user.totalChestXp}.")
                        if (user.lastChestRewardTickets > 0) append(" +${user.lastChestRewardTickets} Rush Tickets.")
                        if (user.lastChestSkrBonusXp > 0 || user.lastChestSkrBonusTickets > 0) {
                            append(" SKR perk included +${user.lastChestSkrBonusXp} XP")
                            if (user.lastChestSkrBonusTickets > 0) {
                                append(" and +${user.lastChestSkrBonusTickets} ticket${if (user.lastChestSkrBonusTickets == 1) "" else "s"}")
                            }
                            append(".")
                        }
                    }
                } else {
                    "Complete today’s quests to unlock your chest."
                },
                icon = Icons.Filled.EmojiEvents,
            )
        }
        item {
            ProfileInfoRow(
                title = "Wallet",
                value = user.walletAddress,
                helper = if (uiState.isWalletConnected) {
                    "Connected and ready for wallet quests."
                } else {
                    "Connect your Solana wallet to continue."
                },
                icon = Icons.Filled.AccountBalanceWallet,
                copyValue = user.walletAddress.takeIf { uiState.isWalletConnected },
                copyLabel = "Copy Address",
            )
        }
        item {
            if (uiState.isWalletConnected) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    enabled = !uiState.walletActionInProgress,
                    shape = RoundedCornerShape(16.dp),
                    onClick = { showDisconnectConfirmation = true },
                ) {
                    AdaptiveButtonText(if (uiState.walletActionInProgress) "Disconnecting…" else "Disconnect Wallet")
                }
            } else {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    enabled = uiState.isFirebaseReady && !uiState.walletActionInProgress,
                    shape = RoundedCornerShape(16.dp),
                    onClick = onConnectWallet,
                ) {
                    AdaptiveButtonText(if (uiState.walletActionInProgress) "Opening Wallet…" else "Connect Wallet")
                }
            }
        }
        item {
            SectionTitle(
                title = "SKR Passport",
                body = "Liquid + active staked SKR unlock daily perks, chest boosts, and cosmetics.",
            )
        }
        item {
            PassportCrestCard(
                tier = user.skrTier,
                eligibleBalance = user.skrEligibleBalance,
                stakeBoostActive = user.skrStakeBoostActive,
                frameLabel = user.skrFrameLabel,
                auraLabel = user.skrAuraLabel,
            )
        }
        item {
            ProfileInfoRow(
                title = "Passport Eligible SKR",
                value = user.skrEligibleBalance,
                helper = "Liquid + active stake.",
                icon = Icons.Filled.Token,
                copyValue = user.skrMint,
                copyLabel = "Copy Mint",
            )
        }
        item {
            ProfileInfoRow(
                title = "Liquid SKR",
                value = user.skrBalance,
                helper = "Last refreshed: ${user.lastSkrChecked ?: "Not yet"}",
                icon = Icons.Filled.Token,
            )
        }
        item {
            ProfileInfoRow(
                title = "Daily Holder Perks",
                value = if (user.hasSkr) {
                    buildString {
                        append("+${user.skrDailyBonusTickets} bonus ticket${if (user.skrDailyBonusTickets == 1) "" else "s"}/day")
                        append(" • Chest +${user.skrChestBonusXp} XP")
                        if (user.skrChestBonusTickets > 0) append(" +${user.skrChestBonusTickets} ticket${if (user.skrChestBonusTickets == 1) "" else "s"}")
                    }
                } else {
                    "Explorer • no holder perks yet"
                },
                helper = if (user.hasSkr) {
                    buildString {
                        append("${user.skrDailyBonusTicketsGrantedToday}/${user.skrDailyBonusTickets} daily bonus tickets claimed.")
                        if (user.skrStakeBoostActive) append(" ${user.skrStakeBoostLabel} active.")
                    }
                } else {
                    "Hold or actively stake SKR to unlock Passport perks."
                },
                icon = Icons.Filled.ConfirmationNumber,
            )
        }
        item {
            PassportCosmeticPreviewCard(
                avatarSymbol = profileAvatar.symbol,
                tier = user.skrTier,
                frameLabel = user.skrFrameLabel,
                auraLabel = user.skrAuraLabel,
                collectibleLabel = user.skrHolderCollectibleLabel,
                hasHolderPerks = user.hasSkr,
                stakeBoostActive = user.skrStakeBoostActive,
            )
        }
        item {
            ProfileInfoRow(
                title = "Active Staked SKR",
                value = if (user.skrStakedVerified) user.skrStakedBalance else "Read unavailable",
                helper = buildString {
                    append(user.skrStakedStatus)
                    if (user.skrStakeBoostActive) append(" • ${user.skrStakeBoostLabel}: +1 Casual ticket/day, +25 Chest XP")
                    else if (user.skrStakedVerified) append(" • Stake Boost unlocks with active stake")
                },
                icon = Icons.Filled.Security,
                copyValue = user.skrStakingProgramId,
                copyLabel = "Copy Staking Program",
            )
        }
        if (user.skrStakedVerified && user.skrUnstakingBalance != "0 SKR") {
            item {
                ProfileInfoRow(
                    title = "Unstaking SKR",
                    value = user.skrUnstakingBalance,
                    helper = if (user.skrUnstakingReady) {
                        "Cooldown complete. Ready to withdraw."
                    } else {
                        "Cooldown in progress."
                    },
                    icon = Icons.Filled.Security,
                )
            }
        }
        if (!user.lastSignedMessageSignature.isNullOrBlank()) {
            item {
                ProfileInfoRow(
                    title = "Last Signed Proof",
                    value = shortenForProfile(user.lastSignedMessageSignature.orEmpty()),
                    helper = "Today’s signed wallet proof.",
                    icon = Icons.Filled.Security,
                    copyValue = user.lastSignedMessageSignature,
                    copyLabel = "Copy Signature",
                )
            }
        }
        if (!user.lastOnChainTxSignature.isNullOrBlank()) {
            item {
                ProfileInfoRow(
                    title = "Last Memo Transaction",
                    value = shortenForProfile(user.lastOnChainTxSignature.orEmpty()),
                    helper = user.lastOnChainExplorerUrl ?: "Today’s memo transaction.",
                    icon = Icons.Filled.Token,
                    copyValue = user.lastOnChainExplorerUrl ?: user.lastOnChainTxSignature,
                    copyLabel = if (!user.lastOnChainExplorerUrl.isNullOrBlank()) "Copy Explorer" else "Copy TX",
                )
            }
        }
        item {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight),
                enabled = !uiState.walletActionInProgress,
                shape = RoundedCornerShape(16.dp),
                onClick = onRetryFirebase,
            ) {
                AdaptiveButtonText("Refresh Profile")
            }
        }
    }

    if (showDisconnectConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = { Text("Disconnect wallet?") },
            text = {
                Text("Your Radiant Circle profile and earned rewards stay intact. Wallet-only features will be unavailable until you reconnect.")
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmation = false }) {
                    AdaptiveButtonText("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.walletActionInProgress,
                    onClick = {
                        showDisconnectConfirmation = false
                        onDisconnectWallet()
                    },
                ) {
                    AdaptiveButtonText("Disconnect")
                }
            },
        )
    }
}

@Composable
private fun AccountProtectionCard(
    isTemporary: Boolean,
    providerLabel: String,
    email: String?,
    actionInProgress: Boolean,
    walletActionInProgress: Boolean,
    message: String?,
    onProtectAccount: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTemporary) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isTemporary) "Keep your Circle" else "Circle account protected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isTemporary) {
                            "Keep your profile, Daily Radiance and Circle if you reinstall or move to another phone."
                        } else {
                            buildString {
                                append("Protected with $providerLabel")
                                email?.takeIf { it.isNotBlank() }?.let { append(" • $it") }
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isTemporary) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onProtectAccount,
                    enabled = !actionInProgress && !walletActionInProgress,
                ) {
                    AdaptiveButtonText(
                        text = when {
                            actionInProgress -> "Opening Google…"
                            walletActionInProgress -> "Finish Wallet Action…"
                            else -> "Continue with Google"
                        },
                    )
                }
            }

            message?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SocialProfileEditor(
    profile: CircleProfilePreview,
    enabled: Boolean,
    loading: Boolean,
    onSave: (CircleProfilePreview) -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    var showEditor by remember { mutableStateOf(false) }
    val clean = SharedSparkRules.sanitizeProfile(profile)
    val filledCount = SharedSparkRules.filledInterestCount(clean)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "About you",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Share a few things you enjoy so Radiant Circle can highlight what you have in common with other people.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true,
            )
            if (clean.motto.isNotBlank()) {
                Text(
                    text = "“${clean.motto}”",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    softWrap = true,
                )
            }
            Text(
                text = when {
                    loading -> "Loading your shared interests…"
                    filledCount == 0 -> "Nothing shared yet. Every field is optional."
                    else -> "$filledCount interest${if (filledCount == 1) "" else "s"} shared on your Circle profile."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight),
                onClick = { showEditor = true },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
            ) {
                AdaptiveButtonText(
                    text = if (filledCount == 0) "Add interests" else "Edit shared interests",
                    compactText = if (filledCount == 0) "Add interests" else "Edit interests",
                    tinyText = "Edit interests",
                )
            }
            Text(
                text = "Only the fields you fill in are shared. Your email, wallet, exact location, and account id are never part of this profile.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = true,
            )
        }
    }

    if (showEditor) {
        SocialProfileEditDialog(
            profile = clean,
            enabled = enabled,
            onSave = {
                onSave(it)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
private fun SocialProfileEditDialog(
    profile: CircleProfilePreview,
    enabled: Boolean,
    onSave: (CircleProfilePreview) -> Unit,
    onDismiss: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    var motto by remember(profile) { mutableStateOf(profile.motto) }
    var favoriteFood by remember(profile) { mutableStateOf(profile.favoriteFood) }
    var music by remember(profile) { mutableStateOf(profile.music) }
    var games by remember(profile) { mutableStateOf(profile.games) }
    var hobbies by remember(profile) { mutableStateOf(profile.hobbies) }
    var books by remember(profile) { mutableStateOf(profile.books) }
    var pets by remember(profile) { mutableStateOf(profile.pets) }
    var currentlyInto by remember(profile) { mutableStateOf(profile.currentlyInto) }
    var weekendVibe by remember(profile) { mutableStateOf(profile.weekendVibe) }
    var talkAbout by remember(profile) { mutableStateOf(profile.talkAbout) }

    fun draft(): CircleProfilePreview = SharedSparkRules.sanitizeProfile(
        profile.copy(
            motto = motto,
            favoriteFood = favoriteFood,
            music = music,
            games = games,
            hobbies = hobbies,
            books = books,
            pets = pets,
            currentlyInto = currentlyInto,
            weekendVibe = weekendVibe,
            talkAbout = talkAbout,
        ),
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(responsive.cardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "What do you want to share?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        softWrap = true,
                    )
                    Text(
                        text = "All fields are optional. Separate multiple interests with commas or slashes so Shared Sparks can spot similarities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                    )
                }

                item { SocialProfileField("Motto", motto, SharedSparkRules.MAX_MOTTO_LENGTH, enabled) { motto = it } }
                item { SocialProfileField("🍜 Favorite food", favoriteFood, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { favoriteFood = it } }
                item { SocialProfileField("🎵 Music", music, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { music = it } }
                item { SocialProfileField("🎮 Games", games, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { games = it } }
                item { SocialProfileField("🎨 Hobbies", hobbies, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { hobbies = it } }
                item { SocialProfileField("📚 Books", books, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { books = it } }
                item { SocialProfileField("🐾 Pets", pets, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { pets = it } }
                item { SocialProfileField("✨ Currently into", currentlyInto, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { currentlyInto = it } }
                item { SocialProfileField("🌤 Weekend vibe", weekendVibe, SharedSparkRules.MAX_SHORT_FIELD_LENGTH, enabled) { weekendVibe = it } }
                item { SocialProfileField("💬 I can talk for hours about…", talkAbout, SharedSparkRules.MAX_TALK_ABOUT_LENGTH, enabled) { talkAbout = it } }

                item {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight),
                        onClick = { onSave(draft()) },
                        enabled = enabled,
                    ) {
                        AdaptiveButtonText("Save Circle Profile", compactText = "Save profile", tinyText = "Save")
                    }
                }
                item {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.buttonHeight),
                        onClick = onDismiss,
                    ) {
                        AdaptiveButtonText("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialProfileField(
    label: String,
    value: String,
    maxLength: Int,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        enabled = enabled,
        label = { Text(label, softWrap = true) },
        supportingText = { Text("${value.length}/$maxLength") },
        minLines = 1,
        maxLines = 2,
    )
}

@Composable
private fun PublicIdentityEditor(
    displayName: String,
    avatarId: String,
    enabled: Boolean,
    onSave: (String, String) -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    var draftName by remember(displayName) { mutableStateOf(displayName) }
    var selectedAvatarId by remember(avatarId) {
        mutableStateOf(PublicProfileRules.normalizeAvatarId(avatarId))
    }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("Animals") }
    val selectedAvatar = PublicProfileRules.avatarFor(selectedAvatarId)
    val sanitized = PublicProfileRules.sanitizeDisplayName(draftName)
    val changed = sanitized != displayName || selectedAvatarId != PublicProfileRules.normalizeAvatarId(avatarId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Public identity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Choose the name and icon shown on Radiant Circle leaderboards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (responsive.isCompact || responsive.hasLargeText) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(if (responsive.isTiny) 72.dp else 82.dp)
                                .clickable(enabled = enabled) {
                                    selectedCategory = selectedAvatar.category
                                    showAvatarPicker = true
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = selectedAvatar.symbol,
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        Text(
                            text = "Tap to change icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draftName,
                        onValueChange = { value -> draftName = value.take(PublicProfileRules.MAX_DISPLAY_NAME_LENGTH) },
                        enabled = enabled,
                        singleLine = true,
                        label = { Text("Display name") },
                        supportingText = { Text("${draftName.length}/${PublicProfileRules.MAX_DISPLAY_NAME_LENGTH}") },
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable(enabled = enabled) {
                                    selectedCategory = selectedAvatar.category
                                    showAvatarPicker = true
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = selectedAvatar.symbol,
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        Text(
                            text = "Change icon",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = draftName,
                        onValueChange = { value -> draftName = value.take(PublicProfileRules.MAX_DISPLAY_NAME_LENGTH) },
                        enabled = enabled,
                        singleLine = true,
                        label = { Text("Display name") },
                        supportingText = { Text("${draftName.length}/${PublicProfileRules.MAX_DISPLAY_NAME_LENGTH}") },
                    )
                }
            }
            Text(
                text = "${selectedAvatar.category} • ${selectedAvatar.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = responsive.buttonHeight),
                enabled = enabled && changed,
                shape = RoundedCornerShape(16.dp),
                onClick = { onSave(sanitized, selectedAvatarId) },
            ) {
                AdaptiveButtonText("Save Public Profile", compactText = "Save Profile", tinyText = "Save")
            }
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            selectedAvatarId = selectedAvatarId,
            selectedCategory = selectedCategory,
            enabled = enabled,
            onCategoryChanged = { selectedCategory = it },
            onAvatarSelected = {
                selectedAvatarId = it
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false },
        )
    }
}

@Composable
private fun AvatarPickerDialog(
    selectedAvatarId: String,
    selectedCategory: String,
    enabled: Boolean,
    onCategoryChanged: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()
    val visibleAvatars = PublicProfileRules.avatarsForCategory(selectedCategory)
    val selectedAvatar = PublicProfileRules.avatarFor(selectedAvatarId)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (responsive.isTiny) 12.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Choose your profile icon",
                    modifier = Modifier.fillMaxWidth(),
                    style = if (responsive.isTiny) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                )
                Text(
                    text = "Tap an icon to use it. Swipe categories or scroll the icon grid.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                ) {
                    Text(
                        text = "Selected • ${selectedAvatar.symbol} ${selectedAvatar.label}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        softWrap = true,
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp),
                ) {
                    items(PublicProfileRules.avatarCategories, key = { it }) { category ->
                        FilterChip(
                            selected = category == selectedCategory,
                            onClick = { onCategoryChanged(category) },
                            label = {
                                Text(
                                    text = category,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (responsive.isTiny) 62.dp else 70.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    gridItems(visibleAvatars, key = { it.id }) { avatar ->
                        val selected = avatar.id == selectedAvatarId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(18.dp),
                                )
                                .clickable(enabled = enabled) { onAvatarSelected(avatar.id) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                            },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = avatar.symbol,
                                    style = if (responsive.isTiny) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = responsive.buttonHeight),
                    onClick = onDismiss,
                ) {
                    AdaptiveButtonText("Close")
                }
            }
        }
    }
}

@Composable
private fun PassportCosmeticPreviewCard(
    avatarSymbol: String,
    tier: String,
    frameLabel: String,
    auraLabel: String,
    collectibleLabel: String,
    hasHolderPerks: Boolean,
    stakeBoostActive: Boolean,
) {
    val responsive = rememberResponsiveUiSpec()
    val auraColor = if (hasHolderPerks) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
    val frameColor = if (hasHolderPerks) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(responsive.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Passport look",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "A visual preview of the cosmetics styling your Circle identity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (responsive.isTiny) 190.dp else 220.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (responsive.isTiny) 156.dp else 180.dp)
                        .background(auraColor.copy(alpha = if (hasHolderPerks) 0.16f else 0.06f), CircleShape)
                        .border(
                            width = if (hasHolderPerks) 3.dp else 1.dp,
                            color = auraColor.copy(alpha = if (hasHolderPerks) 0.48f else 0.28f),
                            shape = CircleShape,
                        ),
                )

                Box(
                    modifier = Modifier
                        .size(if (responsive.isTiny) 128.dp else 146.dp)
                        .border(
                            width = if (hasHolderPerks) 5.dp else 2.dp,
                            color = frameColor,
                            shape = CircleShape,
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = avatarSymbol,
                                style = MaterialTheme.typography.displaySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (frameLabel.contains("Crown", ignoreCase = true)) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "♛",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }

                if (stakeBoostActive || auraLabel.contains("Guardian", ignoreCase = true)) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = if (responsive.isTiny) 30.dp else 52.dp, bottom = 12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Guardian Glow",
                            modifier = Modifier.padding(9.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Text(
                text = tier,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "$frameLabel • $auraLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                softWrap = true,
            )
            if (hasHolderPerks && !collectibleLabel.startsWith("No ", ignoreCase = true)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = collectibleLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

private fun shortenForProfile(value: String): String {
    val clean = value.trim()
    return if (clean.length <= 18) clean else "${clean.take(8)}…${clean.takeLast(8)}"
}

@Composable
private fun ProfileInfoRow(
    title: String,
    value: String,
    helper: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    copyValue: String? = null,
    copyLabel: String = "Copy",
) {
    val responsive = rememberResponsiveUiSpec()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    fun copy() {
        if (!copyValue.isNullOrBlank()) {
            clipboardManager.setText(AnnotatedString(copyValue))
            Toast.makeText(context, "$copyLabel copied", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        softWrap = true,
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    softWrap = true,
                )
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true,
                )
                if (!copyValue.isNullOrBlank()) {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = responsive.compactButtonHeight),
                        shape = RoundedCornerShape(14.dp),
                        onClick = ::copy,
                    ) {
                        AdaptiveButtonText(
                            text = copyLabel,
                            compactText = when {
                                copyLabel.contains("Signature") -> "Copy Sign"
                                copyLabel.contains("Explorer") -> "Copy Link"
                                copyLabel.contains("Address") -> "Copy Addr"
                                copyLabel.contains("Program") -> "Copy Program"
                                else -> copyLabel
                            },
                            tinyText = "Copy",
                        )
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(text = value, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = helper,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!copyValue.isNullOrBlank()) {
                        OutlinedButton(
                            modifier = Modifier.heightIn(min = responsive.compactButtonHeight),
                            shape = RoundedCornerShape(14.dp),
                            onClick = ::copy,
                        ) {
                            AdaptiveButtonText(copyLabel)
                        }
                    }
                }
            }
        }
    }
}
