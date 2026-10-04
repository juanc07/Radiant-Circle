package com.thinkblox.radiantrush.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.OreAccruedToday
import com.thinkblox.radiantrush.data.OrePortfolioSnapshot
import com.thinkblox.radiantrush.data.OrePortfolioUiState
import com.thinkblox.radiantrush.logic.OrePortfolioPresentationRules
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrePortfolioScreen(
    state: OrePortfolioUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val responsive = rememberResponsiveUiSpec()

    Scaffold(
        modifier = Modifier.testTag(UiTestTags.ORE_PORTFOLIO_SCREEN),
        topBar = {
            TopAppBar(
                title = { Text("ORE") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier.testTag(UiTestTags.ORE_PORTFOLIO_REFRESH),
                        enabled = state !is OrePortfolioUiState.Loading && state !is OrePortfolioUiState.NoWallet,
                        onClick = onRefresh,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh from Solana")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = responsive.screenPadding,
                end = responsive.screenPadding,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (responsive.isTiny) 10.dp else 14.dp),
        ) {
            when (state) {
                OrePortfolioUiState.NoWallet -> item {
                    CompactStatusCard(
                        icon = Icons.Filled.AccountBalanceWallet,
                        title = "Connect wallet",
                        body = "Connect your Solana wallet to view verified ORE on Mainnet.",
                    )
                }

                is OrePortfolioUiState.Loading -> item {
                    Card(shape = RoundedCornerShape(22.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Syncing ORE", fontWeight = FontWeight.Bold)
                                Text("Reading verified Mainnet state…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                is OrePortfolioUiState.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CompactStatusCard(
                            icon = Icons.Filled.Refresh,
                            title = "Couldn't refresh ORE",
                            body = state.message,
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth().heightIn(min = responsive.buttonHeight),
                            onClick = onRefresh,
                        ) { Text("Try again") }
                    }
                }

                is OrePortfolioUiState.Ready -> {
                    val snapshot = state.snapshot
                    item { PortfolioHero(snapshot) }
                    item { PortfolioMetrics(snapshot, state.accruedToday) }
                    item { WalletActionsSafetyCard() }
                    item { CircleStakePreviewCard() }
                    item { AdvancedProofCard(snapshot) }
                }
            }
        }
    }
}

@Composable
private fun PortfolioHero(snapshot: OrePortfolioSnapshot) {
    val totalRaw = OrePortfolioPresentationRules.totalPositionRaw(snapshot.liquidRaw, snapshot.stakedRaw)
    val totalDisplay = OrePortfolioPresentationRules.overviewAmount(totalRaw)
    val stakedPercent = OrePortfolioPresentationRules.stakedPercent(snapshot.liquidRaw, snapshot.stakedRaw)
    val active = snapshot.stakedRaw.toBigIntegerOrNull()?.signum() == 1

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("ORE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Your ORE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(shortAddress(snapshot.walletAddress), style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(
                    onClick = {},
                    leadingIcon = { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Mainnet") },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TOTAL POSITION", style = MaterialTheme.typography.labelMedium)
                Text(
                    totalDisplay,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (active) "$stakedPercent% currently staked" else "No active stake",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PortfolioMetrics(snapshot: OrePortfolioSnapshot, accruedToday: OreAccruedToday) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.AccountBalanceWallet,
                label = "Available",
                value = OrePortfolioPresentationRules.overviewAmount(snapshot.liquidRaw),
            )
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Lock,
                label = "Staked",
                value = OrePortfolioPresentationRules.overviewAmount(snapshot.stakedRaw),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.AutoAwesome,
                label = "Claimable",
                value = OrePortfolioPresentationRules.overviewAmount(snapshot.unclaimedRewardsRaw),
            )
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Observed today",
                value = OrePortfolioPresentationRules.overviewAmount(accruedToday.rawAmount),
            )
        }
        Text(
            "Today is measured from this app's saved baseline. Claimable is live protocol state from Solana.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactMetricTile(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                minLines = 2,
            )
        }
    }
}

@Composable
private fun WalletActionsSafetyCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wallet-signed ORE actions", fontWeight = FontWeight.Bold)
                    Text("Stake • Claim • Withdraw", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                "This checkpoint is read-only. Actions stay locked until the Mainnet instruction, wallet signature, submission, and resulting on-chain state are all verified end-to-end.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CircleStakePreviewCard() {
    Card(shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Circle Stake", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "The social layer will show opt-in Circle participation after real staking actions are proven. Your ORE will stay in your own wallet and protocol stake — Radiant Circle will not pool custody.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AdvancedProofCard(snapshot: OrePortfolioSnapshot) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.testTag(UiTestTags.ORE_POSITION_MATCH),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Advanced • Solana proof", fontWeight = FontWeight.Bold)
                    Text(
                        "Exact balances, programs, PDAs • ${checkedTime(snapshot.checkedAtClientMs)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }

            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Exact portfolio", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    DetailLine("Liquid", snapshot.liquidDisplay)
                    DetailLine("Staked", snapshot.stakedDisplay)
                    DetailLine("Claimable", snapshot.unclaimedRewardsDisplay)
                    DetailLine("Lifetime rewards", snapshot.lifetimeRewardsDisplay)
                    Spacer(Modifier.height(4.dp))
                    DetailLine("Wallet", shortAddress(snapshot.walletAddress))
                    DetailLine("Network", snapshot.network)
                    DetailLine("ORE mint", shortAddress(snapshot.mint))
                    snapshot.rpcSlot?.let { DetailLine("RPC slot", it.toString()) }

                    snapshot.protocolPositions.forEach { proof ->
                        Spacer(Modifier.height(6.dp))
                        Text(proof.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        DetailLine("Program", shortAddress(proof.programId))
                        DetailLine("Stake PDA", shortAddress(proof.stakeAddress))
                        DetailLine("Account", if (proof.accountFound) "verified" else "not found")
                        if (proof.accountFound) {
                            DetailLine("Staked", proof.stakedDisplay)
                            DetailLine("Claimable", proof.yieldDisplay)
                            DetailLine("Lifetime", proof.lifetimeRewardsDisplay)
                        }
                    }

                    if (snapshot.hasStakeAccount) {
                        Spacer(Modifier.height(6.dp))
                        Text("Protocol timing", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        DetailLine("Stored rewards", snapshot.storedRewardsDisplay)
                        DetailLine("Pending accrual", snapshot.pendingAccrualDisplay)
                        protocolTimeText("Last deposit", snapshot.lastDepositAtSeconds)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        protocolTimeText("Last withdraw", snapshot.lastWithdrawAtSeconds)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        protocolTimeText("Last claim", snapshot.lastClaimAtSeconds)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun CompactStatusCard(icon: ImageVector, title: String, body: String) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun checkedTime(clientMs: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(clientMs))

private fun protocolTimeText(label: String, epochSeconds: Long?): String? =
    epochSeconds?.takeIf { it > 0L }?.let {
        "$label: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it * 1_000L))}"
    }

private fun shortAddress(value: String): String = when {
    value.length <= 12 -> value
    else -> "${value.take(5)}…${value.takeLast(5)}"
}
