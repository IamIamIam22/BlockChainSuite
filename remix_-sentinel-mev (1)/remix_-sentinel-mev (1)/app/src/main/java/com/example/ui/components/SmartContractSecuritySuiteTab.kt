package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class SecuritySuiteSubTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FEATURES("Features Catalog", Icons.Filled.Shield),
    ADVISOR("Threat Advisor", Icons.Filled.Psychology),
    SOLIDITY("Hardened Solidity", Icons.Filled.Code),
    SAVED_PROFILES("Stored Vault", Icons.Filled.FolderSpecial)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartContractSecuritySuiteTab(
    repository: SentinelRepository,
    onShowSnackbar: (String) -> Unit = {},
    onDeployWithConfig: (contractName: String, network: String, source: String, features: List<String>) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var subTab by remember { mutableStateOf(SecuritySuiteSubTab.FEATURES) }

    // Active working state
    var selectedNetwork by remember { mutableStateOf("Ethereum Mainnet") }
    var selectedTxType by remember { mutableStateOf("MEV Arbitrage & Sandwich Bot") }
    var enabledFeatureIds by remember {
        mutableStateOf(
            setOf(
                SecurityFeaturesCatalog.REENTRANCY_GUARD.id,
                SecurityFeaturesCatalog.ACCESS_CONTROL.id,
                SecurityFeaturesCatalog.SLIPPAGE_LOCK.id,
                SecurityFeaturesCatalog.PAUSABLE_CIRCUIT_BREAKER.id,
                SecurityFeaturesCatalog.SAFEMATH_OVERFLOW_SHIELD.id
            )
        )
    }

    var customSlippageBps by remember { mutableStateOf(50) } // 0.5%
    var customRateLimitEth by remember { mutableStateOf(10.0) }
    var selectedCategoryFilter by remember { mutableStateOf<SecurityCategory?>(null) }
    var expandedFeatureId by remember { mutableStateOf<String?>(SecurityFeaturesCatalog.REENTRANCY_GUARD.id) }

    // Dialog States
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveProfileName by remember { mutableStateOf("") }
    var saveProfileNotes by remember { mutableStateOf("") }
    var showPresetSheet by remember { mutableStateOf(false) }

    // Dynamic calculations from Advisor Engine
    val recommendedFeatureIds = remember(selectedNetwork, selectedTxType) {
        SecurityAdvisorEngine.getRecommendedFeatures(selectedNetwork, selectedTxType)
    }

    val threats = remember(selectedNetwork, selectedTxType, enabledFeatureIds) {
        SecurityAdvisorEngine.getThreatsForConfiguration(selectedNetwork, selectedTxType, enabledFeatureIds)
    }

    val securityScore = remember(selectedNetwork, selectedTxType, enabledFeatureIds) {
        SecurityAdvisorEngine.calculateSecurityScore(selectedNetwork, selectedTxType, enabledFeatureIds)
    }

    val generatedContractSource = remember(selectedNetwork, selectedTxType, enabledFeatureIds, customSlippageBps, customRateLimitEth) {
        SecurityAdvisorEngine.generateFortifiedSolidityContract(
            contractName = "SentinelProtected${selectedTxType.split(" ").firstOrNull() ?: "Contract"}",
            network = selectedNetwork,
            txType = selectedTxType,
            enabledFeatureIds = enabledFeatureIds,
            customSlippageBps = customSlippageBps,
            customRateLimitEth = customRateLimitEth
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
    ) {
        // --- 1. Top Header Banner & Security Score Dashboard ---
        SecuritySuiteHeader(
            securityScore = securityScore,
            selectedNetwork = selectedNetwork,
            selectedTxType = selectedTxType,
            onSelectNetwork = { selectedNetwork = it },
            onSelectTxType = { selectedTxType = it },
            onApplyRecommendations = {
                enabledFeatureIds = recommendedFeatureIds
                onShowSnackbar("Applied ${recommendedFeatureIds.size} recommended features for $selectedNetwork ($selectedTxType)")
            },
            onOpenPresets = { showPresetSheet = true },
            onOpenSaveDialog = {
                saveProfileName = "$selectedNetwork ${selectedTxType.take(15)} Armor"
                saveProfileNotes = "Fortified security profile tailored for $selectedNetwork"
                showSaveDialog = true
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // --- 2. Sub-Tab Navigation Bar ---
        ScrollableTabRow(
            selectedTabIndex = subTab.ordinal,
            containerColor = CosmicSurface,
            contentColor = SentinelGold,
            edgePadding = 12.dp,
            divider = { HorizontalDivider(color = CosmicDivider) },
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subTab.ordinal]),
                    color = SentinelGold,
                    height = 3.dp
                )
            }
        ) {
            SecuritySuiteSubTab.values().forEach { tab ->
                Tab(
                    selected = subTab == tab,
                    onClick = { subTab = tab },
                    icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(16.dp)) },
                    text = {
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (subTab == tab) FontWeight.Bold else FontWeight.Normal,
                            color = if (subTab == tab) SentinelGold else CosmicTextSecondary
                        )
                    }
                )
            }
        }

        // --- 3. Sub-Tab Content View ---
        Box(modifier = Modifier.weight(1f)) {
            when (subTab) {
                SecuritySuiteSubTab.FEATURES -> {
                    FeaturesCatalogView(
                        categoryFilter = selectedCategoryFilter,
                        onSelectCategoryFilter = { selectedCategoryFilter = it },
                        enabledFeatureIds = enabledFeatureIds,
                        recommendedFeatureIds = recommendedFeatureIds,
                        expandedFeatureId = expandedFeatureId,
                        onToggleFeature = { featId, isChecked ->
                            enabledFeatureIds = if (isChecked) {
                                enabledFeatureIds + featId
                            } else {
                                enabledFeatureIds - featId
                            }
                        },
                        onExpandFeature = { featId ->
                            expandedFeatureId = if (expandedFeatureId == featId) null else featId
                        },
                        onCopySnippet = { snippet, featName ->
                            clipboardManager.setText(AnnotatedString(snippet))
                            onShowSnackbar("Copied $featName Solidity implementation to clipboard")
                        },
                        customSlippageBps = customSlippageBps,
                        onUpdateSlippage = { customSlippageBps = it },
                        customRateLimitEth = customRateLimitEth,
                        onUpdateRateLimit = { customRateLimitEth = it }
                    )
                }
                SecuritySuiteSubTab.ADVISOR -> {
                    ThreatAdvisorView(
                        threats = threats,
                        securityScore = securityScore,
                        selectedNetwork = selectedNetwork,
                        selectedTxType = selectedTxType,
                        enabledFeatureIds = enabledFeatureIds,
                        onEnableFeature = { featId ->
                            enabledFeatureIds = enabledFeatureIds + featId
                            onShowSnackbar("Enabled security feature: $featId")
                        },
                        onApplyAll = {
                            enabledFeatureIds = recommendedFeatureIds
                            onShowSnackbar("Fortified all critical threat vectors!")
                        }
                    )
                }
                SecuritySuiteSubTab.SOLIDITY -> {
                    HardenedSolidityView(
                        contractSource = generatedContractSource,
                        selectedNetwork = selectedNetwork,
                        selectedTxType = selectedTxType,
                        enabledCount = enabledFeatureIds.size,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(generatedContractSource))
                            onShowSnackbar("Copied hardened Solidity contract source!")
                        },
                        onDeploy = {
                            val contractName = "Sentinel${selectedTxType.replace(" ", "").take(12)}Shield"
                            onDeployWithConfig(
                                contractName,
                                selectedNetwork,
                                generatedContractSource,
                                enabledFeatureIds.toList()
                            )
                            onShowSnackbar("Loaded $contractName into Contract Lifecycle for deployment!")
                        }
                    )
                }
                SecuritySuiteSubTab.SAVED_PROFILES -> {
                    SavedProfilesVaultView(
                        repository = repository,
                        activeEnabledIds = enabledFeatureIds,
                        onLoadProfile = { profile ->
                            selectedNetwork = profile.targetNetwork
                            selectedTxType = profile.transactionType
                            enabledFeatureIds = profile.enabledFeatureIds
                            customSlippageBps = profile.customMaxSlippageBps
                            customRateLimitEth = profile.customMaxRateLimitEth
                            repository.setActiveSecurityProfile(profile)
                            onShowSnackbar("Loaded profile: '${profile.name}'")
                            subTab = SecuritySuiteSubTab.FEATURES
                        },
                        onDeleteProfile = { profileId, profileName ->
                            repository.deleteSecurityConfiguration(profileId)
                            onShowSnackbar("Deleted profile '$profileName'")
                        },
                        onCopyProfileJson = { jsonStr ->
                            clipboardManager.setText(AnnotatedString(jsonStr))
                            onShowSnackbar("Exported configuration JSON to clipboard")
                        }
                    )
                }
            }
        }
    }

    // --- Save Profile Dialog ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = CosmicSurface,
            titleContentColor = CosmicTextPrimary,
            textContentColor = CosmicTextSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null, tint = SentinelGold)
                    Text("Store Security Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Save the current feature matrix, slippage tolerances, and network settings into the local Room database.",
                        fontSize = 12.sp,
                        color = CosmicTextSecondary
                    )

                    OutlinedTextField(
                        value = saveProfileName,
                        onValueChange = { saveProfileName = it },
                        label = { Text("Profile Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = saveProfileNotes,
                        onValueChange = { saveProfileNotes = it },
                        label = { Text("Security Notes & Context") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Score: $securityScore%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SentinelEmerald)
                        Text("${enabledFeatureIds.size} Features Enabled", fontSize = 12.sp, color = SentinelGold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (saveProfileName.isNotBlank()) {
                            val newProfile = SecurityProfile(
                                name = saveProfileName.trim(),
                                targetNetwork = selectedNetwork,
                                transactionType = selectedTxType,
                                enabledFeatureIds = enabledFeatureIds,
                                reentrancyGuardEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.REENTRANCY_GUARD.id),
                                accessControlEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.ACCESS_CONTROL.id),
                                slippageLockEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.SLIPPAGE_LOCK.id),
                                safeMathEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.SAFEMATH_OVERFLOW_SHIELD.id),
                                pausableEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.PAUSABLE_CIRCUIT_BREAKER.id),
                                eip712Enabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.EIP712_SIGNATURES.id),
                                oracleTwapGuardEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.ORACLE_TWAP_GUARD.id),
                                erc4337PaymasterEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.ERC4337_PAYMASTER_VALIDATOR.id),
                                antiFrontrunningEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.ANTI_FRONTRUNNING_COMMIT_REVEAL.id),
                                rateLimiterEnabled = enabledFeatureIds.contains(SecurityFeaturesCatalog.RATE_LIMITER_THROTTLE.id),
                                customMaxSlippageBps = customSlippageBps,
                                customMaxRateLimitEth = customRateLimitEth,
                                securityScore = securityScore,
                                notes = saveProfileNotes.trim()
                            )
                            repository.saveSecurityConfiguration(newProfile)
                            showSaveDialog = false
                            onShowSnackbar("Configuration '${newProfile.name}' stored successfully in Room DB")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                ) {
                    Text("Save to Vault", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = CosmicTextSecondary)
                }
            }
        )
    }

    // --- Preset Selection Modal Sheet ---
    if (showPresetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPresetSheet = false },
            containerColor = CosmicSurface,
            contentColor = CosmicTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = SentinelGold)
                    Text("Select Security Preset Template", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                repository.securityConfigurations.forEach { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedNetwork = profile.targetNetwork
                                selectedTxType = profile.transactionType
                                enabledFeatureIds = profile.enabledFeatureIds
                                customSlippageBps = profile.customMaxSlippageBps
                                customRateLimitEth = profile.customMaxRateLimitEth
                                showPresetSheet = false
                                onShowSnackbar("Loaded preset '${profile.name}'")
                            },
                        colors = CardDefaults.cardColors(containerColor = CosmicCardInner),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDivider)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CosmicTextPrimary)
                                Text("${profile.targetNetwork} • ${profile.transactionType}", fontSize = 11.sp, color = CosmicTextSecondary)
                                if (profile.notes.isNotBlank()) {
                                    Text(profile.notes, fontSize = 10.sp, color = CosmicTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${profile.securityScore}%", fontWeight = FontWeight.Bold, color = SentinelEmerald, fontSize = 14.sp)
                                Text("${profile.enabledFeatureIds.size} modules", fontSize = 10.sp, color = SentinelGold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- Component 1: Header Dashboard ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecuritySuiteHeader(
    securityScore: Int,
    selectedNetwork: String,
    selectedTxType: String,
    onSelectNetwork: (String) -> Unit,
    onSelectTxType: (String) -> Unit,
    onApplyRecommendations: () -> Unit,
    onOpenPresets: () -> Unit,
    onOpenSaveDialog: () -> Unit
) {
    var networkExpanded by remember { mutableStateOf(false) }
    var txTypeExpanded by remember { mutableStateOf(false) }

    val scoreColor = when {
        securityScore >= 90 -> SentinelEmerald
        securityScore >= 75 -> SentinelGold
        securityScore >= 50 -> Color(0xFFFF7043)
        else -> Color(0xFFEF5350)
    }

    val scoreLabel = when {
        securityScore >= 90 -> "FORTIFIED • AUDIT READY"
        securityScore >= 75 -> "HIGHLY PROTECTED"
        securityScore >= 50 -> "MODERATE DEFENSE"
        else -> "VULNERABLE • ACTION NEEDED"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDivider)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Quick Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.linearGradient(listOf(SentinelGold, Color(0xFFAB47BC)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.GppGood, contentDescription = null, tint = CosmicBackground, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text("SECURITY SUITE", fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = CosmicTextPrimary)
                        Text("Smart Contract Vulnerability Shield", fontSize = 10.sp, color = CosmicTextSecondary)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = onOpenPresets,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = CosmicCardInner)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "Presets", tint = SentinelGold, modifier = Modifier.size(16.dp))
                    }
                    FilledTonalIconButton(
                        onClick = onOpenSaveDialog,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = CosmicCardInner)
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "Save", tint = SentinelEmerald, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Meter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CosmicCardInner)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("SECURITY RATING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CosmicTextSecondary)
                    Text(scoreLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$securityScore", fontSize = 24.sp, fontWeight = FontWeight.Black, color = scoreColor, fontFamily = FontFamily.Monospace)
                    Text("/100", fontSize = 12.sp, color = CosmicTextSecondary, modifier = Modifier.padding(bottom = 3.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selectors for Target Network & Transaction Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Network Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(
                        onClick = { networkExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = CosmicCardInner),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDivider)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NETWORK", fontSize = 8.sp, color = CosmicTextSecondary, fontWeight = FontWeight.Bold)
                                Text(selectedNetwork, fontSize = 11.sp, color = CosmicTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = SentinelGold, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = networkExpanded,
                        onDismissRequest = { networkExpanded = false },
                        modifier = Modifier.background(CosmicSurface)
                    ) {
                        SecurityFeaturesCatalog.allNetworks.forEach { net ->
                            DropdownMenuItem(
                                text = { Text(net, color = if (net == selectedNetwork) SentinelGold else CosmicTextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    onSelectNetwork(net)
                                    networkExpanded = false
                                }
                            )
                        }
                    }
                }

                // Transaction Type Dropdown
                Box(modifier = Modifier.weight(1.3f)) {
                    OutlinedCard(
                        onClick = { txTypeExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = CosmicCardInner),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDivider)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TRANSACTION TYPE", fontSize = 8.sp, color = CosmicTextSecondary, fontWeight = FontWeight.Bold)
                                Text(selectedTxType, fontSize = 11.sp, color = CosmicTextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = SentinelGold, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = txTypeExpanded,
                        onDismissRequest = { txTypeExpanded = false },
                        modifier = Modifier.background(CosmicSurface)
                    ) {
                        SecurityFeaturesCatalog.allTransactionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = if (type == selectedTxType) SentinelGold else CosmicTextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    onSelectTxType(type)
                                    txTypeExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // One-tap Auto Recommend Button
            Button(
                onClick = onApplyRecommendations,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SentinelGoldDim,
                    contentColor = SentinelGold
                ),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Auto-Apply Recommended Suite for $selectedNetwork", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Component 2: Features Catalog View ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturesCatalogView(
    categoryFilter: SecurityCategory?,
    onSelectCategoryFilter: (SecurityCategory?) -> Unit,
    enabledFeatureIds: Set<String>,
    recommendedFeatureIds: Set<String>,
    expandedFeatureId: String?,
    onToggleFeature: (featId: String, isChecked: Boolean) -> Unit,
    onExpandFeature: (featId: String) -> Unit,
    onCopySnippet: (snippet: String, name: String) -> Unit,
    customSlippageBps: Int,
    onUpdateSlippage: (Int) -> Unit,
    customRateLimitEth: Double,
    onUpdateRateLimit: (Double) -> Unit
) {
    val filteredFeatures = remember(categoryFilter) {
        if (categoryFilter == null) SecurityFeaturesCatalog.allFeatures
        else SecurityFeaturesCatalog.allFeatures.filter { it.category == categoryFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoryFilter == null,
                        onClick = { onSelectCategoryFilter(null) },
                        label = { Text("All Features (${SecurityFeaturesCatalog.allFeatures.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SentinelGold,
                            selectedLabelColor = CosmicBackground,
                            containerColor = CosmicSurface,
                            labelColor = CosmicTextSecondary
                        )
                    )
                }
                items(SecurityCategory.values()) { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { onSelectCategoryFilter(if (categoryFilter == cat) null else cat) },
                        label = { Text(cat.title, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(cat.badgeColorHex),
                            selectedLabelColor = CosmicBackground,
                            containerColor = CosmicSurface,
                            labelColor = CosmicTextSecondary
                        )
                    )
                }
            }
        }

        // Features List
        items(filteredFeatures, key = { it.id }) { feat ->
            val isEnabled = enabledFeatureIds.contains(feat.id)
            val isRecommended = recommendedFeatureIds.contains(feat.id)
            val isExpanded = expandedFeatureId == feat.id

            FeatureItemCard(
                feature = feat,
                isEnabled = isEnabled,
                isRecommended = isRecommended,
                isExpanded = isExpanded,
                onToggle = { isChecked -> onToggleFeature(feat.id, isChecked) },
                onExpand = { onExpandFeature(feat.id) },
                onCopy = { onCopySnippet(feat.soliditySnippet, feat.name) },
                customSlippageBps = customSlippageBps,
                onUpdateSlippage = onUpdateSlippage,
                customRateLimitEth = customRateLimitEth,
                onUpdateRateLimit = onUpdateRateLimit
            )
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun FeatureItemCard(
    feature: SecurityFeature,
    isEnabled: Boolean,
    isRecommended: Boolean,
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onExpand: () -> Unit,
    onCopy: () -> Unit,
    customSlippageBps: Int,
    onUpdateSlippage: (Int) -> Unit,
    customRateLimitEth: Double,
    onUpdateRateLimit: (Double) -> Unit
) {
    val borderColor = when {
        isEnabled -> SentinelGold.copy(alpha = 0.6f)
        isRecommended -> Color(feature.category.badgeColorHex).copy(alpha = 0.4f)
        else -> CosmicDivider
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onExpand() },
        colors = CardDefaults.cardColors(containerColor = if (isEnabled) CosmicSurface else CosmicBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Category Badge + Recommendation Tag + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(feature.category.badgeColorHex).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = feature.category.title,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(feature.category.badgeColorHex)
                        )
                    }

                    if (isRecommended) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SentinelEmeraldDim)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("RECOMMENDED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SentinelEmerald)
                        }
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CosmicBackground,
                        checkedTrackColor = SentinelGold,
                        uncheckedThumbColor = CosmicTextSecondary,
                        uncheckedTrackColor = CosmicCardInner
                    ),
                    modifier = Modifier.height(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Feature Name & Tagline
            Text(
                text = feature.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isEnabled) SentinelGold else CosmicTextPrimary
            )

            Text(
                text = feature.tagline,
                fontSize = 11.sp,
                color = CosmicTextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Threat tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                feature.threatVectorsPrevented.take(2).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CosmicCardInner)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "🛡 $tag",
                            fontSize = 9.sp,
                            color = CosmicTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Expandable Content
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = CosmicDivider)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. What it does (Description)
                    Text("WHAT IT DOES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SentinelGold)
                    Text(
                        text = feature.description,
                        fontSize = 11.sp,
                        color = CosmicTextPrimary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    // 2. How to use (Guide)
                    Text("HOW TO USE IN SOLIDITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SentinelEmerald)
                    Text(
                        text = feature.howToUse,
                        fontSize = 11.sp,
                        color = CosmicTextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    // Gas overhead
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardInner)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gas Overhead:", fontSize = 10.sp, color = CosmicTextSecondary)
                        Text(feature.gasOverhead, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SentinelGold)
                    }

                    // Special Config Parameters if Applicable
                    if (feature.id == SecurityFeaturesCatalog.SLIPPAGE_LOCK.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Max Allowed Slippage Tolerance: ${customSlippageBps / 100.0}% (${customSlippageBps} BPS)", fontSize = 10.sp, color = SentinelGold)
                        Slider(
                            value = customSlippageBps.toFloat(),
                            onValueChange = { onUpdateSlippage(it.toInt()) },
                            valueRange = 10f..200f,
                            steps = 18,
                            colors = SliderDefaults.colors(thumbColor = SentinelGold, activeTrackColor = SentinelGold)
                        )
                    } else if (feature.id == SecurityFeaturesCatalog.RATE_LIMITER_THROTTLE.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Max Outflow Per 1-Hour Epoch: ${customRateLimitEth.toInt()} ETH", fontSize = 10.sp, color = SentinelGold)
                        Slider(
                            value = customRateLimitEth.toFloat(),
                            onValueChange = { onUpdateRateLimit(it.toDouble()) },
                            valueRange = 1f..100f,
                            steps = 98,
                            colors = SliderDefaults.colors(thumbColor = SentinelGold, activeTrackColor = SentinelGold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Solidity Code Snippet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SOLIDITY IMPLEMENTATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CosmicTextSecondary)
                        TextButton(
                            onClick = onCopy,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp), tint = SentinelGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code", fontSize = 10.sp, color = SentinelGold)
                        }
                    }

                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D0C12))
                                .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = feature.soliditySnippet,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFC3E88D),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Component 3: Threat Advisor View ---
@Composable
private fun ThreatAdvisorView(
    threats: List<ThreatAssessment>,
    securityScore: Int,
    selectedNetwork: String,
    selectedTxType: String,
    enabledFeatureIds: Set<String>,
    onEnableFeature: (String) -> Unit,
    onApplyAll: () -> Unit
) {
    val unmitigatedCount = threats.count { !it.isMitigated }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDivider)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.CrisisAlert, contentDescription = null, tint = if (unmitigatedCount > 0) Color(0xFFFF7043) else SentinelEmerald)
                            Text("THREAT MATRIX ANALYSIS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CosmicTextPrimary)
                        }
                        Text("$unmitigatedCount Exposed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (unmitigatedCount > 0) Color(0xFFEF5350) else SentinelEmerald)
                    }

                    Text(
                        "Risk assessment calibrated for $selectedNetwork executing $selectedTxType.",
                        fontSize = 11.sp,
                        color = CosmicTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    if (unmitigatedCount > 0) {
                        Button(
                            onClick = onApplyAll,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = CosmicBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fortify All $unmitigatedCount Threat Vectors", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        items(threats) { threat ->
            val isSecured = threat.isMitigated
            val threatColor = if (isSecured) SentinelEmerald else Color(threat.riskLevel.colorHex)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isSecured) CosmicSurface else CosmicCardInner),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, threatColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(threat.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CosmicTextPrimary)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(threatColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSecured) "MITIGATED" else threat.riskLevel.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = threatColor
                            )
                        }
                    }

                    Text(
                        text = threat.description,
                        fontSize = 11.sp,
                        color = CosmicTextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recommended: ${threat.recommendedFeatureId}", fontSize = 10.sp, color = SentinelGold, fontFamily = FontFamily.Monospace)

                        if (!isSecured) {
                            FilledTonalButton(
                                onClick = { onEnableFeature(threat.recommendedFeatureId) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                            ) {
                                Icon(Icons.Filled.AddModerator, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Enable Guard", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SentinelEmerald, modifier = Modifier.size(14.dp))
                                Text("Active Shield", fontSize = 10.sp, color = SentinelEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Component 4: Hardened Solidity Contract View ---
@Composable
private fun HardenedSolidityView(
    contractSource: String,
    selectedNetwork: String,
    selectedTxType: String,
    enabledCount: Int,
    onCopy: () -> Unit,
    onDeploy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDivider)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PRODUCTION SOLIDITY SUITE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CosmicTextPrimary)
                    Text("$selectedNetwork • $enabledCount Security Modules Embedded", fontSize = 10.sp, color = SentinelGold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onCopy,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CosmicCardInner, contentColor = SentinelGold),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 10.sp)
                    }
                    Button(
                        onClick = onDeploy,
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = CosmicBackground),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deploy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0A090E))
                .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            LazyColumn {
                item {
                    Text(
                        text = contractSource,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFE6E1E5),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// --- Component 5: Saved Profiles Vault View (Room Database) ---
@Composable
private fun SavedProfilesVaultView(
    repository: SentinelRepository,
    activeEnabledIds: Set<String>,
    onLoadProfile: (SecurityProfile) -> Unit,
    onDeleteProfile: (profileId: String, name: String) -> Unit,
    onCopyProfileJson: (String) -> Unit
) {
    val profiles = repository.securityConfigurations

    if (profiles.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.FolderOff, contentDescription = null, tint = CosmicTextSecondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No Saved Configurations in Vault", color = CosmicTextSecondary, fontSize = 14.sp)
                Text("Use 'Save to Vault' on any configuration to store it permanently in Room DB.", fontSize = 11.sp, color = CosmicTextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "STORED SECURITY CONFIGURATIONS (${profiles.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SentinelGold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(profiles, key = { it.id }) { profile ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(profile.updatedAt))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CosmicDivider)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CosmicTextPrimary)
                                Text("${profile.targetNetwork} • ${profile.transactionType}", fontSize = 11.sp, color = CosmicTextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${profile.securityScore}%", fontWeight = FontWeight.Black, fontSize = 16.sp, color = SentinelEmerald, fontFamily = FontFamily.Monospace)
                                Text("${profile.enabledFeatureIds.size} modules", fontSize = 10.sp, color = SentinelGold)
                            }
                        }

                        if (profile.notes.isNotBlank()) {
                            Text(
                                text = profile.notes,
                                fontSize = 11.sp,
                                color = CosmicTextSecondary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        // Feature badges preview
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            items(profile.enabledFeatureIds.toList()) { featId ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CosmicCardInner)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(featId, fontSize = 8.sp, color = CosmicTextSecondary, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Updated $dateStr", fontSize = 9.sp, color = CosmicTextSecondary)

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        val json = """
{
  "id": "${profile.id}",
  "name": "${profile.name}",
  "network": "${profile.targetNetwork}",
  "transactionType": "${profile.transactionType}",
  "features": [${profile.enabledFeatureIds.joinToString(",") { "\"$it\"" }}],
  "securityScore": ${profile.securityScore},
  "maxSlippageBps": ${profile.customMaxSlippageBps},
  "maxRateLimitEth": ${profile.customMaxRateLimitEth}
}
                                        """.trimIndent()
                                        onCopyProfileJson(json)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Export", tint = CosmicTextSecondary, modifier = Modifier.size(14.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteProfile(profile.id, profile.name) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                                }

                                Button(
                                    onClick = { onLoadProfile(profile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Load Active", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
