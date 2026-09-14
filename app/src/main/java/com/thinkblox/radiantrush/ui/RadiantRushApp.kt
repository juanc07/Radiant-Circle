package com.thinkblox.radiantrush.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.thinkblox.radiantrush.data.AppDestination
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.RadiantChestStatus
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.firebase.FirebaseRadiantRepository
import com.thinkblox.radiantrush.solana.MobileWalletRepository
import com.thinkblox.radiantrush.solana.SkrBalanceRepository
import com.thinkblox.radiantrush.solana.SkrBalanceResult
import com.thinkblox.radiantrush.solana.WalletConnectResult
import com.thinkblox.radiantrush.solana.WalletDisconnectResult
import com.thinkblox.radiantrush.solana.WalletMemoProofResult
import com.thinkblox.radiantrush.solana.WalletSignedProofResult
import com.thinkblox.radiantrush.logic.DailyPlanRefreshRules
import com.thinkblox.radiantrush.logic.RadiantRunResult
import com.thinkblox.radiantrush.logic.RadiantChestPresentationRules
import com.thinkblox.radiantrush.ui.components.AdaptiveNavLabel
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.screens.BadgesScreen
import com.thinkblox.radiantrush.ui.screens.HomeScreen
import com.thinkblox.radiantrush.ui.screens.LeaderboardScreen
import com.thinkblox.radiantrush.ui.screens.ProfileScreen
import com.thinkblox.radiantrush.ui.screens.DemoScreen
import com.thinkblox.radiantrush.ui.screens.QuestsScreen
import com.thinkblox.radiantrush.ui.screens.RadiantRunScreen
import com.thinkblox.radiantrush.ui.screens.WelcomeScreen
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RadiantRushApp(walletRepository: MobileWalletRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) {
        FirebaseRadiantRepository(context.applicationContext)
    }
    val skrRepository = remember {
        SkrBalanceRepository()
    }
    var appState by remember { mutableStateOf(PreviewContent.defaultState()) }
    var enteredShell by rememberSaveable { mutableStateOf(false) }
    // Radiant Rush owns transient in-memory gameplay state. Do not restore the run route
    // across Activity recreation/process restoration; restoring only the route can reopen a
    // half-reset run and also makes device tests depend on whatever screen was previously open.
    var showRadiantRun by remember { mutableStateOf(false) }
    // Keep shell navigation outside RadiantRushShell so entering the full-screen game
    // does not dispose and recreate the selected tab as Home on return.
    var shellDestination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var todayReturnToRushRequest by rememberSaveable { mutableStateOf(0) }
    var firebaseRefreshGeneration by remember { mutableStateOf(0) }

    fun questsWithStatus(questId: String, status: QuestStatus): List<QuestPreview> =
        appState.quests.map { quest ->
            if (quest.id == questId) quest.copy(status = status) else quest
        }

    fun markQuestStatus(questId: String, status: QuestStatus) {
        appState = appState.copy(quests = questsWithStatus(questId, status))
    }

    fun markChestStatus(status: RadiantChestStatus, buttonLabel: String = appState.radiantChest.buttonLabel) {
        appState = appState.copy(
            radiantChest = appState.radiantChest.copy(
                status = status,
                buttonLabel = buttonLabel,
            ),
        )
    }

    fun beginChestAction(message: String): Boolean {
        if (appState.walletActionInProgress) {
            appState = appState.copy(
                lastMessage = "Please wait — ${appState.activeQuestId ?: "another quest"} is still working.",
            )
            return false
        }
        if (appState.radiantChest.status != RadiantChestStatus.Ready) {
            appState = appState.copy(lastMessage = "Complete every daily proof before opening the Daily Radiant Chest.")
            return false
        }

        firebaseRefreshGeneration += 1
        appState = appState.copy(
            walletActionInProgress = true,
            activeQuestId = QuestIds.DAILY_RADIANT_CHEST,
            lastMessage = message,
            radiantChest = appState.radiantChest.copy(
                status = RadiantChestStatus.Opening,
                buttonLabel = "Opening…",
            ),
        )
        return true
    }

    fun beginQuestAction(questId: String, message: String): Boolean {
        if (appState.walletActionInProgress) {
            appState = appState.copy(
                lastMessage = "Please wait — ${appState.activeQuestId ?: "another quest"} is still working.",
            )
            return false
        }

        firebaseRefreshGeneration += 1
        appState = appState.copy(
            walletActionInProgress = true,
            activeQuestId = questId,
            lastMessage = message,
            quests = questsWithStatus(questId, QuestStatus.Syncing),
        )
        return true
    }

    fun failQuestAction(
        questId: String,
        message: String,
        fallbackStatus: QuestStatus = QuestStatus.Ready,
    ) {
        appState = appState.copy(
            walletActionInProgress = false,
            activeQuestId = null,
            lastMessage = message,
            quests = questsWithStatus(questId, fallbackStatus),
        )
    }

    fun applyRepositoryState(nextState: RushUiState, activeQuestId: String? = null) {
        // Repository operations can emit a lightweight Loading state before the real profile.
        // Never replace an already-usable screen with preview/default data during that step.
        if (
            nextState.firebaseStatus == FirebaseStatus.Loading &&
            appState.firebaseStatus == FirebaseStatus.Ready
        ) {
            appState = appState.copy(
                firebaseStatus = FirebaseStatus.Ready,
                backgroundSyncInProgress = true,
                lastMessage = nextState.lastMessage,
                walletActionInProgress = activeQuestId != null || appState.walletActionInProgress,
                activeQuestId = activeQuestId ?: appState.activeQuestId,
                quests = if (activeQuestId != null) {
                    questsWithStatus(activeQuestId, QuestStatus.Syncing)
                } else {
                    appState.quests
                },
            )
            return
        }

        val stabilized = DailyPlanRefreshRules.stabilize(appState, nextState)
        val activeQuestStillSyncing = activeQuestId != null &&
            stabilized.quests.firstOrNull { it.id == activeQuestId }?.status == QuestStatus.Syncing
        val keepActionLocked = activeQuestStillSyncing && stabilized.backgroundSyncInProgress

        appState = stabilized.copy(
            walletActionInProgress = keepActionLocked,
            activeQuestId = activeQuestId.takeIf { keepActionLocked },
        )
    }

    fun refreshFirebase() {
        if (appState.walletActionInProgress) {
            appState = appState.copy(lastMessage = "Please wait for the current action to finish.")
            return
        }

        val requestGeneration = firebaseRefreshGeneration + 1
        firebaseRefreshGeneration = requestGeneration
        val stateBeforeRefresh = appState

        repository.bootstrap { nextState ->
            if (requestGeneration == firebaseRefreshGeneration) {
                if (
                    nextState.firebaseStatus == FirebaseStatus.Loading &&
                    stateBeforeRefresh.firebaseStatus == FirebaseStatus.Ready
                ) {
                    appState = DailyPlanRefreshRules.stabilize(appState, stateBeforeRefresh).copy(
                        firebaseStatus = FirebaseStatus.Ready,
                        backgroundSyncInProgress = true,
                        lastMessage = "Refreshing in background…",
                    )
                } else {
                    applyRepositoryState(nextState)
                }
            }
        }

        // Do not manufacture a sync failure from a local timer. Firestore/Auth already
        // report real failures through their listeners. The previous watchdog could fire
        // while a legitimate read was still in flight, which made Home show a false error
        // and disabled wallet entry. Core profile readiness is now emitted as soon as the
        // profile document loads; secondary quest/rank/Cup hydration continues quietly.
    }

    fun savePublicProfile(displayName: String, avatarId: String) {
        if (appState.walletActionInProgress) {
            appState = appState.copy(lastMessage = "Please wait for the current action to finish.")
            return
        }
        firebaseRefreshGeneration += 1
        appState = appState.copy(
            walletActionInProgress = true,
            lastMessage = "Saving your public profile…",
        )
        repository.updatePublicProfile(
            displayName = displayName,
            avatarId = avatarId,
        ) { nextState ->
            applyRepositoryState(nextState)
        }
    }

    fun connectWallet() {
        if (appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Wallet is already connected.")
            return
        }
        if (!beginQuestAction(QuestIds.WALLET_CONNECT, "Opening your wallet…")) return

        scope.launch {
            when (val result = walletRepository.connectWallet()) {
                is WalletConnectResult.Connected -> {
                    appState = appState.copy(lastMessage = "Wallet connected.")
                    repository.saveWalletConnection(
                        publicKey = result.publicKey,
                        accountLabel = result.accountLabel,
                    ) { nextState ->
                        applyRepositoryState(nextState, QuestIds.WALLET_CONNECT)
                    }
                }
                WalletConnectResult.NoWalletFound -> {
                    failQuestAction(
                        questId = QuestIds.WALLET_CONNECT,
                        message = "No compatible Solana wallet was found on this device.",
                    )
                }
                is WalletConnectResult.Failure -> {
                    failQuestAction(
                        questId = QuestIds.WALLET_CONNECT,
                        message = "Wallet connection failed: ${result.message}",
                    )
                }
            }
        }
    }

    fun disconnectWallet() {
        if (appState.walletActionInProgress) {
            appState = appState.copy(lastMessage = "Please wait for the current action to finish.")
            return
        }
        firebaseRefreshGeneration += 1
        appState = appState.copy(
            walletActionInProgress = true,
            activeQuestId = null,
            lastMessage = "Disconnecting wallet…",
        )

        scope.launch {
            when (val result = walletRepository.disconnectWallet()) {
                WalletDisconnectResult.Disconnected -> {
                    repository.clearWalletConnection { nextState ->
                        applyRepositoryState(nextState)
                    }
                }
                WalletDisconnectResult.NoWalletFound -> {
                    repository.clearWalletConnection { nextState ->
                        applyRepositoryState(
                            nextState.copy(lastMessage = "Wallet disconnected from Radiant Circle."),
                        )
                    }
                }
                is WalletDisconnectResult.Failure -> {
                    appState = appState.copy(
                        walletActionInProgress = false,
                        activeQuestId = null,
                        lastMessage = "Wallet disconnect failed: ${result.message}",
                    )
                }
            }
        }
    }

    fun saveDailyCheckIn(quest: QuestPreview) {
        if (!beginQuestAction(QuestIds.DAILY_CHECK_IN, "Checking in…")) return
        repository.completeDailyFirebaseCheckIn(quest) { nextState ->
            applyRepositoryState(nextState, QuestIds.DAILY_CHECK_IN)
        }
    }

    fun signDailyProof() {
        if (!appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Connect a Solana wallet before signing the daily proof.")
            return
        }
        if (!beginQuestAction(QuestIds.SIGN_DAILY_PROOF, "Opening your wallet to sign today’s challenge…")) return

        scope.launch {
            when (val result = walletRepository.signDailyProof(appState.todayKey)) {
                is WalletSignedProofResult.Signed -> {
                    appState = appState.copy(lastMessage = "Daily proof signed.")
                    markQuestStatus(QuestIds.SIGN_DAILY_PROOF, QuestStatus.Syncing)
                    repository.saveDailySignedProof(
                        walletAddress = result.walletAddress,
                        message = result.message,
                        signature = result.signature,
                    ) { nextState ->
                        applyRepositoryState(nextState, QuestIds.SIGN_DAILY_PROOF)
                    }
                }
                WalletSignedProofResult.NoWalletFound -> {
                    failQuestAction(
                        questId = QuestIds.SIGN_DAILY_PROOF,
                        message = "No compatible Solana wallet was found for signing.",
                    )
                }
                is WalletSignedProofResult.Failure -> {
                    failQuestAction(
                        questId = QuestIds.SIGN_DAILY_PROOF,
                        message = "Daily proof signing failed: ${result.message}",
                    )
                }
            }
        }
    }

    fun sendDailyMemoProof() {
        if (!appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Connect a Solana wallet before submitting an on-chain memo proof.")
            return
        }
        if (!beginQuestAction(QuestIds.ON_CHAIN_PROOF, "Opening your wallet for today’s memo quest…")) return

        scope.launch {
            when (val result = walletRepository.sendDailyMemoProof(appState.todayKey)) {
                is WalletMemoProofResult.Submitted -> {
                    appState = appState.copy(lastMessage = "Memo quest submitted.")
                    markQuestStatus(QuestIds.ON_CHAIN_PROOF, QuestStatus.Syncing)
                    repository.saveDailyMemoProof(
                        walletAddress = result.walletAddress,
                        memoText = result.memoText,
                        transactionSignature = result.transactionSignature,
                        explorerUrl = result.explorerUrl,
                    ) { nextState ->
                        applyRepositoryState(nextState, QuestIds.ON_CHAIN_PROOF)
                    }
                }
                WalletMemoProofResult.NoWalletFound -> {
                    failQuestAction(
                        questId = QuestIds.ON_CHAIN_PROOF,
                        message = "No compatible Solana wallet was found for the memo quest.",
                    )
                }
                is WalletMemoProofResult.Failure -> {
                    failQuestAction(
                        questId = QuestIds.ON_CHAIN_PROOF,
                        message = "Memo couldn’t be submitted. Check your wallet, make sure it has test SOL for the network fee, and try again.",
                    )
                }
            }
        }
    }

    fun checkSkrBalance() {
        if (!appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Connect a Solana wallet before scanning your SKR Passport.")
            return
        }
        if (!beginQuestAction(QuestIds.SKR_HOLDER, "Refreshing your SKR Passport…")) return

        scope.launch {
            when (val result = skrRepository.fetchSkrBalance(appState.user.walletAddress)) {
                is SkrBalanceResult.Success -> {
                    val snapshot = result.snapshot
                    appState = appState.copy(
                        lastMessage = "SKR Passport updated: ${snapshot.tierLabel}.",
                    )
                    repository.saveSkrBalanceSnapshot(snapshot) { nextState ->
                        applyRepositoryState(nextState, QuestIds.SKR_HOLDER)
                    }
                }
                is SkrBalanceResult.Failure -> {
                    failQuestAction(
                        questId = QuestIds.SKR_HOLDER,
                        message = "SKR balance check failed: ${result.message}",
                    )
                }
            }
        }
    }

    fun claimDailyRadiantChest() {
        if (!beginChestAction("Opening your Daily Radiant Chest…")) return

        // Start the real save immediately. A very fast response is held only long
        // enough for the opening pose to register; slow network responses reveal as
        // soon as they arrive with no extra artificial wait.
        val startedAt = SystemClock.elapsedRealtime()
        repository.claimDailyRadiantChest { nextState ->
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            val remaining = (RadiantChestPresentationRules.MINIMUM_OPENING_MS - elapsed).coerceAtLeast(0L)
            scope.launch {
                if (remaining > 0L) delay(remaining)
                applyRepositoryState(nextState, QuestIds.DAILY_RADIANT_CHEST)
            }
        }
    }

    fun completeRadiantRun(result: RadiantRunResult) {
        if (appState.walletActionInProgress) {
            appState = appState.copy(lastMessage = "Please wait for the current save to finish.")
            return
        }
        if (!appState.radiantRun.canPlay) {
            appState = appState.copy(lastMessage = "No Rush Tickets left. Complete daily quests or the Daily Radiant Chest to earn more.")
            return
        }

        firebaseRefreshGeneration += 1
        appState = appState.copy(
            walletActionInProgress = true,
            activeQuestId = QuestIds.RADIANT_RUN,
            lastMessage = "Saving Radiant Rush score and opening capsule…",
        )
        repository.completeRadiantRun(result) { nextState ->
            applyRepositoryState(nextState, QuestIds.RADIANT_RUN)
        }
    }

    fun completeQuest(quest: QuestPreview) {
        if (appState.walletActionInProgress) {
            appState = appState.copy(
                lastMessage = "Please wait — ${appState.activeQuestId ?: "another quest"} is still working.",
            )
            return
        }

        when (quest.status) {
            QuestStatus.Completed -> {
                // SKR Passport is a read-only balance refresh, not a one-shot signing
                // action. Re-scan is allowed; Firebase grant rules are idempotent.
                if (quest.id != QuestIds.SKR_HOLDER) {
                    appState = appState.copy(lastMessage = "${quest.title} is already completed for today.")
                    return
                }
            }
            QuestStatus.Syncing -> {
                appState = appState.copy(lastMessage = "${quest.title} is still saving. Please wait for Done.")
                return
            }
            QuestStatus.Blocked -> {
                appState = appState.copy(lastMessage = "${quest.title} needs a connected wallet first.")
                return
            }
            QuestStatus.Locked, QuestStatus.Ready -> Unit
        }

        when (quest.id) {
            QuestIds.DAILY_CHECK_IN -> saveDailyCheckIn(quest)
            QuestIds.WALLET_CONNECT -> connectWallet()
            QuestIds.SIGN_DAILY_PROOF -> signDailyProof()
            QuestIds.ON_CHAIN_PROOF -> sendDailyMemoProof()
            QuestIds.SKR_HOLDER -> checkSkrBalance()
            else -> appState = appState.copy(
                lastMessage = "${quest.title} isn’t available yet.",
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshFirebase()
    }

    if (!enteredShell) {
        WelcomeScreen(
            onEnterDemoShell = { enteredShell = true },
        )
        return
    }

    if (showRadiantRun) {
        RadiantRunScreen(
            uiState = appState,
            onSubmitResult = ::completeRadiantRun,
            onExit = {
                if (!appState.walletActionInProgress) {
                    // Radiant Rush is launched from Today. Return the player to the
                    // Rush section instead of dropping them at Home and breaking replay flow.
                    shellDestination = AppDestination.Quests
                    todayReturnToRushRequest += 1
                    showRadiantRun = false
                }
            },
        )
        return
    }

    RadiantRushShell(
        uiState = appState,
        destination = shellDestination,
        onDestinationChange = { shellDestination = it },
        todayReturnToRushRequest = todayReturnToRushRequest,
        onTodayReturnToRushHandled = { todayReturnToRushRequest = 0 },
        onRetryFirebase = ::refreshFirebase,
        onCompleteQuest = ::completeQuest,
        onConnectWallet = ::connectWallet,
        onDisconnectWallet = ::disconnectWallet,
        onSavePublicProfile = ::savePublicProfile,
        onClaimRadiantChest = ::claimDailyRadiantChest,
        onPlayRadiantRun = {
            if (appState.isFirebaseReady && appState.radiantRun.canPlay && !appState.walletActionInProgress) {
                showRadiantRun = true
            } else {
                appState = appState.copy(lastMessage = "Radiant Rush needs cloud sync and at least 1 Rush Ticket.")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadiantRushShell(
    uiState: RushUiState,
    destination: AppDestination,
    onDestinationChange: (AppDestination) -> Unit,
    todayReturnToRushRequest: Int,
    onTodayReturnToRushHandled: () -> Unit,
    onRetryFirebase: () -> Unit,
    onCompleteQuest: (QuestPreview) -> Unit,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
    onSavePublicProfile: (String, String) -> Unit,
    onClaimRadiantChest: () -> Unit,
    onPlayRadiantRun: () -> Unit,
) {
    val responsive = rememberResponsiveUiSpec()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (destination) {
                            AppDestination.Home -> "Radiant Circle"
                            AppDestination.Quests -> "Daily Plan"
                            AppDestination.Badges -> "Badges"
                            AppDestination.Leaderboard -> "Ranks"
                            AppDestination.Profile -> "Profile"
                            AppDestination.Demo -> "Guide"
                        },
                        softWrap = false,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        modifier = Modifier.testTag(
                            when (item) {
                                AppDestination.Home -> UiTestTags.NAV_HOME
                                AppDestination.Quests -> UiTestTags.NAV_QUESTS
                                AppDestination.Badges -> UiTestTags.NAV_BADGES
                                AppDestination.Leaderboard -> UiTestTags.NAV_LEADERBOARD
                                AppDestination.Profile -> UiTestTags.NAV_PROFILE
                                AppDestination.Demo -> UiTestTags.NAV_DEMO
                            },
                        ),
                        selected = destination == item,
                        onClick = { onDestinationChange(item) },
                        alwaysShowLabel = !responsive.isCompact && !responsive.hasLargeText,
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                            )
                        },
                        label = {
                            AdaptiveNavLabel(
                                text = item.label,
                                compactText = when (item) {
                                    AppDestination.Home -> "Home"
                                    AppDestination.Quests -> "Today"
                                    AppDestination.Badges -> "Badge"
                                    AppDestination.Leaderboard -> "Ranks"
                                    AppDestination.Profile -> "Me"
                                    AppDestination.Demo -> "Guide"
                                },
                                tinyText = when (item) {
                                    AppDestination.Home -> "Home"
                                    AppDestination.Quests -> "Today"
                                    AppDestination.Badges -> "Badge"
                                    AppDestination.Leaderboard -> "Rank"
                                    AppDestination.Profile -> "Me"
                                    AppDestination.Demo -> "Guide"
                                },
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScreenContent(
                destination = destination,
                contentPadding = PaddingValues(),
                uiState = uiState,
                onRetryFirebase = onRetryFirebase,
                onCompleteQuest = onCompleteQuest,
                onConnectWallet = onConnectWallet,
                onDisconnectWallet = onDisconnectWallet,
                onSavePublicProfile = onSavePublicProfile,
                onClaimRadiantChest = onClaimRadiantChest,
                onPlayRadiantRun = onPlayRadiantRun,
                returnToRushRequest = todayReturnToRushRequest,
                onReturnToRushHandled = onTodayReturnToRushHandled,
                onNavigate = onDestinationChange,
            )
        }
    }
}

@Composable
private fun ScreenContent(
    destination: AppDestination,
    contentPadding: PaddingValues,
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
    onCompleteQuest: (QuestPreview) -> Unit,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
    onSavePublicProfile: (String, String) -> Unit,
    onClaimRadiantChest: () -> Unit,
    onPlayRadiantRun: () -> Unit,
    returnToRushRequest: Int,
    onReturnToRushHandled: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
) {
    when (destination) {
        AppDestination.Home -> HomeScreen(
            contentPadding = contentPadding,
            uiState = uiState,
            onRetryFirebase = onRetryFirebase,
            onConnectWallet = onConnectWallet,
            onOpenToday = { onNavigate(AppDestination.Quests) },
        )
        AppDestination.Quests -> QuestsScreen(
            contentPadding = contentPadding,
            uiState = uiState,
            onRetryFirebase = onRetryFirebase,
            onCompleteQuest = onCompleteQuest,
            onConnectWallet = onConnectWallet,
            onClaimRadiantChest = onClaimRadiantChest,
            onPlayRadiantRun = onPlayRadiantRun,
            returnToRushRequest = returnToRushRequest,
            onReturnToRushHandled = onReturnToRushHandled,
        )
        AppDestination.Badges -> BadgesScreen(contentPadding, uiState.badges)
        AppDestination.Leaderboard -> LeaderboardScreen(contentPadding, uiState)
        AppDestination.Profile -> ProfileScreen(
            contentPadding = contentPadding,
            uiState = uiState,
            onRetryFirebase = onRetryFirebase,
            onConnectWallet = onConnectWallet,
            onDisconnectWallet = onDisconnectWallet,
            onSavePublicProfile = onSavePublicProfile,
        )
        AppDestination.Demo -> DemoScreen(contentPadding, uiState)
    }
}
