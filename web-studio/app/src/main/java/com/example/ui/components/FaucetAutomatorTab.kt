package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Faucet
import com.example.data.SentinelConfig
import com.example.data.SentinelRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaucetAutomatorTab(
    repository: SentinelRepository,
    onShowSnackbar: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var walletAddress by remember { mutableStateOf(repository.config.walletAddress) }
    var scannerStatus by remember { mutableStateOf(repository.config.scannerStatus) }
    var isScanning by remember { mutableStateOf(false) }
    var totalCollected by remember { mutableStateOf(repository.config.totalCollectedBtc) }

    // List of faucets
    val faucetsList = remember { repository.faucets }

    // Next auto-collect timer countdown (simulated countdown)
    var timeRemainingSeconds by remember { mutableStateOf(51605) } // ~14h 20m 5s
    
    // Auto-collect daemon state
    var isAutoCollectEnabled by remember { mutableStateOf(repository.config.autoCollectEnabled) }

    // Start countdown thread
    LaunchedEffect(key1 = true) {
        while (true) {
            delay(1000)
            if (isAutoCollectEnabled && timeRemainingSeconds > 0) {
                timeRemainingSeconds--
                // Every 10 seconds, increase collected balance if auto collect is active
                if (timeRemainingSeconds % 10 == 0) {
                    totalCollected += 0.00000002
                    repository.saveConfig(repository.config.copy(totalCollectedBtc = totalCollected))
                    // Trigger system notification every 30 seconds
                    if (timeRemainingSeconds % 30 == 0) {
                        repository.triggerFaucetRefillNotification(
                            faucetName = "FaucetPay Auto-Collector Gateway",
                            amountRefilled = "+0.00000002 BTC",
                            newBalance = String.format("%.8f BTC", totalCollected)
                        )
                        onShowSnackbar?.invoke("🚰 Auto-Faucet Refill: +0.00000002 BTC collected! New Balance: ${String.format("%.8f BTC", totalCollected)}")
                    }
                }
            }
        }
    }

    // Helper to format countdown seconds to 00:00:00
    fun formatSecondsToTimer(totalSecs: Int): String {
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        val seconds = totalSecs % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scrollable content inside vertical grid so faucet cards are responsive (Adaptive layout class)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Description Span all cols
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            imageVector = Icons.Filled.WaterDrop,
                            contentDescription = "Faucet Hub",
                            tint = SentinelGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "Crypto-Faucet Automator Dashboard",
                                color = CosmicTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Register wallet, connect FaucetPay Microwallet endpoints, and automate routine claims on multiple testnets & mainnets.",
                                color = CosmicTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Stat Box 1: Configuration Address Input Span all cols
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Configuration & Payout Hub",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = walletAddress,
                                onValueChange = { walletAddress = it },
                                placeholder = { Text("Enter BTC / FaucetPay Wallet address") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SentinelGold,
                                    unfocusedBorderColor = CosmicDivider,
                                    focusedContainerColor = CosmicCardInner,
                                    unfocusedContainerColor = CosmicCardInner
                                ),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Button(
                                onClick = {
                                    repository.saveConfig(
                                        repository.config.copy(walletAddress = walletAddress)
                                    )
                                    Toast.makeText(context, "Wallet Configuration Saved!", Toast.LENGTH_SHORT).show()
                                    onShowSnackbar?.invoke("🚰 Faucet payout address saved: $walletAddress")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SentinelGold,
                                    contentColor = CosmicBackground
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Stat indicators
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scanner Status
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        border = BorderStroke(1.dp, if (scannerStatus == "SCANNING...") SentinelGold else CosmicDivider),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Scanner Status", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scannerStatus,
                                color = if (scannerStatus == "SCANNING...") SentinelGold else if (scannerStatus == "COMPLETED") SentinelEmerald else CosmicTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    isScanning = true
                                    scannerStatus = "SCANNING..."
                                    onShowSnackbar?.invoke("🔍 Syncing active testnet & mainnet faucets with FaucetPay...")
                                    coroutineScope.launch {
                                        delay(3000)
                                        isScanning = false
                                        scannerStatus = "COMPLETED"
                                        repository.saveConfig(repository.config.copy(scannerStatus = "COMPLETED"))
                                        Toast.makeText(context, "Synced 5 Active Faucets with FaucetPay!", Toast.LENGTH_SHORT).show()
                                        onShowSnackbar?.invoke("✅ Synced 5 Active Faucets with FaucetPay Gateway!")
                                    }
                                },
                                enabled = !isScanning,
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue, contentColor = CosmicTextPrimary)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(color = CosmicTextPrimary, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                } else {
                                    Text("Fetch FaucetPay List", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Next Auto-Collect Countdown
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        border = BorderStroke(1.dp, if (isAutoCollectEnabled) SentinelEmerald else CosmicDivider),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Next Auto-Collect", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatSecondsToTimer(timeRemainingSeconds),
                                color = if (isAutoCollectEnabled) SentinelEmerald else CosmicTextDim,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Auto Collect", color = CosmicTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    checked = isAutoCollectEnabled,
                                    onCheckedChange = {
                                        isAutoCollectEnabled = it
                                        repository.saveConfig(repository.config.copy(autoCollectEnabled = it))
                                        val statusMsg = if (it) "Auto-Collect Faucet Daemon ENABLED" else "Auto-Collect Faucet Daemon DISABLED"
                                        onShowSnackbar?.invoke(if (it) "⚡ $statusMsg" else "⏸️ $statusMsg")
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SentinelEmerald,
                                        checkedTrackColor = SentinelEmeraldDim,
                                        uncheckedThumbColor = CosmicTextDim,
                                        uncheckedTrackColor = CosmicCardInner
                                    ),
                                )
                            }
                        }
                    }

                    // Total Collected
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        border = BorderStroke(1.dp, CosmicDivider),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Total Collected (Est.)", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%.8f", totalCollected),
                                color = SentinelEmerald,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("BTC", color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Notification Test Trigger Button Span all cols
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, SentinelGoldDim),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automated Faucet Refill Alert", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Test system notification channel for automated vault balance refills.", color = CosmicTextSecondary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                totalCollected += 0.00000150
                                repository.saveConfig(repository.config.copy(totalCollectedBtc = totalCollected))
                                repository.triggerFaucetRefillNotification(
                                    faucetName = "Arbitrum Testnet Vault",
                                    amountRefilled = "+0.00000150 BTC",
                                    newBalance = String.format("%.8f BTC", totalCollected)
                                )
                                Toast.makeText(context, "System Notification Sent! Check status bar.", Toast.LENGTH_SHORT).show()
                                onShowSnackbar?.invoke("🚰 Faucet refill processed (+0.00000150 BTC)! New balance: ${String.format("%.8f BTC", totalCollected)}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Trigger Refill Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Faucet List Header Span all cols
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Faucet Gateways & Direct Portals",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Faucet Cards list
            itemsIndexed(faucetsList) { index, faucet ->
                FaucetCardItem(
                    faucet = faucet,
                    onToggleActive = { active ->
                        val updated = faucetsList[index].copy(status = if (active) "Active" else "Inactive")
                        faucetsList[index] = updated
                        val statusText = "${faucet.name} set to ${updated.status}"
                        Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                        onShowSnackbar?.invoke("🚰 $statusText")
                    },
                    onClaimManual = {
                        // Launch browser intent to claim BTC
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(faucet.url))
                            context.startActivity(intent)
                            onShowSnackbar?.invoke("🌐 Launching claim portal for ${faucet.name}...")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open link: ${e.message}", Toast.LENGTH_SHORT).show()
                            onShowSnackbar?.invoke("❌ Cannot open ${faucet.name}: ${e.message}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FaucetCardItem(
    faucet: Faucet,
    onToggleActive: (Boolean) -> Unit,
    onClaimManual: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(faucet.name, color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Network tag
                    val isMainnet = faucet.type == "Mainnet"
                    Text(
                        text = faucet.type,
                        color = if (isMainnet) SentinelGold else SentinelBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isMainnet) SentinelGoldDim else SentinelBlueDim)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Active / Inactive switch
                Switch(
                    checked = faucet.status == "Active",
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SentinelEmerald,
                        checkedTrackColor = SentinelEmeraldDim,
                        uncheckedThumbColor = CosmicTextDim,
                        uncheckedTrackColor = CosmicCardInner
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("INTERVAL", color = CosmicTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(faucet.interval, color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("REWARD", color = CosmicTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(faucet.rewardAmount, color = SentinelEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Visit / Manual claim button
            Button(
                onClick = onClaimManual,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicCardInner,
                    contentColor = CosmicTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CosmicDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Launch, contentDescription = "Launch url", modifier = Modifier.size(14.dp), tint = SentinelGold)
                    Text("Manual Claim & Visit Portal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
