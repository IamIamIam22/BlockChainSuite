package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SentinelRepository
import com.example.ui.components.ContractLifecycleTab
import com.example.ui.components.FaucetAutomatorTab
import com.example.ui.components.MevBotTab
import com.example.ui.components.SmartWalletTab
import com.example.ui.components.ProfileTab
import com.example.ui.components.TroubleshootingTab
import com.example.ui.theme.*

enum class DashboardTab(
    val title: String,
    val shortLabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    CONTRACT_MANAGER("Contract Manager", "Contracts", Icons.Filled.ViewInAr),
    MEV_TRACKER("MEV Tracker", "MEV Tracker", Icons.Filled.Hub),
    FAUCET_CONTROLLER("Faucet Controller", "Faucets", Icons.Filled.WaterDrop)
}

enum class SecondarySheet {
    NONE, WALLETS, PROFILE, DIAGNOSTICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: SentinelRepository,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(DashboardTab.CONTRACT_MANAGER) }
    var activeSecondarySheet by remember { mutableStateOf(SecondarySheet.NONE) }
    val context = LocalContext.current

    val trackedContractsCount = repository.trackedContracts.size
    val activeOpportunitiesCount = repository.arbitrageOpportunities.size
    val faucetCount = repository.faucets.size
    val healthScore by repository.overallSystemHealthScore
    val unresolvedDiags = repository.diagnosticLogs.count { !it.isResolved }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground),
        topBar = {
            Column {
                // Top App Bar
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Immersive UI Gradient Logo Badge
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(SentinelGold, SentinelBlue)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = "Logo",
                                    tint = CosmicBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SENTINEL MEV",
                                    color = CosmicTextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = when (activeTab) {
                                        DashboardTab.CONTRACT_MANAGER -> "Smart Contract Studio"
                                        DashboardTab.MEV_TRACKER -> "Mempool & Arbitrage Engine"
                                        DashboardTab.FAUCET_CONTROLLER -> "Multi-Chain Testnet Faucets"
                                    },
                                    color = SentinelGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    actions = {
                        val isRealMode by repository.isRealMainnetMode
                        
                        // Mainnet Real/Simulated Toggle Switch Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isRealMode) SentinelGoldDim else CosmicCardInner)
                                .border(1.dp, if (isRealMode) SentinelGold else CosmicDivider, RoundedCornerShape(20.dp))
                                .clickable {
                                    repository.saveMainnetMode(!isRealMode)
                                    val newMode = if (!isRealMode) "Real Mainnet Mode" else "Simulated Sandbox Mode"
                                    Toast.makeText(context, "Switched operation mode to: $newMode", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isRealMode) SentinelGold else CosmicTextDim)
                            )
                            Text(
                                text = if (isRealMode) "MAINNET" else "SANDBOX",
                                color = if (isRealMode) SentinelGold else CosmicTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Smart Wallets Quick Action Button
                        IconButton(
                            onClick = { activeSecondarySheet = SecondarySheet.WALLETS },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = "Smart Wallets",
                                tint = CosmicTextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Diagnostics & Health Quick Action Badge
                        IconButton(
                            onClick = { activeSecondarySheet = SecondarySheet.DIAGNOSTICS },
                            modifier = Modifier.size(36.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unresolvedDiags > 0) {
                                        Badge(
                                            containerColor = Color(0xFFFFB74D),
                                            contentColor = CosmicBackground
                                        ) {
                                            Text("$unresolvedDiags", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (unresolvedDiags > 0) Icons.Filled.WarningAmber else Icons.Filled.BuildCircle,
                                    contentDescription = "Diagnostics",
                                    tint = if (unresolvedDiags > 0) Color(0xFFFFB74D) else SentinelEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Profile & Settings Quick Action Button
                        IconButton(
                            onClick = { activeSecondarySheet = SecondarySheet.PROFILE },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Profile & Sync",
                                tint = CosmicTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CosmicBackground,
                        titleContentColor = CosmicTextPrimary
                    )
                )

                // High fidelity Hero Banner using generated image asset
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, CosmicDivider, RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sentinel_hero),
                        contentDescription = "Sentinel Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Transparent overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color(0xCC0F0D13))
                    )
                    
                    // Banner text overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "AUTOMATED MEMPOOL & DEFI COMMAND",
                            color = SentinelGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Contract Manager • MEV Tracker • Faucet Controller",
                            color = CosmicTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "Contracts: $trackedContractsCount",
                                color = SentinelEmerald,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "•",
                                color = CosmicTextDim,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Opps: $activeOpportunitiesCount",
                                color = SentinelGold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "•",
                                color = CosmicTextDim,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Faucets: $faucetCount",
                                color = SentinelBlue,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Primary Tab Row for top switching
                PrimaryTabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = CosmicBackground,
                    contentColor = SentinelGold,
                    divider = { HorizontalDivider(color = CosmicDivider, thickness = 1.dp) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    DashboardTab.values().forEach { tab ->
                        val isSelected = activeTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { activeTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            selectedContentColor = SentinelGold,
                            unselectedContentColor = CosmicTextSecondary
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Standard Navigation Bar styled with 3 primary tab sections
            NavigationBar(
                containerColor = CosmicSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.border(BorderStroke(1.dp, CosmicDivider))
            ) {
                DashboardTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CosmicBackground,
                            selectedTextColor = SentinelGold,
                            indicatorColor = SentinelGold,
                            unselectedIconColor = CosmicTextSecondary,
                            unselectedTextColor = CosmicTextSecondary
                        )
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        // Render content inside crossfade for smooth screen transitioning
        Crossfade(
            targetState = activeTab,
            animationSpec = tween(250),
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                DashboardTab.CONTRACT_MANAGER -> {
                    ContractLifecycleTab(
                        repository = repository
                    )
                }
                DashboardTab.MEV_TRACKER -> {
                    MevBotTab(
                        repository = repository,
                        onNavigateToLifecycle = { activeTab = DashboardTab.CONTRACT_MANAGER }
                    )
                }
                DashboardTab.FAUCET_CONTROLLER -> {
                    FaucetAutomatorTab(
                        repository = repository
                    )
                }
            }
        }

        // Interactive Opportunity Alert Dialog popup
        val activeOpportunity = repository.activeOpportunityPrompt.value
        if (activeOpportunity != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    repository.activeOpportunityPrompt.value = null
                },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                var isExecuting by remember { mutableStateOf(false) }
                var isFinished by remember { mutableStateOf(false) }
                var executionSuccess by remember { mutableStateOf(false) }
                val consoleLogs = remember { mutableStateListOf<String>() }
                val scope = rememberCoroutineScope()

                val defaultPool = when (activeOpportunity.asset) {
                    "WBTC" -> "Aave V3 Capital Pool"
                    "ETH", "WETH" -> "Aave V3 Capital Pool"
                    "USDC", "USDT" -> "Balancer V2 Vault (Zero-Fee)"
                    "LINK" -> "Uniswap V3 Flash-swap"
                    else -> "Balancer V2 Vault (Zero-Fee)"
                }
                var selectedPool by remember { mutableStateOf(defaultPool) }
                var showPoolDropdown by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(2.dp, if (isFinished && executionSuccess) SentinelEmerald else SentinelGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (isFinished) {
                                    if (executionSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error
                                } else Icons.Filled.NotificationsActive,
                                contentDescription = "Alert",
                                tint = if (isFinished) {
                                    if (executionSuccess) SentinelEmerald else Color(0xFFEF5350)
                                } else SentinelGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (isFinished) "EXECUTION RESULT" else "MEV PATH DETECTED",
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (!isExecuting && !isFinished) {
                            // Opportunity specs
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Asset Pathway:", color = CosmicTextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = "${activeOpportunity.asset} (Arbitrage)",
                                        color = SentinelGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Routing DEX:", color = CosmicTextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = "${activeOpportunity.buyDex} ➔ ${activeOpportunity.sellDex}",
                                        color = CosmicTextPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Profit Target:", color = CosmicTextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = String.format("+%.2f%% ($%.2f)", activeOpportunity.priceDiffPct, activeOpportunity.estProfitUsd),
                                        color = SentinelEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Est. Gas Subsidies:", color = CosmicTextSecondary, fontSize = 12.sp)
                                    Text(
                                        text = String.format("$%.2f Gas", activeOpportunity.requiredGasUsd),
                                        color = SentinelBlue,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Interactive Borrowing Pool Selector
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.AccountBalance,
                                            contentDescription = "Pool",
                                            tint = SentinelEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Borrowing Capital Pool",
                                            color = CosmicTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CosmicBackground)
                                            .clickable { showPoolDropdown = !showPoolDropdown }
                                            .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = selectedPool,
                                                color = SentinelGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Icon(
                                                imageVector = if (showPoolDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = "Expand",
                                                tint = CosmicTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                if (showPoolDropdown) {
                                    val availablePools = listOf(
                                        "Aave V3 Capital Pool",
                                        "Balancer V2 Vault (Zero-Fee)",
                                        "Uniswap V3 Flash-swap",
                                        "dYdX SoloMargin"
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        availablePools.forEach { pool ->
                                            val isSelected = pool == selectedPool
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSelected) CosmicBackground else Color.Transparent)
                                                    .clickable {
                                                        selectedPool = pool
                                                        showPoolDropdown = false
                                                    }
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = pool,
                                                    color = if (isSelected) SentinelEmerald else CosmicTextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = SentinelEmerald,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "A lucrative arbitrage opportunity has been intercepted by your background daemons. Would you like to ride the financials directly on-chain via the actual mainnet application, or simulate the sandbox run?",
                                color = CosmicTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            // Action Buttons
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = {
                                        isExecuting = true
                                        consoleLogs.clear()
                                        repository.executeArbitrageOpportunity(
                                            opportunity = activeOpportunity,
                                            isRealMainnet = true,
                                            selectedPool = selectedPool,
                                            onLog = { log -> consoleLogs.add(log) },
                                            onCompleted = { success ->
                                                executionSuccess = success
                                                isExecuting = false
                                                isFinished = true
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SentinelGold,
                                        contentColor = CosmicBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Bolt, contentDescription = "Mainnet", tint = CosmicBackground)
                                        Text("🟢 RIDE FINANCIALS ON MAINNET", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CosmicBackground)
                                    }
                                }

                                Button(
                                    onClick = {
                                        isExecuting = true
                                        consoleLogs.clear()
                                        repository.executeArbitrageOpportunity(
                                            opportunity = activeOpportunity,
                                            isRealMainnet = false,
                                            selectedPool = selectedPool,
                                            onLog = { log -> consoleLogs.add(log) },
                                            onCompleted = { success ->
                                                executionSuccess = success
                                                isExecuting = false
                                                isFinished = true
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CosmicCardInner,
                                        contentColor = CosmicTextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CosmicDivider)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Science, contentDescription = "Simulated", tint = CosmicTextPrimary)
                                        Text("🧪 RUN SIMULATED SANDBOX", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        repository.activeOpportunityPrompt.value = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Ignore & Dismiss Opportunity", color = CosmicTextSecondary, fontSize = 12.sp)
                                }
                            }
                        } else {
                            // Is executing or finished
                            if (isExecuting) {
                                Text(
                                    text = "EXECUTING ARBITRAGE BUNDLE...",
                                    color = SentinelGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = SentinelGold,
                                    trackColor = CosmicCardInner
                                )
                            } else {
                                Text(
                                    text = if (executionSuccess) "⚡ BUNDLE EXECUTION SUCCESS" else "❌ TRANSACTION REVERTED",
                                    color = if (executionSuccess) SentinelEmerald else Color(0xFFEF5350),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Scrollable Console Terminal
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CosmicBackground)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(consoleLogs.size) { index ->
                                        val log = consoleLogs[index]
                                        Text(
                                            text = log,
                                            color = if (log.startsWith("✅")) SentinelEmerald else if (log.startsWith("❌")) Color(0xFFEF5350) else CosmicTextPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (isFinished) {
                                Button(
                                    onClick = {
                                        repository.activeOpportunityPrompt.value = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (executionSuccess) SentinelEmeraldDim else CosmicCardInner,
                                        contentColor = if (executionSuccess) SentinelEmerald else CosmicTextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (executionSuccess) SentinelEmerald else CosmicDivider)
                                ) {
                                    Text("DISMISS PROTOCOL SCREEN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Secondary Tools Sheet (Wallets, Diagnostics, Profile)
        if (activeSecondarySheet != SecondarySheet.NONE) {
            ModalBottomSheet(
                onDismissRequest = { activeSecondarySheet = SecondarySheet.NONE },
                containerColor = CosmicSurface,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CosmicDivider)
                        )
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .padding(horizontal = 8.dp)
                ) {
                    when (activeSecondarySheet) {
                        SecondarySheet.WALLETS -> {
                            SmartWalletTab(repository = repository)
                        }
                        SecondarySheet.DIAGNOSTICS -> {
                            TroubleshootingTab(repository = repository)
                        }
                        SecondarySheet.PROFILE -> {
                            ProfileTab(
                                repository = repository,
                                onNavigateToTroubleshooting = {
                                    activeSecondarySheet = SecondarySheet.DIAGNOSTICS
                                }
                            )
                        }
                        SecondarySheet.NONE -> {}
                    }
                }
            }
        }
    }
}
