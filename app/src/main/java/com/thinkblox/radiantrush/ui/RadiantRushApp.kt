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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.thinkblox.radiantrush.data.AppDestination
import com.thinkblox.radiantrush.data.PreviewContent
import com.thinkblox.radiantrush.data.QuestPreview
import com.thinkblox.radiantrush.data.RushUiState
import com.thinkblox.radiantrush.firebase.FirebaseRadiantRepository
import com.thinkblox.radiantrush.ui.screens.BadgesScreen
import com.thinkblox.radiantrush.ui.screens.HomeScreen
import com.thinkblox.radiantrush.ui.screens.LeaderboardScreen
import com.thinkblox.radiantrush.ui.screens.ProfileScreen
import com.thinkblox.radiantrush.ui.screens.QuestsScreen
import com.thinkblox.radiantrush.ui.screens.WelcomeScreen

@Composable
fun RadiantRushApp() {
    val context = LocalContext.current
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

    fun completeQuest(quest: QuestPreview) {
        repository.completeDailyFirebaseCheckIn(quest) { nextState ->
            appState = nextState
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
        )
        return
    }

    RadiantRushShell(
        uiState = appState,
        onRetryFirebase = ::refreshFirebase,
        onCompleteQuest = ::completeQuest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadiantRushShell(
    uiState: RushUiState,
    onRetryFirebase: () -> Unit,
    onCompleteQuest: (QuestPreview) -> Unit,
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
) {
    when (destination) {
        AppDestination.Home -> HomeScreen(contentPadding, uiState, onCompleteQuest)
        AppDestination.Quests -> QuestsScreen(contentPadding, uiState, onCompleteQuest)
        AppDestination.Badges -> BadgesScreen(contentPadding, uiState.badges)
        AppDestination.Leaderboard -> LeaderboardScreen(contentPadding, uiState.leaderboard)
        AppDestination.Profile -> ProfileScreen(contentPadding, uiState, onRetryFirebase)
    }
}
