package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ArbitrageOpportunity
import com.example.data.SentinelConfig
import com.example.data.SentinelRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MevBotTab(
    repository: SentinelRepository,
    onNavigateToLifecycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    
    var rpcUrl by remember { mutableStateOf(repository.config.rpcUrl) }
    var dailyLimit by remember { mutableStateOf(repository.config.dailyLimitContracts.toFloat()) }
    var targetProfit by remember { mutableStateOf(repository.config.targetProfitPct.toFloat()) }
    var gasPolicyEnabled by remember { mutableStateOf(repository.config.gasPolicyEnabled) }
    
    // Security triggers
    var reentrancyGuard by remember { mutableStateOf(true) }
    var accessControl by remember { mutableStateOf(true) }
    var safemathLock by remember { mutableStateOf(true) }
    var slippageLimit by remember { mutableStateOf(true) }

    // Live telemetry from repository daemon
    val latestBlock by repository.lastScannedBlock
    val gasPriceGwei by repository.lastScannedGasPrice
    val isDaemonActive by repository.isDaemonActive
    val daemonLogs = repository.daemonLogs
    val arbitrageList = repository.arbitrageOpportunities

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Section Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicSurface)
                    .border(1.dp, SentinelGoldDim, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Hub,
                        contentDescription = "MEV hub",
                        tint = SentinelGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "MEV Bot & Daemon Controller",
                            color = CosmicTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "A fully operational background bot scanning RPC mempools and routing zero-hardcoded real-time opportunities.",
                            color = CosmicTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // --- HISTORICAL MEV PROFIT LINE CHART ---
        item {
            MevProfitLineChartSection(
                repository = repository,
                wallets = repository.smartWallets
            )
        }

        // --- REAL-TIME TELEMETRY PANEL ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, SentinelGoldDim),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDaemonActive) SentinelEmerald else CosmicTextDim)
                            )
                            Text(
                                text = "ON-CHAIN TELEMETRY ENGINE",
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Start / Pause Daemon Button
                        Button(
                            onClick = {
                                if (isDaemonActive) {
                                    repository.stopBlockchainDaemon()
                                } else {
                                    repository.startBlockchainDaemon()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDaemonActive) Color(0x22EF5350) else SentinelEmeraldDim,
                                contentColor = if (isDaemonActive) Color(0xFFEF5350) else SentinelEmerald
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isDaemonActive) "PAUSE BOT" else "RESUME BOT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Block Indicator Widget
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicCardInner)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                .clickable {
                                    latestBlock?.let { block ->
                                        uriHandler.openUri("https://etherscan.io/block/$block")
                                    }
                                }
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("LAST SCANNED BLOCK", color = CosmicTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = latestBlock?.toString() ?: "PENDING...",
                                color = if (latestBlock != null) SentinelGold else CosmicTextDim,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            if (latestBlock != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("🔗 View on Etherscan", color = SentinelBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Gas Gwei Indicator Widget
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicCardInner)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MAINNET BASE FEE", color = CosmicTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (gasPriceGwei != null) String.format("%.2f Gwei", gasPriceGwei) else "SYNCING...",
                                color = if (gasPriceGwei != null) SentinelBlue else CosmicTextDim,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Sandbox Success Rate Widget
                        val simRate by repository.simulatedSuccessRate
                        val simTotal by repository.totalSimulatedRuns
                        val simSuccess by repository.successfulSimulatedRuns
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicCardInner)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SANDBOX WIN RATE", color = CosmicTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.1f%%", simRate),
                                color = SentinelEmerald,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$simSuccess/$simTotal runs",
                                color = CosmicTextSecondary,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // --- LIVE DAEMON PROTOCOL CONSOLE ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Terminal, contentDescription = "Terminal", tint = SentinelBlue, modifier = Modifier.size(16.dp))
                            Text("LIVE DAEMON DEPLOYMENT PROTOCOL", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDaemonActive) SentinelEmeraldDim else Color(0x1F757575))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isDaemonActive) "DAEMON ENGAGED" else "DAEMON IDLE",
                                color = if (isDaemonActive) SentinelEmerald else Color(0xFF9E9E9E),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Log terminal console window
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicBackground)
                            .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        if (daemonLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Awaiting first blockchain block sync...", color = CosmicTextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = false) {
                                items(daemonLogs) { logLine ->
                                    val logColor = when {
                                        logLine.contains("🔥") || logLine.contains("PROFITABLE") -> SentinelEmerald
                                        logLine.contains("⚠️") -> SentinelGold
                                        logLine.contains("❌") -> Color(0xFFEF5350)
                                        logLine.contains("📡") || logLine.contains("🛰️") -> SentinelBlue
                                        else -> CosmicTextSecondary
                                    }
                                    Text(
                                        text = logLine,
                                        color = logColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 1.dp),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // RPC & Node Configuration
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SettingsInputHdmi, contentDescription = "RPC config", tint = SentinelBlue, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Chain Node Configuration",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Text(
                        text = "Access the blockchain via custom Alchemy, Infura, or Quicknode endpoints to run mempool arbitrage scanning bots.",
                        color = CosmicTextSecondary,
                        fontSize = 12.sp
                    )
                    
                    OutlinedTextField(
                        value = rpcUrl,
                        onValueChange = { 
                            rpcUrl = it
                            repository.saveConfig(repository.config.copy(rpcUrl = it))
                        },
                        label = { Text("Mempool WebSockets RPC URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedLabelColor = SentinelGold,
                            unfocusedLabelColor = CosmicTextSecondary,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        leadingIcon = {
                            Icon(Icons.Filled.Link, contentDescription = "Link", tint = SentinelBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Alchemy Gas Policies", color = CosmicTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Scan and execute with covered transaction fee gas subsidies.", color = CosmicTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = gasPolicyEnabled,
                            onCheckedChange = { 
                                gasPolicyEnabled = it
                                repository.saveConfig(repository.config.copy(gasPolicyEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SentinelEmerald,
                                checkedTrackColor = SentinelEmeraldDim,
                                uncheckedThumbColor = CosmicTextDim,
                                uncheckedTrackColor = CosmicCardInner
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var alertEnabled by remember { mutableStateOf(repository.isOpportunityAlertEnabled.value) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interactive Prompt Alerts", color = CosmicTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Receive live popups to decide between simulated or real mainnet runs.", color = CosmicTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = alertEnabled,
                            onCheckedChange = { 
                                alertEnabled = it
                                repository.isOpportunityAlertEnabled.value = it
                                repository.saveSimulatedStats()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SentinelEmerald,
                                checkedTrackColor = SentinelEmeraldDim,
                                uncheckedThumbColor = CosmicTextDim,
                                uncheckedTrackColor = CosmicCardInner
                            )
                        )
                    }
                }
            }
        }

        // Daily Contract Sliders
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "Sliders", tint = SentinelGold, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Daily Limits & Profit Target",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Slider 1: Number of smart contracts created/executed daily
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Max Daily Deployed Contracts", color = CosmicTextPrimary, fontSize = 13.sp)
                            Text("${dailyLimit.toInt()} Contracts", color = SentinelGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = dailyLimit,
                            onValueChange = {
                                dailyLimit = it
                                repository.saveConfig(repository.config.copy(dailyLimitContracts = it.toInt()))
                            },
                            valueRange = 1f..30f,
                            steps = 29,
                            colors = SliderDefaults.colors(
                                thumbColor = SentinelGold,
                                activeTrackColor = SentinelGold,
                                inactiveTrackColor = CosmicDivider
                            )
                        )
                    }

                    Divider(color = CosmicDivider)

                    // Slider 2: Target profit margin
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Min Target Profit Margin", color = CosmicTextPrimary, fontSize = 13.sp)
                            Text(String.format("%.1f%% Margin", targetProfit), color = SentinelGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = targetProfit,
                            onValueChange = {
                                targetProfit = it
                                repository.saveConfig(repository.config.copy(targetProfitPct = it.toDouble()))
                            },
                            valueRange = 0.5f..20f,
                            colors = SliderDefaults.colors(
                                thumbColor = SentinelGold,
                                activeTrackColor = SentinelGold,
                                inactiveTrackColor = CosmicDivider
                            )
                        )
                    }
                }
            }
        }

        // Smart Contract Security Features (Toggle Panel)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Shield, contentDescription = "Security Features", tint = SentinelEmerald, modifier = Modifier.size(22.dp))
                            Text(
                                text = "Contract Security Shield",
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    Text(
                        text = "Identify and bundle required security layers to prevent reentrancy attacks, flashloan drain, and sandwich counter-attacks.",
                        color = CosmicTextSecondary,
                        fontSize = 12.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecurityToggleItem(
                            title = "ReentrancyGuard Modifiers",
                            description = "Prevents exploits by locking concurrent execution states.",
                            checked = reentrancyGuard,
                            onCheckedChange = { reentrancyGuard = it }
                        )
                        SecurityToggleItem(
                            title = "Ownable Access Control",
                            description = "Ensures only your verified private key can withdraw funds.",
                            checked = accessControl,
                            onCheckedChange = { accessControl = it }
                        )
                        SecurityToggleItem(
                            title = "Slippage Protections Limit",
                            description = "Cancels transaction if dynamic trade slippage exceeds safety target.",
                            checked = slippageLimit,
                            onCheckedChange = { slippageLimit = it }
                        )
                        SecurityToggleItem(
                            title = "SafeMath Precision",
                            description = "Prevents integer underflow/overflow during calculations.",
                            checked = safemathLock,
                            onCheckedChange = { safemathLock = it }
                        )
                    }
                }
            }
        }

        // Tracked Contracts Status Dashboard Panel (MEV active status, deployment date)
        item {
            TrackedContractsDashboardCard(
                contracts = repository.trackedContracts,
                isDaemonActive = isDaemonActive,
                onNavigateToLifecycle = onNavigateToLifecycle,
                onCopyAddress = { addr ->
                    clipboardManager.setText(AnnotatedString(addr))
                    Toast.makeText(context, "Copied contract address: $addr", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Dynamic Arbitrage opportunities found by active daemons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Thunderstorm, contentDescription = "Daemons", tint = SentinelEmerald, modifier = Modifier.size(20.dp))
                Text(
                    text = "Arbitrage Mempool Scanner Output",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        if (arbitrageList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicDivider, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(color = SentinelBlue, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scanning blocks for gas pathways...",
                            color = CosmicTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            // Real-Time Arbitrage Data Grid Display
            item {
                ArbitrageOpportunitiesGrid(
                    opportunities = arbitrageList,
                    onExecute = {
                        onNavigateToLifecycle()
                    }
                )
            }
        }
    }
}

@Composable
fun ArbitrageOpportunitiesGrid(
    opportunities: List<ArbitrageOpportunity>,
    onExecute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row of the Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicCardInner)
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ROUTE / ASSET", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                Text("PRICE DELTA", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                Text("GAS ANALYSIS", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                Text("NET PROFIT", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.0f), textAlign = TextAlign.End)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rows of the Grid
            opportunities.forEachIndexed { index, opp ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Col 1: Route & Asset
                        Column(modifier = Modifier.weight(1.3f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = opp.asset,
                                    color = SentinelGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SentinelBlueDim)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(opp.buyDex.split(" ")[0], color = SentinelBlue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "${opp.sourceChain.take(8)} ➔ ${opp.targetChain.take(8)}",
                                color = CosmicTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Col 2: Price Delta
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (opp.priceDiffPct >= 1.0) SentinelEmeraldDim else SentinelBlueDim)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = String.format("+%.2f%%", opp.priceDiffPct),
                                color = if (opp.priceDiffPct >= 1.0) SentinelEmerald else SentinelBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Col 3: Gas Analysis
                        Column(
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = String.format("\$%.2f", opp.requiredGasUsd),
                                color = CosmicTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "160K gas Units",
                                color = CosmicTextDim,
                                fontSize = 9.sp
                            )
                        }

                        // Col 4: Net Profit Calculation
                        val netProfit = opp.estProfitUsd - opp.requiredGasUsd
                        Column(
                            modifier = Modifier.weight(1.0f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = String.format("\$%.2f", netProfit),
                                color = if (netProfit > 0) SentinelEmerald else Color(0xFFEF5350),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format("Gross: \$%.1f", opp.estProfitUsd),
                                color = CosmicTextDim,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row Action Button
                    Button(
                        onClick = onExecute,
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmicCardInner,
                            contentColor = CosmicTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, SentinelGoldDim)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = "Flash", modifier = Modifier.size(12.dp), tint = SentinelGold)
                            Text("DEPLOY ARBITRAGE SMART CONTRACT", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (index < opportunities.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = CosmicDivider, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CosmicCardInner)
            .clickable { onCheckedChange(!checked) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = CosmicTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = CosmicTextSecondary, fontSize = 11.sp)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = SentinelEmerald,
                uncheckedColor = CosmicDivider,
                checkmarkColor = CosmicBackground
            )
        )
    }
}

@Composable
fun ArbitrageItemCard(
    opportunity: ArbitrageOpportunity,
    onExecute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = opportunity.asset,
                        color = SentinelGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${opportunity.sourceChain} ➔ ${opportunity.targetChain}",
                        color = CosmicTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Price Diff Tag
                Text(
                    text = String.format("+%.2f%%", opportunity.priceDiffPct),
                    color = if (opportunity.priceDiffPct >= 1.0) SentinelEmerald else SentinelBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (opportunity.priceDiffPct >= 1.0) SentinelEmeraldDim else SentinelBlueDim)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "BUY: ${opportunity.buyDex}", color = CosmicTextSecondary, fontSize = 11.sp)
                    Text(text = "SELL: ${opportunity.sellDex}", color = CosmicTextSecondary, fontSize = 11.sp)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("Est. Profit: \$%.2f", opportunity.estProfitUsd),
                        color = SentinelEmerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("Gas Cost: \$%.2f", opportunity.requiredGasUsd),
                        color = CosmicTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onExecute,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCardInner,
                    contentColor = CosmicTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SentinelGoldDim)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.DynamicForm, contentDescription = "Build Contract", modifier = Modifier.size(16.dp), tint = SentinelGold)
                    Text("Auto-Generate & Deploy Smart Contract", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TrackedContractsDashboardCard(
    contracts: List<com.example.data.TrackedContract>,
    isDaemonActive: Boolean,
    onNavigateToLifecycle: () -> Unit,
    onCopyAddress: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Troubleshoot,
                        contentDescription = "Contract Tracking",
                        tint = SentinelGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Smart Contract Security Status",
                        color = CosmicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                val verifiedCount = contracts.count { it.status == "VERIFIED" }
                val monitoringActive = isDaemonActive && verifiedCount > 0
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (monitoringActive) SentinelEmerald.copy(alpha = 0.15f) else CosmicCardInner)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (monitoringActive) "⚡ MONITORING ACTIVE" else "⏸️ MONITORING PAUSED",
                        color = if (monitoringActive) SentinelEmerald else CosmicTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Text(
                text = "Observe live compiler states, consensus nodes, and MEV frontrun guard status for deployed capital pools.",
                color = CosmicTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Divider(color = CosmicDivider)

            if (contracts.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCardInner)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LayersClear,
                            contentDescription = "No Contracts",
                            tint = CosmicTextDim,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No Deployed Smart Contracts Tracked",
                            color = CosmicTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Compile and deploy safe contracts in the Lifecycles tab to initiate MEV mempool scanning.",
                            color = CosmicTextDim,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onNavigateToLifecycle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SentinelGold.copy(alpha = 0.15f),
                                contentColor = SentinelGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Deploy", modifier = Modifier.size(14.dp))
                                Text("Go Deploy Contract", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val displayContracts = contracts.take(3)
                    displayContracts.forEach { contract ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicCardInner)
                                .border(0.5.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = contract.name,
                                        color = CosmicTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    when (contract.network) {
                                                        "Ethereum Mainnet" -> SentinelBlue
                                                        "Arbitrum One" -> SentinelEmerald
                                                        "Optimism L2" -> Color(0xFFEF5350)
                                                        "Base Network" -> SentinelGold
                                                        else -> CosmicTextDim
                                                    }
                                                )
                                        )
                                        Text(
                                            text = contract.network,
                                            color = CosmicTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                val statusColor = when (contract.status) {
                                    "VERIFIED" -> SentinelEmerald
                                    "SUCCESS_COMPLETED" -> SentinelEmerald
                                    "EXECUTING" -> SentinelBlue
                                    "FAILED" -> Color(0xFFEF5350)
                                    "DEPRECATED" -> CosmicTextDim
                                    else -> SentinelGold
                                }
                                Text(
                                    text = when (contract.status) {
                                        "SUCCESS_COMPLETED" -> "COMPLETED"
                                        else -> contract.status
                                    },
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicBackground.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Visibility,
                                        contentDescription = "MEV Status",
                                        tint = if (contract.status == "DEPRECATED") CosmicTextDim else if (isDaemonActive && contract.status == "VERIFIED") SentinelEmerald else CosmicTextDim,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "MEV Tracking:",
                                        color = CosmicTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                val monitoringText = when {
                                    contract.status == "DEPRECATED" -> "Decommissioned (Deprecated)"
                                    !isDaemonActive -> "Paused (Bot Offline)"
                                    contract.status == "VERIFIED" -> "Active Guardian (Secured)"
                                    contract.status == "SUCCESS_COMPLETED" -> "Settled (Yield Realized)"
                                    contract.status == "EXECUTING" -> "Executing Mempool Block..."
                                    contract.status == "FAILED" -> "Failed / Terminated"
                                    else -> "Waiting Verification"
                                }
                                val monitoringColor = when {
                                    contract.status == "DEPRECATED" -> CosmicTextDim
                                    !isDaemonActive -> CosmicTextDim
                                    contract.status == "VERIFIED" -> SentinelEmerald
                                    contract.status == "SUCCESS_COMPLETED" -> SentinelBlue
                                    contract.status == "EXECUTING" -> SentinelGold
                                    else -> CosmicTextSecondary
                                }

                                Text(
                                    text = monitoringText,
                                    color = monitoringColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable { onCopyAddress(contract.address) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy Contract",
                                        tint = CosmicTextDim,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Addr: ${contract.address.take(6)}...${contract.address.takeLast(4)}",
                                        color = CosmicTextDim,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                val dateStr = try {
                                    dateFormat.format(Date(contract.deployedAt))
                                } catch (e: Exception) {
                                    "Just now"
                                }
                                Text(
                                    text = "Deployed: $dateStr",
                                    color = CosmicTextDim,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    if (contracts.size > 3) {
                        TextButton(
                            onClick = onNavigateToLifecycle,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "View All ${contracts.size} Lifecycles",
                                color = SentinelGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
