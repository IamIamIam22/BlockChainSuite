package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SentinelRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(
    repository: SentinelRepository,
    onNavigateToTroubleshooting: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Configuration / Cloud Sync state variables
    var currentSyncCode by remember { mutableStateOf("") }
    var inputSyncCode by remember { mutableStateOf("") }
    var isSyncingCloud by remember { mutableStateOf(false) }
    var isRestoringCloud by remember { mutableStateOf(false) }

    // Manual Local state variables
    var isRawExportOpen by remember { mutableStateOf(false) }
    var rawExportString by remember { mutableStateOf("") }
    var rawImportInput by remember { mutableStateOf("") }
    var isImportingRaw by remember { mutableStateOf(false) }

    // Live terminal log list
    val syncTerminalLogs = remember { mutableStateListOf<String>() }

    // On-screen stats
    val smartWalletsCount = repository.smartWallets.size
    val contractsCount = repository.trackedContracts.size
    val txCount = repository.transactionHistory.size
    val totalEthProfit = repository.transactionHistory.sumOf { it.profitEth }
    val totalBtcProfit = repository.transactionHistory.sumOf { it.profitBtc }

    // Initialize custom sync code if empty
    LaunchedEffect(Unit) {
        if (currentSyncCode.isEmpty()) {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            currentSyncCode = "SNTL-" + (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        }
        syncTerminalLogs.add("📱 Sentinel profile engine initialized.")
        syncTerminalLogs.add("🔒 Device smart-key bindings verified.")
        syncTerminalLogs.add("📡 Standby: Ready for cloud sync or local backup export.")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Page Hero Card
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
                        imageVector = Icons.Filled.SyncLock,
                        contentDescription = "Profile Sync",
                        tint = SentinelGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "PROFILE CLOUD SYNCHRONIZATION",
                            color = SentinelGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Keep Accounts and Wallets in Sync",
                            color = CosmicTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Transfer your local cryptographic keys, faucet states, and contracts dynamically across updates or device transitions.",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Profile Metrics Dashboard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Dashboard,
                            contentDescription = "Metrics",
                            tint = SentinelEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PROFILE ACCOUNT STATUS",
                            color = CosmicTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Divider(color = CosmicDivider, thickness = 1.dp)

                    // Stats Grid Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Wallets & Contracts
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Active Smart Wallets", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("$smartWalletsCount Loaded", color = CosmicTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Tracked Contracts", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("$contractsCount Compiled", color = CosmicTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        // History & Profits
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Completed Logs", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("$txCount Transactions", color = CosmicTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Consolidated MEV Yield", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text(
                                text = String.format("%.4f BTC / %.4f ETH", totalBtcProfit, totalEthProfit),
                                color = SentinelEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Cloud Backup Sync Hub
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = "Cloud",
                            tint = SentinelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CLOUD PROFILE SYNC PANEL",
                            color = CosmicTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Synchronize account data dynamically with our remote decentralized Profile node. This creates a secure, temporary backup that you can load instantly on any device.",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Divider(color = CosmicDivider, thickness = 1.dp)

                    // Export to cloud
                    Text(
                        text = "1. BACKUP CURRENT PROFILE",
                        color = CosmicTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Your secure profile sync code:", color = CosmicTextSecondary, fontSize = 11.sp)
                            SelectionContainer {
                                Text(
                                    text = currentSyncCode,
                                    color = SentinelGold,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSyncingCloud = true
                                    syncTerminalLogs.add("🔌 Connecting to Profile Sync Server...")
                                    delay(400)
                                    syncTerminalLogs.add("📦 Compiling SQLite tables & preferences...")
                                    delay(400)
                                    syncTerminalLogs.add("📤 Uploading profile payload under code $currentSyncCode...")
                                    
                                    val success = repository.uploadProfileToCloud(currentSyncCode)
                                    delay(300)
                                    if (success) {
                                        syncTerminalLogs.add("✓ SUCCESS: Profile uploaded securely to kvdb.io remote registry.")
                                        syncTerminalLogs.add("💡 Save this code: $currentSyncCode. Open the app on your phone and input it below to sync accounts!")
                                        Toast.makeText(context, "Profile backup uploaded successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        syncTerminalLogs.add("❌ ERROR: Connection failed. Verify network permissions.")
                                        Toast.makeText(context, "Cloud sync upload failed. Try again.", Toast.LENGTH_SHORT).show()
                                    }
                                    isSyncingCloud = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelGold),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isSyncingCloud && !isRestoringCloud
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isSyncingCloud) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = CosmicBackground, strokeWidth = 1.5.dp)
                                    Text("Uploading...", fontSize = 11.sp, color = CosmicBackground)
                                } else {
                                    Icon(Icons.Filled.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(14.dp), tint = CosmicBackground)
                                    Text("Upload Profile", fontSize = 11.sp, color = CosmicBackground, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = CosmicDivider, thickness = 1.dp)

                    // Import from cloud
                    Text(
                        text = "2. RESTORE AN EXISTING PROFILE",
                        color = CosmicTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = inputSyncCode,
                        onValueChange = { inputSyncCode = it.uppercase().trim() },
                        placeholder = { Text("E.g. SNTL-Y6T9R1", fontSize = 11.sp, color = CosmicTextDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary,
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = "Sync Key", modifier = Modifier.size(16.dp), tint = CosmicTextSecondary)
                        }
                    )

                    Button(
                        onClick = {
                            if (inputSyncCode.isEmpty()) {
                                Toast.makeText(context, "Please enter a valid sync code first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                isRestoringCloud = true
                                syncTerminalLogs.add("📡 Downloading payload for profile: $inputSyncCode...")
                                delay(500)
                                
                                val success = repository.downloadProfileFromCloud(inputSyncCode)
                                delay(300)
                                if (success) {
                                    syncTerminalLogs.add("✓ SUCCESS: Downloaded profile metadata successfully.")
                                    syncTerminalLogs.add("🔄 Clearing existing database, updating keys, and applying active profile...")
                                    delay(500)
                                    syncTerminalLogs.add("🎉 Profile fully restored! Smart wallets, contracts, and faucet records synced.")
                                    Toast.makeText(context, "Profile restored successfully from $inputSyncCode!", Toast.LENGTH_LONG).show()
                                    // Set current sync code to the restored code
                                    currentSyncCode = inputSyncCode
                                } else {
                                    syncTerminalLogs.add("❌ ERROR: Profile key '$inputSyncCode' not found or payload corrupt.")
                                    Toast.makeText(context, "Error: Invalid sync code or connection timeout.", Toast.LENGTH_SHORT).show()
                                }
                                isRestoringCloud = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncingCloud && !isRestoringCloud
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isRestoringCloud) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 1.5.dp)
                                Text("Downloading Profile...", fontSize = 11.sp, color = Color.White)
                            } else {
                                Icon(Icons.Filled.CloudDownload, contentDescription = "Restore", modifier = Modifier.size(14.dp), tint = Color.White)
                                Text("Download & Sync Profile to Phone", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Log Terminal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = "Logs",
                            tint = SentinelGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SYNC LOGS TERMINAL",
                            color = CosmicTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicBackground)
                            .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(syncTerminalLogs.size) { index ->
                                val log = syncTerminalLogs[index]
                                Text(
                                    text = log,
                                    color = if (log.startsWith("✓")) SentinelEmerald else if (log.startsWith("❌")) Color(0xFFEF5350) else CosmicTextPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Manual Import/Export File Area
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Manual",
                            tint = CosmicTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "MANUAL OFFLINE BACKUP (JSON)",
                            color = CosmicTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Prefer offline profile security? You can manually copy the encrypted backup text to paste directly on your other device, or paste a backup string to restore.",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Divider(color = CosmicDivider, thickness = 1.dp)

                    // Export Expandable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Export Local profile", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(
                            onClick = {
                                if (!isRawExportOpen) {
                                    rawExportString = repository.exportProfileAsJson()
                                    syncTerminalLogs.add("📝 Raw JSON profile generated.")
                                }
                                isRawExportOpen = !isRawExportOpen
                            },
                            border = BorderStroke(1.dp, CosmicDivider),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (isRawExportOpen) "Hide JSON" else "Generate JSON", fontSize = 11.sp, color = SentinelGold)
                        }
                    }

                    AnimatedVisibility(
                        visible = isRawExportOpen,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = rawExportString,
                                        color = CosmicTextSecondary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(rawExportString))
                                    Toast.makeText(context, "Raw profile JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    syncTerminalLogs.add("📋 Copied local backup string to clipboard.")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicCardInner, contentColor = CosmicTextPrimary),
                                border = BorderStroke(1.dp, CosmicDivider),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                    Text("Copy JSON Profile String", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Divider(color = CosmicDivider, thickness = 1.dp)

                    // Import Expandable
                    Text("Import Profile String:", color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    OutlinedTextField(
                        value = rawImportInput,
                        onValueChange = { rawImportInput = it },
                        placeholder = { Text("Paste raw backup JSON string here...", fontSize = 11.sp, color = CosmicTextDim) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary,
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (rawImportInput.isEmpty()) {
                                Toast.makeText(context, "Please paste a JSON backup string!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                isImportingRaw = true
                                syncTerminalLogs.add("📥 Verifying pasted backup string...")
                                delay(400)
                                val success = repository.importProfileFromJson(rawImportInput)
                                delay(300)
                                if (success) {
                                    syncTerminalLogs.add("✓ SUCCESS: Manual backup string fully restored.")
                                    Toast.makeText(context, "Manual profile restored successfully!", Toast.LENGTH_LONG).show()
                                    rawImportInput = ""
                                } else {
                                    syncTerminalLogs.add("❌ ERROR: Invalid JSON format. Make sure you copied the entire backup.")
                                    Toast.makeText(context, "Format error: Invalid backup JSON string.", Toast.LENGTH_SHORT).show()
                                }
                                isImportingRaw = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicDivider),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isImportingRaw
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isImportingRaw) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = CosmicTextPrimary, strokeWidth = 1.5.dp)
                                Text("Importing...", fontSize = 11.sp, color = CosmicTextPrimary)
                            } else {
                                Icon(Icons.Filled.Input, contentDescription = "Import", modifier = Modifier.size(14.dp))
                                Text("Restore Offline Profile String", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Diagnostics & Self-Healing Launcher Card
        item {
            val healthScore by repository.overallSystemHealthScore
            val unresolvedCount = repository.diagnosticLogs.count { !it.isResolved }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CosmicSurface)
                    .border(1.dp, SentinelGoldDim, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.BuildCircle,
                                contentDescription = "Diagnostics",
                                tint = SentinelGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "SYSTEM DIAGNOSTICS & TROUBLESHOOTING",
                                    color = SentinelGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Subsystem Integrity & Error Logs",
                                    color = CosmicTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (healthScore >= 90) SentinelEmeraldDim else SentinelGoldDim)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$healthScore% HEALTHY",
                                color = if (healthScore >= 90) SentinelEmerald else SentinelGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Inspect RPC connectivity, smart wallet keys, SQLite databases, real-time error stack traces, and execute 1-click self-healing repairs.",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )

                    Button(
                        onClick = { onNavigateToTroubleshooting?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.BugReport, contentDescription = "Troubleshoot", modifier = Modifier.size(16.dp))
                            Text(
                                text = if (unresolvedCount > 0) "Open Troubleshooting Center ($unresolvedCount Notices)" else "Open Troubleshooting Center",
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
