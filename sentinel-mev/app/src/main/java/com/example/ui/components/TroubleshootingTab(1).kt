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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TroubleshootingTab(
    repository: SentinelRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // State bindings from repository
    val healthScore by repository.overallSystemHealthScore
    val isRunningDiagnostics by repository.isDiagnosticsRunning
    val diagnosticLogs = repository.diagnosticLogs
    val subsystemHealthList = repository.subsystemHealthList
    val lastCheckTime by repository.lastDiagnosticsCheckTime

    // Filter & Search states
    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverityFilter by remember { mutableStateOf<DiagnosticSeverity?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<DiagnosticCategory?>(null) }
    var showOnlyUnresolved by remember { mutableStateOf(false) }

    // Dialog states
    var showSimulationDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }
    var expandedLogId by remember { mutableStateOf<String?>(null) }
    var isExecutingQuickFix by remember { mutableStateOf(false) }

    // Animation for scanning pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Filtered diagnostic logs
    val filteredLogs = remember(diagnosticLogs, searchQuery, selectedSeverityFilter, selectedCategoryFilter, showOnlyUnresolved) {
        diagnosticLogs.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.message.contains(searchQuery, ignoreCase = true) ||
                    (item.stackTrace?.contains(searchQuery, ignoreCase = true) == true)
            val matchesSeverity = selectedSeverityFilter == null || item.severity == selectedSeverityFilter
            val matchesCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
            val matchesUnresolved = !showOnlyUnresolved || !item.isResolved
            matchesSearch && matchesSeverity && matchesCategory && matchesUnresolved
        }
    }

    val unresolvedCount = diagnosticLogs.count { !it.isResolved }
    val criticalCount = diagnosticLogs.count { !it.isResolved && it.severity == DiagnosticSeverity.CRITICAL }
    val errorCount = diagnosticLogs.count { !it.isResolved && it.severity == DiagnosticSeverity.ERROR }
    val warningCount = diagnosticLogs.count { !it.isResolved && it.severity == DiagnosticSeverity.WARNING }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // =====================================================================
        // HERO SYSTEM HEALTH BANNER
        // =====================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                CosmicSurface,
                                if (healthScore >= 90) SentinelEmeraldDim else if (healthScore >= 70) SentinelGoldDim else Color(0x33FF5252)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (healthScore >= 90) SentinelEmerald else if (healthScore >= 70) SentinelGold else Color(0xFFFF5252),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                    .background(if (healthScore >= 90) SentinelEmerald else if (healthScore >= 70) SentinelGold else Color(0xFFFF5252)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (healthScore >= 90) Icons.Filled.VerifiedUser else if (healthScore >= 70) Icons.Filled.WarningAmber else Icons.Filled.Dangerous,
                                    contentDescription = "System Health",
                                    tint = CosmicBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "SYSTEM HEALTH & DIAGNOSTICS",
                                    color = if (healthScore >= 90) SentinelEmerald else if (healthScore >= 70) SentinelGold else Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = if (healthScore >= 90) "All Subsystems Nominal" else if (healthScore >= 70) "$unresolvedCount Actionable Notice(s)" else "Attention Required ($unresolvedCount Issues)",
                                    color = CosmicTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Health Score Badge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$healthScore%",
                                color = if (healthScore >= 90) SentinelEmerald else if (healthScore >= 70) SentinelGold else Color(0xFFFF5252),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "SYSTEM INTEGRITY",
                                color = CosmicTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Key telemetry summary chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicCardInner,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (criticalCount == 0) SentinelEmerald else Color(0xFFFF5252)))
                                Text("Critical: $criticalCount", color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicCardInner,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (errorCount == 0) SentinelEmerald else Color(0xFFFFB74D)))
                                Text("Errors: $errorCount", color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicCardInner,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (warningCount == 0) SentinelEmerald else SentinelGold))
                                Text("Warnings: $warningCount", color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Diagnostic Scan Button & Export Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val score = repository.runFullSystemHealthCheck()
                                    Toast.makeText(context, "Diagnostics complete! System Health: $score%", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isRunningDiagnostics,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SentinelGold,
                                contentColor = CosmicBackground
                            )
                        ) {
                            if (isRunningDiagnostics) {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = "Running",
                                    modifier = Modifier.size(16.dp).rotate(scanRotation)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Running Self-Test...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Run Test", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Run Full Diagnostics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                reportContent = repository.exportDiagnosticBundle()
                                showReportDialog = true
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SentinelGold),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SentinelGold
                            )
                        ) {
                            Icon(Icons.Filled.Description, contentDescription = "Report", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =====================================================================
        // SUBSYSTEM HEALTH MATRIX
        // =====================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CORE SUBSYSTEM STATUS",
                        color = SentinelGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Last verified: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastCheckTime))}",
                        color = CosmicTextSecondary,
                        fontSize = 10.sp
                    )
                }

                // Grid of subsystem cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subsystemHealthList.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { sub ->
                                SubsystemHealthCard(
                                    subsystem = sub,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 1-CLICK SELF-HEALING & REMEDIATION TOOLBAR
        // =====================================================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.BuildCircle, contentDescription = "Self-Healing", tint = SentinelGold, modifier = Modifier.size(18.dp))
                            Text("1-Click Self-Healing Toolbox", color = CosmicTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { showSimulationDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Science, contentDescription = "Simulate", tint = SentinelGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Simulate Error", color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Action buttons row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            QuickRemediationPill(
                                icon = Icons.Filled.WifiProtectedSetup,
                                label = "Cycle RPC Nodes",
                                isExecuting = isExecutingQuickFix
                            ) {
                                scope.launch {
                                    isExecutingQuickFix = true
                                    val (success, msg) = repository.executeRemediation(RemediationType.CYCLE_RPC)
                                    isExecutingQuickFix = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        item {
                            QuickRemediationPill(
                                icon = Icons.Filled.Storage,
                                label = "Repair Database",
                                isExecuting = isExecutingQuickFix
                            ) {
                                scope.launch {
                                    isExecutingQuickFix = true
                                    val (success, msg) = repository.executeRemediation(RemediationType.REPAIR_DATABASE)
                                    isExecutingQuickFix = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        item {
                            QuickRemediationPill(
                                icon = Icons.Filled.DeleteSweep,
                                label = "Flush Mempool Queue",
                                isExecuting = isExecutingQuickFix
                            ) {
                                scope.launch {
                                    isExecutingQuickFix = true
                                    val (success, msg) = repository.executeRemediation(RemediationType.FLUSH_MEMPOOL)
                                    isExecutingQuickFix = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        item {
                            QuickRemediationPill(
                                icon = Icons.Filled.VpnKey,
                                label = "Re-sync Wallets",
                                isExecuting = isExecutingQuickFix
                            ) {
                                scope.launch {
                                    isExecutingQuickFix = true
                                    val (success, msg) = repository.executeRemediation(RemediationType.RECALIBRATE_WALLET)
                                    isExecutingQuickFix = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        item {
                            QuickRemediationPill(
                                icon = Icons.Filled.Shield,
                                label = "Reset Security Locks",
                                isExecuting = isExecutingQuickFix
                            ) {
                                scope.launch {
                                    isExecutingQuickFix = true
                                    val (success, msg) = repository.executeRemediation(RemediationType.RESET_SLIPPAGE_GUARD)
                                    isExecutingQuickFix = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        item {
                            QuickRemediationPill(
                                icon = Icons.Filled.CleaningServices,
                                label = "Purge Cache",
                                isExecuting = isExecutingQuickFix
                            ) {
                                scope.launch {
                                    isExecutingQuickFix = true
                                    val (success, msg) = repository.executeRemediation(RemediationType.CLEAR_CACHE)
                                    isExecutingQuickFix = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // LIVE ERROR & INCIDENT CONSOLE
        // =====================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INCIDENT & ERROR LOGS (${filteredLogs.size})",
                        color = SentinelGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    if (diagnosticLogs.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = { repository.clearResolvedDiagnosticItems() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Clear Resolved", color = CosmicTextSecondary, fontSize = 10.sp)
                            }
                            TextButton(
                                onClick = { repository.clearAllDiagnosticLogs() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Clear All", color = Color(0xFFFF5252), fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search errors, stack traces, or keywords...", fontSize = 12.sp, color = CosmicTextDim) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = CosmicTextSecondary, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = CosmicTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SentinelGold,
                        unfocusedBorderColor = CosmicDivider,
                        focusedContainerColor = CosmicSurface,
                        unfocusedContainerColor = CosmicSurface,
                        focusedTextColor = CosmicTextPrimary,
                        unfocusedTextColor = CosmicTextPrimary
                    ),
                    singleLine = true
                )

                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = showOnlyUnresolved,
                            onClick = { showOnlyUnresolved = !showOnlyUnresolved },
                            label = { Text("Unresolved Only", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SentinelGoldDim,
                                selectedLabelColor = SentinelGold
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSeverityFilter == null,
                            onClick = { selectedSeverityFilter = null },
                            label = { Text("All Severities", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SentinelGoldDim,
                                selectedLabelColor = SentinelGold
                            )
                        )
                    }
                    DiagnosticSeverity.values().filter { it != DiagnosticSeverity.HEALTHY }.forEach { sev ->
                        item {
                            FilterChip(
                                selected = selectedSeverityFilter == sev,
                                onClick = { selectedSeverityFilter = if (selectedSeverityFilter == sev) null else sev },
                                label = { Text(sev.label, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (sev) {
                                        DiagnosticSeverity.CRITICAL -> Color(0x4DFF5252)
                                        DiagnosticSeverity.ERROR -> Color(0x4DFFB74D)
                                        DiagnosticSeverity.WARNING -> Color(0x4DFFD54F)
                                        else -> SentinelBlueDim
                                    },
                                    selectedLabelColor = when (sev) {
                                        DiagnosticSeverity.CRITICAL -> Color(0xFFFF5252)
                                        DiagnosticSeverity.ERROR -> Color(0xFFFFB74D)
                                        DiagnosticSeverity.WARNING -> Color(0xFFFFD54F)
                                        else -> SentinelGold
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }

        // Error log items list
        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircleOutline,
                            contentDescription = "No Issues",
                            tint = SentinelEmerald,
                            modifier = Modifier.size(36.dp)
                        )
                        Text("No Matching Errors or Incidents", color = CosmicTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("All monitored subsystems are clean and functioning normally.", color = CosmicTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { item ->
                DiagnosticLogCard(
                    item = item,
                    isExpanded = expandedLogId == item.id,
                    onToggleExpand = {
                        expandedLogId = if (expandedLogId == item.id) null else item.id
                    },
                    onResolve = { repository.resolveDiagnosticItem(item.id) },
                    onExecuteRemediation = {
                        scope.launch {
                            val (success, msg) = repository.executeRemediation(item.remediationType)
                            repository.resolveDiagnosticItem(item.id)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onCopyTrace = {
                        val trace = item.stackTrace ?: "${item.title}\n${item.message}"
                        clipboardManager.setText(AnnotatedString(trace))
                        Toast.makeText(context, "Stack trace copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // =====================================================================
        // INTERACTIVE TROUBLESHOOTING KNOWLEDGE BASE
        // =====================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "COMMON WEB3 / MEV TROUBLESHOOTING GUIDES",
                    color = SentinelGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                val troubleshootingFaqs = remember {
                    listOf(
                        TroubleshootingFaq(
                            id = "faq_revert",
                            title = "Transaction Reverted (Slippage / Frontrun)",
                            category = DiagnosticCategory.CONTRACT_LIFECYCLE,
                            symptom = "EVM error 0x48f5e714 or 'Profit not realized, aborting'.",
                            rootCause = "A concurrent trade on Uniswap/Sushiswap moved the price pool before atomic settlement, or gas spikes exceeded margin.",
                            resolutionSteps = listOf(
                                "1. Verify ReentrancyGuard and Slippage Lock are active in Compiler Settings.",
                                "2. Lower max trade size or increase minimum profit threshold.",
                                "3. Tap 'Reset Security Locks' to restore atomic guard tolerance."
                            ),
                            quickActionLabel = "Reset Security Locks",
                            remediationType = RemediationType.RESET_SLIPPAGE_GUARD
                        ),
                        TroubleshootingFaq(
                            id = "faq_rpc_429",
                            title = "RPC Node Rate Limiting (HTTP 429)",
                            category = DiagnosticCategory.NETWORK_RPC,
                            symptom = "Network timeouts or 429 Too Many Requests errors.",
                            rootCause = "Public free-tier RPCs throttle after high-frequency block polling.",
                            resolutionSteps = listOf(
                                "1. Switch active endpoint to Cloudflare, LlamaRPC, or Ankr edge nodes.",
                                "2. Add your custom Alchemy/Infura API key in Settings.",
                                "3. Tap 'Cycle RPC Nodes' to measure and bind to the fastest failover node."
                            ),
                            quickActionLabel = "Cycle RPC Nodes",
                            remediationType = RemediationType.CYCLE_RPC
                        ),
                        TroubleshootingFaq(
                            id = "faq_db_lock",
                            title = "Database Write Contention or Memory Lag",
                            category = DiagnosticCategory.DATABASE_ROOM,
                            symptom = "Lag when viewing transaction history or saving new smart contracts.",
                            rootCause = "SQLite write contention between background daemon and UI state collector.",
                            resolutionSteps = listOf(
                                "1. Run SQLite database optimization and auto-vacuum.",
                                "2. Clear duplicate or expired opportunities.",
                                "3. Tap 'Repair Database' to rebuild indices."
                            ),
                            quickActionLabel = "Repair Database",
                            remediationType = RemediationType.REPAIR_DATABASE
                        ),
                        TroubleshootingFaq(
                            id = "faq_wallet_signer",
                            title = "Smart Wallet Nonce / Signature Mismatch",
                            category = DiagnosticCategory.SMART_WALLET,
                            symptom = "UserOperation rejected by ERC-4337 bundler with invalid signature.",
                            rootCause = "Mnemonic seed derivation out of sync with active network chain ID.",
                            resolutionSteps = listOf(
                                "1. Verify 12-word BIP-39 mnemonic phrase backup.",
                                "2. Re-derive cryptographic secp256k1 key pairs.",
                                "3. Tap 'Re-sync Wallets' to re-bind account abstraction signers."
                            ),
                            quickActionLabel = "Re-sync Wallets",
                            remediationType = RemediationType.RECALIBRATE_WALLET
                        )
                    )
                }

                troubleshootingFaqs.forEach { faq ->
                    TroubleshootingFaqCard(
                        faq = faq,
                        onExecuteFix = {
                            scope.launch {
                                val (success, msg) = repository.executeRemediation(faq.remediationType)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // =========================================================================
    // SIMULATE ERROR SCENARIOS DIALOG
    // =========================================================================
    if (showSimulationDialog) {
        AlertDialog(
            onDismissRequest = { showSimulationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Science, contentDescription = "Simulation", tint = SentinelGold)
                    Text("Simulate Error & Test Diagnostics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Trigger a synthetic error condition to verify real-time log capture, diagnostic severity scoring, and self-healing remediation flows:",
                        color = CosmicTextSecondary,
                        fontSize = 12.sp
                    )

                    val scenarios = listOf(
                        Triple("RPC_429_RATE_LIMIT", "HTTP 429 Rate Limiter", "Simulates public RPC rate limit exhaustion and failover recommendation."),
                        Triple("SLIPPAGE_REVERT", "EVM Slippage Revert", "Simulates DEX price impact trigger intercepted by Slippage Guard."),
                        Triple("DATABASE_LOCK_WARNING", "SQLite Database Contention", "Simulates write concurrency lock and auto-reindexing suggestion."),
                        Triple("FAUCET_IP_RATE_EXCEEDED", "Testnet Faucet IP Cooldown", "Simulates 24-hour rate limit on Sepolia testnet faucet.")
                    )

                    scenarios.forEach { (type, name, desc) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CosmicCardInner,
                            border = BorderStroke(1.dp, CosmicDivider),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.simulateErrorScenario(type)
                                    showSimulationDialog = false
                                    Toast.makeText(context, "Simulated $name captured in Diagnostics!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(name, color = SentinelGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(desc, color = CosmicTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSimulationDialog = false }) {
                    Text("Close", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicSurface
        )
    }

    // =========================================================================
    // EXPORT DIAGNOSTIC REPORT DIALOG
    // =========================================================================
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Description, contentDescription = "Report", tint = SentinelGold)
                    Text("Diagnostic Log Report", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CosmicTextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Comprehensive system state ready to export for developer analysis:", color = CosmicTextSecondary, fontSize = 12.sp)
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicBackground)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = reportContent,
                                        color = CosmicTextPrimary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(reportContent))
                        showReportDialog = false
                        Toast.makeText(context, "Full diagnostic report copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Close", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicSurface
        )
    }
}

@Composable
fun SubsystemHealthCard(
    subsystem: SubsystemHealth,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(
            1.dp,
            when (subsystem.status) {
                HealthStatus.OPERATIONAL -> SentinelEmeraldDim
                HealthStatus.DEGRADED -> SentinelGoldDim
                HealthStatus.FAILING -> Color(0x4DFF5252)
                HealthStatus.CHECKING -> CosmicDivider
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subsystem.name,
                    color = CosmicTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (subsystem.status) {
                                HealthStatus.OPERATIONAL -> SentinelEmerald
                                HealthStatus.DEGRADED -> SentinelGold
                                HealthStatus.FAILING -> Color(0xFFFF5252)
                                HealthStatus.CHECKING -> CosmicTextDim
                            }
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subsystem.status.label,
                    color = when (subsystem.status) {
                        HealthStatus.OPERATIONAL -> SentinelEmerald
                        HealthStatus.DEGRADED -> SentinelGold
                        HealthStatus.FAILING -> Color(0xFFFF5252)
                        HealthStatus.CHECKING -> CosmicTextSecondary
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${subsystem.latencyMs}ms",
                    color = CosmicTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = subsystem.details,
                color = CosmicTextSecondary,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun QuickRemediationPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isExecuting: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CosmicCardInner,
        border = BorderStroke(1.dp, SentinelGoldDim),
        modifier = Modifier.clickable(enabled = !isExecuting, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = SentinelGold, modifier = Modifier.size(14.dp))
            Text(label, color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiagnosticLogCard(
    item: DiagnosticItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onResolve: () -> Unit,
    onExecuteRemediation: () -> Unit,
    onCopyTrace: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))

    val severityColor = when (item.severity) {
        DiagnosticSeverity.CRITICAL -> Color(0xFFFF5252)
        DiagnosticSeverity.ERROR -> Color(0xFFFFB74D)
        DiagnosticSeverity.WARNING -> Color(0xFFFFD54F)
        DiagnosticSeverity.INFO -> SentinelGold
        DiagnosticSeverity.HEALTHY -> SentinelEmerald
    }

    val severityBg = when (item.severity) {
        DiagnosticSeverity.CRITICAL -> Color(0x26FF5252)
        DiagnosticSeverity.ERROR -> Color(0x26FFB74D)
        DiagnosticSeverity.WARNING -> Color(0x26FFD54F)
        DiagnosticSeverity.INFO -> SentinelBlueDim
        DiagnosticSeverity.HEALTHY -> SentinelEmeraldDim
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, if (item.isResolved) CosmicDivider else severityColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Severity, Category, Time, Resolved
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(severityBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.severity.label, color = severityColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }

                    Text(
                        text = item.category.displayName,
                        color = CosmicTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.isResolved) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SentinelEmeraldDim)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("RESOLVED", color = SentinelEmerald, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(dateStr, color = CosmicTextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Title and Description
            Text(item.title, color = CosmicTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(item.message, color = CosmicTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)

            // Context Key-Values
            if (item.contextDetails.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item.contextDetails.forEach { (key, value) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicCardInner)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text("$key: $value", color = CosmicTextPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Expanded Stacktrace & Remediation details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recommended Action Callout Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .border(1.dp, SentinelGoldDim, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.TipsAndUpdates, contentDescription = "Fix", tint = SentinelGold, modifier = Modifier.size(14.dp))
                                Text("RECOMMENDED REMEDIATION:", color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                            Text(item.recommendedAction, color = CosmicTextPrimary, fontSize = 11.sp)
                        }
                    }

                    // Stacktrace viewer
                    if (!item.stackTrace.isNullOrBlank()) {
                        SelectionContainer {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicBackground)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = item.stackTrace,
                                    color = Color(0xFFFFB74D),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleExpand,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide Details ▲" else "View Details & Stack Trace ▼",
                        color = SentinelGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.stackTrace != null) {
                        IconButton(
                            onClick = onCopyTrace,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = CosmicTextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (!item.isResolved) {
                        Button(
                            onClick = onExecuteRemediation,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = "Auto Fix", modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Apply Fix", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onResolve,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, CosmicDivider),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Dismiss", color = CosmicTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TroubleshootingFaqCard(
    faq: TroubleshootingFaq,
    onExecuteFix: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider)
    ) {
        Column(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = "FAQ", tint = SentinelGold, modifier = Modifier.size(16.dp))
                    Text(faq.title, color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Expand",
                    tint = CosmicTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text("Symptom: ${faq.symptom}", color = CosmicTextSecondary, fontSize = 11.sp)

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardInner)
                            .padding(8.dp)
                    ) {
                        Text("Root Cause: ${faq.rootCause}", color = CosmicTextPrimary, fontSize = 10.sp)
                    }

                    Text("Resolution Steps:", color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    faq.resolutionSteps.forEach { step ->
                        Text(step, color = CosmicTextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                    }

                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick = onExecuteFix,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Fix", modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(faq.quickActionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
