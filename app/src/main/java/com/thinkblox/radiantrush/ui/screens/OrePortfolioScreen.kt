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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.thinkblox.radiantrush.data.OreAccruedToday
import com.thinkblox.radiantrush.data.OrePortfolioSnapshot
import com.thinkblox.radiantrush.data.OrePortfolioUiState
import com.thinkblox.radiantrush.data.OreStakeAction
import com.thinkblox.radiantrush.data.OreStakeActionUiState
import com.thinkblox.radiantrush.data.OreStakeTransactionReceipt
import com.thinkblox.radiantrush.logic.OrePortfolioPresentationRules
import com.thinkblox.radiantrush.logic.OreStakingRules
import com.thinkblox.radiantrush.ui.components.rememberResponsiveUiSpec
import com.thinkblox.radiantrush.ui.testing.UiTestTags
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrePortfolioScreen(
    state: OrePortfolioUiState,
    actionState: OreStakeActionUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onStake: (String) -> Unit,
    onWithdraw: (String) -> Unit,
    onClaim: () -> Unit,
    onDismissActionReceipt: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val responsive = rememberResponsiveUiSpec()

    actionState.receipt?.let { receipt ->
        OreTransactionReceiptDialog(
            receipt = receipt,
            onDismiss = onDismissActionReceipt,
        )
    }

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
                    item {
                        OreActionsCard(
                            snapshot = snapshot,
                            actionState = actionState,
                            onStake = onStake,
                            onWithdraw = onWithdraw,
                            onClaim = onClaim,
                        )
                    }
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
private fun OreTransactionReceiptDialog(
    receipt: OreStakeTransactionReceipt,
    onDismiss: () -> Unit,
) {
    val beforeLiquid = receipt.beforeLiquidRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val afterLiquid = receipt.afterLiquidRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val beforeStaked = receipt.beforeCurrentStakedRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val afterStaked = receipt.afterCurrentStakedRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val beforeClaimable = receipt.beforeClaimableRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val afterClaimable = receipt.afterClaimableRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO

    fun signedDelta(before: java.math.BigInteger, after: java.math.BigInteger): String {
        val delta = after - before
        if (delta == java.math.BigInteger.ZERO) return "No change"
        val amount = OrePortfolioPresentationRules.overviewAmount(delta.abs())
        return if (delta.signum() > 0) "+$amount" else "−$amount"
    }

    val actionLabel = when (receipt.action) {
        OreStakeAction.Stake -> "Stake"
        OreStakeAction.Withdraw -> "Withdraw"
        OreStakeAction.Claim -> "Claim"
    }
    val title = if (receipt.verified) "$actionLabel verified" else "$actionLabel submitted"
    val actionAmount = OrePortfolioPresentationRules.overviewAmount(receipt.requestedRaw)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (receipt.verified) Icons.Filled.CheckCircle else Icons.Filled.Refresh,
                contentDescription = null,
            )
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (receipt.verified) {
                        when (receipt.action) {
                            OreStakeAction.Stake -> "$actionAmount was submitted to the verified ORE staking program."
                            OreStakeAction.Withdraw -> "$actionAmount was withdrawn from the current ORE stake position."
                            OreStakeAction.Claim -> "$actionAmount of staking rewards was claimed."
                        }
                    } else {
                        "The transaction was submitted to Mainnet, but the required ORE balance change is still awaiting verification."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (receipt.hasFreshAfterSnapshot) {
                    ReceiptChangeRow("Available", receipt.beforeLiquidRaw, receipt.afterLiquidRaw, signedDelta(beforeLiquid, afterLiquid))
                    ReceiptChangeRow("Staked", receipt.beforeCurrentStakedRaw, receipt.afterCurrentStakedRaw, signedDelta(beforeStaked, afterStaked))
                    ReceiptChangeRow("Claimable", receipt.beforeClaimableRaw, receipt.afterClaimableRaw, signedDelta(beforeClaimable, afterClaimable))
                    Text(
                        if (receipt.verified) {
                            "Values above are fresh Mainnet reads after the transaction, not estimated UI balances."
                        } else {
                            "Values above are the latest fresh Mainnet read. The required balance change has not been verified yet; refresh again before retrying the action."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text(
                        "A fresh post-transaction Mainnet state could not be loaded yet. Tap Refresh to verify the final ORE balances before retrying the action.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                receipt.transactionSignature?.let { signature ->
                    Text("Tx ${shortAddress(signature)}", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun ReceiptChangeRow(
    label: String,
    beforeRaw: String,
    afterRaw: String,
    delta: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(
            "${OrePortfolioPresentationRules.overviewAmount(beforeRaw)} → ${OrePortfolioPresentationRules.overviewAmount(afterRaw)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(delta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun OreActionsCard(
    snapshot: OrePortfolioSnapshot,
    actionState: OreStakeActionUiState,
    onStake: (String) -> Unit,
    onWithdraw: (String) -> Unit,
    onClaim: () -> Unit,
) {
    var amountText by remember(snapshot.walletAddress) { mutableStateOf("") }
    val liquidRaw = snapshot.liquidRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val currentStakedRaw = snapshot.currentStakedRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val currentClaimableRaw = snapshot.currentUnclaimedRewardsRaw.toBigIntegerOrNull() ?: java.math.BigInteger.ZERO
    val busy = actionState.inProgress

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    Text("Current verified stake program • Mainnet", style = MaterialTheme.typography.bodySmall)
                }
            }

            Text(
                "Every action opens your wallet for approval. Radiant Circle never receives your private key or custody of your ORE.",
                style = MaterialTheme.typography.bodySmall,
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("Solana Mainnet required", fontWeight = FontWeight.Bold)
                    Text(
                        "Radiant Circle requests Mainnet through Mobile Wallet Adapter. If your wallet is currently on Devnet/Testnet, switch it to Mainnet before approving the transaction.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amountText,
                onValueChange = { value ->
                    val normalized = value.replace(',', '.')
                    val parts = normalized.split('.', limit = 3)
                    val wholeDigitsOk = parts.firstOrNull()?.all(Char::isDigit) != false
                    val decimalDigitsOk = parts.getOrNull(1)?.let { it.length <= OreStakingRules.TOKEN_DECIMALS && it.all(Char::isDigit) } != false
                    if (
                        normalized.length <= 24 &&
                        parts.size <= 2 &&
                        wholeDigitsOk &&
                        decimalDigitsOk
                    ) {
                        amountText = normalized
                    }
                },
                enabled = !busy,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                label = { Text("ORE amount") },
                supportingText = {
                    Text(
                        "Available ${OrePortfolioPresentationRules.overviewAmount(snapshot.liquidRaw)} • Current stake ${OrePortfolioPresentationRules.overviewAmount(snapshot.currentStakedRaw)}",
                    )
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !busy && liquidRaw.signum() == 1,
                    onClick = { onStake(amountText) },
                ) { Text("Stake") }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !busy && currentStakedRaw.signum() == 1,
                    onClick = { onWithdraw(amountText) },
                ) { Text("Withdraw") }
            }
            Text(
                "Stake and Withdraw use the exact amount entered above. Claim collects the currently claimable staking reward.",
                style = MaterialTheme.typography.bodySmall,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && currentClaimableRaw.signum() == 1,
                onClick = onClaim,
            ) {
                Text(
                    if (currentClaimableRaw.signum() == 1) {
                        "Claim ${OrePortfolioPresentationRules.overviewAmount(snapshot.currentUnclaimedRewardsRaw)}"
                    } else {
                        "No rewards to claim"
                    },
                )
            }

            if (busy) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(actionState.message ?: "Waiting for wallet…", style = MaterialTheme.typography.bodySmall)
                }
            } else if (!actionState.message.isNullOrBlank()) {
                Text(
                    actionState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (actionState.verified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                actionState.transactionSignature?.let { signature ->
                    Text("Tx ${shortAddress(signature)}", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (snapshot.stakedRaw != snapshot.currentStakedRaw) {
                Text(
                    "Legacy ORE stake is shown in Total Position but is read-only here. Stake/Withdraw/Claim buttons operate only on ${shortAddress(OreStakingRules.ORE_STAKE_PROGRAM_ID)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                    DetailLine("Verified stake source", OreStakingRules.ORE_STAKE_VERIFIED_SOURCE_COMMIT.take(9))
                    DetailLine("Compound fee (lamports)", snapshot.currentCompoundFeeLamportsRaw)
                    DetailLine("Compound reserve (lamports)", snapshot.currentCompoundFeeReserveLamportsRaw)
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
