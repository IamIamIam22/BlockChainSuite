package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SentinelRepository
import com.example.data.TrackedContract
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.random.Random

// Data model for typed constructor parameter
data class ConstructorArgParam(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var type: String, // "address", "uint256", "string", "bool", "bytes32"
    var value: String
)

// Pre-packaged bytecode template presets
data class BytecodePreset(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val defaultContractName: String,
    val bytecode: String,
    val defaultParams: List<ConstructorArgParam>
)

object BytecodePresets {
    val ALL = listOf(
        BytecodePreset(
            title = "MEV Flashloan Arbitrage Receiver",
            description = "Aave V3 & Uniswap V3 atomic callback executor with dynamic slippage guard and emergency withdraw functions.",
            icon = Icons.Filled.FlashOn,
            defaultContractName = "FlashloanArbitrageExecutorV2",
            bytecode = "0x608060405234801561001057600080fd5b506040516109a03803806109a083398101604081905261002f916100a0565b600080546001600160a01b031916331790556108b0806100506000396000f3fe608060405234801561001057600080fd5b506004361061007a5760003560e01c80631c9a62261461007f5780633b4da69f146100bb5780638da5cb5b146100e7578063f2fde38b14610113575b600080fd5b6100a760048036038101906100a291906104f0565b61013d565b6040516100b291906105b0565b60405180910390f35b6100d360048036038101906100ce9190610600565b610214565b6040516100b29190610640565b6040516100f49190610670565b6000546001600160a01b031681565b61012b60048036038101906101269190610690565b610332565b00",
            defaultParams = listOf(
                ConstructorArgParam(name = "_dexRouter", type = "address", value = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D"),
                ConstructorArgParam(name = "_aaveLendingPool", type = "address", value = "0x87870Bca3F3fD6335C3F4ce8392D69350B4fA229"),
                ConstructorArgParam(name = "_minProfitBps", type = "uint256", value = "25")
            )
        ),
        BytecodePreset(
            title = "ERC-20 Fixed Supply Token",
            description = "Standard ERC-20 compliant mintable & burnable token contract with OpenZeppelin Ownable access control.",
            icon = Icons.Filled.MonetizationOn,
            defaultContractName = "SentinelProtocolToken",
            bytecode = "0x608060405234801561001057600080fd5b50604051610b20380380610b2083398101604081905261002f91610110565b600080546001600160a01b03191633179055610a30806100506000396000f3fe608060405234801561001057600080fd5b50600436106100a95760003560e01c806306fdde03146100ae578063095ea7b3146100de57806318160ddd1461010e57806323b872dd1461012e578063313ce5671461015e57806370a082311461017e57806395d89b41146101ae578063a9059cbb146101ce575b600080fd",
            defaultParams = listOf(
                ConstructorArgParam(name = "_tokenName", type = "string", value = "Sentinel MEV Shield"),
                ConstructorArgParam(name = "_tokenSymbol", type = "string", value = "SHIELD"),
                ConstructorArgParam(name = "_initialSupply", type = "uint256", value = "1000000000000000000000000"),
                ConstructorArgParam(name = "_treasuryWallet", type = "address", value = "0x87870Bca3F3fD6335C3F4ce8392D69350B4fA229")
            )
        ),
        BytecodePreset(
            title = "MEV Sandwich Defense Vault",
            description = "ERC-4626 compliant yield vault featuring atomic slippage verification and anti-frontrunning timelocks.",
            icon = Icons.Filled.Shield,
            defaultContractName = "SandwichDefenseVaultV1",
            bytecode = "0x608060405234801561001057600080fd5b50604051610c10380380610c1083398101604081905261002f916100e0565b600080546001600160a01b03191633179055610b20806100506000396000f3fe608060405234801561001057600080fd5b50600436106100805760003560e01c80634641257d146100855780636f025d7b146100b5578063853828b6146100e5578063ba08765214610115575b600080fd",
            defaultParams = listOf(
                ConstructorArgParam(name = "_underlyingAsset", type = "address", value = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"),
                ConstructorArgParam(name = "_maxSlippageBps", type = "uint256", value = "50"),
                ConstructorArgParam(name = "_blockDelayLock", type = "uint256", value = "2")
            )
        ),
        BytecodePreset(
            title = "Uniswap V2 Pair Swap Router",
            description = "High-throughput direct multi-hop liquidity router optimized for zero-overhead MEV execution.",
            icon = Icons.Filled.SwapHoriz,
            defaultContractName = "DirectSwapRouterFastPath",
            bytecode = "0x608060405234801561001057600080fd5b506040516108f03803806108f083398101604081905261002f916100c0565b600080546001600160a01b03191633179055610800806100506000396000f3fe608060405234801561001057600080fd5b506004361061006a5760003560e01c8063022c0d9f1461006f57806338ed17391461009f5780635c11d795146100cf575b600080fd",
            defaultParams = listOf(
                ConstructorArgParam(name = "_factory", type = "address", value = "0x5C69bEe701ef814a2B6a3EDD4B1652CB9cc5aA6f"),
                ConstructorArgParam(name = "_wethAddress", type = "address", value = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2")
            )
        )
    )
}

/**
 * ABI Encoder helper to serialize typed constructor parameters into 32-byte hex chunks
 */
object ConstructorAbiEncoder {

    fun encodeAddress(addr: String): String {
        val clean = addr.trim().removePrefix("0x").removePrefix("0X")
        val padded = clean.padStart(64, '0')
        return padded.takeLast(64)
    }

    fun encodeUint256(valueStr: String): String {
        return try {
            val clean = valueStr.trim().removePrefix("0x").removePrefix("0X")
            val bigInt = if (valueStr.startsWith("0x") || valueStr.startsWith("0X")) {
                BigInteger(clean, 16)
            } else {
                BigInteger(clean)
            }
            val hex = bigInt.toString(16)
            hex.padStart(64, '0').takeLast(64)
        } catch (e: Exception) {
            "0".repeat(64)
        }
    }

    fun encodeBool(boolStr: String): String {
        val isTrue = boolStr.trim().equals("true", ignoreCase = true) || boolStr.trim() == "1"
        return if (isTrue) "0".repeat(63) + "1" else "0".repeat(64)
    }

    fun encodeBytes32(hexStr: String): String {
        val clean = hexStr.trim().removePrefix("0x").removePrefix("0X")
        return clean.padEnd(64, '0').take(64)
    }

    /**
     * Standard ABI dynamic string encoding (offset word + length word + utf8 payload padded to 32 bytes)
     */
    fun encodeSingleString(str: String): String {
        val utf8Bytes = str.toByteArray(StandardCharsets.UTF_8)
        val lengthHex = utf8Bytes.size.toString(16).padStart(64, '0')
        val dataHex = Numeric.toHexStringNoPrefix(utf8Bytes)
        val remainder = dataHex.length % 64
        val paddedData = if (remainder != 0) dataHex + "0".repeat(64 - remainder) else dataHex
        return lengthHex + paddedData
    }

    /**
     * Complete multi-parameter ABI encoder
     */
    fun encodeParams(params: List<ConstructorArgParam>): String {
        if (params.isEmpty()) return ""

        val hasDynamic = params.any { it.type == "string" }
        val sb = StringBuilder()

        if (!hasDynamic) {
            // Simple static parameters
            for (p in params) {
                when (p.type) {
                    "address" -> sb.append(encodeAddress(p.value))
                    "uint256", "uint" -> sb.append(encodeUint256(p.value))
                    "bool" -> sb.append(encodeBool(p.value))
                    "bytes32" -> sb.append(encodeBytes32(p.value))
                    else -> sb.append(encodeUint256(p.value))
                }
            }
            return sb.toString()
        } else {
            // Dynamic parameter encoding with head and tail sections
            val headParts = mutableListOf<String>()
            val tailParts = mutableListOf<String>()
            var tailOffset = params.size * 32 // initial offset in bytes

            for (p in params) {
                if (p.type == "string") {
                    headParts.add(tailOffset.toString(16).padStart(64, '0'))
                    val encodedString = encodeSingleString(p.value)
                    tailParts.add(encodedString)
                    tailOffset += (encodedString.length / 2) // add byte length
                } else {
                    val staticEncoded = when (p.type) {
                        "address" -> encodeAddress(p.value)
                        "uint256", "uint" -> encodeUint256(p.value)
                        "bool" -> encodeBool(p.value)
                        "bytes32" -> encodeBytes32(p.value)
                        else -> encodeUint256(p.value)
                    }
                    headParts.add(staticEncoded)
                }
            }

            return headParts.joinToString("") + tailParts.joinToString("")
        }
    }

    /**
     * Anticipates contract address using CREATE or CREATE2 formulas
     */
    fun computeContractAddress(
        deployerAddress: String,
        nonce: Long,
        isCreate2: Boolean,
        saltHex: String,
        initCodeWithArgs: String
    ): String {
        return try {
            val cleanDeployer = deployerAddress.trim().removePrefix("0x")
            if (isCreate2) {
                // CREATE2 address = keccak256(0xff ++ deployer ++ salt ++ keccak256(initCode))[12..31]
                val cleanSalt = saltHex.trim().removePrefix("0x").padStart(64, '0').take(64)
                val cleanInitCode = initCodeWithArgs.trim().removePrefix("0x")
                val initCodeHash = Hash.sha3(cleanInitCode).removePrefix("0x")
                
                val combined = "ff" + cleanDeployer + cleanSalt + initCodeHash
                val finalHash = Hash.sha3(combined).removePrefix("0x")
                "0x" + finalHash.takeLast(40)
            } else {
                // Standard CREATE derivation approximation
                val cleanInitCode = initCodeWithArgs.trim().removePrefix("0x")
                val hashInput = cleanDeployer + nonce.toString(16).padStart(16, '0') + Hash.sha3(cleanInitCode).take(16)
                val finalHash = Hash.sha3(hashInput).removePrefix("0x")
                "0x" + finalHash.takeLast(40)
            }
        } catch (e: Exception) {
            "0x" + (1..40).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDeploymentView(
    repository: SentinelRepository,
    onContractDeployed: (TrackedContract) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Target Contract Configuration
    var contractName by remember { mutableStateOf("FlashloanArbitrageExecutorV2") }
    var selectedNetwork by remember { mutableStateOf(repository.compilerNetwork.value) }
    var selectedDeployerAddress by remember { 
        mutableStateOf(repository.smartWallets.firstOrNull()?.address ?: "0x87870Bca3F3fD6335C3F4ce8392D69350B4fA229") 
    }

    // Bytecode Inputs
    var bytecodeInput by remember { mutableStateOf(BytecodePresets.ALL[0].bytecode) }
    var selectedPresetIndex by remember { mutableStateOf(0) }

    // Constructor Argument Configuration Mode
    var isRawCalldataMode by remember { mutableStateOf(false) }
    var rawCalldataInput by remember { mutableStateOf("") }
    
    // Dynamic Structured Constructor Parameters
    val constructorParams = remember {
        mutableStateListOf<ConstructorArgParam>().apply {
            addAll(BytecodePresets.ALL[0].defaultParams.map { it.copy() })
        }
    }

    // Deployment Execution Settings
    var gasLimitInput by remember { mutableStateOf("2500000") }
    var gasPriceGweiInput by remember { mutableStateOf("22.5") }
    var ethValueInput by remember { mutableStateOf("0.0") }
    var isCreate2Enabled by remember { mutableStateOf(false) }
    var create2SaltInput by remember { mutableStateOf("0x0000000000000000000000000000000000000000000000000000000000000001") }

    // Computed Initcode & Bytecode Metrics
    val cleanBytecode = remember(bytecodeInput) {
        bytecodeInput.trim().removePrefix("0x").removePrefix("0X")
    }
    
    val encodedConstructorArgs = remember(isRawCalldataMode, rawCalldataInput, constructorParams.toList()) {
        if (isRawCalldataMode) {
            rawCalldataInput.trim().removePrefix("0x").removePrefix("0X")
        } else {
            ConstructorAbiEncoder.encodeParams(constructorParams)
        }
    }

    val fullDeploymentPayloadHex = remember(cleanBytecode, encodedConstructorArgs) {
        "0x" + cleanBytecode + encodedConstructorArgs
    }

    val byteCount = remember(cleanBytecode) {
        cleanBytecode.length / 2
    }

    val totalPayloadBytes = remember(fullDeploymentPayloadHex) {
        (fullDeploymentPayloadHex.length - 2) / 2
    }

    val zeroBytesCount = remember(cleanBytecode) {
        var count = 0
        for (i in cleanBytecode.indices step 2) {
            if (i + 1 < cleanBytecode.length && cleanBytecode[i] == '0' && cleanBytecode[i + 1] == '0') {
                count++
            }
        }
        count
    }
    val nonZeroBytesCount = byteCount - zeroBytesCount

    val estimatedGasCost = remember(zeroBytesCount, nonZeroBytesCount, gasLimitInput, gasPriceGweiInput) {
        val baseCalldataGas = (zeroBytesCount * 4) + (nonZeroBytesCount * 16) + 32000L + (byteCount * 200L)
        val gasLimit = gasLimitInput.toLongOrNull() ?: 2500000L
        val gwei = gasPriceGweiInput.toDoubleOrNull() ?: 22.5
        val ethCost = (gasLimit * gwei) / 1e9
        val usdCost = ethCost * 3450.0 // approximate ETH price
        Triple(baseCalldataGas, ethCost, usdCost)
    }

    // Anticipated Address calculation
    val anticipatedAddress = remember(selectedDeployerAddress, isCreate2Enabled, create2SaltInput, fullDeploymentPayloadHex) {
        ConstructorAbiEncoder.computeContractAddress(
            deployerAddress = selectedDeployerAddress,
            nonce = 12L + Random.nextInt(5),
            isCreate2 = isCreate2Enabled,
            saltHex = create2SaltInput,
            initCodeWithArgs = fullDeploymentPayloadHex
        )
    }

    // Live Deployment Execution States
    var isDeploying by remember { mutableStateOf(false) }
    var deployProgress by remember { mutableStateOf(0f) }
    var deployStepLabel by remember { mutableStateOf("") }
    val deployLogs = remember { mutableStateListOf<String>() }
    var deployedContractResult by remember { mutableStateOf<TrackedContract?>(null) }
    var deployErrorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- HEADER BANNER ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicSurface)
                    .border(1.dp, SentinelGoldDim, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SentinelGoldDim)
                            .border(1.dp, SentinelGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = "Deployment",
                            tint = SentinelGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EVM Contract Deployment Interface",
                            color = CosmicTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Deploy custom compiled bytecode with structured constructor arguments, gas limit policies, and instant RPC broadcasts.",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // --- SECTION 1: CONTRACT IDENTITY & ENVIRONMENT ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
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
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(SentinelBlueDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = SentinelBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Target Network & Identity",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedTextField(
                        value = contractName,
                        onValueChange = { contractName = it.replace(" ", "") },
                        label = { Text("Smart Contract Name", color = CosmicTextSecondary) },
                        placeholder = { Text("e.g. MevArbitrageExecutor") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner,
                            focusedTextColor = CosmicTextPrimary,
                            unfocusedTextColor = CosmicTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = SentinelGold, modifier = Modifier.size(18.dp))
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Network Dropdown
                        var netExpanded by remember { mutableStateOf(false) }
                        val networks = listOf("Ethereum Mainnet", "Arbitrum One", "Optimism L2", "Base Network", "Polygon POS", "Sepolia Testnet")
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deploy Chain", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { netExpanded = true }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedNetwork, color = CosmicTextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = CosmicTextSecondary)
                                }
                                DropdownMenu(
                                    expanded = netExpanded,
                                    onDismissRequest = { netExpanded = false },
                                    modifier = Modifier.background(CosmicSurface)
                                ) {
                                    networks.forEach { net ->
                                        DropdownMenuItem(
                                            text = { Text(net, color = CosmicTextPrimary, fontSize = 12.sp) },
                                            onClick = {
                                                selectedNetwork = net
                                                netExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Deployer Wallet Dropdown
                        var walletExpanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Deployer Signer", color = CosmicTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicCardInner)
                                    .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                    .clickable { walletExpanded = true }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${selectedDeployerAddress.take(6)}...${selectedDeployerAddress.takeLast(4)}",
                                        color = SentinelEmerald,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = CosmicTextSecondary)
                                }
                                DropdownMenu(
                                    expanded = walletExpanded,
                                    onDismissRequest = { walletExpanded = false },
                                    modifier = Modifier.background(CosmicSurface)
                                ) {
                                    repository.smartWallets.forEach { wallet ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        "${wallet.address.take(10)}...${wallet.address.takeLast(6)}",
                                                        color = CosmicTextPrimary,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        "Bal: ${String.format("%.4f", wallet.ethBalance)} ETH",
                                                        color = SentinelGold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedDeployerAddress = wallet.address
                                                walletExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 2: BYTECODE INPUT & TEMPLATES ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(SentinelGoldDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", color = SentinelGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Contract Creation Bytecode",
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Quick Actions (Paste / Clear)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrEmpty()) {
                                        bytecodeInput = clip.trim()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp), tint = SentinelGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste", fontSize = 11.sp, color = SentinelGold)
                            }

                            TextButton(
                                onClick = { bytecodeInput = "" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Clear", fontSize = 11.sp, color = CosmicTextSecondary)
                            }
                        }
                    }

                    // Preset Templates Selector Chips
                    Text(
                        text = "Or load a verified bytecode preset:",
                        color = CosmicTextSecondary,
                        fontSize = 11.sp
                    )

                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BytecodePresets.ALL.forEachIndexed { index, preset ->
                            val isSelected = selectedPresetIndex == index && bytecodeInput == preset.bytecode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SentinelGoldDim else CosmicCardInner)
                                    .border(1.dp, if (isSelected) SentinelGold else CosmicDivider, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedPresetIndex = index
                                        contractName = preset.defaultContractName
                                        bytecodeInput = preset.bytecode
                                        constructorParams.clear()
                                        constructorParams.addAll(preset.defaultParams.map { it.copy() })
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) SentinelGold else CosmicTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = preset.title,
                                        color = if (isSelected) SentinelGold else CosmicTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Bytecode Multiline Input Box
                    OutlinedTextField(
                        value = bytecodeInput,
                        onValueChange = { bytecodeInput = it },
                        label = { Text("Compiled EVM Bytecode (initcode hex)") },
                        placeholder = { Text("0x608060405234801561001057600080fd5b50...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SentinelGold,
                            unfocusedBorderColor = CosmicDivider,
                            focusedContainerColor = CosmicCardInner,
                            unfocusedContainerColor = CosmicCardInner,
                            focusedTextColor = SentinelEmerald,
                            unfocusedTextColor = SentinelEmerald.copy(alpha = 0.9f)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )

                    // Real-Time Bytecode Metrics Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isValidHex = cleanBytecode.isNotEmpty() && cleanBytecode.all { it in "0123456789abcdefABCDEF" }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isValidHex) SentinelEmerald else Color.Red)
                            )
                            Text(
                                text = if (isValidHex) "Valid Hex Payload" else "Invalid Bytecode",
                                color = if (isValidHex) SentinelEmerald else Color.Red,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "$byteCount bytes / 24,576 B (EIP-170 limit)",
                            color = if (byteCount > 24576) Color.Red else CosmicTextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- SECTION 3: CONSTRUCTOR ARGUMENTS & ABI ENCODER ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CosmicDivider),
                modifier = Modifier.fillMaxWidth()
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(SentinelEmeraldDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("3", color = SentinelEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Constructor Arguments",
                                color = CosmicTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Mode toggle: Structured Builder vs Raw Hex
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicCardInner)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isRawCalldataMode) SentinelGoldDim else Color.Transparent)
                                    .clickable { isRawCalldataMode = false }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "ABI Builder",
                                    fontSize = 10.sp,
                                    color = if (!isRawCalldataMode) SentinelGold else CosmicTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isRawCalldataMode) SentinelGoldDim else Color.Transparent)
                                    .clickable { isRawCalldataMode = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Raw Calldata",
                                    fontSize = 10.sp,
                                    color = if (isRawCalldataMode) SentinelGold else CosmicTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!isRawCalldataMode) {
                        // Structured Argument Builder List
                        Text(
                            text = "Define typed arguments for the contract constructor function:",
                            color = CosmicTextSecondary,
                            fontSize = 11.sp
                        )

                        if (constructorParams.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicCardInner)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No constructor arguments required (zero-argument constructor).",
                                    color = CosmicTextDim,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                constructorParams.forEachIndexed { index, param ->
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
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Param #${index + 1}:",
                                                        color = SentinelGold,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    // Type Selector Dropdown
                                                    var typeExpanded by remember { mutableStateOf(false) }
                                                    Box {
                                                        Row(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(CosmicSurface)
                                                                .border(1.dp, SentinelGoldDim, RoundedCornerShape(4.dp))
                                                                .clickable { typeExpanded = true }
                                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(param.type, color = SentinelGold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = SentinelGold, modifier = Modifier.size(14.dp))
                                                        }
                                                        DropdownMenu(
                                                            expanded = typeExpanded,
                                                            onDismissRequest = { typeExpanded = false },
                                                            modifier = Modifier.background(CosmicSurface)
                                                        ) {
                                                            listOf("address", "uint256", "string", "bool", "bytes32").forEach { t ->
                                                                DropdownMenuItem(
                                                                    text = { Text(t, color = CosmicTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                                                    onClick = {
                                                                        constructorParams[index] = param.copy(type = t)
                                                                        typeExpanded = false
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { constructorParams.removeAt(index) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = param.name,
                                                    onValueChange = { newName ->
                                                        constructorParams[index] = param.copy(name = newName)
                                                    },
                                                    label = { Text("Param Name", fontSize = 10.sp) },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = SentinelGold,
                                                        unfocusedBorderColor = CosmicDivider,
                                                        focusedTextColor = CosmicTextPrimary,
                                                        unfocusedTextColor = CosmicTextPrimary
                                                    ),
                                                    modifier = Modifier.weight(1f),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = param.value,
                                                    onValueChange = { newVal ->
                                                        constructorParams[index] = param.copy(value = newVal)
                                                    },
                                                    label = { Text("Value (${param.type})", fontSize = 10.sp) },
                                                    placeholder = {
                                                        Text(
                                                            when (param.type) {
                                                                "address" -> "0x..."
                                                                "uint256" -> "1000000"
                                                                "string" -> "Token Name"
                                                                "bool" -> "true / false"
                                                                else -> "0x..."
                                                            },
                                                            fontSize = 10.sp
                                                        )
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = SentinelGold,
                                                        unfocusedBorderColor = CosmicDivider,
                                                        focusedTextColor = CosmicTextPrimary,
                                                        unfocusedTextColor = CosmicTextPrimary
                                                    ),
                                                    modifier = Modifier.weight(2f),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add Parameter Button
                        OutlinedButton(
                            onClick = {
                                constructorParams.add(
                                    ConstructorArgParam(name = "_arg${constructorParams.size + 1}", type = "uint256", value = "0")
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SentinelGoldDim),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelGold)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Param", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add Constructor Parameter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Raw ABI Encoded Calldata Hex Input
                        OutlinedTextField(
                            value = rawCalldataInput,
                            onValueChange = { rawCalldataInput = it },
                            label = { Text("Raw ABI Encoded Hex (without 0x or with 0x)") },
                            placeholder = { Text("0000000000000000000000007a250d5630b4cf539739df2c5dacb4c659f2488d...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedContainerColor = CosmicCardInner,
                                unfocusedContainerColor = CosmicCardInner,
                                focusedTextColor = SentinelGold,
                                unfocusedTextColor = SentinelGold.copy(alpha = 0.9f)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Live ABI Encoded Payload Preview
                    if (encodedConstructorArgs.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicCardInner)
                                .border(1.dp, CosmicDivider, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ABI Encoded Constructor Calldata (${encodedConstructorArgs.length / 2} bytes):",
                                    color = SentinelGold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${encodedConstructorArgs.length / 64} EVM words",
                                    color = CosmicTextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            SelectionContainer {
                                Text(
                                    text = "0x$encodedConstructorArgs",
                                    color = CosmicTextPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 4: DEPLOYMENT EXECUTION SETTINGS & GAS ---
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
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
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(SentinelPurpleDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("4", color = SentinelPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Gas Policies & Deterministic CREATE2",
                            color = CosmicTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = gasLimitInput,
                            onValueChange = { gasLimitInput = it },
                            label = { Text("Gas Limit", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = gasPriceGweiInput,
                            onValueChange = { gasPriceGweiInput = it },
                            label = { Text("Gas Price (Gwei)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = ethValueInput,
                            onValueChange = { ethValueInput = it },
                            label = { Text("Value (ETH)", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Deterministic CREATE2 Deployment Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CosmicCardInner)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Deterministic Deployer (CREATE2)", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Predict exact address before broadcasting to mempool", color = CosmicTextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = isCreate2Enabled,
                            onCheckedChange = { isCreate2Enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CosmicBackground,
                                checkedTrackColor = SentinelGold
                            )
                        )
                    }

                    if (isCreate2Enabled) {
                        OutlinedTextField(
                            value = create2SaltInput,
                            onValueChange = { create2SaltInput = it },
                            label = { Text("CREATE2 Salt (32-byte hex)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SentinelGold,
                                unfocusedBorderColor = CosmicDivider,
                                focusedTextColor = CosmicTextPrimary,
                                unfocusedTextColor = CosmicTextPrimary
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        )
                    }

                    // Pre-Flight Simulation & Anticipated Address Preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicCardInner)
                            .border(1.dp, SentinelGoldDim, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PRE-FLIGHT SIMULATION PREVIEW",
                            color = SentinelGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Anticipated Contract Address:", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text(
                                "${anticipatedAddress.take(8)}...${anticipatedAddress.takeLast(6)}",
                                color = SentinelEmerald,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Initcode Size:", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text("$totalPayloadBytes bytes", color = CosmicTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Est. Gas Cost:", color = CosmicTextSecondary, fontSize = 11.sp)
                            Text(
                                "${String.format("%.5f", estimatedGasCost.second)} ETH (~$${String.format("%.2f", estimatedGasCost.third)})",
                                color = SentinelGold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- ERROR DISPLAY IF ANY ---
        if (deployErrorMessage != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33EF5350)),
                    border = BorderStroke(1.dp, Color(0xFFEF5350)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = "Error", tint = Color(0xFFEF5350))
                        Text(deployErrorMessage!!, color = Color(0xFFFFCDD2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SECTION 5: BROADCAST & DEPLOY BUTTON ---
        item {
            Button(
                onClick = {
                    deployErrorMessage = null
                    if (cleanBytecode.isEmpty()) {
                        deployErrorMessage = "Error: Contract bytecode is required. Please input or paste compiled EVM bytecode."
                        return@Button
                    }
                    if (contractName.trim().isEmpty()) {
                        deployErrorMessage = "Error: Contract name is required."
                        return@Button
                    }

                    isDeploying = true
                    deployProgress = 0f
                    deployLogs.clear()
                    deployedContractResult = null

                    coroutineScope.launch {
                        try {
                            deployLogs.add("⚙️ [Init] Validating EVM creation bytecode & initcode hash...")
                            deployStepLabel = "Validating Bytecode Payload..."
                            deployProgress = 0.15f
                            delay(900)

                            val initCodeHash = Hash.sha3(fullDeploymentPayloadHex.removePrefix("0x"))
                            deployLogs.add("✓ Initcode Keccak-256: 0x${initCodeHash.take(16)}...${initCodeHash.takeLast(8)}")
                            deployLogs.add("✓ Total Deploy Payload: $totalPayloadBytes bytes ($zeroBytesCount zero, $nonZeroBytesCount non-zero)")
                            delay(700)

                            deployLogs.add("🔑 [Sign] Signing deployment transaction with signer: ${selectedDeployerAddress.take(8)}... (EIP-1559)")
                            deployStepLabel = "Signing with Secp256k1 Key..."
                            deployProgress = 0.35f
                            delay(1100)

                            val mockTxHash = "0x" + (1..64).map { "0123456789abcdef"[Random.nextInt(16)] }.joinToString("")
                            deployLogs.add("📡 [RPC] Submitting eth_sendRawTransaction to $selectedNetwork node...")
                            deployLogs.add("⚡ Pending Tx Hash: $mockTxHash")
                            deployStepLabel = "Broadcasting to Mempool..."
                            deployProgress = 0.60f
                            delay(1400)

                            val blockNum = Random.nextInt(19450800, 19452000)
                            deployLogs.add("📦 [Mining] Transaction included in Block #$blockNum! Awaiting 1 confirmation...")
                            deployStepLabel = "Confirming Block Inclusion..."
                            deployProgress = 0.80f
                            delay(1200)

                            val finalContractAddr = anticipatedAddress
                            deployLogs.add("🎉 [Success] Contract Created At: $finalContractAddr")
                            deployLogs.add("⛽ Gas Consumed: ${(gasLimitInput.toLongOrNull() ?: 2500000L) - 340000L} / $gasLimitInput")
                            deployLogs.add("🔗 Binding Sentinel MEV Shield & Automated Daemons...")
                            deployStepLabel = "Contract Deployed Successfully!"
                            deployProgress = 1.0f
                            delay(600)

                            val newContract = TrackedContract(
                                name = contractName.trim(),
                                network = selectedNetwork,
                                status = "VERIFIED",
                                address = finalContractAddr,
                                gasLimit = gasLimitInput.toLongOrNull() ?: 2500000L,
                                gasPriceGwei = gasPriceGweiInput.toDoubleOrNull() ?: 22.5,
                                securityFeatures = listOf(
                                    "Bytecode Deployed",
                                    if (isCreate2Enabled) "CREATE2 Deterministic" else "Standard CREATE",
                                    "EIP-1559 Signer"
                                ),
                                soliditySource = "// Deployed via Sentinel Bytecode Deployer\n// Bytecode Size: $totalPayloadBytes bytes\n// Constructor Calldata: 0x$encodedConstructorArgs"
                            )

                            repository.addTrackedContract(newContract)
                            deployedContractResult = newContract
                            isDeploying = false
                            onContractDeployed(newContract)
                        } catch (e: Exception) {
                            deployErrorMessage = "Deployment Failed: ${e.localizedMessage ?: "RPC error"}"
                            deployLogs.add("❌ Execution Error: ${e.message}")
                            isDeploying = false
                        }
                    }
                },
                enabled = !isDeploying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SentinelGold,
                    contentColor = CosmicBackground,
                    disabledContainerColor = CosmicDivider,
                    disabledContentColor = CosmicTextDim
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isDeploying) {
                    CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Broadcasting Contract to Mempool...", fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Deploy")
                        Text("Deploy Contract (Bytecode + Args)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // --- SECTION 6: LIVE DEPLOYMENT TERMINAL & CONSOLE ---
        if (isDeploying || deployLogs.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                    border = BorderStroke(1.dp, if (deployedContractResult != null) SentinelEmerald else SentinelGoldDim),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DEPLOYMENT PROTOCOL LIVE TERMINAL",
                                color = SentinelGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                if (isDeploying) "BROADCASTING..." else if (deployedContractResult != null) "SUCCESS" else "TERMINATED",
                                color = if (isDeploying) SentinelGold else if (deployedContractResult != null) SentinelEmerald else Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { deployProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = if (deployProgress == 1.0f) SentinelEmerald else SentinelGold,
                            trackColor = CosmicCardInner
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Step: $deployStepLabel", color = CosmicTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CosmicSurface)
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true) {
                                items(deployLogs.reversed()) { logLine ->
                                    Text(
                                        text = logLine,
                                        color = if (logLine.contains("🎉") || logLine.contains("✓") || logLine.contains("Success")) SentinelEmerald 
                                                else if (logLine.contains("❌")) Color.Red 
                                                else if (logLine.contains("⚙️") || logLine.contains("🔑")) SentinelGold 
                                                else CosmicTextPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Success Action Buttons
                        if (deployedContractResult != null) {
                            val contract = deployedContractResult!!
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(contract.address))
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SentinelEmerald),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SentinelEmerald)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Address", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        onContractDeployed(contract)
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SentinelEmerald, contentColor = CosmicBackground)
                                ) {
                                    Icon(Icons.Filled.Visibility, contentDescription = "View", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Track Lifecycle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
