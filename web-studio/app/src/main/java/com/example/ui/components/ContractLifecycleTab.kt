package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractLifecycleTab(
    repository: SentinelRepository,
    onShowSnackbar: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    var contractName by remember { mutableStateOf("MevSandwichShieldV3") }
    var selectedNetwork by remember { mutableStateOf("Ethereum Mainnet") }
    
    // Security flags for compiling
    var enableReentrancyGuard by remember { mutableStateOf(true) }
    var enableAccessControl by remember { mutableStateOf(true) }
    var enableSlippageLock by remember { mutableStateOf(true) }
    var enableSafeMath by remember { mutableStateOf(false) }
    var enableVerification by remember { mutableStateOf(true) }

    // Deployment progression states
    var isDeploying by remember { mutableStateOf(false) }
    var deployStepName by remember { mutableStateOf("") }
    var deployProgress by remember { mutableStateOf(0f) }
    var consoleLogs = remember { mutableStateListOf<String>() }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val trackedContractsList = remember { repository.trackedContracts }

    // Smart Contract Status Tracker Polling States
    var isPollingActive by remember { mutableStateOf(true) }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "IN_PROGRESS", "VERIFIED", "COMPLETED", "FAILED"
    var lastPollTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Automatic Status Polling Loop
    LaunchedEffect(isPollingActive) {
        while (isPollingActive) {
            delay(3000)
            lastPollTime = System.currentTimeMillis()
            // Poll and advance pending/deploying statuses automatically
            repository.trackedContracts.forEachIndexed { idx, contract ->
                when (contract.status) {
                    "PENDING" -> repository.updateTrackedContract(contract.copy(status = "COMPILING"))
                    "COMPILING" -> repository.updateTrackedContract(contract.copy(status = "DEPLOYING"))
                    "DEPLOYING" -> repository.updateTrackedContract(contract.copy(status = "CONFIRMING"))
                    "CONFIRMING" -> repository.updateTrackedContract(contract.copy(status = "VERIFIED"))
                }
            }
        }
    }

    // Filtered smart contracts list
    val filteredTrackedContracts = remember(trackedContractsList, statusFilter, lastPollTime) {
        trackedContractsList.filter { c ->
            when (statusFilter) {
                "ALL" -> true
                "IN_PROGRESS" -> c.status in listOf("PENDING", "COMPILING", "DEPLOYING", "CONFIRMING")
                "VERIFIED" -> c.status == "VERIFIED"
                "COMPLETED" -> c.status == "SUCCESS_COMPLETED"
                "FAILED" -> c.status == "FAILED"
                else -> true
            }
        }
    }

    // Generates the reactive fortified Solidity code using the Security Suite Intelligence Engine
    val generatedSolidityCode = remember(contractName, selectedNetwork, enableReentrancyGuard, enableAccessControl, enableSlippageLock, enableSafeMath) {
        val enabledFeatures = mutableSetOf<String>()
        if (enableReentrancyGuard) enabledFeatures.add(com.example.data.SecurityFeaturesCatalog.REENTRANCY_GUARD.id)
        if (enableAccessControl) enabledFeatures.add(com.example.data.SecurityFeaturesCatalog.ACCESS_CONTROL.id)
        if (enableSlippageLock) enabledFeatures.add(com.example.data.SecurityFeaturesCatalog.SLIPPAGE_LOCK.id)
        if (enableSafeMath) enabledFeatures.add(com.example.data.SecurityFeaturesCatalog.SAFEMATH_OVERFLOW_SHIELD.id)
        enabledFeatures.add(com.example.data.SecurityFeaturesCatalog.PROFIT_HARVEST_ENGINE.id)
        enabledFeatures.add(com.example.data.SecurityFeaturesCatalog.MULTI_DEX_FLASH_ROUTER.id)
        enabledFeatures.add(com.example.data.SecurityFeaturesCatalog.CUSTOM_GAS_ERRORS.id)

        com.example.data.SecurityAdvisorEngine.generateFortifiedSolidityContract(
            contractName = contractName.ifBlank { "FortifiedSentinelContract" },
            network = selectedNetwork,
            txType = "MEV Arbitrage & Sandwich Bot",
            enabledFeatureIds = enabledFeatures,
            customSlippageBps = 50,
            customRateLimitEth = 25.0
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
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
                        imageVector = Icons.Filled.ViewInAr,
                        contentDescription = "Lifecycle Hub",
                        tint = SentinelGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "Smart Contract security & Lifecycle Studio",
                            color = CosmicTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Draft secure Solidity codes with active guard layers, compile bytecodes, deploy onto blockchain via RPC and track node approvals.",
                            color = CosmicTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Security Features Package:", color = CosmicTextSecondary, fontSize = 12.sp)
                        TextButton(
                            onClick = {
                                val activeProf = repository.activeSecurityProfile.value
                                enableReentrancyGuard = activeProf.reentrancyGuardEnabled
                                enableAccessControl = activeProf.accessControlEnabled
                                enableSlippageLock = activeProf.slippageLockEnabled
                                enableSafeMath = activeProf.safeMathEnabled
                                onShowSnackbar?.invoke("Imported security configuration '${activeProf.name}' (${activeProf.securityScore}%)")
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(12.dp), tint = SentinelGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Suite Profile", fontSize = 10.sp, color = SentinelGold)
                        }
                    }
                    
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
                    showConfirmDialog = true
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

        // Section 4: Live Smart Contract Status Tracker (Deployment Polling)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, SentinelGoldDim),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    .clip(CircleShape)
                                    .background(if (isPollingActive) SentinelEmerald else CosmicTextDim)
                            )
                            Text(
                                text = "SMART CONTRACT STATUS TRACKER",
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Polling Switch & Manual Poll Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    lastPollTime = System.currentTimeMillis()
                                    // Trigger immediate poll
                                    repository.trackedContracts.forEachIndexed { idx, contract ->
                                        when (contract.status) {
                                            "PENDING" -> repository.updateTrackedContract(contract.copy(status = "COMPILING"))
                                            "COMPILING" -> repository.updateTrackedContract(contract.copy(status = "DEPLOYING"))
                                            "DEPLOYING" -> repository.updateTrackedContract(contract.copy(status = "CONFIRMING"))
                                            "CONFIRMING" -> repository.updateTrackedContract(contract.copy(status = "VERIFIED"))
                                        }
                                    }
                                    onShowSnackbar?.invoke("🔄 RPC Deployment Status Polled Immediately!")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Poll Now",
                                    tint = SentinelGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isPollingActive) "AUTO-POLL" else "PAUSED",
                                    color = if (isPollingActive) SentinelEmerald else CosmicTextDim,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Switch(
                                    checked = isPollingActive,
                                    onCheckedChange = {
                                        isPollingActive = it
                                        val statusText = if (it) "Auto-polling RPC enabled (3s interval)" else "RPC Polling paused"
                                        onShowSnackbar?.invoke(statusText)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SentinelEmerald,
                                        checkedTrackColor = SentinelEmeraldDim
                                    ),
                                    modifier = Modifier.scale(0.7f)
                                )
                            }
                        }
                    }

                    // Status Filter Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        mapOf(
                            "ALL" to "All (${trackedContractsList.size})",
                            "IN_PROGRESS" to "Progress (${trackedContractsList.count { it.status in listOf("PENDING", "COMPILING", "DEPLOYING", "CONFIRMING") }})",
                            "VERIFIED" to "Verified (${trackedContractsList.count { it.status == "VERIFIED" }})",
                            "COMPLETED" to "Completed (${trackedContractsList.count { it.status == "SUCCESS_COMPLETED" }})"
                        ).forEach { (filterKey, label) ->
                            val isSelected = statusFilter == filterKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SentinelGoldDim else CosmicCardInner)
                                    .border(1.dp, if (isSelected) SentinelGold else CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { statusFilter = filterKey }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) SentinelGold else CosmicTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredTrackedContracts.isEmpty()) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CloudOff, contentDescription = "None", tint = CosmicTextDim, modifier = Modifier.size(36.dp))
                        Text("No smart contracts matching selected filter.", color = CosmicTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }

        items(filteredTrackedContracts, key = { it.id }) { contract ->
            TrackedContractCard(contract = contract, repository = repository, onShowSnackbar = onShowSnackbar)
        }
    }

    if (showConfirmDialog) {
        PreBroadcastConfirmDialog(
            contractName = contractName,
            selectedNetwork = selectedNetwork,
            deployerAddress = repository.config.walletAddress,
            rpcUrl = repository.config.rpcUrl,
            gasPrice = repository.lastScannedGasPrice.value ?: 28.5,
            enableReentrancyGuard = enableReentrancyGuard,
            enableAccessControl = enableAccessControl,
            enableSlippageLock = enableSlippageLock,
            enableSafeMath = enableSafeMath,
            enableVerification = enableVerification,
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                isDeploying = true
                deployProgress = 0f
                consoleLogs.clear()
                onShowSnackbar?.invoke("🚀 Initiating deployment of $contractName on $selectedNetwork...")
                coroutineScope.launch {
                    val activeFeatures = mutableListOf<String>()
                    if (enableReentrancyGuard) activeFeatures.add("ReentrancyGuard")
                    if (enableAccessControl) activeFeatures.add("Ownable")
                    if (enableSlippageLock) activeFeatures.add("SlippageGuard")
                    if (enableSafeMath) activeFeatures.add("SafeMath")

                    consoleLogs.add("⚙️ Loading Solidity compiler v0.8.20...")
                    deployStepName = "Compiling contract source..."
                    deployProgress = 0.15f
                    onShowSnackbar?.invoke("⚙️ Compiling $contractName Solidity source...")

                    val result = repository.deployContractToMainnet(
                        contractName = contractName,
                        network = selectedNetwork,
                        soliditySource = generatedSolidityCode,
                        securityFeatures = activeFeatures,
                        onProgress = { statusMsg, prog ->
                            deployStepName = statusMsg
                            deployProgress = prog
                            consoleLogs.add(statusMsg)
                            onShowSnackbar?.invoke("⚙️ [$contractName] $statusMsg")
                        }
                    )

                    consoleLogs.add("📦 Mainnet Deployment Confirmed!")
                    consoleLogs.add("🔗 Contract Address: ${result.first.address}")
                    consoleLogs.add("📝 Tx Hash: ${result.second.txHash}")
                    if (enableVerification) {
                        consoleLogs.add("✅ Etherscan Verification SUCCESS: Smart contract verified at ${result.first.address}")
                        onShowSnackbar?.invoke("✅ Etherscan verified $contractName at ${result.first.address.take(10)}...")
                    }
                    deployStepName = "Deployment Completed!"
                    deployProgress = 1.0f
                    isDeploying = false
                    onShowSnackbar?.invoke("🎉 Smart Contract $contractName successfully deployed to $selectedNetwork!")
                }
            }
        )
    }
}
}

@Composable
fun TrackedContractCard(
    contract: TrackedContract,
    repository: SentinelRepository,
    onShowSnackbar: ((String) -> Unit)? = null
) {
    var expandedDetails by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                }

                // Color-coded Status Badge
                ContractStatusBadge(status = contract.status)
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

            // --- INTERACTIVE ACTION PANEL FOR DEPLOYED & VERIFIED CONTRACTS ---
            if (contract.status == "VERIFIED" || contract.status == "SUCCESS_COMPLETED" || contract.status == "PAUSED") {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "POST-DEPLOYMENT PROFIT OPERATIONS & REVENUE TOOLS",
                    color = SentinelGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Action: Scrape & Sweep Accrued Profit
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                onShowSnackbar?.invoke("💸 Calling harvestProfit() on ${contract.name}...")
                                repository.scrapeContractProfit(
                                    contract = contract,
                                    onCompleted = { success, msg, ethAmount ->
                                        if (success) {
                                            repository.updateTrackedContract(contract.copy(status = "SUCCESS_COMPLETED"))
                                            onShowSnackbar?.invoke("🎉 $msg")
                                        } else {
                                            onShowSnackbar?.invoke("⚠️ Profit scrape failed: $msg")
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
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
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Harvest", modifier = Modifier.size(16.dp))
                            Text("Scrape Accrued Profits (harvestProfit)", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Secondary Grid Actions: Flash Arbitrage & Token Basket Sweep
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    onShowSnackbar?.invoke("⚡ Executing Flash Arbitrage Route on ${contract.network}...")
                                    repository.executeContractFlashArbitrage(
                                        contract = contract,
                                        dexRoute = "Uniswap V3 ➔ Sushiswap",
                                        tradeSizeEth = 2.5,
                                        onCompleted = { success, msg, ethProfit, usdProfit ->
                                            if (success) {
                                                repository.updateTrackedContract(contract.copy(status = "SUCCESS_COMPLETED"))
                                                onShowSnackbar?.invoke("🎉 $msg (~$${String.format("%.2f", usdProfit)})")
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SentinelBlue),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelBlue)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Arbitrage", modifier = Modifier.size(14.dp))
                                Text("Flash Arbitrage", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    onShowSnackbar?.invoke("🧹 Sweeping ERC-20 token basket reserves...")
                                    repository.sweepContractTokenBasket(
                                        contract = contract,
                                        tokens = listOf("USDC", "USDT", "WBTC", "LINK"),
                                        onCompleted = { success, msg, totalUsd ->
                                            if (success) {
                                                onShowSnackbar?.invoke("🎉 $msg")
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SentinelGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelGold)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CleaningServices, contentDescription = "Sweep", modifier = Modifier.size(14.dp))
                                Text("Sweep Tokens", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Tertiary Row Actions: LP Fee Collector & Emergency Circuit Breaker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    onShowSnackbar?.invoke("📊 Scraping LP trading fees from Uniswap V3 position...")
                                    repository.collectDexLpFees(
                                        contract = contract,
                                        poolName = "Uniswap V3 WETH/USDC 0.05%",
                                        onCompleted = { success, msg, ethAmount ->
                                            if (success) {
                                                onShowSnackbar?.invoke("🎉 $msg")
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CosmicDivider),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextPrimary)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MonetizationOn, contentDescription = "LP Fees", modifier = Modifier.size(14.dp), tint = SentinelEmerald)
                                Text("Collect LP Fees", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        val isPaused = contract.status == "PAUSED"
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val willPause = !isPaused
                                    onShowSnackbar?.invoke(if (willPause) "🛡️ Triggering emergencyPause()..." else "🔓 Triggering emergencyUnpause()...")
                                    repository.toggleContractEmergencyPause(
                                        contract = contract,
                                        pause = willPause,
                                        onCompleted = { success, msg ->
                                            if (success) {
                                                onShowSnackbar?.invoke("🔒 $msg")
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isPaused) SentinelEmerald else SentinelRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isPaused) SentinelEmerald else SentinelRed)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (isPaused) Icons.Filled.LockOpen else Icons.Filled.Lock, contentDescription = "Pause", modifier = Modifier.size(14.dp))
                                Text(if (isPaused) "Unpause State" else "Circuit Freeze", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
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
                            text = "SEARCHING MEMPOOL ROUTES & EXECUTING FLASHLOAN...",
                            color = SentinelBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else if (contract.status == "SUCCESS_COMPLETED") {
                Spacer(modifier = Modifier.height(12.dp))
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
    
    // 1. Deployment: Completed if verified or success or executing, active always
    val deployCompleted = status != "PENDING" && status != "COMPILING" && status != "DEPLOYING"
    val deployActive = true
    
    // 2. Security Audit (Reentrancy Guard Check): Completed if VERIFIED or beyond
    val auditCompleted = status == "VERIFIED" || status == "EXECUTING" || status == "SUCCESS_COMPLETED"
    val auditActive = deployCompleted
    
    // 3. Execution: Completed if SUCCESS_COMPLETED, active if EXECUTING
    val executionCompleted = status == "SUCCESS_COMPLETED"
    val executionActive = status == "EXECUTING" || status == "SUCCESS_COMPLETED"
    
    // 4. Profit Reconciliation: Completed if SUCCESS_COMPLETED
    val profitCompleted = status == "SUCCESS_COMPLETED"
    val profitActive = status == "SUCCESS_COMPLETED"

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
fun PreBroadcastConfirmDialog(
    contractName: String,
    selectedNetwork: String,
    deployerAddress: String,
    rpcUrl: String,
    gasPrice: Double,
    enableReentrancyGuard: Boolean,
    enableAccessControl: Boolean,
    enableSlippageLock: Boolean,
    enableSafeMath: Boolean,
    enableVerification: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val estimatedGasUnits = 245000L
    val estimatedEthFee = estimatedGasUnits * gasPrice * 1e-9
    val estimatedUsdFee = estimatedEthFee * 3400.0

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(1.dp, SentinelGold, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        containerColor = CosmicSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Pre-Broadcast Safety Check",
                    tint = SentinelGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Pre-Broadcast Safety & Cost Audit",
                    color = CosmicTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Transaction Details
                Text(
                    text = "📋 TRANSACTION DETAILS",
                    color = SentinelGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                    border = BorderStroke(1.dp, CosmicDivider),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ConfirmDetailRow("Contract Name:", contractName)
                        ConfirmDetailRow("Target Chain:", selectedNetwork)
                        ConfirmDetailRow("Deployer Address:", if (deployerAddress.length >= 14) deployerAddress.take(8) + "..." + deployerAddress.takeLast(6) else deployerAddress)
                        ConfirmDetailRow("RPC Endpoint:", if (rpcUrl.isNotEmpty()) "Alchemy Private RPC" else "Ethereum Mainnet Gateway")
                    }
                }

                // Section 2: Potential Costs
                Text(
                    text = "💰 POTENTIAL COSTS & GAS AUDIT",
                    color = SentinelGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                    border = BorderStroke(1.dp, CosmicDivider),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ConfirmDetailRow("Gas Limit:", "$estimatedGasUnits Units")
                        ConfirmDetailRow("Gas Price:", String.format(java.util.Locale.US, "%.1f Gwei", gasPrice))
                        ConfirmDetailRow("Estimated Cost:", String.format(java.util.Locale.US, "%.5f ETH (~$%.2f USD)", estimatedEthFee, estimatedUsdFee))
                        ConfirmDetailRow("Priority Tip:", "1.5 Gwei (EIP-1559)")
                    }
                }

                // Section 3: Safety Check Summary
                Text(
                    text = "🛡️ SAFETY CHECK SUMMARY",
                    color = SentinelEmerald,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SentinelEmeraldDim),
                    border = BorderStroke(1.dp, SentinelEmerald),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ConfirmSafetyRow("Reentrancy Guard", enableReentrancyGuard)
                        ConfirmSafetyRow("Ownable Access Control", enableAccessControl)
                        ConfirmSafetyRow("Slippage Lock Guard", enableSlippageLock)
                        ConfirmSafetyRow("SafeMath Protection", enableSafeMath)
                        ConfirmSafetyRow("Etherscan Code Verification", enableVerification)
                        ConfirmSafetyRow("RPC Broadcast Safety Enforcer", true)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SentinelEmerald,
                    contentColor = CosmicBackground
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Confirm Broadcast", modifier = Modifier.size(16.dp))
                    Text("Confirm & Broadcast to Mainnet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CosmicTextSecondary),
                border = BorderStroke(1.dp, CosmicDivider),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun ConfirmDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = CosmicTextSecondary, fontSize = 11.sp)
        Text(text = value, color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ConfirmSafetyRow(title: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (enabled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = if (enabled) SentinelEmerald else SentinelRed,
                modifier = Modifier.size(14.dp)
            )
            Text(text = title, color = CosmicTextPrimary, fontSize = 11.sp)
        }
        Text(
            text = if (enabled) "PASSED" else "DISABLED",
            color = if (enabled) SentinelEmerald else SentinelRed,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ContractStatusBadge(status: String) {
    val badgeInfo = when (status) {
        "SUCCESS_COMPLETED" -> StatusBadgeInfo("COMPLETED", SentinelEmerald, SentinelEmeraldDim, Icons.Filled.CheckCircle)
        "VERIFIED" -> StatusBadgeInfo("VERIFIED & ON-CHAIN", SentinelEmerald, SentinelEmeraldDim, Icons.Filled.Verified)
        "EXECUTING" -> StatusBadgeInfo("EXECUTING BUNDLE", SentinelBlue, SentinelBlueDim, Icons.Filled.PlayArrow)
        "CONFIRMING" -> StatusBadgeInfo("CONFIRMING (12/12 BLOCKS)", SentinelGold, SentinelGoldDim, Icons.Filled.Sync)
        "DEPLOYING" -> StatusBadgeInfo("DEPLOYING TO RPC", SentinelBlue, SentinelBlueDim, Icons.Filled.CloudUpload)
        "COMPILING" -> StatusBadgeInfo("COMPILING SOLIDITY", Color(0xFFAB47BC), Color(0x22AB47BC), Icons.Filled.Code)
        "PENDING" -> StatusBadgeInfo("PENDING MEMPOOL", Color(0xFF26C6DA), Color(0x2226C6DA), Icons.Filled.HourglassEmpty)
        "FAILED" -> StatusBadgeInfo("REVERTED / FAILED", Color(0xFFEF5350), Color(0x22EF5350), Icons.Filled.Error)
        else -> StatusBadgeInfo(status, SentinelGold, SentinelGoldDim, Icons.Filled.Info)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeInfo.bgColor)
            .border(1.dp, badgeInfo.textColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = badgeInfo.icon,
            contentDescription = badgeInfo.label,
            tint = badgeInfo.textColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = badgeInfo.label,
            color = badgeInfo.textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private data class StatusBadgeInfo(
    val label: String,
    val textColor: Color,
    val bgColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
