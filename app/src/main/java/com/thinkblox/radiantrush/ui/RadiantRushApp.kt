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
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.firebase.FirebaseRadiantRepository
import com.thinkblox.radiantrush.solana.MobileWalletRepository
import com.thinkblox.radiantrush.solana.WalletConnectResult
import com.thinkblox.radiantrush.solana.WalletDisconnectResult
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

    fun completeQuest(quest: QuestPreview) {
        when (quest.id) {
            QuestIds.DAILY_CHECK_IN -> repository.completeDailyFirebaseCheckIn(quest) { nextState ->
                appState = nextState
            }
            QuestIds.WALLET_CONNECT -> connectWallet()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (destination) {
                            AppDestination.Home -> "Radiant Rush"
                            AppDestination.Quests -> "Daily Quests"
                            AppDestination.Badges -> "Badges"
                            AppDestination.Leaderboard -> "Leaderboard"
                            AppDestination.Profile -> "Profile"
                        },
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
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                            )
                        },
                        label = {
                            Text(text = item.label)
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
