package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SentinelRepository
import com.example.data.TransactionHistoryItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

enum class TransactionStatusFilter {
    ALL, PENDING, CONFIRMED, REVERTED, DEPLOYMENTS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionMonitorTab(
    repository: SentinelRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val transactionList = repository.transactionHistory
    val lastBlock by repository.lastScannedBlock
    val lastGas by repository.lastScannedGasPrice
    val isRealMode by repository.isRealMainnetMode

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TransactionStatusFilter.ALL) }
    var selectedChainFilter by remember { mutableStateOf("All Networks") }

    var selectedTxForInspection by remember { mutableStateOf<TransactionHistoryItem?>(null) }
    var showBroadcastDialog by remember { mutableStateOf(false) }

    // Live filtering
    val filteredTransactions = remember(transactionList, searchQuery, selectedFilter, selectedChainFilter) {
        transactionList.filter { tx ->
            // Search match
            val matchesQuery = searchQuery.isBlank() ||
                    tx.txHash.contains(searchQuery, ignoreCase = true) ||
                    tx.contractAddress.contains(searchQuery, ignoreCase = true) ||
                    tx.type.contains(searchQuery, ignoreCase = true) ||
                    tx.asset.contains(searchQuery, ignoreCase = true) ||
                    tx.chain.contains(searchQuery, ignoreCase = true)

            // Status match
            val matchesStatus = when (selectedFilter) {
                TransactionStatusFilter.ALL -> true
                TransactionStatusFilter.PENDING -> tx.status.equals("PENDING", ignoreCase = true)
                TransactionStatusFilter.CONFIRMED -> tx.status.equals("CONFIRMED", ignoreCase = true) || tx.status.equals("SUCCESS", ignoreCase = true)
                TransactionStatusFilter.REVERTED -> tx.status.equals("REVERTED", ignoreCase = true) || tx.status.equals("FAILED", ignoreCase = true)
                TransactionStatusFilter.DEPLOYMENTS -> tx.type.contains("DEPLOYMENT", ignoreCase = true)
            }

            // Chain match
            val matchesChain = selectedChainFilter == "All Networks" || tx.chain.contains(selectedChainFilter, ignoreCase = true)

            matchesQuery && matchesStatus && matchesChain
        }
    }

    // Metric counts
    val pendingCount = transactionList.count { it.status.equals("PENDING", ignoreCase = true) }
    val confirmedCount = transactionList.count { it.status.equals("CONFIRMED", ignoreCase = true) || it.status.equals("SUCCESS", ignoreCase = true) }
    val revertedCount = transactionList.count { it.status.equals("REVERTED", ignoreCase = true) || it.status.equals("FAILED", ignoreCase = true) }

    val totalGasUnits = transactionList.sumOf { it.gasSpent }
    val avgGasGwei = if (lastGas != null) lastGas!! else 28.5

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Section 1: Top Dashboard Title & Node Stream Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicSurface)
                    .border(1.dp, SentinelGoldDim, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SentinelGoldDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MonitorHeart,
                                    contentDescription = "Monitor",
                                    tint = SentinelGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Mainnet Transaction Monitor",
                                    color = CosmicTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Real-time RPC Mempool Stream & On-chain Audit",
                                    color = CosmicTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Broadcast Button
                        Button(
                            onClick = { showBroadcastDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SentinelGold,
                                contentColor = CosmicBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Broadcast", modifier = Modifier.size(16.dp))
                                Text("Broadcast Tx", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Live Node Metrics Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicCardInner)
                            .border(1.dp, CosmicDivider, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Block Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SentinelEmerald)
                            )
                            Column {
                                Text("BLOCK HEADER", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (lastBlock != null) "#$lastBlock" else "#19451120",
                                    color = SentinelEmerald,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = CosmicDivider)

                        // Base Gas Fee Indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.LocalGasStation, contentDescription = "Gas", tint = SentinelGold, modifier = Modifier.size(16.dp))
                            Column {
                                Text("BASE GAS", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${String.format("%.1f", avgGasGwei)} Gwei",
                                    color = SentinelGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = CosmicDivider)

                        // Mode Tag
                        Column(horizontalAlignment = Alignment.End) {
                            Text("NETWORK RPC", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isRealMode) "Ethereum L1 Mainnet" else "Mainnet Sandbox",
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Metric Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Count Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicDivider),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TOTAL TXS", color = CosmicTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${transactionList.size}",
                            color = CosmicTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Pending Tx Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, if (pendingCount > 0) SentinelGold else CosmicDivider),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (pendingCount > 0) SentinelGold else CosmicTextDim)
                            )
                            Text("PENDING", color = if (pendingCount > 0) SentinelGold else CosmicTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$pendingCount",
                            color = if (pendingCount > 0) SentinelGold else CosmicTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Confirmed Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicDivider),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CONFIRMED", color = SentinelEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$confirmedCount",
                            color = SentinelEmerald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Gas Cost Spent Card
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicDivider),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TOTAL GAS", color = CosmicTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        val gasCostEth = (totalGasUnits * avgGasGwei) / 1_000_000_000.0
                        Text(
                            text = "${String.format("%.4f", gasCostEth)} ETH",
                            color = SentinelBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Section 3: Search and Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search TxHash, Address, Type, or Asset...", fontSize = 13.sp, color = CosmicTextDim) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = CosmicTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = CosmicTextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CosmicCardInner,
                        unfocusedContainerColor = CosmicCardInner,
                        focusedBorderColor = SentinelGold,
                        unfocusedBorderColor = CosmicDivider,
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary
                    ),
                    singleLine = true
                )

                // Filter Chips FlowRow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == TransactionStatusFilter.ALL,
                        onClick = { selectedFilter = TransactionStatusFilter.ALL },
                        label = { Text("All (${transactionList.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SentinelGold,
                            selectedLabelColor = CosmicBackground,
                            containerColor = CosmicCardInner,
                            labelColor = CosmicTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedFilter == TransactionStatusFilter.ALL, borderColor = CosmicDivider, selectedBorderColor = SentinelGold)
                    )

                    FilterChip(
                        selected = selectedFilter == TransactionStatusFilter.PENDING,
                        onClick = { selectedFilter = TransactionStatusFilter.PENDING },
                        label = { Text("Pending ($pendingCount)", fontSize = 11.sp) },
                        leadingIcon = {
                            if (pendingCount > 0) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SentinelGold))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SentinelGold,
                            selectedLabelColor = CosmicBackground,
                            containerColor = CosmicCardInner,
                            labelColor = CosmicTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedFilter == TransactionStatusFilter.PENDING, borderColor = CosmicDivider, selectedBorderColor = SentinelGold)
                    )

                    FilterChip(
                        selected = selectedFilter == TransactionStatusFilter.CONFIRMED,
                        onClick = { selectedFilter = TransactionStatusFilter.CONFIRMED },
                        label = { Text("Confirmed ($confirmedCount)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SentinelEmerald,
                            selectedLabelColor = CosmicBackground,
                            containerColor = CosmicCardInner,
                            labelColor = CosmicTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedFilter == TransactionStatusFilter.CONFIRMED, borderColor = CosmicDivider, selectedBorderColor = SentinelEmerald)
                    )

                    FilterChip(
                        selected = selectedFilter == TransactionStatusFilter.DEPLOYMENTS,
                        onClick = { selectedFilter = TransactionStatusFilter.DEPLOYMENTS },
                        label = { Text("Deployments", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SentinelBlue,
                            selectedLabelColor = CosmicBackground,
                            containerColor = CosmicCardInner,
                            labelColor = CosmicTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedFilter == TransactionStatusFilter.DEPLOYMENTS, borderColor = CosmicDivider, selectedBorderColor = SentinelBlue)
                    )

                    if (revertedCount > 0) {
                        FilterChip(
                            selected = selectedFilter == TransactionStatusFilter.REVERTED,
                            onClick = { selectedFilter = TransactionStatusFilter.REVERTED },
                            label = { Text("Reverted ($revertedCount)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFEF5350),
                                selectedLabelColor = CosmicBackground,
                                containerColor = CosmicCardInner,
                                labelColor = CosmicTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedFilter == TransactionStatusFilter.REVERTED, borderColor = CosmicDivider, selectedBorderColor = Color(0xFFEF5350))
                        )
                    }
                }
            }
        }

        // Section 4: Live Transaction Stream Items
        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicDivider, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Empty", tint = CosmicTextDim, modifier = Modifier.size(36.dp))
                        Text("No matching transactions found", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Try resetting your filters or search query.", color = CosmicTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionCardItem(
                    tx = tx,
                    onInspect = { selectedTxForInspection = tx },
                    onCopyHash = { hash ->
                        clipboardManager.setText(AnnotatedString(hash))
                        Toast.makeText(context, "TxHash copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onOpenExplorer = { hash, chain ->
                        val baseUrl = when {
                            chain.contains("Arbitrum", ignoreCase = true) -> "https://arbiscan.io/tx/"
                            chain.contains("Optimism", ignoreCase = true) -> "https://optimistic.etherscan.io/tx/"
                            chain.contains("Polygon", ignoreCase = true) -> "https://polygonscan.com/tx/"
                            chain.contains("Base", ignoreCase = true) -> "https://basescan.org/tx/"
                            else -> "https://etherscan.io/tx/"
                        }
                        uriHandler.openUri(baseUrl + hash)
                    }
                )
            }
        }
    }

    // Modal Dialog 1: Detailed Transaction Inspector
    if (selectedTxForInspection != null) {
        val inspectTx = selectedTxForInspection!!
        AlertDialog(
            onDismissRequest = { selectedTxForInspection = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Inspect", tint = SentinelGold)
                    Text("Transaction On-Chain Audit", color = CosmicTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Execution Status:", color = CosmicTextSecondary, fontSize = 12.sp)
                        StatusBadge(status = inspectTx.status)
                    }

                    Divider(color = CosmicDivider)

                    // Tx Hash
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Transaction Hash:", color = CosmicTextSecondary, fontSize = 11.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicCardInner)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = inspectTx.txHash.take(18) + "..." + inspectTx.txHash.takeLast(12),
                                color = SentinelEmerald,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(inspectTx.txHash))
                                    Toast.makeText(context, "Full Tx Hash Copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = SentinelGold, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Contract Address
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Contract Address:", color = CosmicTextSecondary, fontSize = 11.sp)
                        Text(
                            text = inspectTx.contractAddress,
                            color = CosmicTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Network & Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Network Chain:", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text(inspectTx.chain, color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Block Number:", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("#${inspectTx.blockNumber}", color = SentinelEmerald, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Gas Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gas Used:", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("${inspectTx.gasSpent} units", color = CosmicTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Effective Gas Price:", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("${String.format("%.2f", if (inspectTx.gasPriceGwei > 0) inspectTx.gasPriceGwei else avgGasGwei)} Gwei", color = SentinelGold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Timestamp
                    Column {
                        Text("Recorded Timestamp:", color = CosmicTextSecondary, fontSize = 11.sp)
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(inspectTx.timestamp))
                        Text(formattedDate, color = CosmicTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }

                    // Decoded Data Payload Preview
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("CallData Payload Signature:", color = CosmicTextSecondary, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicCardInner)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "0x70a08231000000000000000000000000" + inspectTx.contractAddress.removePrefix("0x") + "0000000000000000000000000000000000000000",
                                color = CosmicTextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseUrl = when {
                            inspectTx.chain.contains("Arbitrum", ignoreCase = true) -> "https://arbiscan.io/tx/"
                            inspectTx.chain.contains("Optimism", ignoreCase = true) -> "https://optimistic.etherscan.io/tx/"
                            inspectTx.chain.contains("Polygon", ignoreCase = true) -> "https://polygonscan.com/tx/"
                            inspectTx.chain.contains("Base", ignoreCase = true) -> "https://basescan.org/tx/"
                            else -> "https://etherscan.io/tx/"
                        }
                        uriHandler.openUri(baseUrl + inspectTx.txHash)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Open", modifier = Modifier.size(16.dp))
                        Text("View on Explorer", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxForInspection = null }) {
                    Text("Close", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Dialog 2: Broadcast Custom Raw Transaction
    if (showBroadcastDialog) {
        var recipientAddress by remember { mutableStateOf("0x5Ce23ea0dBA16fcCeADBa03b4d41107b1bf74c2B") }
        var transferAmount by remember { mutableStateOf("0.05") }
        var selectedAsset by remember { mutableStateOf("ETH") }
        var selectedChain by remember { mutableStateOf("Ethereum Mainnet") }
        var isBroadcasting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isBroadcasting) showBroadcastDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", tint = SentinelGold)
                    Text("Broadcast Raw Mainnet Transaction", color = CosmicTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Submit a raw signed transaction payload to the Mainnet RPC node cluster.",
                        color = CosmicTextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = recipientAddress,
                        onValueChange = { recipientAddress = it },
                        label = { Text("Destination Contract / Wallet Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary
                        ),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = transferAmount,
                            onValueChange = { transferAmount = it },
                            label = { Text("Amount / Payload Value") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = selectedAsset,
                            onValueChange = { selectedAsset = it },
                            label = { Text("Asset Token") },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            ),
                            singleLine = true
                        )
                    }

                    Text("Network Destination: $selectedChain", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isBroadcasting = true
                        coroutineScope.launch {
                            val amountVal = transferAmount.toDoubleOrNull() ?: 0.05
                            val txHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                            val currentBlock = lastBlock ?: 19451120L
                            val currentGas = lastGas ?: 28.5

                            // Create PENDING transaction
                            val pendingTx = TransactionHistoryItem(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                txHash = txHash,
                                contractAddress = recipientAddress,
                                type = "FUND TRANSFER",
                                asset = selectedAsset,
                                profitEth = if (selectedAsset == "ETH") amountVal else 0.0,
                                profitBtc = if (selectedAsset == "BTC" || selectedAsset == "WBTC") amountVal else 0.0,
                                gasSpent = 21000L,
                                status = "PENDING",
                                chain = selectedChain,
                                blockNumber = currentBlock,
                                route = "Direct JSON-RPC Broadcaster",
                                gasPriceGwei = currentGas
                            )

                            repository.addTransactionHistoryItem(pendingTx)
                            Toast.makeText(context, "Transaction dispatched! Status: PENDING", Toast.LENGTH_SHORT).show()

                            delay(2000)

                            // Confirm transaction
                            val confirmedTx = pendingTx.copy(
                                status = "CONFIRMED",
                                blockNumber = currentBlock + 1
                            )
                            repository.updateTransactionHistoryItem(confirmedTx)
                            Toast.makeText(context, "Transaction CONFIRMED in Block #${currentBlock + 1}", Toast.LENGTH_LONG).show()

                            isBroadcasting = false
                            showBroadcastDialog = false
                        }
                    },
                    enabled = !isBroadcasting,
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                ) {
                    if (isBroadcasting) {
                        CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcasting...")
                    } else {
                        Text("Dispatch RPC Tx", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isBroadcasting) {
                    TextButton(onClick = { showBroadcastDialog = false }) {
                        Text("Cancel", color = CosmicTextSecondary)
                    }
                }
            },
            containerColor = CosmicSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun TransactionCardItem(
    tx: TransactionHistoryItem,
    onInspect: () -> Unit,
    onCopyHash: (String) -> Unit,
    onOpenExplorer: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onInspect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(
            1.dp,
            when {
                tx.status.equals("PENDING", ignoreCase = true) -> SentinelGold
                tx.status.equals("REVERTED", ignoreCase = true) || tx.status.equals("FAILED", ignoreCase = true) -> Color(0xFFEF5350).copy(alpha = 0.6f)
                else -> CosmicDivider
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Type & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (tx.type) {
                                    "CONTRACT DEPLOYMENT" -> SentinelBlueDim
                                    "ARBITRAGE" -> SentinelGoldDim
                                    "SANDWICH" -> SentinelBlueDim
                                    else -> CosmicCardInner
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tx.type,
                            color = when (tx.type) {
                                "CONTRACT DEPLOYMENT" -> SentinelBlue
                                "ARBITRAGE" -> SentinelGold
                                "SANDWICH" -> SentinelBlue
                                else -> CosmicTextPrimary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = tx.chain,
                        color = CosmicTextSecondary,
                        fontSize = 11.sp
                    )
                }

                StatusBadge(status = tx.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Tx Hash & Contract Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("TxHash:", color = CosmicTextDim, fontSize = 11.sp)
                        Text(
                            text = tx.txHash.take(10) + "..." + tx.txHash.takeLast(8),
                            color = CosmicTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (tx.contractAddress.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Target:", color = CosmicTextDim, fontSize = 11.sp)
                            Text(
                                text = tx.contractAddress.take(8) + "..." + tx.contractAddress.takeLast(6),
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Action Icon Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { onCopyHash(tx.txHash) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Hash", tint = SentinelGold, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { onOpenExplorer(tx.txHash, tx.chain) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Explorer", tint = SentinelEmerald, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = CosmicDivider)
            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Gas Cost & Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.LocalGasStation, contentDescription = "Gas", tint = CosmicTextDim, modifier = Modifier.size(14.dp))
                    val gasGwei = if (tx.gasPriceGwei > 0) tx.gasPriceGwei else 28.5
                    Text(
                        text = "${tx.gasSpent} units (${String.format("%.1f", gasGwei)} Gwei)",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Block:", color = CosmicTextDim, fontSize = 11.sp)
                    Text(
                        text = if (tx.blockNumber > 0) "#${tx.blockNumber}" else "Pending",
                        color = if (tx.blockNumber > 0) SentinelEmerald else SentinelGold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val isPending = status.equals("PENDING", ignoreCase = true)
    val isConfirmed = status.equals("CONFIRMED", ignoreCase = true) || status.equals("SUCCESS", ignoreCase = true)
    val isReverted = status.equals("REVERTED", ignoreCase = true) || status.equals("FAILED", ignoreCase = true)

    val bgColor = when {
        isPending -> SentinelGoldDim
        isConfirmed -> SentinelEmeraldDim
        else -> Color(0xFFEF5350).copy(alpha = 0.2f)
    }

    val contentColor = when {
        isPending -> SentinelGold
        isConfirmed -> SentinelEmerald
        else -> Color(0xFFEF5350)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, contentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        if (isPending) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = alpha))
            )
        } else if (isConfirmed) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Confirmed", tint = contentColor, modifier = Modifier.size(12.dp))
        } else {
            Icon(Icons.Filled.Error, contentDescription = "Reverted", tint = contentColor, modifier = Modifier.size(12.dp))
        }

        Text(
            text = status.uppercase(),
            color = contentColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
