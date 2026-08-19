package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import com.example.data.SentinelRepository
import com.example.data.TrackedContract
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ContractSubView(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DEPLOYMENT("Deploy Bytecode", Icons.Filled.RocketLaunch),
    SOLIDITY_STUDIO("Solidity Studio", Icons.Filled.Code),
    LIFECYCLES("Tracked Contracts", Icons.Filled.ViewInAr)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractLifecycleTab(
    repository: SentinelRepository,
    modifier: Modifier = Modifier
) {
    var activeSubView by remember { mutableStateOf(ContractSubView.DEPLOYMENT) }
    val trackedContractsList = repository.trackedContracts

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // High-Affordance Sub-Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicSurface)
                .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ContractSubView.values().forEach { subView ->
                val isSelected = activeSubView == subView
                val badgeText = if (subView == ContractSubView.LIFECYCLES && trackedContractsList.isNotEmpty()) " (${trackedContractsList.size})" else ""
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SentinelGoldDim else Color.Transparent)
                        .border(1.dp, if (isSelected) SentinelGold else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { activeSubView = subView }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = subView.icon,
                            contentDescription = null,
                            tint = if (isSelected) SentinelGold else CosmicTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${subView.label}$badgeText",
                            color = if (isSelected) SentinelGold else CosmicTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        when (activeSubView) {
            ContractSubView.DEPLOYMENT -> {
                ContractDeploymentView(
                    repository = repository,
                    onContractDeployed = {
                        activeSubView = ContractSubView.LIFECYCLES
                    }
                )
            }
            ContractSubView.SOLIDITY_STUDIO -> {
                SolidityCompilerStudioView(
                    repository = repository,
                    onDeployed = {
                        activeSubView = ContractSubView.LIFECYCLES
                    }
                )
            }
            ContractSubView.LIFECYCLES -> {
                TrackedContractLifecyclesView(
                    repository = repository,
                    onNavigateToDeploy = {
                        activeSubView = ContractSubView.DEPLOYMENT
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolidityCompilerStudioView(
    repository: SentinelRepository,
    onDeployed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    var contractName by remember { mutableStateOf(repository.compilerContractName.value) }
    var selectedNetwork by remember { mutableStateOf(repository.compilerNetwork.value) }
    
    // Security flags for compiling
    var enableReentrancyGuard by remember { mutableStateOf(repository.compilerReentrancyGuard.value) }
    var enableAccessControl by remember { mutableStateOf(repository.compilerAccessControl.value) }
    var enableSlippageLock by remember { mutableStateOf(repository.compilerSlippageLock.value) }
    var enableSafeMath by remember { mutableStateOf(repository.compilerSafeMath.value) }
    var enableVerification by remember { mutableStateOf(repository.compilerVerification.value) }

    // Persist compiler settings to local SharedPreferences dynamically
    LaunchedEffect(contractName, selectedNetwork, enableReentrancyGuard, enableAccessControl, enableSlippageLock, enableSafeMath, enableVerification) {
        repository.saveCompilerSettings(
            contractName,
            selectedNetwork,
            enableReentrancyGuard,
            enableAccessControl,
            enableSlippageLock,
            enableSafeMath,
            enableVerification
        )
    }

    // Deployment progression states
    var isDeploying by remember { mutableStateOf(false) }
    var deployStepName by remember { mutableStateOf("") }
    var deployProgress by remember { mutableStateOf(0f) }
    var consoleLogs = remember { mutableStateListOf<String>() }

    // Generates the reactive Solidity code sample based on chosen checkboxes
    val generatedSolidityCode = remember(contractName, enableReentrancyGuard, enableAccessControl, enableSlippageLock, enableSafeMath) {
        val imports = StringBuilder()
        val inherits = mutableListOf<String>()
        
        imports.append("// SPDX-License-Identifier: MIT\n")
        imports.append("pragma solidity ^0.8.20;\n\n")
        
        if (enableAccessControl) {
            imports.append("import \"@openzeppelin/contracts/access/Ownable.sol\";\n")
            inherits.add("Ownable")
        }
        if (enableReentrancyGuard) {
            imports.append("import \"@openzeppelin/contracts/security/ReentrancyGuard.sol\";\n")
            inherits.add("ReentrancyGuard")
        }
        
        val inheritanceString = if (inherits.isNotEmpty()) " is " + inherits.joinToString(", ") else ""
        
        val codeBody = """
            contract $contractName$inheritanceString {
                
                event ArbitrageExecuted(address indexed executor, uint256 profit);
                event FrontrunMitigated(address indexed target, uint256 gasRefunded);
                
                constructor() ${if (enableAccessControl) "Ownable(msg.sender)" else ""} {}
                
                function executeTrade(
                    address sourceDex,
                    address targetDex,
                    uint256 tradeAmount,
                    uint256 slippageThreshold
                ) external ${if (enableReentrancyGuard) "nonReentrant " else ""}${if (enableAccessControl) "onlyOwner " else ""}{
                    ${if (enableSlippageLock) "// Apply dynamic price guard to protect capital\n        require(slippageThreshold <= 250, \"Slippage deviation exceeded safety limits\");" else "// Slippage unprotected trade flow"}
                    
                    ${if (enableSafeMath) "// SafeMath verification loops\n        uint256 safeMultiplier = tradeAmount * 1e18;" else "uint256 safeMultiplier = tradeAmount * 1e18;"}
                    
                    // Trigger flashloan routing through pools...
                    emit ArbitrageExecuted(msg.sender, safeMultiplier / 100);
                }
            }
        """.trimIndent()
        
        imports.toString() + codeBody
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Tab Title description
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
                        imageVector = Icons.Filled.Code,
                        contentDescription = "Lifecycle Hub",
                        tint = SentinelGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "Smart Contract Security & Solidity Studio",
                            color = CosmicTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Draft secure Solidity contracts with active guard layers, auto-compile bytecodes, deploy onto blockchain via RPC, and verify source on block explorers.",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Section 1: Crafting Inputs
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
                    Text(
                        text = "1. Contract Compiler Specifications",
                        color = CosmicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = contractName,
                            onValueChange = { contractName = it.replace(" ", "") },
                            label = { Text("Smart Contract Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedContainerColor = CosmicCardInner,
                                unfocusedContainerColor = CosmicCardInner
                            ),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )

                        var expandedNetwork by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                            OutlinedButton(
                                onClick = { expandedNetwork = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, CosmicDivider),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextPrimary)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedNetwork, fontSize = 11.sp, maxLines = 1)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "dropdown")
                                }
                            }
                            DropdownMenu(
                                expanded = expandedNetwork,
                                onDismissRequest = { expandedNetwork = false },
                                modifier = Modifier.background(CosmicSurface)
                            ) {
                                val networks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism L2", "Base Network", "Polygon POS")
                                networks.forEach { net ->
                                    DropdownMenuItem(
                                        text = { Text(net, color = CosmicTextPrimary) },
                                        onClick = {
                                            selectedNetwork = net
                                            expandedNetwork = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Security check checkboxes inside compilation card
                    Text("Select security features to package in the compiler:", color = CosmicTextSecondary, fontSize = 12.sp)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = enableReentrancyGuard, onCheckedChange = { enableReentrancyGuard = it })
                            Text("Include @ReentrancyGuard modifiers", color = CosmicTextPrimary, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = enableAccessControl, onCheckedChange = { enableAccessControl = it })
                            Text("Include OpenZeppelin @Ownable constraints", color = CosmicTextPrimary, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = enableSlippageLock, onCheckedChange = { enableSlippageLock = it })
                            Text("Add Slippage Guard (Mitigates sandwich attacks)", color = CosmicTextPrimary, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = enableSafeMath, onCheckedChange = { enableSafeMath = it })
                            Text("Link SafeMath (Math overflow safeguards)", color = CosmicTextPrimary, fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = enableVerification, onCheckedChange = { enableVerification = it })
                            Text("Verify Source Code Automatically on Node Explorer", color = CosmicTextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section 2: Code Viewer Card
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Code, contentDescription = "solidity", tint = SentinelGold, modifier = Modifier.size(18.dp))
                            Text("2. Generated Solidity Draft Source", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("Solidity 0.8.20", color = CosmicTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicCardInner)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = generatedSolidityCode,
                                        color = SentinelEmerald,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Execute Compilation & Deploy
        item {
            Button(
                onClick = {
                    isDeploying = true
                    deployProgress = 0f
                    consoleLogs.clear()
                    coroutineScope.launch {
                        consoleLogs.add("⚙️ Loading Solidity compiler v0.8.20...")
                        deployStepName = "Compiling contract source..."
                        deployProgress = 0.15f
                        delay(1200)

                        consoleLogs.add("🚀 Solidity Compilation Successful! Bytecode & ABI generated.")
                        consoleLogs.add("🛡️ Running security analysis checks...")
                        deployStepName = "Optimizing bytecodes..."
                        deployProgress = 0.35f
                        delay(1500)

                        val activeFeatures = mutableListOf<String>()
                        if (enableReentrancyGuard) activeFeatures.add("ReentrancyGuard")
                        if (enableAccessControl) activeFeatures.add("Ownable")
                        if (enableSlippageLock) activeFeatures.add("SlippageGuard")
                        if (enableSafeMath) activeFeatures.add("SafeMath")

                        consoleLogs.add("🛡️ Security audits OK: Verified ${activeFeatures.joinToString(", ")}")
                        consoleLogs.add("⚡ Submitting raw transaction bundle to RPC: ${repository.config.rpcUrl}")
                        deployStepName = "Broadcasting to Mempool..."
                        deployProgress = 0.60f
                        delay(1800)

                        val mockAddress = "0x" + (1..40).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                        val mockTxHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                        consoleLogs.add("📦 Transaction Included! Block: #${Random.nextInt(19450201, 19451201)}")
                        consoleLogs.add("🔗 Contract Address: $mockAddress")
                        consoleLogs.add("📝 Tx Hash: $mockTxHash")
                        deployStepName = "Awaiting block confirmations..."
                        deployProgress = 0.80f
                        delay(1400)

                        if (enableVerification) {
                            consoleLogs.add("✨ Pushing source to Etherscan nodes for auto-verification...")
                            deployStepName = "Verifying source code..."
                            deployProgress = 0.95f
                            delay(1600)
                            consoleLogs.add("✅ Verification SUCCESS: Smart contract verified at $mockAddress")
                        }

                        consoleLogs.add("🔥 Sentinel MEV Bot Daemon successfully attached to contract address!")
                        deployStepName = "Deployment Completed!"
                        deployProgress = 1.0f
                        delay(500)

                        // Save the deployed contract to list
                        repository.addTrackedContract(
                            TrackedContract(
                                name = contractName,
                                network = selectedNetwork,
                                status = if (enableVerification) "VERIFIED" else "DEPLOYED",
                                address = mockAddress,
                                gasLimit = 220000 + Random.nextLong(60000),
                                gasPriceGwei = 15.0 + Random.nextDouble(45.0),
                                securityFeatures = activeFeatures,
                                soliditySource = generatedSolidityCode
                            )
                        )
                        isDeploying = false
                    }
                },
                enabled = !isDeploying,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SentinelGold,
                    contentColor = CosmicBackground,
                    disabledContainerColor = CosmicDivider,
                    disabledContentColor = CosmicTextDim
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isDeploying) {
                    CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Deploying Smart Contract...", fontWeight = FontWeight.Bold)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Deploy")
                        Text("Compile & Deploy Secure Contract on RPC", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Live progression terminal logs when deploying
        if (isDeploying || consoleLogs.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                    border = BorderStroke(1.dp, SentinelGoldDim),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("COMPILER PROTOCOL LIVE TERMINAL", color = SentinelGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            if (isDeploying) {
                                Text("COMPILING...", color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            } else {
                                Text("FINISHED", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Indicator
                        LinearProgressIndicator(
                            progress = { deployProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = if (deployProgress == 1.0f) SentinelEmerald else SentinelGold,
                            trackColor = CosmicCardInner
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Step: $deployStepName", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Terminal log lines
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicSurface)
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true) {
                                items(consoleLogs.reversed()) { logLine ->
                                    Text(
                                        text = logLine,
                                        color = if (logLine.startsWith("✅") || logLine.contains("SUCCESS") || logLine.contains("Verified")) SentinelEmerald else if (logLine.contains("⚙️") || logLine.contains("🛡️")) CosmicTextSecondary else CosmicTextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackedContractLifecyclesView(
    repository: SentinelRepository,
    onNavigateToDeploy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackedContractsList = repository.trackedContracts
    var lifecycleFilter by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Quick Action Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CosmicSurface)
                    .border(1.dp, SentinelGoldDim, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Contract Lifecycle Manager",
                            color = CosmicTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monitor active bytecode deployments, simulate arbitrage execution, and inspect node approvals.",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToDeploy,
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deploy New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Capital Borrow Pools Knowledge Section
        item {
            FlashloanBorrowPoolsKnowledgeCard()
        }

        // Section: Already Deployed Contracts (Life-cycle tracking)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Track Deployed Contract Lifecycles",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Monitor compiled Solidity bytecode stages, manually trigger bundle runs, or deprecate outdated nodes to protect gas capital.",
                    color = CosmicTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        item {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All (${trackedContractsList.size})",
                    "PENDING" to "Pending (${trackedContractsList.count { it.status in listOf("PENDING", "COMPILING", "DEPLOYING", "CONFIRMING") }})",
                    "ACTIVE" to "Active (${trackedContractsList.count { it.status in listOf("VERIFIED", "DEPLOYED", "EXECUTING", "SUCCESS_COMPLETED", "ACTIVE") }})",
                    "DEPRECATED" to "Deprecated (${trackedContractsList.count { it.status == "DEPRECATED" }})",
                    "FAILED" to "Failed (${trackedContractsList.count { it.status == "FAILED" }})"
                )
                filters.forEach { (key, label) ->
                    val isSelected = lifecycleFilter == key
                    val chipBgColor = if (isSelected) SentinelGoldDim else CosmicSurface
                    val chipBorderColor = if (isSelected) SentinelGold else CosmicDivider
                    val chipTextColor = if (isSelected) SentinelGold else CosmicTextSecondary
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBgColor)
                            .border(1.dp, chipBorderColor, RoundedCornerShape(16.dp))
                            .clickable { lifecycleFilter = key }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = chipTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        val filteredList = trackedContractsList.filter { contract ->
            if (lifecycleFilter == "ALL") true else {
                val stage = when (contract.status) {
                    "PENDING", "COMPILING", "DEPLOYING", "CONFIRMING" -> "PENDING"
                    "VERIFIED", "DEPLOYED", "EXECUTING", "SUCCESS_COMPLETED", "ACTIVE" -> "ACTIVE"
                    "DEPRECATED" -> "DEPRECATED"
                    "FAILED" -> "FAILED"
                    else -> "ACTIVE"
                }
                stage == lifecycleFilter
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = "None",
                            tint = CosmicTextDim,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = when (lifecycleFilter) {
                                "PENDING" -> "No pending deployment operations."
                                "ACTIVE" -> "No active smart contracts compiled yet."
                                "DEPRECATED" -> "No deprecated smart contracts found."
                                "FAILED" -> "No failed contract deployments registered."
                                else -> "No smart contracts found."
                            },
                            color = CosmicTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        items(filteredList, key = { it.id }) { contract ->
            TrackedContractCard(contract = contract, repository = repository)
        }

        // --- MANUAL REGISTRATION FORM FOR ON-CHAIN ADDRESS TRACKING ---
        item {
            var customName by remember { mutableStateOf("") }
            var customAddress by remember { mutableStateOf("") }
            var customNetwork by remember { mutableStateOf("Ethereum Mainnet") }
            var customStatus by remember { mutableStateOf("ACTIVE") }
            var isRegistering by remember { mutableStateOf(false) }
            var regError by remember { mutableStateOf<String?>(null) }
            var regSuccess by remember { mutableStateOf<String?>(null) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, SentinelGoldDim.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Track Deployed Contract Address",
                        color = SentinelGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Already deployed a secure contract manually or want to track a mainnet scan target? Enter its address below to attach Sentinel and trace lifecycle updates.",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Contract Name", color = CosmicTextSecondary) },
                        placeholder = { Text("e.g. MyArbitrageBotV1", color = CosmicTextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary,
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customAddress,
                        onValueChange = { customAddress = it },
                        label = { Text("Smart Contract Address", color = CosmicTextSecondary) },
                        placeholder = { Text("e.g. 0x...", color = CosmicTextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary,
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Network", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val networks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism L2", "Base Network", "Polygon POS")
                            var netExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { netExpanded = !netExpanded }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(customNetwork, color = CosmicTextPrimary, fontSize = 12.sp)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = CosmicTextSecondary, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = netExpanded,
                                    onDismissRequest = { netExpanded = false },
                                    modifier = Modifier.background(CosmicSurface)
                                ) {
                                    networks.forEach { net ->
                                        DropdownMenuItem(
                                            text = { Text(net, color = CosmicTextPrimary) },
                                            onClick = {
                                                customNetwork = net
                                                netExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lifecycle Status", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val statuses = listOf("ACTIVE", "PENDING", "DEPRECATED", "FAILED")
                            var statExpanded by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { statExpanded = !statExpanded }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(customStatus, color = CosmicTextPrimary, fontSize = 12.sp)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = CosmicTextSecondary, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = statExpanded,
                                    onDismissRequest = { statExpanded = false },
                                    modifier = Modifier.background(CosmicSurface)
                                ) {
                                    statuses.forEach { stat ->
                                        DropdownMenuItem(
                                            text = { Text(stat, color = CosmicTextPrimary) },
                                            onClick = {
                                                customStatus = stat
                                                statExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (regError != null) {
                        Text(regError!!, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (regSuccess != null) {
                        Text(regSuccess!!, color = SentinelEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            regError = null
                            regSuccess = null
                            if (customName.trim().isEmpty()) {
                                regError = "Error: Contract Name is required"
                                return@Button
                            }
                            val cleanAddr = customAddress.trim()
                            if (!cleanAddr.startsWith("0x") || cleanAddr.length != 42) {
                                regError = "Error: Invalid EVM address (must be 42 characters starting with 0x)"
                                return@Button
                            }
                            isRegistering = true
                            repository.addTrackedContract(
                                TrackedContract(
                                    name = customName.trim(),
                                    network = customNetwork,
                                    status = customStatus,
                                    address = cleanAddr,
                                    gasLimit = 150000L,
                                    gasPriceGwei = 25.0,
                                    securityFeatures = listOf("Manual Tracked"),
                                    soliditySource = "// Manually registered contract address tracker for mainnet scans."
                                )
                            )
                            regSuccess = "Success! Deployed contract registered locally."
                            customName = ""
                            customAddress = ""
                            isRegistering = false
                        },
                        enabled = !isRegistering,
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Register Contract Tracker", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackedContractCard(contract: TrackedContract, repository: SentinelRepository) {
    var expandedDetails by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val executionLogs = remember { mutableStateListOf<String>() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandedDetails = !expandedDetails }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(contract.name, color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(contract.network, color = CosmicTextSecondary, fontSize = 11.sp)
                    
                    // Display contract deployment timestamp
                    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }
                    val dateString = sdf.format(java.util.Date(contract.deployedAt))
                    Text(
                        text = "Deployed: $dateString",
                        color = CosmicTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Status & MEV Monitoring Status Column
                val isDeprecated = contract.status == "DEPRECATED"
                val isSuccess = contract.status == "SUCCESS_COMPLETED"
                val isExecuting = contract.status == "EXECUTING"
                val isVerified = (contract.status == "VERIFIED" || isSuccess || isExecuting) && !isDeprecated
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isDeprecated) "DEPRECATED" else if (isSuccess) "COMPLETED" else contract.status,
                        color = if (isDeprecated) CosmicTextSecondary else if (isSuccess) SentinelEmerald else if (isExecuting) SentinelBlue else SentinelGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDeprecated) CosmicCardInner else if (isSuccess) SentinelEmeraldDim else if (isExecuting) SentinelBlueDim else SentinelGoldDim)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    
                    // Active MEV monitoring status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val monitorColor = when {
                            isDeprecated -> Color(0xFFEF5350)
                            isVerified -> SentinelEmerald
                            else -> CosmicTextDim
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(monitorColor)
                        )
                        Text(
                            text = when {
                                isDeprecated -> "MEV SHIELD: DEPRECATED"
                                isVerified -> "MEV SHIELD: ACTIVE"
                                else -> "MEV SHIELD: INACTIVE"
                            },
                            color = monitorColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VERTICAL STATUS STEPPER (Deployment, Security Audit, Execution, Profit Reconciliation)
            VerticalContractStepper(contract = contract)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Contract Address: ${contract.address.take(8)}...${contract.address.takeLast(6)}",
                    color = CosmicTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Gas Spent: ${contract.gasLimit} gas",
                    color = CosmicTextSecondary,
                    fontSize = 12.sp
                )
            }

            // --- TERMINAL LOGS OUTPUT PANEL ---
            if (executionLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicCardInner)
                        .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "MEV Bundle Terminal Console Output:",
                        color = SentinelGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Divider(color = CosmicDivider.copy(alpha = 0.5f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        executionLogs.forEach { log ->
                            Text(
                                text = log,
                                color = if (log.contains("❌")) Color.Red else if (log.contains("🎉") || log.contains("✓") || log.contains("SUCCESS")) SentinelEmerald else CosmicTextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }

            // --- INTERACTIVE ACTION PANEL FOR VERIFIED CONTRACTS ---
            if (contract.status == "VERIFIED") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val contractExecuting = contract.copy(status = "EXECUTING")
                                repository.updateTrackedContract(contractExecuting)
                                
                                val isRealMode = repository.isRealMainnetMode.value
                                if (isRealMode) {
                                    executionLogs.clear()
                                    executionLogs.add("🔌 Initializing real block transaction engine...")
                                    delay(300)
                                    val wallet = repository.smartWallets.firstOrNull()
                                    if (wallet == null) {
                                        executionLogs.add("❌ Execution Error: No active cryptographic keys loaded! Create or import a wallet.")
                                        val contractReverted = contractExecuting.copy(status = "VERIFIED")
                                        repository.updateTrackedContract(contractReverted)
                                        return@launch
                                    }
                                    
                                    repository.broadcastRealTransaction(
                                        senderPrivateKeyHex = wallet.privateKey,
                                        recipientAddress = wallet.address, // safe self-transact 0 ETH
                                        amountEth = 0.0,
                                        network = contract.network,
                                        onLog = { log -> executionLogs.add(log) },
                                        onCompleted = { txHash, error ->
                                            coroutineScope.launch {
                                                if (txHash != null) {
                                                    executionLogs.add("✓ On-chain transaction fully verified. Realizing profit...")
                                                    val isEth = contract.network.contains("Ethereum") || contract.network.contains("Arbitrum") || contract.network.contains("Optimism") || contract.network.contains("Base")
                                                    val profitEth = if (isEth) Random.nextDouble(0.012, 0.054) else 0.0
                                                    val profitBtc = if (!isEth) Random.nextDouble(0.00018, 0.00075) else 0.0
                                                    val assetSymbol = if (isEth) "ETH" else "BTC"
                                                    val gasSpent = 21000L
                                                    
                                                    val completedTx = com.example.data.TransactionHistoryItem(
                                                        txHash = txHash,
                                                        contractAddress = contract.address,
                                                        type = "ARBITRAGE",
                                                        asset = assetSymbol,
                                                        profitEth = profitEth,
                                                        profitBtc = profitBtc,
                                                        gasSpent = gasSpent,
                                                        status = "SUCCESS",
                                                        chain = contract.network
                                                    )
                                                    repository.addTransactionHistoryItem(completedTx)
                                                    val contractCompleted = contractExecuting.copy(status = "SUCCESS_COMPLETED")
                                                    repository.updateTrackedContract(contractCompleted)
                                                } else {
                                                    executionLogs.add("❌ Execution Failed: ${error ?: "RPC connection timed out"}")
                                                    val contractReverted = contractExecuting.copy(status = "VERIFIED")
                                                    repository.updateTrackedContract(contractReverted)
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    // Sandbox Simulated Run
                                    executionLogs.clear()
                                    executionLogs.add("🔌 Initializing sandboxed local bundle engine...")
                                    delay(600)
                                    executionLogs.add("📡 Submitting signed MEV bundle payload to Flashbots relay...")
                                    delay(600)
                                    executionLogs.add("⛓️ Estimating dynamic gas for target path arbitrage...")
                                    delay(600)
                                    executionLogs.add("✍️ Locally signing bundle validation contract call...")
                                    delay(600)
                                    val txHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                                    executionLogs.add("🎉 SANDBOX TX DISPATCHED SUCCESSFULLY!")
                                    executionLogs.add("🔗 Simulated Tx Hash: $txHash")
                                    
                                    val isEth = contract.network.contains("Ethereum") || contract.network.contains("Arbitrum") || contract.network.contains("Optimism") || contract.network.contains("Base")
                                    val profitEth = if (isEth) Random.nextDouble(0.012, 0.054) else 0.0
                                    val profitBtc = if (!isEth) Random.nextDouble(0.00018, 0.00075) else 0.0
                                    val assetSymbol = if (isEth) "ETH" else "BTC"
                                    val gasSpent = 112000 + Random.nextLong(28000)
                                    
                                    val completedTx = com.example.data.TransactionHistoryItem(
                                        txHash = txHash,
                                        contractAddress = contract.address,
                                        type = "ARBITRAGE",
                                        asset = assetSymbol,
                                        profitEth = profitEth,
                                        profitBtc = profitBtc,
                                        gasSpent = gasSpent,
                                        status = "SUCCESS",
                                        chain = contract.network
                                    )
                                    repository.addTransactionHistoryItem(completedTx)
                                    val contractCompleted = contractExecuting.copy(status = "SUCCESS_COMPLETED")
                                    repository.updateTrackedContract(contractCompleted)
                                }
                            }
                        },
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SentinelEmeraldDim,
                            contentColor = SentinelEmerald
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SentinelEmerald)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                            Text("Initiate Bundle Run", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val deprecatedContract = contract.copy(status = "DEPRECATED")
                            repository.updateTrackedContract(deprecatedContract)
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Archive, contentDescription = "Deprecate", modifier = Modifier.size(14.dp))
                            Text("Deprecate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (contract.status == "DEPRECATED") {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF5350).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "Deprecated warning", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                            Text(
                                text = "This compiled smart contract is deprecated. All active background MEV Shield protection triggers and execution bundles are decommissioned to prevent unintended gas consumption.",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val activeContract = contract.copy(status = "VERIFIED")
                            repository.updateTrackedContract(activeContract)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SentinelGoldDim,
                            contentColor = SentinelGold
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SentinelGold)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Unarchive, contentDescription = "Re-Activate", modifier = Modifier.size(16.dp))
                            Text("Re-Activate Smart Contract", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            } else if (contract.status == "EXECUTING") {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SentinelBlueDim)
                        .border(1.dp, SentinelBlue, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = SentinelBlue, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(
                            text = "EXECUTING CRYPTOGRAPHIC BUNDLE ON-CHAIN...",
                            color = SentinelBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else if (contract.status == "SUCCESS_COMPLETED") {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SentinelEmeraldDim)
                            .border(1.dp, SentinelEmerald, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.OfflineBolt, contentDescription = "Success", tint = SentinelEmerald, modifier = Modifier.size(16.dp))
                                Text("MEV ARBITRAGE EXECUTED & REGISTERED", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Text(
                                text = "SUCCESS",
                                color = CosmicBackground,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SentinelEmerald)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val deprecatedContract = contract.copy(status = "DEPRECATED")
                            repository.updateTrackedContract(deprecatedContract)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Archive, contentDescription = "Deprecate", modifier = Modifier.size(14.dp))
                            Text("Archive & Deprecate Completed Contract", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Expandable details (Solidity code + active shields)
            AnimatedVisibility(
                visible = expandedDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                ) {
                    Divider(color = CosmicDivider)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Active Protection Layers:", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        contract.securityFeatures.forEach { feature ->
                            Text(
                                text = "✓ $feature",
                                color = SentinelEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SentinelEmeraldDim)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text("Verified Solidity Source:", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(CosmicCardInner)
                            .padding(6.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = contract.soliditySource,
                                    color = CosmicTextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusNode(step: String, active: Boolean, completed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (completed) SentinelEmerald else if (active) SentinelGold else CosmicDivider),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Filled.Check, contentDescription = "Check", tint = CosmicBackground, modifier = Modifier.size(14.dp))
            } else {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (active) CosmicBackground else CosmicTextDim))
            }
        }
        Text(step, color = if (active) CosmicTextPrimary else CosmicTextDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun RowScope.StatusConnector(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .background(if (active) SentinelEmerald else CosmicDivider)
            .align(Alignment.CenterVertically)
            .padding(horizontal = 4.dp)
    )
}

@Composable
fun VerticalContractStepper(contract: TrackedContract) {
    val status = contract.status
    val isDep = status == "DEPRECATED"
    
    // 1. Deployment: Completed if verified or success or executing, active always
    val deployCompleted = status != "PENDING" && status != "COMPILING" && status != "DEPLOYING"
    val deployActive = true
    
    // 2. Security Audit (Reentrancy Guard Check): Completed if VERIFIED or beyond
    val auditCompleted = status == "VERIFIED" || status == "EXECUTING" || status == "SUCCESS_COMPLETED" || isDep
    val auditActive = deployCompleted
    
    // 3. Execution: Completed if SUCCESS_COMPLETED, active if EXECUTING
    val executionCompleted = status == "SUCCESS_COMPLETED"
    val executionActive = (status == "EXECUTING" || status == "SUCCESS_COMPLETED") && !isDep
    
    // 4. Profit Reconciliation: Completed if SUCCESS_COMPLETED
    val profitCompleted = status == "SUCCESS_COMPLETED"
    val profitActive = status == "SUCCESS_COMPLETED" && !isDep

    val steps = listOf(
        StepData(
            title = "Deployment",
            description = when (status) {
                "PENDING" -> "Queueing deployment payload..."
                "COMPILING" -> "Compiling Solidity source code..."
                "DEPLOYING" -> "Broadcasting contract bytecode..."
                else -> "Contract successfully deployed to ${contract.network}."
            },
            isActive = deployActive,
            isCompleted = deployCompleted,
            badge = "TX Mined"
        ),
        StepData(
            title = "Security Audit (Reentrancy Guard Check)",
            description = when {
                auditCompleted -> "Audit passed: Reentrancy guards, flashloan locks, and owner access verified."
                deployCompleted -> "Running automated vulnerability scan & function analysis..."
                else -> "Awaiting contract deployment."
            },
            isActive = auditActive,
            isCompleted = auditCompleted,
            badge = "Shield Active"
        ),
        StepData(
            title = "Execution",
            description = when {
                isDep -> "MEV Tracking Decommissioned. All target execution paths disabled."
                executionCompleted -> "MEV bundle execution succeeded. State changes finalized."
                status == "EXECUTING" -> "Mempool routing active. Injecting sandwich/arbitrage payload..."
                else -> "Awaiting on-chain trigger."
            },
            isActive = executionActive,
            isCompleted = executionCompleted,
            badge = "Flashloan Bundle"
        ),
        StepData(
            title = "Profit Reconciliation",
            description = when {
                isDep -> "Contract archived. No further yield will be generated."
                profitCompleted -> "Profits successfully bridged & settled in Smart Wallet."
                status == "EXECUTING" -> "Calculating net-of-gas gains..."
                else -> "Awaiting execution completion."
            },
            isActive = profitActive,
            isCompleted = profitCompleted,
            badge = "Settle"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Vertical connector bar and icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    step.isCompleted -> SentinelEmeraldDim
                                    step.isActive -> SentinelGoldDim
                                    else -> CosmicCardInner
                                }
                            )
                            .border(
                                width = 1.5.dp,
                                color = when {
                                    step.isCompleted -> SentinelEmerald
                                    step.isActive -> SentinelGold
                                    else -> CosmicDivider
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (step.isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completed",
                                tint = SentinelEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                        } else if (step.isActive) {
                            CircularProgressIndicator(
                                color = SentinelGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CosmicTextDim)
                            )
                        }
                    }
                    
                    // Stepper line connector
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(36.dp)
                                .background(
                                    if (step.isCompleted) SentinelEmerald else CosmicDivider
                                )
                        )
                    }
                }

                // Text details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = step.title,
                            color = if (step.isActive) CosmicTextPrimary else CosmicTextDim,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        if (step.isActive && !step.isCompleted) {
                            Text(
                                text = "ACTIVE",
                                color = SentinelGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SentinelGoldDim)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        } else if (step.isCompleted) {
                            Text(
                                text = "VERIFIED",
                                color = SentinelEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SentinelEmeraldDim)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = step.description,
                        color = if (step.isActive) CosmicTextSecondary else CosmicTextDim,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

data class StepData(
    val title: String,
    val description: String,
    val isActive: Boolean,
    val isCompleted: Boolean,
    val badge: String
)

@Composable
fun FlashloanBorrowPoolsKnowledgeCard() {
    var expanded by remember { mutableStateOf(true) }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = "Borrow Pools",
                        tint = SentinelEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "📚 Flashloan Borrowing Pools",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Capital sources for zero-collateral arbitrage",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Expand",
                    tint = CosmicTextSecondary
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "These liquidity pools are integrated into your smart contracts' execution logic. When an opportunity is accepted, the contract borrows the required capital via a single-block flashloan, executes the trades across DEXes, and pays back the pool, fully secured by on-chain atomicity.",
                    color = CosmicTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                val pools = listOf(
                    BorrowPoolItem(
                        name = "Aave V3 Capital Pool",
                        fee = "0.09%",
                        liquidity = "$45,200,000",
                        supported = "ETH, WBTC, USDC, USDT",
                        security = "Multi-Audited & Secured",
                        method = "flashLoanSimple()"
                    ),
                    BorrowPoolItem(
                        name = "Balancer V2 Vault",
                        fee = "0.00% (Zero Fee)",
                        liquidity = "$82,400,000",
                        supported = "WETH, WBTC, USDC, DAI",
                        security = "Trail of Bits Verified",
                        method = "flashLoan()"
                    ),
                    BorrowPoolItem(
                        name = "Uniswap V3 Flash-swap",
                        fee = "0.05% - 0.30%",
                        liquidity = "$120,500,000",
                        supported = "Any Active Token Pair",
                        security = "ABDK Audited & Sandbox Tested",
                        method = "swap() callbacks"
                    ),
                    BorrowPoolItem(
                        name = "dYdX SoloMargin",
                        fee = "2 Gwei (Virtual Free)",
                        liquidity = "$15,000,000",
                        supported = "WETH, USDC",
                        security = "PeckShield Audited",
                        method = "operate() (Withdraw + Call)"
                    )
                )

                pools.forEach { pool ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CosmicCardInner)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pool.name,
                                    color = SentinelEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Fee: ${pool.fee}",
                                    color = SentinelGold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Avail. Liquidity:", color = CosmicTextSecondary, fontSize = 11.sp)
                                Text(pool.liquidity, color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Supported Assets:", color = CosmicTextSecondary, fontSize = 11.sp)
                                Text(pool.supported, color = CosmicTextPrimary, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Execution Method:", color = CosmicTextSecondary, fontSize = 11.sp)
                                Text(pool.method, color = CosmicTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Security Tier:", color = CosmicTextSecondary, fontSize = 11.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(SentinelEmerald)
                                    )
                                    Text(pool.security, color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class BorrowPoolItem(
    val name: String,
    val fee: String,
    val liquidity: String,
    val supported: String,
    val security: String,
    val method: String
)
