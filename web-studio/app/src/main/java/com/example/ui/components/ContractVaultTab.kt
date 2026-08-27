package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ContractVaultExporter
import com.example.data.SentinelRepository
import com.example.data.TrackedContract
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContractVaultTab(
    repository: SentinelRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val webServer = repository.webServer
    val isServerRunning by webServer.isRunning
    val serverUrl by webServer.serverUrl

    val contractsList = repository.trackedContracts
    val smartWalletsList = repository.smartWallets
    val config = repository.config

    var searchQuery by remember { mutableStateOf("") }
    var expandedContractId by remember { mutableStateOf<String?>(null) }

    var showInAppWebPortalDialog by remember { mutableStateOf(false) }
    var showSecurityConfigDialog by remember { mutableStateOf(false) }

    // Filter contracts
    val filteredContracts = remember(contractsList, searchQuery) {
        contractsList.filter { c ->
            searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    c.network.contains(searchQuery, ignoreCase = true) ||
                    c.address.contains(searchQuery, ignoreCase = true) ||
                    c.securityFeatures.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Section 1: Header & Web Integration Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicSurface)
                    .border(1.dp, SentinelGold, RoundedCornerShape(16.dp))
                    .padding(16.dp)
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SentinelGoldDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Laptop,
                                    contentDescription = "Laptop",
                                    tint = SentinelGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Web Hub & Contract Vault",
                                    color = CosmicTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Laptop Web Server, Smart Contracts Archive & Wallet Keys",
                                    color = CosmicTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Server Toggle Button
                        Switch(
                            checked = isServerRunning,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    webServer.startServer()
                                    Toast.makeText(context, "Web Server Started at ${webServer.serverUrl.value}", Toast.LENGTH_LONG).show()
                                } else {
                                    webServer.stopServer()
                                    Toast.makeText(context, "Web Server Stopped", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SentinelGold,
                                checkedTrackColor = SentinelGoldDim,
                                uncheckedThumbColor = CosmicTextDim,
                                uncheckedTrackColor = CosmicCardInner
                            )
                        )
                    }

                    // Live Web Integration Status Strip
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CosmicCardInner),
                        border = BorderStroke(1.dp, if (isServerRunning) SentinelEmerald else CosmicDivider),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isServerRunning) SentinelEmerald else Color.Gray)
                                    )
                                    Text(
                                        text = if (isServerRunning) "HTTP Web Server Active" else "HTTP Web Server Offline",
                                        color = if (isServerRunning) SentinelEmerald else CosmicTextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Text(
                                    text = if (isServerRunning) serverUrl else "Port 8080 (0.0.0.0 All Interfaces)",
                                    color = SentinelGold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isServerRunning) {
                                val broadcastIps = repository.webServer.getAllIpAddresses()
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "📡 Network-Wide Broadcasting Active (0.0.0.0:8080):",
                                        color = SentinelEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    broadcastIps.forEach { ip ->
                                        Text(
                                            text = "• http://$ip:8080",
                                            color = CosmicTextSecondary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "Open any of these URLs in any browser on your Wi-Fi/LAN/Hotspot to access the Web Command Center.",
                                        color = CosmicTextDim,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(serverUrl))
                                        Toast.makeText(context, "Laptop Web URL Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGoldDim, contentColor = SentinelGold),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                        Text("Copy Laptop URL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = { showInAppWebPortalDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.Language, contentDescription = "Web Portal", modifier = Modifier.size(14.dp))
                                        Text("In-App Web Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Download All Contracts ZIP & Security Settings Export
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, SentinelGoldDim)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Smart Contracts Archive (${contractsList.size} Written)",
                                color = CosmicTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Includes Solidity files, ABIs, Web Dashboard & Wallet Security Config",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SentinelGoldDim)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${contractsList.size} .sol Files",
                                color = SentinelGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Divider(color = CosmicDivider)

                    // Big DOWNLOAD ALL AS ZIP Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val zipFile = ContractVaultExporter.generateAllContractsZip(
                                    context = context,
                                    trackedContracts = contractsList,
                                    smartWallets = smartWalletsList,
                                    config = config
                                )
                                Toast.makeText(
                                    context,
                                    "ZIP Archive Saved: ${zipFile.name} (${zipFile.length() / 1024} KB)",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Launch Share Intent
                                shareFileIntent(context, zipFile, "application/zip")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.FolderZip, contentDescription = "ZIP Download", modifier = Modifier.size(20.dp))
                            Text("DOWNLOAD ALL CONTRACTS (.ZIP ARCHIVE)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }

                    // Secondary Export Options Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Download wallet_security_config.json
                        OutlinedButton(
                            onClick = { showSecurityConfigDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SentinelGold)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = "Security Keys", tint = SentinelGold, modifier = Modifier.size(14.dp))
                                Text("Wallet Security JSON", color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Export Standalone HTML Web Dashboard
                        OutlinedButton(
                            onClick = {
                                val htmlContent = ContractVaultExporter.generateWebDashboardHtml(
                                    contractsList,
                                    smartWalletsList,
                                    config
                                )
                                val file = ContractVaultExporter.saveSingleTextFile(context, "index.html", htmlContent)
                                Toast.makeText(context, "Saved index.html to Downloads!", Toast.LENGTH_LONG).show()
                                shareFileIntent(context, file, "text/html")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SentinelEmerald)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = "Download HTML", tint = SentinelEmerald, modifier = Modifier.size(14.dp))
                                Text("Export Web Page", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Search Filter for Contracts Repository
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contract name, network, address, or security feature...", fontSize = 12.sp, color = CosmicTextDim) },
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
        }

        // Section 4: Individual Smart Contract Cards
        if (filteredContracts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No smart contracts matching search query.", color = CosmicTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredContracts, key = { it.id }) { contract ->
                val isExpanded = expandedContractId == contract.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedContractId = if (isExpanded) null else contract.id
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, if (isExpanded) SentinelGold else CosmicDivider)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = "Solidity", tint = SentinelGold, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = "${contract.name}.sol",
                                        color = CosmicTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = contract.network,
                                        color = CosmicTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SentinelEmeraldDim)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = contract.status,
                                        color = SentinelEmerald,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = CosmicTextSecondary
                                )
                            }
                        }

                        // Address Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Address:", color = CosmicTextDim, fontSize = 11.sp)
                            Text(
                                text = contract.address,
                                color = SentinelGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Security Features FlowRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            contract.securityFeatures.forEach { feature ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CosmicCardInner)
                                        .border(1.dp, CosmicDivider, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("🛡️ $feature", color = CosmicTextSecondary, fontSize = 9.sp)
                                }
                            }
                        }

                        // Expandable Section: Solidity Source Code & ABI Preview
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Divider(color = CosmicDivider)

                                Text("Solidity Source Code Preview:", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0A0910))
                                        .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = contract.soliditySource,
                                        color = Color(0xFF8FDC9F),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 14.sp
                                    )
                                }

                                // Action Row for single contract download
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Download single .sol file
                                    Button(
                                        onClick = {
                                            val file = ContractVaultExporter.saveSingleTextFile(
                                                context,
                                                "${contract.name}.sol",
                                                contract.soliditySource
                                            )
                                            Toast.makeText(context, "Saved ${file.name} to Downloads!", Toast.LENGTH_SHORT).show()
                                            shareFileIntent(context, file, "text/plain")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Filled.Download, contentDescription = "Download", modifier = Modifier.size(14.dp))
                                            Text("Download .sol", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Download single ABI .json file
                                    OutlinedButton(
                                        onClick = {
                                            val abiContent = ContractVaultExporter.generateAbiForContract(contract)
                                            val file = ContractVaultExporter.saveSingleTextFile(
                                                context,
                                                "${contract.name}.abi.json",
                                                abiContent
                                            )
                                            Toast.makeText(context, "Saved ${file.name} to Downloads!", Toast.LENGTH_SHORT).show()
                                            shareFileIntent(context, file, "application/json")
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, SentinelGold)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Filled.Code, contentDescription = "Download ABI", tint = SentinelGold, modifier = Modifier.size(14.dp))
                                            Text("Download ABI", color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Copy Code
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(contract.soliditySource))
                                            Toast.makeText(context, "Solidity Source Copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(CosmicCardInner)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = SentinelGold, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog 1: In-App Laptop Web Portal Previewer (WebView)
    if (showInAppWebPortalDialog) {
        AlertDialog(
            onDismissRequest = { showInAppWebPortalDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Laptop, contentDescription = "Laptop Web", tint = SentinelGold)
                        Text("Laptop Web Application Portal", color = CosmicTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showInAppWebPortalDialog = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = CosmicTextSecondary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Preview of the Web Application served to your laptop at $serverUrl:",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp
                    )

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true

                                val html = ContractVaultExporter.generateWebDashboardHtml(
                                    contractsList,
                                    smartWalletsList,
                                    config
                                )
                                loadDataWithBaseURL("http://localhost:8080", html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(serverUrl))
                        Toast.makeText(context, "URL Copied: $serverUrl", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                ) {
                    Text("Copy Server URL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInAppWebPortalDialog = false }) {
                    Text("Close Portal", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Dialog 2: Wallet Security Configuration Inspector & Exporter
    if (showSecurityConfigDialog) {
        val securityConfigJson = remember(smartWalletsList, config) {
            ContractVaultExporter.generateWalletSecurityConfigJson(smartWalletsList, config)
        }

        AlertDialog(
            onDismissRequest = { showSecurityConfigDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = "Security", tint = SentinelGold)
                    Text("Wallet Security Settings & Credentials", color = CosmicTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "This file contains wallet addresses, seed phrases, private keys, and gas security settings so you can import them into your laptop dashboard.",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0910))
                            .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = securityConfigJson,
                            color = SentinelGold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = ContractVaultExporter.saveSingleTextFile(
                            context,
                            "wallet_security_config.json",
                            securityConfigJson
                        )
                        Toast.makeText(context, "Saved wallet_security_config.json to Downloads!", Toast.LENGTH_LONG).show()
                        shareFileIntent(context, file, "application/json")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Download, contentDescription = "Save", modifier = Modifier.size(16.dp))
                        Text("Save Security JSON", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(securityConfigJson))
                        Toast.makeText(context, "Wallet Security JSON Copied!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy JSON", color = SentinelGold)
                }
            },
            containerColor = CosmicSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private fun shareFileIntent(context: android.content.Context, file: File, mimeType: String) {
    try {
        val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export ${file.name}"))
    } catch (_: Exception) {
        Toast.makeText(context, "File saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
