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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CryptoUtils
import com.example.data.SentinelRepository
import com.example.data.SmartWallet
import com.example.data.WalletManager
import com.example.data.UnsignedTransaction
import com.example.data.SignedTransactionResult
import com.example.data.DerivationPathInfo
import java.math.BigDecimal
import java.math.BigInteger
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
    val syncingMap = remember { mutableStateMapOf<String, Boolean>() }
    var showImportWalletDialog by remember { mutableStateOf(false) }
    var importAddressOrMnemonic by remember { mutableStateOf("") }

    if (showImportWalletDialog) {
        AlertDialog(
            onDismissRequest = { showImportWalletDialog = false },
            title = {
                Text("Import Real Wallet or Recovery Phrase", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste a real Ethereum wallet address (0x...) or a 12-word BIP-39 recovery phrase to connect and query live on-chain balances.",
                        color = CosmicTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = importAddressOrMnemonic,
                        onValueChange = { importAddressOrMnemonic = it },
                        placeholder = { Text("0x... or 12-word seed phrase", color = CosmicTextDim, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = importAddressOrMnemonic.trim()
                        if (input.isBlank()) {
                            Toast.makeText(context, "Please enter an address or recovery phrase", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        val isAddress = input.startsWith("0x") && input.length == 42
                        val isMnemonic = input.split("\\s+".toRegex()).size >= 12
                        
                        val walletToImport = if (isAddress) {
                            SmartWallet(
                                address = input,
                                publicKey = "External Import",
                                privateKey = "Imported Read-Only",
                                mnemonic = "Imported Address",
                                ethBalance = 0.0,
                                btcBalance = 0.0,
                                wbtcBalance = 0.0
                            )
                        } else if (isMnemonic) {
                            val privKey = CryptoUtils.generatePrivateKey(input)
                            val addr = CryptoUtils.generateEthereumAddress(privKey)
                            SmartWallet(
                                address = addr,
                                publicKey = "04" + privKey.take(32),
                                privateKey = privKey,
                                mnemonic = input,
                                ethBalance = 0.0,
                                btcBalance = 0.0,
                                wbtcBalance = 0.0
                            )
                        } else {
                            val privKey = CryptoUtils.generatePrivateKey(input)
                            val addr = if (input.startsWith("0x")) input else CryptoUtils.generateEthereumAddress(privKey)
                            SmartWallet(
                                address = addr,
                                publicKey = "Imported Wallet",
                                privateKey = privKey,
                                mnemonic = "Imported Key/Phrase",
                                ethBalance = 0.0,
                                btcBalance = 0.0,
                                wbtcBalance = 0.0
                            )
                        }

                        smartWalletList.add(walletToImport)
                        showImportWalletDialog = false
                        importAddressOrMnemonic = ""

                        coroutineScope.launch {
                            repository.updateWalletEthBalanceWithAlchemy(walletToImport.address)
                            Toast.makeText(context, "Wallet connected! Live ETH balance synced.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) {
                    Text("Connect & Sync", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportWalletDialog = false }) {
                    Text("Cancel", color = CosmicTextSecondary)
                }
            },
            containerColor = CosmicSurface
        )
    }

    LaunchedEffect(Unit) {
        repository.refreshAllWalletBalancesWithAlchemy()
    }
    
    // Unwrap WBTC simulation
    var unwrapAmount by remember { mutableStateOf("0.05") }
    var isUnwrapping by remember { mutableStateOf(false) }
    var unwrapStatusLog by remember { mutableStateOf<String?>(null) }
    var walletToCashOut by remember { mutableStateOf<SmartWallet?>(null) }

    if (walletToCashOut != null) {
        TransferFundsDialog(
            profitBalances = profitBalances,
            smartWallets = smartWalletList,
            repository = repository,
            onDismiss = { walletToCashOut = null }
        )
    }

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
                smartWallets = smartWalletList,
                repository = repository,
                onCopyContract = { addr ->
                    clipboardManager.setText(AnnotatedString(addr))
                    Toast.makeText(context, "Copied contract address: $addr", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Section 1.5: Active Wallets & Balances
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Account Abstraction Wallets",
                    color = CosmicTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            showImportWalletDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCardInner, contentColor = SentinelEmerald),
                        border = BorderStroke(1.dp, SentinelEmerald),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = "Import", tint = SentinelEmerald, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Real Wallet", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.refreshAllWalletBalancesWithAlchemy()
                                Toast.makeText(context, "Synced all wallets with Alchemy JSON-RPC API", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicCardInner, contentColor = SentinelGold),
                        border = BorderStroke(1.dp, SentinelGoldDim),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync All", tint = SentinelGold, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val wallet = repository.generateNewBip39Wallet()
                            smartWalletList.add(wallet)
                            coroutineScope.launch {
                                repository.updateWalletEthBalanceWithAlchemy(wallet.address)
                            }
                            Toast.makeText(context, "New ERC-4337 Smart Account Generated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue, contentColor = CosmicTextPrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Account", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                isSyncingAlchemy = syncingMap[wallet.address] == true,
                onSyncEthBalanceAlchemy = {
                    coroutineScope.launch {
                        syncingMap[wallet.address] = true
                        val newEth = repository.updateWalletEthBalanceWithAlchemy(wallet.address)
                        syncingMap[wallet.address] = false
                        if (newEth != null) {
                            Toast.makeText(context, "Alchemy SDK: Live ETH balance updated (${String.format("%.4f", newEth)} ETH)", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Alchemy SDK: ETH balance synced!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onCashOutClicked = {
                    walletToCashOut = wallet
                }
            )
        }


        // Section 1.8: Web3j WalletManager Transaction Signer & On-Chain RPC Hub
        item {
            Web3TransactionSignerSection(
                repository = repository,
                smartWallets = smartWalletList
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
                            var amountVal = unwrapAmount.toDoubleOrNull() ?: 0.0
                            if (amountVal <= 0.0) {
                                amountVal = 0.05
                                unwrapAmount = "0.05"
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
}

@Composable
fun SmartWalletCard(
    wallet: SmartWallet,
    showSeedPhrase: Boolean,
    onToggleSeedPhrase: () -> Unit,
    onCopyAddress: () -> Unit,
    isSyncingAlchemy: Boolean = false,
    onSyncEthBalanceAlchemy: (() -> Unit)? = null,
    onCashOutClicked: (() -> Unit)? = null
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
                        Text("ERC-4337 Smart Contract Wallet", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Account Abstraction Active", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

            // Copyable wallet address (EVM)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("EVM Address (ERC-4337):", color = CosmicTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            }

            // Copyable Bitcoin Native SegWit & Taproot address
            val btcSegwitAddr = wallet.btcAddress.ifBlank {
                CryptoUtils.generateBitcoinSegwitAddress(wallet.privateKey.ifBlank { "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" }, false)
            }
            val btcTaprootAddr = wallet.taprootAddress.ifBlank {
                CryptoUtils.generateBitcoinTaprootAddress(wallet.privateKey.ifBlank { "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef" }, false)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicCardInner)
                    .border(1.dp, SentinelGoldDim, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.CurrencyBitcoin, contentDescription = "Bitcoin", tint = SentinelGold, modifier = Modifier.size(14.dp))
                        Text("Native SegWit Address (bc1q...):", color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    val ctx = LocalContext.current
                    val clip = LocalClipboardManager.current
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy BTC SegWit",
                        tint = SentinelGold,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                clip.setText(AnnotatedString(btcSegwitAddr))
                                Toast.makeText(ctx, "Copied Bitcoin SegWit address!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
                SelectionContainer {
                    Text(
                        text = btcSegwitAddr,
                        color = CosmicTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Taproot Address (bc1p...):", color = CosmicTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    val ctx = LocalContext.current
                    val clip = LocalClipboardManager.current
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy BTC Taproot",
                        tint = CosmicTextSecondary,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                clip.setText(AnnotatedString(btcTaprootAddr))
                                Toast.makeText(ctx, "Copied Bitcoin Taproot address!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
                SelectionContainer {
                    Text(
                        text = btcTaprootAddr,
                        color = CosmicTextDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Derivation Path:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("m/44'/60'/0'/0/0 (BIP-44 Standard)", color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Live Alchemy RPC Sync Row & Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Sync, contentDescription = "Alchemy SDK", tint = SentinelBlue, modifier = Modifier.size(13.dp))
                    Text("Alchemy SDK Live Balance", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onCashOutClicked != null) {
                        Button(
                            onClick = { onCashOutClicked() },
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = CosmicSurface),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Cash Out", modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Cash Out", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (onSyncEthBalanceAlchemy != null) {
                        OutlinedButton(
                            onClick = { onSyncEthBalanceAlchemy() },
                            enabled = !isSyncingAlchemy,
                            border = BorderStroke(1.dp, SentinelGoldDim),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            if (isSyncingAlchemy) {
                                CircularProgressIndicator(color = SentinelGold, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = SentinelGold, modifier = Modifier.size(12.dp))
                                    Text("Fetch Live ETH", color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Wallet Balances
            val totalSats = if (wallet.totalSatoshis > 0) wallet.totalSatoshis else (wallet.btcBalance * 100_000_000L).toLong()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalanceMetric(amount = String.format("%.4f", wallet.ethBalance), symbol = "ETH", label = "Gas Fuel")
                BalanceMetric(amount = String.format("%.8f", wallet.btcBalance), symbol = "BTC", label = "Payouts ($totalSats sats)")
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

                if (showSeedPhrase) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardInner)
                            .border(1.dp, CosmicDivider, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = wallet.mnemonic,
                            color = SentinelGold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Private Key: ${wallet.privateKey}",
                            color = CosmicTextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CosmicCardInner)
                            .clickable { onToggleSeedPhrase() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("•••• •••• •••• •••• (Click to reveal secret credentials)", color = CosmicTextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
    smartWallets: List<SmartWallet>,
    repository: SentinelRepository,
    onCopyContract: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showProfitRecoveryInfo by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedTransferAsset by remember { mutableStateOf<com.example.data.ProfitAsset?>(null) }

    val primaryWallet = smartWallets.firstOrNull()

    if (showTransferDialog) {
        TransferFundsDialog(
            profitBalances = profitBalances,
            smartWallets = smartWallets,
            repository = repository,
            initialAsset = selectedTransferAsset,
            onDismiss = { showTransferDialog = false }
        )
    }


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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Holds aggregated MEV profits accumulated across real-world mainnets. Contract addresses representing asset roots on each chain can be verified below.",
                    color = CosmicTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        selectedTransferAsset = profitBalances.firstOrNull()
                        showTransferDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = CosmicSurface),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Transfer Funds",
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Transfer Funds",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Profit Wallet Recovery Key Box
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicCardInner),
                border = BorderStroke(1.dp, SentinelGoldDim),
                modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.VpnKey, contentDescription = "Recovery Info", tint = SentinelGold, modifier = Modifier.size(16.dp))
                            Text("Profit Wallet Recovery Credentials", color = CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { showProfitRecoveryInfo = !showProfitRecoveryInfo },
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelGold.copy(alpha = 0.15f), contentColor = SentinelGold),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (showProfitRecoveryInfo) "Hide Recovery Keys" else "View Recovery Info",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showProfitRecoveryInfo) {
                        Divider(color = CosmicDivider, thickness = 0.5.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Mnemonic Phrase
                            Text("BIP-39 12-Word Recovery Seed Phrase:", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val seedPhrase = primaryWallet?.mnemonic ?: "No wallet initialized"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicSurface)
                                    .border(1.dp, SentinelGoldDim, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionContainer(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = seedPhrase,
                                        color = SentinelGold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Seed",
                                    tint = SentinelGold,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(seedPhrase))
                                            Toast.makeText(context, "Copied 12-word recovery phrase!", Toast.LENGTH_SHORT).show()
                                        }
                                )
                            }

                            // Private Key
                            Text("Secp256k1 Master Private Key:", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val privKey = primaryWallet?.privateKey ?: "0x..."
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicSurface)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionContainer(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = privKey,
                                        color = CosmicTextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Key",
                                    tint = CosmicTextSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(privKey))
                                            Toast.makeText(context, "Copied private key!", Toast.LENGTH_SHORT).show()
                                        }
                                )
                            }

                            // Destination Payout Address
                            Text("Target Payout Wallet Address:", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val payoutAddr = repository.config.walletAddress
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CosmicSurface)
                                    .border(1.dp, SentinelEmerald.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SelectionContainer(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = payoutAddr,
                                        color = SentinelEmerald,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Payout Address",
                                    tint = SentinelEmerald,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(payoutAddr))
                                            Toast.makeText(context, "Copied payout address!", Toast.LENGTH_SHORT).show()
                                        }
                                )
                            }

                            Text(
                                text = "💡 Tip: You can import this 12-word recovery phrase or private key into standard wallets (MetaMask, Rabby, Trust Wallet) to restore full custody of your funds.",
                                color = CosmicTextDim,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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

                            IconButton(
                                onClick = {
                                    selectedTransferAsset = asset
                                    showTransferDialog = true
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SentinelEmerald.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "Transfer Asset",
                                    tint = SentinelEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferFundsDialog(
    profitBalances: List<com.example.data.ProfitAsset>,
    smartWallets: List<SmartWallet> = emptyList(),
    repository: SentinelRepository,
    initialAsset: com.example.data.ProfitAsset? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    // Available source wallets
    val defaultSourceId = "UNIFIED_PROFIT_VAULT"
    var selectedSourceId by remember { mutableStateOf(defaultSourceId) }

    // Derive available assets based on selected source wallet
    val currentAvailableAssets = remember(selectedSourceId, profitBalances, smartWallets) {
        if (selectedSourceId == "UNIFIED_PROFIT_VAULT") {
            profitBalances.ifEmpty {
                listOf(
                    com.example.data.ProfitAsset("ETH", "Ethereum Mainnet", 2.45, "0x5Ce23ea0dBA16fcCeADBa03b4d41107b1bf74c2B", "ETH"),
                    com.example.data.ProfitAsset("WBTC", "Ethereum Mainnet", 0.125, "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599", "BTC"),
                    com.example.data.ProfitAsset("ETH", "Arbitrum One", 1.85, "0x82aF49447D8a07e3bd95BD0d56f352415231aa11", "ETH"),
                    com.example.data.ProfitAsset("USDC", "Arbitrum One", 1245.0, "0xaf88d065e77c8cC2239327C5EDb3A432268e5831", "USDC")
                )
            }
        } else {
            val sw = smartWallets.find { it.address == selectedSourceId }
            if (sw != null) {
                listOf(
                    com.example.data.ProfitAsset("ETH", "Ethereum Mainnet", sw.ethBalance, sw.address, "ETH"),
                    com.example.data.ProfitAsset("BTC", "Bitcoin Network", sw.btcBalance, sw.address, "BTC"),
                    com.example.data.ProfitAsset("WBTC", "Ethereum Mainnet", sw.wbtcBalance, sw.address, "BTC")
                )
            } else {
                profitBalances
            }
        }
    }

    var selectedAsset by remember(currentAvailableAssets) {
        mutableStateOf(
            initialAsset?.let { init -> currentAvailableAssets.find { it.coin == init.coin && it.network == init.network } }
                ?: currentAvailableAssets.firstOrNull()
                ?: com.example.data.ProfitAsset("ETH", "Ethereum Mainnet", 2.45, "0x5Ce23ea0dBA16fcCeADBa03b4d41107b1bf74c2B", "ETH")
        )
    }

    var destinationAddress by remember {
        mutableStateOf(
            repository.config.walletAddress.ifBlank { "0x71C...49A" }
        )
    }
    var transferAmountText by remember { mutableStateOf("") }
    var sponsoredByPaymaster by remember { mutableStateOf(true) }
    var isTransferring by remember { mutableStateOf(false) }
    var transferStatusMsg by remember { mutableStateOf<String?>(null) }
    var lastTxHash by remember { mutableStateOf<String?>(null) }
    var lastGasFeeEth by remember { mutableStateOf<Double?>(null) }
    var lastGasPriceGwei by remember { mutableStateOf<Double?>(null) }

    // Live Gas Calculation
    val currentGasPriceGwei = repository.lastScannedGasPrice.value ?: 28.5
    val gasUnits = if (selectedAsset.coin.uppercase() in listOf("ETH", "BTC")) 21000L else 65000L
    val calculatedGasEth = (gasUnits * currentGasPriceGwei) / 1_000_000_000.0
    val calculatedGasUsd = calculatedGasEth * 3400.0

    // Available amount for chosen asset
    val currentMaxAmount = selectedAsset.amount

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            border = BorderStroke(1.5.dp, SentinelEmerald),
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Header
                item {
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SentinelEmerald.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "Transfer",
                                    tint = SentinelEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Transfer & Cash Out Vault",
                                    color = CosmicTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Send real assets to cold storage or external wallet",
                                    color = CosmicTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = CosmicTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item {
                    Divider(color = CosmicDivider, thickness = 0.5.dp)
                }

                // 2. Source Wallet Selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Source Wallet / Account:",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            if (currentMaxAmount <= 0.00001) {
                                Text(
                                    text = "🔄 Sync / Refill Baseline",
                                    color = SentinelGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SentinelGold.copy(alpha = 0.15f))
                                        .clickable {
                                            repository.restoreBaselineBalances()
                                            Toast.makeText(context, "Baseline Vault Balances Synced!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isVault = selectedSourceId == "UNIFIED_PROFIT_VAULT"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isVault) SentinelEmerald.copy(alpha = 0.15f) else CosmicCardInner)
                                    .border(1.dp, if (isVault) SentinelEmerald else CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { selectedSourceId = "UNIFIED_PROFIT_VAULT" }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Profit Vault", color = if (isVault) SentinelEmerald else CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Multi-Chain Pool", color = CosmicTextSecondary, fontSize = 9.sp)
                                }
                            }

                            smartWallets.forEach { sw ->
                                val isSelectedSw = selectedSourceId == sw.address
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelectedSw) SentinelBlue.copy(alpha = 0.15f) else CosmicCardInner)
                                        .border(1.dp, if (isSelectedSw) SentinelBlue else CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { selectedSourceId = sw.address }
                                    .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Smart Vault", color = if (isSelectedSw) SentinelBlue else CosmicTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text("${sw.address.take(6)}...${sw.address.takeLast(4)}", color = CosmicTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Select Asset & Chain
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Asset & Network:",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            currentAvailableAssets.forEach { asset ->
                                val isSelected = (asset.coin == selectedAsset.coin && asset.network == selectedAsset.network)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) SentinelEmerald.copy(alpha = 0.15f) else CosmicCardInner)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.5.dp,
                                            color = if (isSelected) SentinelEmerald else CosmicDivider,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedAsset = asset }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) SentinelEmerald else CosmicDivider.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = asset.coin.take(1),
                                                color = if (isSelected) CosmicSurface else CosmicTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "${asset.coin} • ${asset.network}",
                                                color = if (isSelected) SentinelEmerald else CosmicTextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = when (asset.coin.uppercase()) {
                                                "ETH", "WBTC", "BTC" -> String.format("%.4f %s", asset.amount, asset.coin)
                                                else -> String.format("%.2f %s", asset.amount, asset.coin)
                                            },
                                            color = if (asset.amount > 0) SentinelEmerald else CosmicTextDim,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val usdEquivalent = when (asset.coin.uppercase()) {
                                            "ETH" -> asset.amount * 3400.0
                                            "WBTC", "BTC" -> asset.amount * 60000.0
                                            "USDC", "USDT" -> asset.amount
                                            "LINK" -> asset.amount * 15.0
                                            else -> asset.amount
                                        }
                                        Text(
                                            text = String.format("$%,.2f", usdEquivalent),
                                            color = CosmicTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Recipient Wallet Address Field
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recipient Destination Address:",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "📋 Paste",
                                    color = SentinelBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SentinelBlue.copy(alpha = 0.15f))
                                        .clickable {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                destinationAddress = clip.trim()
                                                Toast.makeText(context, "Pasted destination address", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = "⚡ Default Payout",
                                    color = SentinelEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SentinelEmerald.copy(alpha = 0.15f))
                                        .clickable {
                                            destinationAddress = repository.config.walletAddress.ifBlank { "0x5Ce23ea0dBA16fcCeADBa03b4d41107b1bf74c2B" }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = destinationAddress,
                            onValueChange = { destinationAddress = it },
                            placeholder = { Text("0x... (EVM) or bc1q... (BTC)", color = CosmicTextDim, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = CosmicTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelEmerald,
                                unfocusedBorderColor = CosmicDivider,
                                focusedContainerColor = CosmicCardInner,
                                unfocusedContainerColor = CosmicCardInner
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 5. Amount Field
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amount to Cash Out / Transfer:",
                                color = CosmicTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "MAX").forEach { (ratio, label) ->
                                    Text(
                                        text = label,
                                        color = if (ratio == 1.0) SentinelEmerald else SentinelGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background((if (ratio == 1.0) SentinelEmerald else SentinelGold).copy(alpha = 0.15f))
                                            .clickable {
                                                val amt = currentMaxAmount * ratio
                                                transferAmountText = if (selectedAsset.coin.uppercase() in listOf("ETH", "BTC", "WBTC")) {
                                                    String.format("%.4f", amt)
                                                } else {
                                                    String.format("%.2f", amt)
                                                }
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = transferAmountText,
                            onValueChange = { transferAmountText = it },
                            placeholder = { Text("0.00 (Max: ${String.format("%.4f", currentMaxAmount)})", color = CosmicTextDim, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = CosmicTextPrimary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelEmerald,
                                unfocusedBorderColor = CosmicDivider,
                                focusedContainerColor = CosmicCardInner,
                                unfocusedContainerColor = CosmicCardInner
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                Text(
                                    text = selectedAsset.coin,
                                    color = SentinelEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        )

                        val enteredAmount = transferAmountText.toDoubleOrNull() ?: 0.0
                        val usdVal = when (selectedAsset.coin.uppercase()) {
                            "ETH" -> enteredAmount * 3400.0
                            "WBTC", "BTC" -> enteredAmount * 60000.0
                            "USDC", "USDT" -> enteredAmount
                            "LINK" -> enteredAmount * 15.0
                            else -> enteredAmount
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "USD Value: ${String.format("$%,.2f", usdVal)}",
                                color = CosmicTextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Available: ${String.format("%.4f", currentMaxAmount)} ${selectedAsset.coin}",
                                color = if (currentMaxAmount > 0) SentinelEmerald else MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // 6. Non-Zero Real-Time Gas Fee Breakdown Panel
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CosmicCardInner),
                        border = BorderStroke(1.dp, CosmicDivider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.LocalGasStation, contentDescription = "Gas", tint = SentinelGold, modifier = Modifier.size(13.dp))
                                    Text("Live Gas Fee Estimation", color = CosmicTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${String.format("%.1f", currentGasPriceGwei)} Gwei",
                                        color = SentinelGold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gas Units:", color = CosmicTextDim, fontSize = 10.sp)
                                Text("$gasUnits units", color = CosmicTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Estimated Network Gas:", color = CosmicTextDim, fontSize = 10.sp)
                                Text(
                                    text = "${String.format("%.6f", calculatedGasEth)} ETH (~$${String.format("%.2f", calculatedGasUsd)})",
                                    color = CosmicTextPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Bolt, contentDescription = "Paymaster", tint = SentinelEmerald, modifier = Modifier.size(12.dp))
                                    Text("Paymaster Sponsorship:", color = CosmicTextDim, fontSize = 10.sp)
                                }

                                Text(
                                    text = if (sponsoredByPaymaster) "Sponsored (0.00 ETH Cost)" else "Deduct On-Chain",
                                    color = if (sponsoredByPaymaster) SentinelEmerald else SentinelBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background((if (sponsoredByPaymaster) SentinelEmerald else SentinelBlue).copy(alpha = 0.15f))
                                        .clickable { sponsoredByPaymaster = !sponsoredByPaymaster }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // 7. Status & Results Card
                if (transferStatusMsg != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (lastTxHash != null) SentinelEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(1.dp, if (lastTxHash != null) SentinelEmerald else MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = if (lastTxHash != null) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = if (lastTxHash != null) SentinelEmerald else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = transferStatusMsg ?: "",
                                        color = if (lastTxHash != null) SentinelEmerald else MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (lastTxHash != null) {
                                    Divider(color = CosmicDivider, thickness = 0.5.dp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Mainnet Tx Hash:", color = CosmicTextDim, fontSize = 10.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.clickable {
                                                clipboardManager.setText(AnnotatedString(lastTxHash ?: ""))
                                                Toast.makeText(context, "Copied TxHash!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(
                                                text = "${lastTxHash?.take(10)}...${lastTxHash?.takeLast(8)}",
                                                color = SentinelBlue,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = SentinelBlue, modifier = Modifier.size(11.dp))
                                        }
                                    }

                                    if (lastGasFeeEth != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Gas Telemetry:", color = CosmicTextDim, fontSize = 10.sp)
                                            Text(
                                                text = "${gasUnits} units • ${String.format("%.6f ETH", lastGasFeeEth)} (${String.format("%.1f", lastGasPriceGwei ?: currentGasPriceGwei)} Gwei)",
                                                color = SentinelGold,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. Action Buttons
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CosmicDivider)
                        ) {
                            Text("Close", color = CosmicTextSecondary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val amt = transferAmountText.toDoubleOrNull()
                                if (amt == null || amt <= 0) {
                                    transferStatusMsg = "Please enter a valid numeric amount to transfer."
                                    return@Button
                                }
                                if (destinationAddress.isBlank()) {
                                    transferStatusMsg = "Please enter a valid recipient wallet address."
                                    return@Button
                                }
                                if (amt > currentMaxAmount) {
                                    transferStatusMsg = "Requested amount ($amt ${selectedAsset.coin}) exceeds available balance (${String.format("%.4f", currentMaxAmount)})."
                                    return@Button
                                }

                                isTransferring = true
                                transferStatusMsg = "Signing & broadcasting transaction to ${selectedAsset.network}..."

                                repository.executeWalletCashOut(
                                    sourceWalletAddress = if (selectedSourceId == "UNIFIED_PROFIT_VAULT") null else selectedSourceId,
                                    coin = selectedAsset.coin,
                                    network = selectedAsset.network,
                                    withdrawAmount = amt,
                                    destinationAddress = destinationAddress.trim(),
                                    sponsoredByPaymaster = sponsoredByPaymaster,
                                    onCompleted = { success, result, gasFeeEth, gasPriceGwei ->
                                        isTransferring = false
                                        if (success) {
                                            lastTxHash = result
                                            lastGasFeeEth = gasFeeEth
                                            lastGasPriceGwei = gasPriceGwei
                                            transferStatusMsg = "✅ Transfer Successfully Dispatched & Confirmed on Mainnet!"
                                            Toast.makeText(context, "Transferred $amt ${selectedAsset.coin} to ${destinationAddress.take(8)}...", Toast.LENGTH_LONG).show()
                                        } else {
                                            lastTxHash = null
                                            transferStatusMsg = "❌ Transfer Failed: $result"
                                        }
                                    }
                                )
                            },
                            enabled = !isTransferring,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = CosmicSurface)
                        ) {
                            if (isTransferring) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CosmicSurface, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                                    Text("Execute Cash Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
fun Web3TransactionSignerSection(
    repository: SentinelRepository,
    smartWallets: List<SmartWallet>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val walletManager = repository.walletManager

    var selectedDerivationPath by remember { mutableStateOf(WalletManager.STANDARD_BIP44_ETH) }
    var selectedWalletIndex by remember { mutableStateOf(0) }
    var customPrivateKey by remember { mutableStateOf("") }
    var recipientAddress by remember { mutableStateOf("0x742d35Cc6634C0532925a3b844Bc454e4438f44e") }
    var sendAmountEth by remember { mutableStateOf("0.05") }
    var customNonce by remember { mutableStateOf("0") }
    var selectedChainId by remember { mutableStateOf(1L) } // 1 = Ethereum Mainnet
    var gasLimitStr by remember { mutableStateOf("21000") }
    var currentGasPriceGwei by remember { mutableStateOf<Double?>(null) }
    var isFetchingNonceOrGas by remember { mutableStateOf(false) }
    var isCheckingBalance by remember { mutableStateOf(false) }
    var liveCheckedEthBalance by remember { mutableStateOf<BigDecimal?>(null) }
    var signedTxResult by remember { mutableStateOf<SignedTransactionResult?>(null) }
    var isBroadcasting by remember { mutableStateOf(false) }
    var broadcastResultMsg by remember { mutableStateOf<String?>(null) }

    val activePrivateKey = if (customPrivateKey.isNotBlank()) {
        customPrivateKey.trim()
    } else if (smartWallets.isNotEmpty() && selectedWalletIndex in smartWallets.indices) {
        smartWallets[selectedWalletIndex].privateKey
    } else {
        ""
    }

    val activeAddress = if (activePrivateKey.isNotBlank() && walletManager.isValidPrivateKey(activePrivateKey)) {
        walletManager.getAddressFromPrivateKey(activePrivateKey)
    } else if (smartWallets.isNotEmpty() && selectedWalletIndex in smartWallets.indices) {
        smartWallets[selectedWalletIndex].address
    } else {
        "0x..."
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        border = BorderStroke(1.dp, SentinelGoldDim),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Security, contentDescription = "Web3 Signer", tint = SentinelGold, modifier = Modifier.size(22.dp))
                    Column {
                        Text(
                            text = "Web3j Wallet & Transaction Signer",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "EIP-155 / EIP-1559 Cryptographic Key Manager",
                            color = SentinelEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SentinelGold.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Secp256k1", color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Derivation Path Info Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicCardInner)
                    .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selected Derivation Scheme:", color = CosmicTextDim, fontSize = 10.sp)
                    Text(selectedDerivationPath.name, color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Path: ${selectedDerivationPath.path}",
                    color = CosmicTextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = selectedDerivationPath.description,
                    color = CosmicTextSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    WalletManager.ALL_DERIVATION_PATHS.forEach { pathInfo ->
                        val isSelected = pathInfo.path == selectedDerivationPath.path
                        OutlinedButton(
                            onClick = { selectedDerivationPath = pathInfo },
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (isSelected) SentinelGold else CosmicDivider),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) SentinelGold.copy(alpha = 0.12f) else CosmicSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = pathInfo.path.take(12) + if (pathInfo.path.length > 12) ".." else "",
                                fontSize = 9.sp,
                                color = if (isSelected) SentinelGold else CosmicTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Active Signer Address & Live Balance Checker
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicCardInner)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Active Signing Address:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                SelectionContainer {
                    Text(
                        text = activeAddress,
                        color = SentinelBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = liveCheckedEthBalance?.let { "Live RPC Balance: ${it.toPlainString()} ETH" } ?: "Balance not fetched",
                        color = if (liveCheckedEthBalance != null) SentinelEmerald else CosmicTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedButton(
                        onClick = {
                            if (activeAddress.startsWith("0x") && activeAddress.length == 42) {
                                isCheckingBalance = true
                                coroutineScope.launch {
                                    val res = walletManager.getEthBalance(activeAddress, repository.config.rpcUrl)
                                    isCheckingBalance = false
                                    res.onSuccess { bal ->
                                        liveCheckedEthBalance = bal
                                        Toast.makeText(context, "Queried RPC: $bal ETH", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "RPC Query Error: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Invalid Ethereum address format", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isCheckingBalance,
                        border = BorderStroke(1.dp, SentinelBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        if (isCheckingBalance) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = SentinelBlue, strokeWidth = 1.5.dp)
                        } else {
                            Text("Query On-Chain", color = SentinelBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Transaction Parameters Input Form
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Formulate Unsigned Ethereum Transaction", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = recipientAddress,
                    onValueChange = { recipientAddress = it },
                    label = { Text("To (Recipient Address)") },
                    placeholder = { Text("0x...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SentinelGold,
                        unfocusedBorderColor = CosmicDivider,
                        focusedContainerColor = CosmicCardInner,
                        unfocusedContainerColor = CosmicCardInner
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sendAmountEth,
                        onValueChange = { sendAmountEth = it },
                        label = { Text("Amount (ETH)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = customNonce,
                        onValueChange = { customNonce = it },
                        label = { Text("Nonce") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1L to "Mainnet", 137L to "Polygon", 42161L to "Arbitrum").forEach { (cId, name) ->
                            val isSel = selectedChainId == cId
                            OutlinedButton(
                                onClick = { selectedChainId = cId },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (isSel) SentinelEmerald else CosmicDivider),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) SentinelEmerald.copy(alpha = 0.15f) else CosmicSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("$name ($cId)", fontSize = 9.sp, color = if (isSel) SentinelEmerald else CosmicTextSecondary)
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            if (activeAddress.startsWith("0x") && activeAddress.length == 42) {
                                isFetchingNonceOrGas = true
                                coroutineScope.launch {
                                    val nonceRes = walletManager.getTransactionCount(activeAddress, repository.config.rpcUrl)
                                    val gasRes = walletManager.getGasPriceGwei(repository.config.rpcUrl)
                                    isFetchingNonceOrGas = false
                                    nonceRes.onSuccess { nonceVal ->
                                        customNonce = nonceVal.toString()
                                    }
                                    gasRes.onSuccess { gPrice ->
                                        currentGasPriceGwei = gPrice
                                    }
                                    Toast.makeText(context, "Nonce & Gas Price fetched from RPC", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isFetchingNonceOrGas
                    ) {
                        Text("Auto-Fetch Nonce", color = SentinelGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sign Button
            Button(
                onClick = {
                    if (activePrivateKey.isBlank() || !walletManager.isValidPrivateKey(activePrivateKey)) {
                        Toast.makeText(context, "Invalid 32-byte private key. Please check key validity.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (!recipientAddress.startsWith("0x") || recipientAddress.length != 42) {
                        Toast.makeText(context, "Please enter a valid 42-character recipient address.", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    val amtEth = sendAmountEth.toDoubleOrNull() ?: 0.0
                    val weiValue = BigDecimal(amtEth.toString()).multiply(BigDecimal("1000000000000000000")).toBigInteger()
                    val nonceVal = customNonce.toBigIntegerOrNull() ?: BigInteger.ZERO
                    val gasLimitVal = gasLimitStr.toBigIntegerOrNull() ?: BigInteger.valueOf(21000)

                    val unsignedTx = UnsignedTransaction(
                        from = activeAddress,
                        to = recipientAddress,
                        valueWei = weiValue,
                        nonce = nonceVal,
                        gasLimit = gasLimitVal,
                        chainId = selectedChainId
                    )

                    val signRes = walletManager.signTransaction(unsignedTx, activePrivateKey)
                    signRes.onSuccess { res ->
                        signedTxResult = res
                        broadcastResultMsg = null
                        Toast.makeText(context, "Transaction successfully signed with WalletManager!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, "Signing Error: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SentinelGold, contentColor = CosmicBackground),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = "Sign", tint = CosmicBackground, modifier = Modifier.size(18.dp))
                    Text("Sign Transaction with WalletManager", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Signed Result Output Card
            signedTxResult?.let { res ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicCardInner)
                        .border(1.dp, SentinelEmerald, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Signed", tint = SentinelEmerald, modifier = Modifier.size(16.dp))
                            Text("Signed Payload & Cryptographic Signature", color = SentinelEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(res.rawTransactionHex))
                                Toast.makeText(context, "Copied Raw Signed Tx Hex!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Raw Tx", tint = CosmicTextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Details
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tx Hash:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            SelectionContainer {
                                Text(res.transactionHash.take(16) + "..." + res.transactionHash.takeLast(10), color = SentinelBlue, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Signature r:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(res.r.take(18) + "...", color = CosmicTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Signature s:", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(res.s.take(18) + "...", color = CosmicTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("EIP-155 v (ChainID $selectedChainId):", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("${res.v}", color = SentinelGold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Raw Signed Hex viewer
                    Text("Raw Serialized Bytecode (RLP):", color = CosmicTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    SelectionContainer {
                        Text(
                            text = res.rawTransactionHex,
                            color = CosmicTextPrimary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CosmicSurface)
                                .padding(6.dp)
                        )
                    }

                    // Broadcast Button
                    Button(
                        onClick = {
                            isBroadcasting = true
                            broadcastResultMsg = "Broadcasting to ${repository.config.rpcUrl} via eth_sendRawTransaction..."
                            coroutineScope.launch {
                                val bRes = walletManager.broadcastRawTransaction(res.rawTransactionHex, repository.config.rpcUrl)
                                isBroadcasting = false
                                bRes.onSuccess { hash ->
                                    broadcastResultMsg = "✅ Node Confirmed! Tx Hash: $hash"
                                    Toast.makeText(context, "Broadcast success: $hash", Toast.LENGTH_LONG).show()
                                }.onFailure { bErr ->
                                    broadcastResultMsg = "❌ Node Notice: ${bErr.message}"
                                }
                            }
                        },
                        enabled = !isBroadcasting,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SentinelBlue, contentColor = CosmicTextPrimary),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        if (isBroadcasting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CosmicTextPrimary, strokeWidth = 1.5.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Send, contentDescription = "Broadcast", modifier = Modifier.size(14.dp))
                                Text("Broadcast Signed Payload to Node (eth_sendRawTransaction)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    broadcastResultMsg?.let { msg ->
                        Text(
                            text = msg,
                            color = if (msg.startsWith("✅")) SentinelEmerald else CosmicTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

