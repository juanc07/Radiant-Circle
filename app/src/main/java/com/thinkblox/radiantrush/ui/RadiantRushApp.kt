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
import com.thinkblox.radiantrush.data.FirebaseStatus
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.data.QuestIds
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
import com.thinkblox.radiantrush.ui.components.AdaptiveNavLabel
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.screens.BadgesScreen
import com.thinkblox.radiantrush.ui.screens.HomeScreen
import com.thinkblox.radiantrush.ui.screens.LeaderboardScreen
import com.thinkblox.radiantrush.ui.screens.ProfileScreen
import com.thinkblox.radiantrush.ui.screens.DemoScreen
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
    val skrRepository = remember {
        SkrBalanceRepository()
    }
    var appState by remember { mutableStateOf(PreviewContent.defaultState()) }
    var enteredShell by rememberSaveable { mutableStateOf(false) }

    fun questsWithStatus(questId: String, status: QuestStatus): List<QuestPreview> =
        appState.quests.map { quest ->
            if (quest.id == questId) quest.copy(status = status) else quest
        }

    fun markQuestStatus(questId: String, status: QuestStatus) {
        appState = appState.copy(quests = questsWithStatus(questId, status))
    }

    fun beginQuestAction(questId: String, message: String): Boolean {
        if (appState.walletActionInProgress) {
            appState = appState.copy(
                lastMessage = "Please wait — ${appState.activeQuestId ?: "another quest"} is still working.",
            )
            return false
        }

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
        val keepActionLocked = nextState.firebaseStatus == FirebaseStatus.Loading
        appState = if (keepActionLocked && activeQuestId != null) {
            appState.copy(
                firebaseStatus = nextState.firebaseStatus,
                lastMessage = nextState.lastMessage,
                walletActionInProgress = true,
                activeQuestId = activeQuestId,
                quests = questsWithStatus(activeQuestId, QuestStatus.Syncing),
            )
        } else {
            nextState.copy(walletActionInProgress = false, activeQuestId = null)
        }
    }

    fun refreshFirebase() {
        if (appState.walletActionInProgress) {
            appState = appState.copy(lastMessage = "Wait for the current quest action to finish before refreshing Firebase.")
            return
        }
        repository.bootstrap { nextState ->
            appState = nextState.copy(walletActionInProgress = false, activeQuestId = null)
        }
    }

    fun connectWallet() {
        if (appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Wallet is already connected.")
            return
        }
        if (!beginQuestAction(QuestIds.WALLET_CONNECT, "Opening Phantom or another MWA wallet. Approve the connection once.")) return

        scope.launch {
            when (val result = walletRepository.connectWallet()) {
                is WalletConnectResult.Connected -> {
                    appState = appState.copy(lastMessage = "Wallet approved. Saving public address to Firebase…")
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
                        message = "No MWA-compatible wallet found. Install Phantom or another Solana Mobile compatible wallet on this Android device.",
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
            appState = appState.copy(lastMessage = "Wait for the current quest action to finish before disconnecting.")
            return
        }
        appState = appState.copy(
            walletActionInProgress = true,
            activeQuestId = null,
            lastMessage = "Disconnecting wallet…",
        )

        scope.launch {
            when (val result = walletRepository.disconnectWallet()) {
                WalletDisconnectResult.Disconnected -> {
                    repository.clearWalletConnection { nextState ->
                        appState = nextState.copy(walletActionInProgress = false, activeQuestId = null)
                    }
                }
                WalletDisconnectResult.NoWalletFound -> {
                    repository.clearWalletConnection { nextState ->
                        appState = nextState.copy(
                            walletActionInProgress = false,
                            activeQuestId = null,
                            lastMessage = "Wallet app was not found, but local Firebase wallet state was cleared.",
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
        if (!beginQuestAction(QuestIds.DAILY_CHECK_IN, "Saving today’s Firebase check-in. Please wait for Done.")) return
        repository.completeDailyFirebaseCheckIn(quest) { nextState ->
            applyRepositoryState(nextState, QuestIds.DAILY_CHECK_IN)
        }
    }

    fun signDailyProof() {
        if (!appState.isWalletConnected) {
            appState = appState.copy(lastMessage = "Connect a Solana wallet before signing the daily proof.")
            return
        }
        if (!beginQuestAction(QuestIds.SIGN_DAILY_PROOF, "Opening Phantom for message signature. Wait for the approval screen.")) return

        scope.launch {
            when (val result = walletRepository.signDailyProof(appState.todayKey)) {
                is WalletSignedProofResult.Signed -> {
                    appState = appState.copy(lastMessage = "Wallet signed today’s proof. Saving signature to Firebase…")
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
                        message = "No MWA-compatible wallet found for message signing.",
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
        if (!beginQuestAction(QuestIds.ON_CHAIN_PROOF, "Preparing devnet memo, then opening Phantom for transaction approval.")) return

        scope.launch {
            when (val result = walletRepository.sendDailyMemoProof(appState.todayKey)) {
                is WalletMemoProofResult.Submitted -> {
                    appState = appState.copy(lastMessage = "Wallet submitted the memo. Saving transaction proof to Firebase…")
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
                        message = "No MWA-compatible wallet found for memo transaction.",
                    )
                }
                is WalletMemoProofResult.Failure -> {
                    failQuestAction(
                        questId = QuestIds.ON_CHAIN_PROOF,
                        message = "Memo transaction failed: ${result.message}. Keep Phantom on Devnet and make sure the wallet has devnet SOL for fees.",
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
        if (!beginQuestAction(QuestIds.SKR_HOLDER, "Scanning mainnet SKR by public wallet address. No Phantom popup is expected.")) return

        scope.launch {
            when (val result = skrRepository.fetchSkrBalance(appState.user.walletAddress)) {
                is SkrBalanceResult.Success -> {
                    val snapshot = result.snapshot
                    appState = appState.copy(
                        lastMessage = "SKR scan complete. Saving ${snapshot.tierLabel} tier to Firebase…",
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

    fun completeQuest(quest: QuestPreview) {
        if (appState.walletActionInProgress) {
            appState = appState.copy(
                lastMessage = "Please wait — ${appState.activeQuestId ?: "another quest"} is still working.",
            )
            return
        }

        when (quest.status) {
            QuestStatus.Completed -> {
                appState = appState.copy(lastMessage = "${quest.title} is already completed for today.")
                return
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
                            AppDestination.Demo -> "Demo"
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
                                    AppDestination.Demo -> "Demo"
                                },
                                tinyText = when (item) {
                                    AppDestination.Home -> "Home"
                                    AppDestination.Quests -> "Quest"
                                    AppDestination.Badges -> "Badge"
                                    AppDestination.Leaderboard -> "Rank"
                                    AppDestination.Profile -> "Me"
                                    AppDestination.Demo -> "Demo"
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
        AppDestination.Demo -> DemoScreen(contentPadding, uiState)
    }
}
