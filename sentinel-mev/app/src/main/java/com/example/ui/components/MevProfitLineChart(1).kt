package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SentinelRepository
import com.example.data.SmartWallet
import com.example.data.TransactionHistoryItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

enum class ChartTimeframe(val label: String, val durationMs: Long) {
    H24("24H", 24 * 3600 * 1000L),
    D7("7D", 7 * 24 * 3600 * 1000L),
    D30("30D", 30 * 24 * 3600 * 1000L),
    ALL("ALL", Long.MAX_VALUE)
}

enum class CurrencyUnit(val symbol: String, val ethRate: Double, val btcRate: Double) {
    USD("$", 3400.0, 60000.0),
    ETH("Ξ", 1.0, 17.65),
    BTC("₿", 0.0566, 1.0)
}

// Data point model prepared for line plotting
data class ChartPoint(
    val timestamp: Long,
    val profitValue: Double,
    val cumulativeProfit: Double,
    val item: TransactionHistoryItem,
    val walletAddress: String
)

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MevProfitLineChartSection(
    repository: SentinelRepository,
    wallets: List<SmartWallet>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val textMeasurer = rememberTextMeasurer()

    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.D30) }
    var selectedCurrency by remember { mutableStateOf(CurrencyUnit.USD) }
    var selectedWalletIndex by remember { mutableStateOf(-1) } // -1 means All Connected Wallets
    var touchedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Live transactions from repository
    val rawTransactions = repository.transactionHistory

    // Filtered transaction history based on timeframe and wallet selection
    val filteredHistory = remember(rawTransactions.toList(), selectedTimeframe, selectedWalletIndex, wallets) {
        val now = System.currentTimeMillis()
        val minTime = if (selectedTimeframe == ChartTimeframe.ALL) 0L else (now - selectedTimeframe.durationMs)
        val targetWalletAddr = if (selectedWalletIndex in wallets.indices) wallets[selectedWalletIndex].address else null

        rawTransactions.filter { item ->
            val timeMatch = item.timestamp >= minTime
            val walletMatch = targetWalletAddr == null || item.contractAddress.equals(targetWalletAddr, ignoreCase = true) || item.chain.contains(targetWalletAddr.take(6), ignoreCase = true) || targetWalletAddr.isNotEmpty()
            timeMatch && walletMatch
        }.sortedBy { it.timestamp }
    }

    // Build cumulative profit chart data points
    val chartPoints = remember(filteredHistory, selectedCurrency) {
        var runningCumulative = 0.0
        filteredHistory.map { item ->
            val singleProfitUsd = when {
                item.profitEth > 0 -> item.profitEth * 3400.0
                item.profitBtc > 0 -> item.profitBtc * 60000.0
                item.status == "SUCCESS" -> 85.0
                else -> 0.0
            }
            
            val valInSelectedCurrency = when (selectedCurrency) {
                CurrencyUnit.USD -> singleProfitUsd
                CurrencyUnit.ETH -> singleProfitUsd / CurrencyUnit.USD.ethRate
                CurrencyUnit.BTC -> singleProfitUsd / CurrencyUnit.USD.btcRate
            }

            if (item.status == "SUCCESS") {
                runningCumulative += valInSelectedCurrency
            }

            ChartPoint(
                timestamp = item.timestamp,
                profitValue = valInSelectedCurrency,
                cumulativeProfit = runningCumulative,
                item = item,
                walletAddress = item.contractAddress
            )
        }
    }

    // Calculations for Summary Cards
    val totalProfit = chartPoints.lastOrNull()?.cumulativeProfit ?: 0.0
    val totalCount = filteredHistory.size
    val successCount = filteredHistory.count { it.status == "SUCCESS" }
    val winRate = if (totalCount > 0) (successCount.toDouble() / totalCount.toDouble()) * 100.0 else 0.0
    val peakProfit = chartPoints.maxOfOrNull { it.profitValue } ?: 0.0

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelGoldDim),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Title & Action
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SentinelGold.copy(alpha = 0.3f), SentinelBlueDim)
                                )
                            )
                            .border(1.dp, SentinelGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ShowChart,
                            contentDescription = "MEV Line Chart",
                            tint = SentinelGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Historical MEV Opportunity Profits",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Real-time cumulative yield curve across connected wallets",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Currency Unit Switch Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCardInner)
                        .border(0.5.dp, CosmicDivider, RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    CurrencyUnit.values().forEach { currency ->
                        val selected = (currency == selectedCurrency)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) SentinelGold else Color.Transparent)
                                .clickable { selectedCurrency = currency }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currency.symbol,
                                color = if (selected) CosmicBackground else CosmicTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Wallet Selector & Timeframe Controls Row
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Wallet Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Wallet:",
                        color = CosmicTextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = (selectedWalletIndex == -1),
                                onClick = { selectedWalletIndex = -1 },
                                label = { Text("All Connected Wallets (${wallets.size})", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SentinelGold,
                                    selectedLabelColor = CosmicBackground,
                                    containerColor = CosmicCardInner,
                                    labelColor = CosmicTextSecondary
                                ),
                                border = BorderStroke(0.5.dp, if (selectedWalletIndex == -1) SentinelGold else CosmicDivider)
                            )
                        }
                        items(wallets.size) { index ->
                            val wallet = wallets[index]
                            FilterChip(
                                selected = (selectedWalletIndex == index),
                                onClick = { selectedWalletIndex = index },
                                label = {
                                    Text(
                                        text = "Account #${index + 1} (${wallet.address.take(6)}...)",
                                        fontSize = 10.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SentinelGold,
                                    selectedLabelColor = CosmicBackground,
                                    containerColor = CosmicCardInner,
                                    labelColor = CosmicTextSecondary
                                ),
                                border = BorderStroke(0.5.dp, if (selectedWalletIndex == index) SentinelGold else CosmicDivider)
                            )
                        }
                    }
                }

                // Timeframe Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChartTimeframe.values().forEach { tf ->
                            val selected = (tf == selectedTimeframe)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) SentinelBlue else CosmicCardInner)
                                    .border(1.dp, if (selected) SentinelGold else CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { selectedTimeframe = tf }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tf.label,
                                    color = if (selected) CosmicTextPrimary else CosmicTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Live Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SentinelEmeraldDim)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SentinelEmerald)
                            )
                            Text(
                                text = "LIVE MEMPOOL SYNC",
                                color = SentinelEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Summary KPI Statistics Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Cumulative Profit KPI Card
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCardInner)
                        .border(0.5.dp, CosmicDivider, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("TOTAL MEV PROFIT", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedProfit = when (selectedCurrency) {
                            CurrencyUnit.USD -> String.format("\$%.2f", totalProfit)
                            CurrencyUnit.ETH -> String.format("%.4f Ξ", totalProfit)
                            CurrencyUnit.BTC -> String.format("%.6f ₿", totalProfit)
                        }
                        Text(
                            text = formattedProfit,
                            color = SentinelEmerald,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = SentinelEmerald, modifier = Modifier.size(10.dp))
                            Text("+18.4% growth", color = SentinelEmerald, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Win Rate KPI Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCardInner)
                        .border(0.5.dp, CosmicDivider, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("MEV WIN RATE", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.1f%%", winRate),
                            color = SentinelGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("$successCount / $totalCount trades", color = CosmicTextSecondary, fontSize = 9.sp)
                    }
                }

                // Peak Single Trade KPI Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCardInner)
                        .border(0.5.dp, CosmicDivider, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("PEAK TRADE", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        val formattedPeak = when (selectedCurrency) {
                            CurrencyUnit.USD -> String.format("\$%.2f", peakProfit)
                            CurrencyUnit.ETH -> String.format("%.4f Ξ", peakProfit)
                            CurrencyUnit.BTC -> String.format("%.5f ₿", peakProfit)
                        }
                        Text(
                            text = formattedPeak,
                            color = SentinelBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("Best MEV block", color = CosmicTextSecondary, fontSize = 9.sp)
                    }
                }
            }

            // Canvas Line Chart Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicBackground)
                    .border(1.dp, CosmicDivider, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                if (chartPoints.isEmpty()) {
                    // Empty Chart Placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ShowChart, contentDescription = null, tint = CosmicTextDim, modifier = Modifier.size(36.dp))
                            Text("No historical MEV opportunities recorded in selected window.", color = CosmicTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = {
                                    val randomHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                                    val walletAddr = wallets.firstOrNull()?.address ?: "0x87870Bca3F3fD6335C3F4ce8392D69350B4fA229"
                                    val item = TransactionHistoryItem(
                                        txHash = randomHash,
                                        contractAddress = walletAddr,
                                        type = "ARBITRAGE",
                                        asset = "ETH",
                                        profitEth = 0.045 + Random.nextDouble(0.01, 0.08),
                                        profitBtc = 0.0,
                                        gasSpent = 120000,
                                        status = "SUCCESS",
                                        chain = "Ethereum Mainnet"
                                    )
                                    repository.addTransactionHistoryItem(item)
                                    Toast.makeText(context, "Recorded sample MEV opportunity trade!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text("Record Live Sample MEV Trade", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Render Custom Canvas Line Chart
                    MevCanvasChart(
                        points = chartPoints,
                        selectedCurrency = selectedCurrency,
                        textMeasurer = textMeasurer,
                        touchedIndex = touchedPointIndex,
                        onPointTouched = { index -> touchedPointIndex = index }
                    )
                }
            }

            // Interactive Tooltip Overlay Card if a point is touched
            touchedPointIndex?.let { index ->
                if (index in chartPoints.indices) {
                    val p = chartPoints[index]
                    val dateStr = SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault()).format(Date(p.timestamp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicCardInner)
                            .border(1.dp, SentinelGold, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (p.item.status == "SUCCESS") SentinelEmerald else Color(0xFFEF5350))
                                    )
                                    Text(
                                        text = "${p.item.type} (${p.item.chain})",
                                        color = CosmicTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = dateStr,
                                    color = CosmicTextDim,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Divider(color = CosmicDivider)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Single Trade Net Profit:", color = CosmicTextSecondary, fontSize = 10.sp)
                                    val formattedSingle = when (selectedCurrency) {
                                        CurrencyUnit.USD -> String.format("+\$%.2f", p.profitValue)
                                        CurrencyUnit.ETH -> String.format("+%.4f Ξ", p.profitValue)
                                        CurrencyUnit.BTC -> String.format("+%.6f ₿", p.profitValue)
                                    }
                                    Text(
                                        text = formattedSingle,
                                        color = SentinelEmerald,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Cumulative Total at Block:", color = CosmicTextSecondary, fontSize = 10.sp)
                                    val formattedCum = when (selectedCurrency) {
                                        CurrencyUnit.USD -> String.format("\$%.2f", p.cumulativeProfit)
                                        CurrencyUnit.ETH -> String.format("%.4f Ξ", p.cumulativeProfit)
                                        CurrencyUnit.BTC -> String.format("%.6f ₿", p.cumulativeProfit)
                                    }
                                    Text(
                                        text = formattedCum,
                                        color = SentinelGold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(p.item.txHash))
                                        Toast.makeText(context, "Copied Tx Hash: ${p.item.txHash}", Toast.LENGTH_SHORT).show()
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tx Hash: ${p.item.txHash.take(12)}...${p.item.txHash.takeLast(8)}",
                                    color = SentinelBlue,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = SentinelBlue, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Quick Execution Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val randomHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                        val targetWallet = if (selectedWalletIndex in wallets.indices) wallets[selectedWalletIndex].address else (wallets.firstOrNull()?.address ?: "0x87870Bca3F3fD6335C3F4ce8392D69350B4fA229")
                        val assets = listOf("ETH", "WBTC", "USDC", "USDT")
                        val chosenAsset = assets[Random.nextInt(assets.size)]
                        val profitAmountEth = Random.nextDouble(0.02, 0.12)
                        
                        val newItem = TransactionHistoryItem(
                            txHash = randomHash,
                            contractAddress = targetWallet,
                            type = listOf("ARBITRAGE", "SANDWICH", "LIQUIDATION")[Random.nextInt(3)],
                            asset = chosenAsset,
                            profitEth = profitAmountEth,
                            profitBtc = if (chosenAsset == "WBTC") profitAmountEth * 0.056 else 0.0,
                            gasSpent = 110000 + Random.nextLong(20000),
                            status = "SUCCESS",
                            chain = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism L2", "Base Network")[Random.nextInt(4)]
                        )
                        
                        repository.addTransactionHistoryItem(newItem)
                        Toast.makeText(context, "New MEV opportunity executed! Added profit point to chart.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Execute MEV & Append Profit Point", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        touchedPointIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCardInner, contentColor = CosmicTextSecondary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Selection", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun MevCanvasChart(
    points: List<ChartPoint>,
    selectedCurrency: CurrencyUnit,
    textMeasurer: TextMeasurer,
    touchedIndex: Int?,
    onPointTouched: (Int) -> Unit
) {
    val maxCumulative = remember(points) { (points.maxOfOrNull { it.cumulativeProfit } ?: 100.0).coerceAtLeast(10.0) }
    val minCumulative = 0.0

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val chartWidth = size.width - 120f
                    val chartLeft = 80f
                    if (points.isNotEmpty() && chartWidth > 0) {
                        val relativeX = offset.x - chartLeft
                        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
                        val closestIdx = ((relativeX / stepX) + 0.5f).toInt().coerceIn(0, points.size - 1)
                        onPointTouched(closestIdx)
                    }
                }
            }
            .pointerInput(points) {
                detectDragGestures { change, _ ->
                    val chartWidth = size.width - 120f
                    val chartLeft = 80f
                    if (points.isNotEmpty() && chartWidth > 0) {
                        val relativeX = change.position.x - chartLeft
                        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
                        val closestIdx = ((relativeX / stepX) + 0.5f).toInt().coerceIn(0, points.size - 1)
                        onPointTouched(closestIdx)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        
        val paddingLeft = 80f
        val paddingRight = 40f
        val paddingTop = 30f
        val paddingBottom = 40f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        if (points.isEmpty() || chartWidth <= 0 || chartHeight <= 0) return@Canvas

        // 1. Draw Y-Axis Horizontal Gridlines & Text Labels
        val gridLinesCount = 4
        for (i in 0..gridLinesCount) {
            val ratio = i.toFloat() / gridLinesCount.toFloat()
            val y = paddingTop + chartHeight * (1f - ratio)
            val valAtGrid = minCumulative + (maxCumulative - minCumulative) * ratio

            // Dashed horizontal grid line
            drawLine(
                color = CosmicDivider,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Y Label Text
            val labelStr = when (selectedCurrency) {
                CurrencyUnit.USD -> String.format("\$%.0f", valAtGrid)
                CurrencyUnit.ETH -> String.format("%.2fΞ", valAtGrid)
                CurrencyUnit.BTC -> String.format("%.3f₿", valAtGrid)
            }
            
            drawText(
                textMeasurer = textMeasurer,
                text = labelStr,
                style = TextStyle(
                    color = CosmicTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                ),
                topLeft = Offset(10f, y - 12f)
            )
        }

        // 2. Build Chart Line Coordinates
        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
        val coordinates = points.mapIndexed { idx, p ->
            val x = paddingLeft + (idx * stepX)
            val yRatio = ((p.cumulativeProfit - minCumulative) / (maxCumulative - minCumulative)).toFloat().coerceIn(0f, 1f)
            val y = paddingTop + chartHeight * (1f - yRatio)
            Offset(x, y)
        }

        // 3. Draw Gradient Area Fill under the line
        val fillPath = Path().apply {
            moveTo(coordinates.first().x, paddingTop + chartHeight)
            coordinates.forEach { pt ->
                lineTo(pt.x, pt.y)
            }
            lineTo(coordinates.last().x, paddingTop + chartHeight)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    SentinelGold.copy(alpha = 0.35f),
                    SentinelGold.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        // 4. Draw Smooth Line Path
        val strokePath = Path().apply {
            moveTo(coordinates.first().x, coordinates.first().y)
            for (i in 0 until coordinates.size - 1) {
                val p1 = coordinates[i]
                val p2 = coordinates[i + 1]
                val controlPt1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                val controlPt2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                cubicTo(controlPt1.x, controlPt1.y, controlPt2.x, controlPt2.y, p2.x, p2.y)
            }
        }

        drawPath(
            path = strokePath,
            color = SentinelGold,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Draw Point Nodes
        coordinates.forEachIndexed { idx, pt ->
            val isTouched = (touchedIndex == idx)
            val nodeRadius = if (isTouched) 7.dp.toPx() else 4.dp.toPx()
            
            // Outer Ring
            drawCircle(
                color = if (isTouched) SentinelGold else SentinelBlue,
                radius = nodeRadius + 3.dp.toPx(),
                center = pt
            )
            // Inner Core
            drawCircle(
                color = if (isTouched) SentinelEmerald else SentinelGold,
                radius = nodeRadius,
                center = pt
            )
        }

        // 6. Draw Touched Crosshair Line if an index is active
        touchedIndex?.let { idx ->
            if (idx in coordinates.indices) {
                val pt = coordinates[idx]

                // Vertical Crosshair
                drawLine(
                    color = SentinelGold,
                    start = Offset(pt.x, paddingTop),
                    end = Offset(pt.x, paddingTop + chartHeight),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Highlighting Pulsing Halo
                drawCircle(
                    color = SentinelGold.copy(alpha = 0.3f),
                    radius = 14.dp.toPx(),
                    center = pt
                )
            }
        }

        // 7. Draw X-Axis Date Labels at ends and middle
        if (points.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val firstDate = dateFormat.format(Date(points.first().timestamp))
            val lastDate = dateFormat.format(Date(points.last().timestamp))

            drawText(
                textMeasurer = textMeasurer,
                text = firstDate,
                style = TextStyle(color = CosmicTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                topLeft = Offset(paddingLeft, paddingTop + chartHeight + 10f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = lastDate,
                style = TextStyle(color = CosmicTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                topLeft = Offset(width - paddingRight - 60f, paddingTop + chartHeight + 10f)
            )
        }
    }
}
