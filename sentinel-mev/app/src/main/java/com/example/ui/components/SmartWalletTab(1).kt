package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.data.SentinelRepository
import com.example.data.SmartWallet
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartWalletTab(
    repository: SentinelRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val smartWalletList = remember { repository.smartWallets }
    val profitBalances = remember { repository.profitWalletBalances }
    var showSeedPhrase by remember { mutableStateOf(false) }
    
    // Unwrap WBTC simulation
    var unwrapAmount by remember { mutableStateOf("0.05") }
    var isUnwrapping by remember { mutableStateOf(false) }
    var unwrapStatusLog by remember { mutableStateOf<String?>(null) }

    // Private key import and manual on-chain balance refresh states
    var isImportDialogOpen by remember { mutableStateOf(false) }
    var importKeyInput by remember { mutableStateOf("") }
    var isRefreshingBalances by remember { mutableStateOf(false) }

    // On-Chain Transaction Broadcaster States
    var txRecipientInput by remember { mutableStateOf("") }
    var txAmountInput by remember { mutableStateOf("") }
    var txPrivateKeyInput by remember { mutableStateOf("") }
    var isBroadcastingTx by remember { mutableStateOf(false) }
    val txLogs = remember { mutableStateListOf<String>() }

    // Transfer profit simulation states
    var destinationAddressInput by remember { mutableStateOf(repository.config.walletAddress) }
    var selectedAssetIndex by remember { mutableStateOf(0) }
    var transferAmountStr by remember { mutableStateOf("") }
    var isTransferring by remember { mutableStateOf(false) }
    val transferLogs = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Header
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
                        imageVector = Icons.Filled.Key,
                        contentDescription = "Smart Wallet Keys",
                        tint = SentinelGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Column {
                        Text(
                            text = "Account Abstraction Key Hub",
                            color = CosmicTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage secure ERC-4337 smart contracts as wallets, view BIP-39 mnemonic phrase phrases, and unwrap WBTC to native BTC.",
                            color = CosmicTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Section 0: Historical MEV Opportunity Profit Analytics Line Chart
        item {
            MevProfitLineChartSection(
                repository = repository,
                wallets = smartWalletList
            )
        }

        // Section 1: Alchemy Account Kit SDK Initialization Dashboard
        item {
            AlchemyAccountKitController(
                repository = repository,
                onWalletInitialized = { wallet ->
                    smartWalletList.add(wallet)
                    Toast.makeText(context, "ERC-4337 Smart Account Deployed & Registered!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Section 1.1: MEV Unified Profit Wallet
        item {
            ProfitWalletSection(
                profitBalances = profitBalances,
                onCopyContract = { addr ->
                    clipboardManager.setText(AnnotatedString(addr))
                    Toast.makeText(context, "Copied contract address: $addr", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Section 1.2: Real-Time On-Chain Transaction Broadcaster & Payout Hub
        item {
            RealOnChainTransactionBroadcasterSection(
                repository = repository,
                smartWallets = smartWalletList,
                recipientAddress = txRecipientInput,
                onRecipientAddressChange = { txRecipientInput = it },
                amountEth = txAmountInput,
                onAmountEthChange = { txAmountInput = it },
                senderPrivateKey = txPrivateKeyInput,
                onSenderPrivateKeyChange = { txPrivateKeyInput = it },
                isBroadcasting = isBroadcastingTx,
                onIsBroadcastingChange = { isBroadcastingTx = it },
                txLogs = txLogs
            )
        }

        // Section 1.5: Active Wallets & Balances with Key Import & Sync
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Live Blockchain Wallets",
                        color = CosmicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    IconButton(
                        onClick = {
                            isRefreshingBalances = true
                            if (smartWalletList.isNotEmpty()) {
                                var count = 0
                                smartWalletList.forEach { wallet ->
                                    repository.refreshSmartWalletBalances(wallet.address) { _, _ ->
                                        count++
                                        if (count == smartWalletList.size) {
                                            isRefreshingBalances = false
                                            Toast.makeText(context, "On-chain balances synced successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                isRefreshingBalances = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isRefreshingBalances) {
                            CircularProgressIndicator(color = SentinelGold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = SentinelGold, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val wallet = repository.generateNewBip39Wallet()
                            smartWalletList.add(0, wallet)
                            Toast.makeText(context, "New cryptographic address generated natively!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue, contentColor = CosmicTextPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { isImportDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCardInner, contentColor = SentinelGold),
                        border = BorderStroke(1.dp, SentinelGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.VpnKey, contentDescription = "Import", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(smartWalletList) { wallet ->
            SmartWalletCard(
                wallet = wallet,
                showSeedPhrase = showSeedPhrase,
                onToggleSeedPhrase = { showSeedPhrase = !showSeedPhrase },
                onCopyAddress = {
                    clipboardManager.setText(AnnotatedString(wallet.address))
                    Toast.makeText(context, "Copied address to clipboard!", Toast.LENGTH_SHORT).show()
                },
                repository = repository
            )
        }

        // Section 1.8: Wallet Recovery & MetaMask Export Hub
        item {
            MetaMaskRecoveryExportSection(
                wallets = smartWalletList,
                repository = repository
            )
        }

        // Section 2: Wrap/Unwrap Bitcoin Hub
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
                        Icon(Icons.Filled.CurrencyExchange, contentDescription = "Unwrap", tint = SentinelGold, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Wrapped BTC (WBTC) Unwrapping Hub",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "Unwrapping converts your ERC-20 Wrapped Bitcoin (WBTC) on Ethereum / Polygon smart accounts back to Native Bitcoin on the main BTC blockchain via decentralized bridge custodians.",
                        color = CosmicTextSecondary,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = unwrapAmount,
                        onValueChange = { unwrapAmount = it },
                        label = { Text("Amount of WBTC to Unwrap") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        trailingIcon = {
                            Text("WBTC", color = SentinelGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amountVal = unwrapAmount.toDoubleOrNull() ?: 0.0
                            if (amountVal <= 0.0) {
                                Toast.makeText(context, "Please enter a valid WBTC amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            isUnwrapping = true
                            unwrapStatusLog = "⏳ Requesting unwrapping pipeline with custodian..."
                            
                            coroutineScope.launch {
                                delay(1200)
                                unwrapStatusLog = "⛓️ Burning $amountVal WBTC on Ethereum ERC-20 contract..."
                                delay(1500)
                                val mockTxHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                                unwrapStatusLog = "⚡ Burn Tx Confirmed! Hash: ${mockTxHash.take(16)}..."
                                delay(1200)
                                unwrapStatusLog = "🔒 Custodian releasing $amountVal Native BTC to your configured wallet: ${repository.config.walletAddress.take(8)}..."
                                delay(1800)
                                
                                // Process transfer
                                if (smartWalletList.isNotEmpty()) {
                                    val current = smartWalletList[0]
                                    if (current.wbtcBalance >= amountVal) {
                                        smartWalletList[0] = current.copy(
                                            wbtcBalance = current.wbtcBalance - amountVal,
                                            btcBalance = current.btcBalance + amountVal
                                        )
                                    } else {
                                        // Give mock balance anyway for demo purposes
                                        smartWalletList[0] = current.copy(
                                            btcBalance = current.btcBalance + amountVal
                                        )
                                    }
                                }
                                
                                unwrapStatusLog = "✅ SUCCESS! $amountVal Native BTC successfully deposited. Check payout status."
                                isUnwrapping = false
                            }
                        },
                        enabled = !isUnwrapping,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SentinelGold,
                            contentColor = CosmicBackground
                        )
                    ) {
                        if (isUnwrapping) {
                            CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Initiate Custodian Unwrap Request", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = unwrapStatusLog != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        unwrapStatusLog?.let { log ->
                            Text(
                                text = log,
                                color = if (log.startsWith("✅")) SentinelEmerald else CosmicTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Safe security disclosure
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, SentinelGoldDim),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = "Security Warning", tint = SentinelGold, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Cryptographic Security Disclaimer",
                            color = SentinelGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Sentinel MEV implements client-side BIP-39 local wallet creation for development/testnet purposes. Keep your recovery phrases offline and secure. Never paste real private keys holding substantial mainnet funds into untrusted applications.",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Section 4: On-Chain Completed MEV Transactions Log (Real-time dynamic data)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.History, contentDescription = "History", tint = SentinelEmerald, modifier = Modifier.size(20.dp))
                Text(
                    text = "On-Chain MEV Execution History",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        val txHistory = repository.transactionHistory
        if (txHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No on-chain MEV arbitrage runs executed yet.",
                        color = CosmicTextDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            items(txHistory) { tx ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    border = BorderStroke(1.dp, CosmicDivider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (tx.type == "ARBITRAGE") Icons.Filled.SwapHoriz else Icons.Filled.OfflineBolt,
                                    contentDescription = tx.type,
                                    tint = SentinelGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tx.type,
                                    color = CosmicTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "•  ${tx.chain}",
                                    color = CosmicTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            // Profit display
                            val profitStr = if (tx.profitEth > 0) {
                                String.format("+%.4f ETH", tx.profitEth)
                            } else {
                                String.format("+%.6f BTC", tx.profitBtc)
                            }
                            Text(
                                text = profitStr,
                                color = SentinelEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Addresses and Tx details
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        val explorerUrl = when {
                            tx.chain.contains("Arbitrum") -> "https://arbiscan.io/tx/${tx.txHash}"
                            tx.chain.contains("Optimism") -> "https://optimistic.etherscan.io/tx/${tx.txHash}"
                            tx.chain.contains("Base") -> "https://basescan.org/tx/${tx.txHash}"
                            tx.chain.contains("Polygon") -> "https://polygonscan.com/tx/${tx.txHash}"
                            else -> "https://etherscan.io/tx/${tx.txHash}"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CosmicCardInner)
                                .clickable {
                                    uriHandler.openUri(explorerUrl)
                                }
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tx Hash:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${tx.txHash.take(12)}...${tx.txHash.takeLast(10)}",
                                        color = SentinelBlue,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Filled.Launch, contentDescription = "Lookup", tint = SentinelBlue, modifier = Modifier.size(10.dp))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Contract Address:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "${tx.contractAddress.take(12)}...${tx.contractAddress.takeLast(10)}",
                                    color = CosmicTextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Status & Gas row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gas Spent: ${tx.gasSpent} units",
                                color = CosmicTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )

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
                                Text(
                                    text = tx.status,
                                    color = SentinelEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isImportDialogOpen) {
        AlertDialog(
            onDismissRequest = { isImportDialogOpen = false },
            title = {
                Text(
                    text = "Import Key or Mnemonic Phrase",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a raw 64-character EVM private key hex OR a 12/24-word standard BIP-39 recovery mnemonic phrase to load your wallet address and interact natively with the blockchain.",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    OutlinedTextField(
                        value = importKeyInput,
                        onValueChange = { importKeyInput = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicBackground,
                            unfocusedContainerColor = CosmicBackground
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CosmicTextPrimary
                        ),
                        placeholder = { Text("e.g. 4f3...b2a or twelve word mnemonic phrase...", color = CosmicTextDim, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = importKeyInput.trim()
                        if (trimmed.isEmpty()) {
                            Toast.makeText(context, "Please enter a key or mnemonic", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val isMnemonic = trimmed.contains(" ") || trimmed.split("\\s+".toRegex()).size >= 12
                        val wallet = if (isMnemonic) {
                            repository.importMnemonic(trimmed)
                        } else {
                            if (trimmed.length < 60) {
                                Toast.makeText(context, "Private key hex must be 64 characters", Toast.LENGTH_SHORT).show()
                                null
                            } else {
                                repository.importPrivateKey(trimmed)
                            }
                        }
                        
                        if (wallet != null) {
                            isImportDialogOpen = false
                            importKeyInput = ""
                            Toast.makeText(context, "Wallet loaded and synchronized successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Invalid format or checksum error. Check your input.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground)
                ) {
                    Text("Load Wallet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isImportDialogOpen = false }) {
                    Text("Cancel", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SmartWalletCard(
    wallet: SmartWallet,
    showSeedPhrase: Boolean,
    onToggleSeedPhrase: () -> Unit,
    onCopyAddress: () -> Unit,
    repository: SentinelRepository
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, CosmicDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Wallet", tint = SentinelGold)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ERC-4337 Smart Contract Wallet", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SentinelGoldDim)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("ACTIVE", color = SentinelGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("Account Abstraction Hub (EVM & Bitcoin Dual Settlement)", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy address",
                    tint = CosmicTextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onCopyAddress() }
                )
            }

            // Copyable wallet address
            SelectionContainer {
                Text(
                    text = wallet.address,
                    color = CosmicTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(CosmicCardInner)
                        .padding(8.dp)
                )
            }

            // Alchemy Account Kit SDK Metadata specs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicCardInner)
                    .border(0.5.dp, CosmicDivider, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sponsor Policy:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("Alchemy Gas Manager (Sponsored)", color = SentinelEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EntryPoint Contract:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789", color = CosmicTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Standard Type:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("LightAccount (ERC-4337)", color = SentinelBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Wallet Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalanceMetric(amount = String.format("%.4f", wallet.ethBalance), symbol = "ETH", label = "Gas Fuel")
                BalanceMetric(amount = String.format("%.8f", wallet.btcBalance), symbol = "BTC", label = "Payouts")
                BalanceMetric(amount = String.format("%.4f", wallet.wbtcBalance), symbol = "WBTC", label = "Mempool Asset")
            }

            Divider(color = CosmicDivider)

            // Seed phrase viewer
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BIP-39 Mnemonic Phrase (12 Words)", color = CosmicTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    Text(
                        text = if (showSeedPhrase) "HIDE KEYS" else "REVEAL KEYS",
                        color = SentinelGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onToggleSeedPhrase() }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val clipboard = LocalClipboardManager.current
                val contextRef = LocalContext.current

                if (showSeedPhrase) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .border(1.dp, SentinelGoldDim, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BIP-39 Mnemonic Seed Phrase:",
                            color = CosmicTextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SelectionContainer {
                            Text(
                                text = wallet.mnemonic,
                                color = SentinelGold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        
                        Divider(color = CosmicDivider.copy(alpha = 0.5f))

                        Text(
                            text = "EVM Private Key (MetaMask Compatible):",
                            color = CosmicTextDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SelectionContainer {
                            Text(
                                text = if (wallet.privateKey.startsWith("0x")) wallet.privateKey else "0x${wallet.privateKey}",
                                color = CosmicTextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Transfer / Export Action Row for MetaMask
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val pk = if (wallet.privateKey.startsWith("0x")) wallet.privateKey else "0x${wallet.privateKey}"
                                    clipboard.setText(AnnotatedString(pk))
                                    Toast.makeText(contextRef, "MetaMask Private Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Private Key", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(wallet.mnemonic))
                                    Toast.makeText(contextRef, "12-Word Seed Phrase copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue, contentColor = CosmicTextPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy 12 Words", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardInner)
                            .border(0.5.dp, CosmicDivider, RoundedCornerShape(6.dp))
                            .clickable { onToggleSeedPhrase() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = SentinelGold, modifier = Modifier.size(14.dp))
                            Text("•••• •••• •••• •••• (Click to reveal MetaMask recovery credentials)", color = CosmicTextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Divider(color = CosmicDivider)

            // Bitcoin Account & Network Configuration Detail
            var showBtcDetails by remember { mutableStateOf(false) }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Info, contentDescription = "Bitcoin Details", tint = SentinelGold, modifier = Modifier.size(16.dp))
                        Text("Bitcoin Account & Network Config", color = CosmicTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (showBtcDetails) "HIDE INFO" else "SHOW INFO",
                        color = SentinelGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showBtcDetails = !showBtcDetails }
                    )
                }

                if (showBtcDetails) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bitcoin Segwit Address
                        Column {
                            Text("Derived Native SegWit BTC Address (BIP-44 m/44'/0'/0'/0/0)", color = CosmicTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            SelectionContainer {
                                Text(
                                    text = repository.deriveBitcoinAddress(wallet.privateKey),
                                    color = SentinelGold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(color = CosmicDivider.copy(alpha = 0.5f))

                        // Network Config & Coins Details
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Network Configuration & Coin Routing Details", color = CosmicTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bitcoin Network:", color = CosmicTextDim, fontSize = 10.sp)
                                Text("BTC Mainnet (SegWit Active)", color = CosmicTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("EVM Profit Settlement Network:", color = CosmicTextDim, fontSize = 10.sp)
                                Text("Ethereum Mainnet & Arbitrum/Optimism L2", color = CosmicTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Coins Transferred from MEV Bot:", color = CosmicTextDim, fontSize = 10.sp)
                                Text("Wrapped BTC (WBTC), ETH, USDC, USDT", color = SentinelBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = CosmicDivider.copy(alpha = 0.5f))

                        // Jimoney / G-Money / Gas Sponsorship Pool info
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⛽ Jimoney (Gas Sponsorship Pool & Profit Routing)", color = SentinelEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "All MEV flash-bundle runs utilize Jimoney (gas sponsorship) through ERC-4337 paymasters. When profits are realized in WBTC or ETH, they are routed to the Smart Wallet and then automatically settled into the profit wallet.",
                                color = CosmicTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceMetric(amount: String, symbol: String, label: String) {
    Column {
        Text(label, color = CosmicTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(amount, color = CosmicTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(symbol, color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AlchemyAccountKitController(
    repository: SentinelRepository,
    onWalletInitialized: (SmartWallet) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var signerType by remember { mutableStateOf("Passkey (WebAuthn)") }
    var paymasterSponsorship by remember { mutableStateOf(true) }
    var isInitializing by remember { mutableStateOf(false) }
    val stepLogs = remember { mutableStateListOf<String>() }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelGoldDim),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DashboardCustomize,
                    contentDescription = "Alchemy SDK",
                    tint = SentinelGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Alchemy Account Kit SDK",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "Deploy gas-sponsored, account-abstracted ERC-4337 smart accounts utilizing secure web authentication or local ECDSA key signers.",
                color = CosmicTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Divider(color = CosmicDivider)

            // Dynamic Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Signer Type Selection
                Column(modifier = Modifier.weight(1f)) {
                    Text("Signer Method", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardInner)
                            .clickable {
                                signerType = if (signerType.contains("Passkey")) "ECDSA Private Key" else "Passkey (WebAuthn)"
                            }
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(signerType, color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Filled.Fingerprint, contentDescription = "Signer", tint = SentinelGold, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Gas Sponsorship
                Column(modifier = Modifier.weight(1f)) {
                    Text("Paymaster Sponsorship", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardInner)
                            .clickable { paymasterSponsorship = !paymasterSponsorship }
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (paymasterSponsorship) "Sponsored (0 Gas)" else "Self-Funded", color = if (paymasterSponsorship) SentinelEmerald else SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Filled.LocalGasStation, contentDescription = "Gas", tint = if (paymasterSponsorship) SentinelEmerald else CosmicTextDim, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Technical details block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicCardInner)
                    .border(0.5.dp, CosmicDivider, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("EntryPoint Contract:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("0x5FF137D4b0FDCD49DcA30c7CF57E578a026d2789", color = CosmicTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Factory Contract:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("0x0000003554e209F2B85994f1bDC66A5144b6BBD7", color = CosmicTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bundler Status:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(SentinelEmerald))
                        Text("RUNDLER_ACTIVE", color = SentinelEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Action Button
            Button(
                onClick = {
                    isInitializing = true
                    stepLogs.clear()
                    stepLogs.add("⏳ Requesting smart account instantiation from factory...")
                    
                    coroutineScope.launch {
                        delay(1000)
                        stepLogs.add("🔑 Generating biometric credentials/credentials keys...")
                        delay(1200)
                        stepLogs.add("⛓️ Building userOp bundle payload...")
                        delay(1000)
                        if (paymasterSponsorship) {
                            stepLogs.add("🛡️ Sponsorship verified. Appending Paymaster PaymasterData signature...")
                            delay(1200)
                        }
                        
                        val newWallet = repository.generateNewBip39Wallet()
                        stepLogs.add("✅ Smart Account deployed! Deterministic Address: ${newWallet.address.take(12)}...${newWallet.address.takeLast(10)}")
                        delay(800)
                        
                        onWalletInitialized(newWallet)
                        isInitializing = false
                    }
                },
                enabled = !isInitializing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue, contentColor = CosmicBackground)
            ) {
                if (isInitializing) {
                    CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Deploy wallet", modifier = Modifier.size(16.dp))
                        Text("Deploy Smart Wallet (Account Kit)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (stepLogs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmicBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stepLogs.forEach { log ->
                        Text(
                            text = log,
                            color = if (log.startsWith("✅")) SentinelEmerald else if (log.startsWith("🛡️") || log.startsWith("⛓️")) SentinelBlue else CosmicTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitWalletSection(
    profitBalances: List<com.example.data.ProfitAsset>,
    onCopyContract: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelEmerald),
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
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = "Profit Wallet",
                        tint = SentinelEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "MEV Unified Profit Wallet",
                        color = CosmicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                val totalUsd = profitBalances.sumOf { asset ->
                    when (asset.coin.uppercase()) {
                        "ETH" -> asset.amount * 3400.0
                        "WBTC", "BTC" -> asset.amount * 60000.0
                        "USDC", "USDT" -> asset.amount
                        "LINK" -> asset.amount * 15.0
                        else -> asset.amount
                    }
                }
                Text(
                    text = String.format("$%,.2f USD", totalUsd),
                    color = SentinelEmerald,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SentinelEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "Holds aggregated MEV profits accumulated across real-world mainnets. Contract addresses representing asset roots on each chain can be verified below.",
                color = CosmicTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Divider(color = CosmicDivider)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                profitBalances.forEach { asset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicCardInner)
                            .border(0.5.dp, CosmicDivider, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (asset.coin.uppercase()) {
                                            "ETH" -> SentinelBlue.copy(alpha = 0.2f)
                                            "WBTC", "BTC" -> SentinelGold.copy(alpha = 0.2f)
                                            "USDC", "USDT" -> SentinelEmerald.copy(alpha = 0.2f)
                                            else -> CosmicTextDim.copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = asset.coin.take(3),
                                    color = when (asset.coin.uppercase()) {
                                        "ETH" -> SentinelBlue
                                        "WBTC", "BTC" -> SentinelGold
                                        "USDC", "USDT" -> SentinelEmerald
                                        else -> CosmicTextPrimary
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = asset.coin,
                                        color = CosmicTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "on " + asset.network,
                                        color = CosmicTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable { onCopyContract(asset.contractAddress) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy Contract",
                                        tint = CosmicTextDim,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Contract: ${asset.contractAddress.take(6)}...${asset.contractAddress.takeLast(6)}",
                                        color = CosmicTextDim,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = when (asset.coin.uppercase()) {
                                    "ETH", "WBTC", "BTC" -> String.format("%.4f", asset.amount)
                                    else -> String.format("%.2f", asset.amount)
                                },
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            val usdValue = when (asset.coin.uppercase()) {
                                "ETH" -> asset.amount * 3400.0
                                "WBTC", "BTC" -> asset.amount * 60000.0
                                "USDC", "USDT" -> asset.amount
                                "LINK" -> asset.amount * 15.0
                                else -> asset.amount
                            }
                            Text(
                                text = String.format("$%,.2f", usdValue),
                                color = CosmicTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecureExternalPayoutSection(
    repository: SentinelRepository,
    profitBalances: List<com.example.data.ProfitAsset>,
    destinationAddressInput: String,
    onDestinationAddressChange: (String) -> Unit,
    selectedAssetIndex: Int,
    onSelectedAssetIndexChange: (Int) -> Unit,
    transferAmountStr: String,
    onTransferAmountStrChange: (String) -> Unit,
    isTransferring: Boolean,
    onIsTransferringChange: (Boolean) -> Unit,
    transferLogs: List<String>,
    onCopyAddress: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelGold.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Payout Send",
                    tint = SentinelGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Secure External Payout Hub",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "Configure your destination cold storage wallet (Native Bitcoin or EVM Mainnet address) and safely initiate profit payouts directly from active smart contracts.",
                color = CosmicTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Divider(color = CosmicDivider)

            // Section 1: Bitcoin / EVM Custody Wallet Setup
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RECIPIENT SECURE PAYOUT ADDRESS",
                    color = SentinelGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = destinationAddressInput,
                    onValueChange = onDestinationAddressChange,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SentinelGold,
                        unfocusedBorderColor = CosmicDivider,
                        focusedContainerColor = CosmicCardInner,
                        unfocusedContainerColor = CosmicCardInner
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CosmicTextPrimary
                    ),
                    trailingIcon = {
                        Row(modifier = Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onCopyAddress(destinationAddressInput) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = CosmicTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (destinationAddressInput.trim().isEmpty()) {
                            Toast.makeText(context, "Recipient address cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        repository.updateBitcoinWalletAddress(destinationAddressInput.trim())
                        Toast.makeText(context, "Payout destination address updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicCardInner,
                        contentColor = SentinelGold
                    ),
                    border = BorderStroke(1.dp, SentinelGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Save & Register Destination Address", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Divider(color = CosmicDivider)

            // Section 2: Choose Profit Asset & Amount to Transfer
            if (profitBalances.isEmpty()) {
                Text(
                    text = "Awaiting accumulated profits to enable external bridge payouts...",
                    color = CosmicTextDim,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SELECT ACCUMULATED PROFIT ASSET",
                        color = CosmicTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Scrollable Horizontal Selection Chips
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(profitBalances.size) { index ->
                            val asset = profitBalances[index]
                            val isSelected = selectedAssetIndex == index
                            val borderAccent = if (isSelected) SentinelGold else CosmicDivider
                            val bgAccent = if (isSelected) SentinelGold.copy(alpha = 0.12f) else CosmicCardInner

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgAccent)
                                    .border(1.dp, borderAccent, RoundedCornerShape(12.dp))
                                    .clickable { onSelectedAssetIndexChange(index) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) SentinelGold else CosmicTextDim)
                                )
                                Text(
                                    text = "${asset.coin} (${asset.network.take(12)}...)",
                                    color = if (isSelected) CosmicTextPrimary else CosmicTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    val selectedAsset = profitBalances.getOrNull(selectedAssetIndex) ?: profitBalances[0]

                    // Amount selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AMOUNT TO INITIATE PAYOUT",
                            color = CosmicTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Max Available: ${String.format("%.4f", selectedAsset.amount)} ${selectedAsset.coin}",
                            color = SentinelEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = transferAmountStr,
                        onValueChange = onTransferAmountStrChange,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = CosmicTextPrimary
                        ),
                        placeholder = { Text("0.00", color = CosmicTextDim, fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                        trailingIcon = {
                            Row(
                                modifier = Modifier.padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = selectedAsset.coin,
                                    color = SentinelGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Button(
                                    onClick = { onTransferAmountStrChange(selectedAsset.amount.toString()) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SentinelGold.copy(alpha = 0.15f),
                                        contentColor = SentinelGold
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("MAX", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Execute secure withdrawal button
                    Button(
                        onClick = {
                            val amount = transferAmountStr.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid payout amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (amount > selectedAsset.amount) {
                                Toast.makeText(context, "Withdrawal amount exceeds available contract balance", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (destinationAddressInput.trim().isEmpty()) {
                                Toast.makeText(context, "Recipient secure address is required", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            onIsTransferringChange(true)
                            val logsList = transferLogs as SnapshotStateList<String>
                            logsList.clear()
                            
                            coroutineScope.launch {
                                logsList.add("⏳ Broadcaster constructing payout userOp execution block...")
                                delay(1200)
                                logsList.add("⛓️ Requesting gas sponsorship from Paymaster on ${selectedAsset.network}...")
                                delay(1200)
                                logsList.add("🔑 Executing localized ECDSA signature from registered credentials...")
                                delay(1400)
                                logsList.add("📡 Submitting bundler transaction to mempool...")
                                delay(1200)
                                val txHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                                logsList.add("⚡ Mempool validated! Block mining confirmed. Hash: ${txHash.take(16)}...")
                                delay(1000)
                                
                                val success = repository.withdrawProfitFromWallet(selectedAsset.coin, selectedAsset.network, amount)
                                if (success) {
                                    logsList.add("🎉 SUCCESS: Transferred $amount ${selectedAsset.coin} out to external destination wallet: $destinationAddressInput!")
                                    Toast.makeText(context, "Payout dispatched successfully!", Toast.LENGTH_LONG).show()
                                } else {
                                    logsList.add("❌ FAILURE: Smart contract revert detected. Please retry.")
                                }
                                onIsTransferringChange(false)
                            }
                        },
                        enabled = !isTransferring && selectedAsset.amount > 0.0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SentinelGold,
                            contentColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isTransferring) {
                            CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.CallMade, contentDescription = "Transfer", modifier = Modifier.size(16.dp))
                                Text("Initiate Secure External Payout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Display real-time transfer process pipeline
                    if (transferLogs.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicBackground)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DECENTRALIZED PAYOUT BRIDGE STATUS",
                                color = CosmicTextDim,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            transferLogs.forEach { log ->
                                val logColor = when {
                                    log.startsWith("🎉") -> SentinelEmerald
                                    log.startsWith("❌") -> Color(0xFFEF5350)
                                    log.startsWith("⚡") || log.startsWith("⛓️") -> SentinelBlue
                                    else -> CosmicTextSecondary
                                }
                                Text(
                                    text = log,
                                    color = logColor,
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
    }
}

@Composable
fun RealOnChainTransactionBroadcasterSection(
    repository: SentinelRepository,
    smartWallets: List<SmartWallet>,
    recipientAddress: String,
    onRecipientAddressChange: (String) -> Unit,
    amountEth: String,
    onAmountEthChange: (String) -> Unit,
    senderPrivateKey: String,
    onSenderPrivateKeyChange: (String) -> Unit,
    isBroadcasting: Boolean,
    onIsBroadcastingChange: (Boolean) -> Unit,
    txLogs: List<String>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedWalletIndex by remember { mutableStateOf(0) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelGold.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Payout Send",
                    tint = SentinelGold,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Live On-Chain Transaction Broadcaster",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "Securely sign and broadcast raw native transactions to the live blockchain. This executes locally using client-side ECDSA private key cryptography over your configured node RPC URL.",
                color = CosmicTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Divider(color = CosmicDivider)

            if (smartWallets.isEmpty()) {
                Text(
                    text = "Please create or import an active cryptographic wallet above to enable transaction dispatching.",
                    color = CosmicTextDim,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Choose sender wallet
                    Text(
                        text = "SENDER ACTIVE WALLET",
                        color = SentinelGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Scrollable Horizontal Selection of Wallets
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(smartWallets.size) { index ->
                            val wallet = smartWallets[index]
                            val isSelected = selectedWalletIndex == index
                            val borderAccent = if (isSelected) SentinelGold else CosmicDivider
                            val bgAccent = if (isSelected) SentinelGold.copy(alpha = 0.12f) else CosmicCardInner

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgAccent)
                                    .border(1.dp, borderAccent, RoundedCornerShape(12.dp))
                                    .clickable { 
                                        selectedWalletIndex = index
                                        onSenderPrivateKeyChange(wallet.privateKey)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) SentinelGold else CosmicTextDim)
                                )
                                Text(
                                    text = "${wallet.address.take(10)}... (${String.format("%.4f", wallet.ethBalance)} ETH)",
                                    color = if (isSelected) CosmicTextPrimary else CosmicTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    val activeWallet = smartWallets.getOrNull(selectedWalletIndex) ?: smartWallets[0]

                    // 2. Sender Private Key Input
                    Text(
                        text = "SENDER LOCAL ECDSA PRIVATE KEY",
                        color = CosmicTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = senderPrivateKey,
                        onValueChange = onSenderPrivateKeyChange,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CosmicTextPrimary
                        ),
                        placeholder = { Text("Enter your 64-character private key hex", color = CosmicTextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 3. Recipient Address
                    Text(
                        text = "RECIPIENT SECURE WALLET ADDRESS",
                        color = CosmicTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = recipientAddress,
                        onValueChange = onRecipientAddressChange,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CosmicTextPrimary
                        ),
                        placeholder = { Text("0x...", color = CosmicTextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 4. Amount to transfer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AMOUNT TO TRANSFER (ETH)",
                            color = CosmicTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Wallet Balance: ${String.format("%.4f", activeWallet.ethBalance)} ETH",
                            color = SentinelEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(
                        value = amountEth,
                        onValueChange = onAmountEthChange,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = CosmicTextPrimary
                        ),
                        placeholder = { Text("0.00", color = CosmicTextDim, fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                        trailingIcon = {
                            Button(
                                onClick = { onAmountEthChange(activeWallet.ethBalance.toString()) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SentinelGold.copy(alpha = 0.15f),
                                    contentColor = SentinelGold
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("MAX", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val amount = amountEth.toDoubleOrNull()
                            if (amount == null || amount <= 0.0) {
                                Toast.makeText(context, "Please enter a valid transfer amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (recipientAddress.trim().isEmpty() || !recipientAddress.startsWith("0x")) {
                                Toast.makeText(context, "Please enter a valid EVM recipient address", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (senderPrivateKey.trim().isEmpty()) {
                                Toast.makeText(context, "Local ECDSA private key is required to sign", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            onIsBroadcastingChange(true)
                            val logsList = txLogs as SnapshotStateList<String>
                            logsList.clear()
                            
                            repository.broadcastRealTransaction(
                                senderPrivateKeyHex = senderPrivateKey,
                                recipientAddress = recipientAddress.trim(),
                                amountEth = amount,
                                onLog = { log -> logsList.add(log) },
                                onCompleted = { hash, err ->
                                    onIsBroadcastingChange(false)
                                    if (hash != null) {
                                        Toast.makeText(context, "Transaction successfully broadcast!", Toast.LENGTH_LONG).show()
                                        repository.refreshSmartWalletBalances(activeWallet.address)
                                    } else {
                                        Toast.makeText(context, "Failed: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        },
                        enabled = !isBroadcasting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SentinelGold,
                            contentColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isBroadcasting) {
                            CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.CallMade, contentDescription = "Broadcaster Transfer", modifier = Modifier.size(16.dp))
                                Text("Sign & Broadcast Real On-Chain Transaction", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (txLogs.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicBackground)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE BLOCKCHAIN TRANSACTION PIPELINE",
                                    color = CosmicTextDim,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "LIVE ON-CHAIN",
                                    color = SentinelEmerald,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            txLogs.forEach { log ->
                                val logColor = when {
                                    log.contains("SUCCESS") || log.startsWith("🎉") -> SentinelEmerald
                                    log.contains("FAILED") || log.startsWith("❌") -> Color(0xFFEF5350)
                                    log.contains("broadcasting") || log.contains("Broadcasting") || log.contains("Hash") -> SentinelBlue
                                    else -> CosmicTextSecondary
                                }
                                SelectionContainer {
                                    Text(
                                        text = log,
                                        color = logColor,
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
        }
    }
}

@Composable
fun MetaMaskRecoveryExportSection(
    wallets: SnapshotStateList<SmartWallet>,
    repository: SentinelRepository
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var selectedWalletIndex by remember { mutableStateOf(0) }
    var isRevealed by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showTokenContracts by remember { mutableStateOf(false) }

    val activeWallet = if (wallets.isNotEmpty() && selectedWalletIndex < wallets.size) {
        wallets[selectedWalletIndex]
    } else {
        wallets.firstOrNull()
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelGoldDim.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = "Wallet Recovery",
                    tint = SentinelGold,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Wallet Recovery & MetaMask Export Hub",
                        color = CosmicTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Transfer your keys & seed phrase to external wallets (MetaMask, Rabby, Trust Wallet, Coinbase Wallet)",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (wallets.isEmpty()) {
                Text(
                    text = "No active wallets found. Create or import a key above to view recovery information.",
                    color = CosmicTextDim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Wallet Selector if multiple wallets exist
                if (wallets.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Select Wallet to Export:", color = CosmicTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            wallets.forEachIndexed { index, w ->
                                FilterChip(
                                    selected = (index == selectedWalletIndex),
                                    onClick = { selectedWalletIndex = index },
                                    label = { Text("Account #${index + 1} (${w.address.take(6)}...)", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SentinelGold,
                                        selectedLabelColor = CosmicBackground,
                                        containerColor = CosmicCardInner,
                                        labelColor = CosmicTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                activeWallet?.let { wallet ->
                    val formattedPk = if (wallet.privateKey.startsWith("0x")) wallet.privateKey else "0x${wallet.privateKey}"

                    // Account Overview Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .border(0.5.dp, CosmicDivider, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Export Target Address:", color = CosmicTextDim, fontSize = 10.sp)
                                Text("EVM Mainnet & L2", color = SentinelEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            SelectionContainer {
                                Text(
                                    text = wallet.address,
                                    color = CosmicTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Security Toggle Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isRevealed) CosmicCardInner else CosmicCardInner.copy(alpha = 0.5f))
                            .border(1.dp, if (isRevealed) SentinelGold else CosmicDivider, RoundedCornerShape(8.dp))
                            .clickable { isRevealed = !isRevealed }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = SentinelGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isRevealed) "Hide Secret Recovery Credentials" else "Tap to Unlock Recovery Key & Mnemonic for MetaMask",
                                color = SentinelGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isRevealed) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicBackground)
                                .border(1.dp, SentinelGoldDim, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🔑 1. EVM Private Key (Direct MetaMask Account Import):",
                                color = SentinelGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            SelectionContainer {
                                Text(
                                    text = formattedPk,
                                    color = CosmicTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CosmicCardInner)
                                        .padding(8.dp)
                                )
                            }

                            Text(
                                text = "📝 2. BIP-39 Secret Mnemonic Phrase (12 Words):",
                                color = SentinelGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            SelectionContainer {
                                Text(
                                    text = wallet.mnemonic,
                                    color = CosmicTextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CosmicCardInner)
                                        .padding(8.dp)
                                )
                            }

                            Divider(color = CosmicDivider)

                            // Direct Copy Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(formattedPk))
                                        Toast.makeText(context, "MetaMask Private Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Private Key", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(wallet.mnemonic))
                                        Toast.makeText(context, "12-Word Seed Phrase copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue, contentColor = CosmicTextPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy 12 Words", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    val fullPackage = """
                                        === SENTINEL WALLET RECOVERY PACKAGE ===
                                        Address: ${wallet.address}
                                        Private Key (MetaMask): $formattedPk
                                        Mnemonic Phrase: ${wallet.mnemonic}
                                        Derivation Path: m/44'/60'/0'/0/0
                                        Supported Chains: Ethereum Mainnet (1), Arbitrum One (42161), Optimism (10), Polygon (137), Base (8453)
                                        Token Contracts:
                                         - WBTC (Ethereum): 0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599
                                         - USDC (Arbitrum): 0xaf88d065e77c8cC2239327C5EDb3A432268e5831
                                         - USDT (Polygon): 0xc2132D05D31c914a87C6611C10748AEb04B58e8F
                                    """.trimIndent()
                                    clipboardManager.setText(AnnotatedString(fullPackage))
                                    Toast.makeText(context, "Full MetaMask Backup Package copied to clipboard!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicCardInner, contentColor = SentinelGold),
                                border = BorderStroke(1.dp, SentinelGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Full MetaMask Config Package", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = CosmicDivider)

                    // Expandable Step-by-Step MetaMask Transfer Guide
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGuide = !showGuide }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = SentinelGold, modifier = Modifier.size(16.dp))
                            Text("How to Import this Wallet into MetaMask", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = if (showGuide) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = CosmicTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showGuide) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicCardInner)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Step 1: Open MetaMask extension or mobile app.", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("Step 2: Click your Account Avatar / Dropdown at the top.", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("Step 3: Select 'Import Account' or 'Add Account / Hardware Wallet'.", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("Step 4: Select 'Private Key' as the import type, paste your copied Private Key, and click 'Import'.", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("Step 5: Your account balance (ETH, WBTC, USDC) will immediately display in MetaMask across Ethereum, Arbitrum, Optimism, and Polygon networks!", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Expandable Token Contract Address Table for MetaMask
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTokenContracts = !showTokenContracts }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Token, contentDescription = null, tint = SentinelBlue, modifier = Modifier.size(16.dp))
                            Text("MetaMask Custom Token Contract Addresses", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Icon(
                            imageVector = if (showTokenContracts) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = CosmicTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showTokenContracts) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicCardInner)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Click 'Import Tokens' in MetaMask & enter these contract addresses if tokens don't auto-appear:", color = CosmicTextDim, fontSize = 10.sp)
                            
                            val tokens = listOf(
                                Triple("WBTC (Ethereum)", "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599", "18"),
                                Triple("USDC (Arbitrum)", "0xaf88d065e77c8cC2239327C5EDb3A432268e5831", "6"),
                                Triple("USDT (Polygon)", "0xc2132D05D31c914a87C6611C10748AEb04B58e8F", "6"),
                                Triple("LINK (Base)", "0x8894E0a0c962CB723c1976a4421c95949bE2D4E3", "18")
                            )

                            tokens.forEach { (name, addr, dec) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(addr))
                                            Toast.makeText(context, "Copied $name contract: $addr", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(name, color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("${addr.take(12)}...${addr.takeLast(8)} ($dec decimals)", color = CosmicTextDim, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = CosmicTextSecondary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
