package com.thinkblox.radiantrush.ui

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
import com.thinkblox.radiantrush.data.AppDestination
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.QuestStatus
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.firebase.FirebaseRadiantRepository
import com.thinkblox.radiantrush.solana.MobileWalletRepository
import com.thinkblox.radiantrush.solana.WalletConnectResult
import com.thinkblox.radiantrush.solana.WalletDisconnectResult
import com.thinkblox.radiantrush.solana.WalletMemoProofResult
import com.thinkblox.radiantrush.solana.WalletSignedProofResult
import com.thinkblox.radiantrush.ui.components.AdaptiveNavLabel
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.screens.BadgesScreen
import com.thinkblox.radiantrush.ui.screens.HomeScreen
import com.thinkblox.radiantrush.ui.screens.LeaderboardScreen
import com.thinkblox.radiantrush.ui.screens.ProfileScreen
import com.thinkblox.radiantrush.ui.screens.QuestsScreen
import com.thinkblox.radiantrush.ui.screens.WelcomeScreen
import kotlinx.coroutines.launch

@Composable
fun RadiantRushApp(walletRepository: MobileWalletRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) {
        FirebaseRadiantRepository(context.applicationContext)
    }
    var appState by remember { mutableStateOf(PreviewContent.defaultState()) }
    var enteredShell by rememberSaveable { mutableStateOf(false) }

    fun markQuestStatus(questId: String, status: QuestStatus) {
        appState = appState.copy(
            quests = appState.quests.map { quest ->
                if (quest.id == questId) quest.copy(status = status) else quest
            },
        )
    }

    fun applyRepositoryState(nextState: RushUiState, activeQuestId: String? = null) {
        val keepActionLocked = nextState.firebaseStatus == com.thinkblox.radiantrush.data.FirebaseStatus.Loading
        appState = if (keepActionLocked && activeQuestId != null) {
            appState.copy(
                firebaseStatus = nextState.firebaseStatus,
                lastMessage = nextState.lastMessage,
                walletActionInProgress = true,
                quests = appState.quests.map { quest ->
                    if (quest.id == activeQuestId) quest.copy(status = QuestStatus.Syncing) else quest
                },
            )
        } else {
            nextState.copy(walletActionInProgress = false)
        }
    }

    fun refreshFirebase() {
        repository.bootstrap { nextState ->
            appState = nextState
        }
    }

    fun connectWallet() {
        if (appState.walletActionInProgress) return
        appState = appState.copy(
            walletActionInProgress = true,
            lastMessage = "Opening an MWA-compatible Solana wallet…",
        )

        scope.launch {
            when (val result = walletRepository.connectWallet()) {
                is WalletConnectResult.Connected -> {
                    repository.saveWalletConnection(
                        publicKey = result.publicKey,
                        accountLabel = result.accountLabel,
                    ) { nextState ->
                        appState = nextState.copy(walletActionInProgress = false)
                    }
                }
                WalletConnectResult.NoWalletFound -> {
                    appState = appState.copy(
                        walletActionInProgress = false,
                        lastMessage = "No MWA-compatible wallet found. Install a Solana Mobile compatible wallet on this Android device.",
                    )
                }
                is WalletConnectResult.Failure -> {
                    appState = appState.copy(
                        walletActionInProgress = false,
                        lastMessage = "Wallet connection failed: ${result.message}",
                    )
                }
            }
        }
    }

    fun disconnectWallet() {
        if (appState.walletActionInProgress) return
        appState = appState.copy(
            walletActionInProgress = true,
            lastMessage = "Disconnecting wallet…",
        )

        scope.launch {
            when (val result = walletRepository.disconnectWallet()) {
                WalletDisconnectResult.Disconnected -> {
                    repository.clearWalletConnection { nextState ->
                        appState = nextState.copy(walletActionInProgress = false)
                    }
                }
                WalletDisconnectResult.NoWalletFound -> {
                    repository.clearWalletConnection { nextState ->
                        appState = nextState.copy(
                            walletActionInProgress = false,
                            lastMessage = "Wallet app was not found, but local Firebase wallet state was cleared.",
                        )
                    }
                }
                is WalletDisconnectResult.Failure -> {
                    appState = appState.copy(
                        walletActionInProgress = false,
                        lastMessage = "Wallet disconnect failed: ${result.message}",
                    )
                }
            }
        }
    }

    fun signDailyProof() {
        if (appState.walletActionInProgress) return
        if (!appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Connect a Solana wallet before signing the daily proof.")
            return
        }

        appState = appState.copy(
            walletActionInProgress = true,
            lastMessage = "Opening wallet to sign today’s proof message…",
        )

        scope.launch {
            when (val result = walletRepository.signDailyProof(appState.todayKey)) {
                is WalletSignedProofResult.Signed -> {
                    appState = appState.copy(
                        lastMessage = "Wallet signed today’s proof. Saving to Firebase…",
                    )
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
                    appState = appState.copy(
                        walletActionInProgress = false,
                        lastMessage = "No MWA-compatible wallet found for message signing.",
                    )
                }
                is WalletSignedProofResult.Failure -> {
                    appState = appState.copy(
                        walletActionInProgress = false,
                        lastMessage = "Daily proof signing failed: ${result.message}",
                    )
                }
            }
        }
    }

    fun sendDailyMemoProof() {
        if (appState.walletActionInProgress) return
        if (!appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Connect a Solana wallet before submitting an on-chain memo proof.")
            return
        }

        appState = appState.copy(
            walletActionInProgress = true,
            lastMessage = "Opening wallet to sign and submit a devnet memo transaction…",
        )

        scope.launch {
            when (val result = walletRepository.sendDailyMemoProof(appState.todayKey)) {
                is WalletMemoProofResult.Submitted -> {
                    appState = appState.copy(
                        lastMessage = "Wallet submitted the memo. Saving transaction proof to Firebase…",
                    )
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
                    appState = appState.copy(
                        walletActionInProgress = false,
                        lastMessage = "No MWA-compatible wallet found for memo transaction.",
                    )
                }
                is WalletMemoProofResult.Failure -> {
                    appState = appState.copy(
                        walletActionInProgress = false,
                        lastMessage = "Memo transaction failed: ${result.message}. Make sure Phantom is in test/devnet mode and has devnet SOL for fees.",
                    )
                }
            }
        }
    }

    fun completeQuest(quest: QuestPreview) {
        when (quest.status) {
            QuestStatus.Completed -> {
                appState = appState.copy(lastMessage = "${quest.title} is already completed for today.")
                return
            }
            QuestStatus.Syncing -> {
                appState = appState.copy(lastMessage = "${quest.title} is still saving. Please wait a moment.")
                return
            }
            QuestStatus.Blocked -> {
                appState = appState.copy(lastMessage = "${quest.title} needs a connected wallet first.")
                return
            }
            QuestStatus.Locked, QuestStatus.Ready -> Unit
        }

        when (quest.id) {
            QuestIds.DAILY_CHECK_IN -> repository.completeDailyFirebaseCheckIn(quest) { nextState ->
                appState = nextState
            }
            QuestIds.WALLET_CONNECT -> connectWallet()
            QuestIds.SIGN_DAILY_PROOF -> signDailyProof()
            QuestIds.ON_CHAIN_PROOF -> sendDailyMemoProof()
            else -> appState = appState.copy(
                lastMessage = "${quest.title} unlocks in a later phase when the real implementation exists.",
            )
        }
    }

    LaunchedEffect(Unit) {
        refreshFirebase()
    }

    if (!enteredShell) {
        WelcomeScreen(
            uiState = appState,
            onEnterDemoShell = { enteredShell = true },
            onRetryFirebase = ::refreshFirebase,
            onConnectWallet = ::connectWallet,
        )
        return
    }

    RadiantRushShell(
        uiState = appState,
        onRetryFirebase = ::refreshFirebase,
        onCompleteQuest = ::completeQuest,
        onConnectWallet = ::connectWallet,
        onDisconnectWallet = ::disconnectWallet,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadiantRushShell(
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
    onCompleteQuest: (QuestPreview) -> Unit,
    onConnectWallet: () -> Unit,
    onDisconnectWallet: () -> Unit,
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    val responsive = rememberResponsiveUiSpec()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (destination) {
                            AppDestination.Home -> "Radiant Rush"
                            AppDestination.Quests -> "Quests"
                            AppDestination.Badges -> "Badges"
                            AppDestination.Leaderboard -> "Ranks"
                            AppDestination.Profile -> "Profile"
                        },
                        maxLines = 1,
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
                        selected = destination == item,
                        onClick = { destination = item },
                        alwaysShowLabel = !responsive.isTiny,
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
                                    AppDestination.Home -> "Today"
                                    AppDestination.Quests -> "Quest"
                                    AppDestination.Badges -> "Badge"
                                    AppDestination.Leaderboard -> "Ranks"
                                    AppDestination.Profile -> "Me"
                                },
                                tinyText = when (item) {
                                    AppDestination.Home -> "Home"
                                    AppDestination.Quests -> "Quest"
                                    AppDestination.Badges -> "Badge"
                                    AppDestination.Leaderboard -> "Rank"
                                    AppDestination.Profile -> "Me"
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
) {
    when (destination) {
        AppDestination.Home -> HomeScreen(contentPadding, uiState, onCompleteQuest, onConnectWallet)
        AppDestination.Quests -> QuestsScreen(contentPadding, uiState, onCompleteQuest)
        AppDestination.Badges -> BadgesScreen(contentPadding, uiState.badges)
        AppDestination.Leaderboard -> LeaderboardScreen(contentPadding, uiState.leaderboard)
        AppDestination.Profile -> ProfileScreen(
            contentPadding = contentPadding,
            uiState = uiState,
            onRetryFirebase = onRetryFirebase,
            onConnectWallet = onConnectWallet,
            onDisconnectWallet = onDisconnectWallet,
        )
    }
}
