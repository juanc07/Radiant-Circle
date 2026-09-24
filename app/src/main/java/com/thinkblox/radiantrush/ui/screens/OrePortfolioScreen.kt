package com.thinkblox.radiantrush.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.OreAccruedToday
import com.thinkblox.radiantrush.data.OrePortfolioSnapshot
import com.thinkblox.radiantrush.data.OrePortfolioUiState
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
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
                        body = "Connect your Solana wallet to view your ORE portfolio.",
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
                                Text("Reading verified Solana state…", style = MaterialTheme.typography.bodySmall)
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
                    item {
                        MetricGrid(
                            snapshot = snapshot,
                            accruedToday = state.accruedToday.displayAmount,
                        )
                    }
                    item { ObservedRewardsCard(state.accruedToday) }
                    item { AllocationCard(snapshot) }
                    item { CompactProofCard(snapshot) }
                }
            }
        }
    }
}

@Composable
private fun PortfolioHero(snapshot: OrePortfolioSnapshot) {
    val active = snapshot.stakedRaw.toBigIntegerOrNull()?.signum() == 1
    val historical = !active && snapshot.lifetimeRewardsRaw.toBigIntegerOrNull()?.signum() == 1
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
                    label = { Text("On-chain") },
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("STAKED", style = MaterialTheme.typography.labelMedium)
                    Text(
                        snapshot.stakedDisplay,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val statusText = when {
                    active -> "Active stake"
                    historical -> "Past stake"
                    else -> "Not staked"
                }
                Text(statusText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun MetricGrid(snapshot: OrePortfolioSnapshot, accruedToday: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.AccountBalanceWallet,
                label = "Liquid",
                value = snapshot.liquidDisplay,
            )
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Lock,
                label = "Staked",
                value = snapshot.stakedDisplay,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.AutoAwesome,
                label = "Yield",
                value = snapshot.unclaimedRewardsDisplay,
            )
            CompactMetricTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.TrendingUp,
                label = "Today",
                value = accruedToday,
            )
        }
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AllocationCard(snapshot: OrePortfolioSnapshot) {
    val liquid = snapshot.liquidRaw.toBigIntegerOrNull()?.coerceAtLeast(BigInteger.ZERO) ?: BigInteger.ZERO
    val staked = snapshot.stakedRaw.toBigIntegerOrNull()?.coerceAtLeast(BigInteger.ZERO) ?: BigInteger.ZERO
    val total = liquid + staked
    val stakedFraction = if (total.signum() == 1) {
        BigDecimal(staked).divide(BigDecimal(total), 6, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else 0f
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Card(shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(modifier = Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().aspectRatio(1f)) {
                    val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(track, -90f, 360f, false, style = stroke)
                    if (stakedFraction > 0f) {
                        drawArc(primary, -90f, 360f * stakedFraction, false, style = stroke)
                    }
                }
                Text("${(stakedFraction * 100).toInt()}%", fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ORE allocation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Staked vs liquid", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Lifetime rewards  ${snapshot.lifetimeRewardsDisplay}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ObservedRewardsCard(accruedToday: OreAccruedToday) {
    val raw = accruedToday.rawAmount.toBigIntegerOrNull()?.coerceAtLeast(BigInteger.ZERO) ?: BigInteger.ZERO
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Card(shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TrendingUp, null, tint = primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Observed rewards today", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(accruedToday.displayAmount, style = MaterialTheme.typography.labelLarge)
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                val yStart = size.height * 0.78f
                val yEnd = if (raw.signum() == 1) size.height * 0.22f else yStart
                drawLine(track, Offset(0f, yStart), Offset(size.width, yStart), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(primary, Offset(0f, yStart), Offset(size.width, yEnd), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(primary, radius = 4.dp.toPx(), center = Offset(0f, yStart))
                drawCircle(primary, radius = 4.dp.toPx(), center = Offset(size.width, yEnd))
            }
            Text(
                "Baseline → latest refresh only. This is observed app data, not reconstructed continuous history.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CompactProofCard(snapshot: OrePortfolioSnapshot) {
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
                    Text("Verified on Solana", fontWeight = FontWeight.Bold)
                    Text(
                        "Mainnet • ${checkedTime(snapshot.checkedAtClientMs)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
            }

            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    DetailLine("Wallet", shortAddress(snapshot.walletAddress))
                    DetailLine("Primary stake PDA", shortAddress(snapshot.stakeAddress))
                    DetailLine("Primary program", shortAddress(snapshot.stakingProgramId))
                    DetailLine("Mint", shortAddress(snapshot.mint))
                    snapshot.protocolPositions.forEach { proof ->
                        Spacer(Modifier.height(4.dp))
                        Text(proof.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        DetailLine("Program", shortAddress(proof.programId))
                        DetailLine("Stake PDA", shortAddress(proof.stakeAddress))
                        DetailLine("Account", if (proof.accountFound) "verified" else "not found")
                        if (proof.accountFound) {
                            DetailLine("Staked", proof.stakedDisplay)
                            DetailLine("Yield", proof.yieldDisplay)
                        }
                    }
                    snapshot.rpcSlot?.let { DetailLine("RPC slot", it.toString()) }
                    DetailLine("Account", if (snapshot.hasStakeAccount) "verified" else "not found")
                    if (snapshot.hasStakeAccount) {
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
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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

private fun shortAddress(value: String): String =
    if (value.length <= 14) value else "${value.take(6)}…${value.takeLast(6)}"
