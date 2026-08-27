package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.util.concurrent.TimeUnit

/**
 * Supported Web3 Networks with pre-configured redundant RPC nodes.
 */
data class Web3Network(
    val name: String,
    val chainId: Long,
    val currencySymbol: String,
    val rpcUrls: List<String>,
    val explorerUrl: String,
    val isTestnet: Boolean = false
)

data class TransactionReceipt(
    val txHash: String,
    val status: Boolean, // true = 0x1 success, false = 0x0 reverted
    val blockNumber: Long,
    val gasUsed: Long,
    val contractAddress: String? = null,
    val logs: List<String> = emptyList()
)

data class NodeHealth(
    val rpcUrl: String,
    val latencyMs: Long,
    val blockHeight: Long,
    val isHealthy: Boolean
)

/**
 * Production-ready Web3 JSON-RPC 2.0 Client.
 * Provides end-to-end communication, reading, writing, contract execution,
 * gas estimation, transaction broadcasting, and receipt confirmation across EVM chains.
 */
class Web3RpcClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!

    companion object {
        val ETHEREUM_MAINNET = Web3Network(
            name = "Ethereum Mainnet",
            chainId = 1L,
            currencySymbol = "ETH",
            rpcUrls = listOf(
                "https://cloudflare-eth.com",
                "https://eth.llamarpc.com",
                "https://rpc.ankr.com/eth",
                "https://ethereum-rpc.publicnode.com"
            ),
            explorerUrl = "https://etherscan.io"
        )

        val ARBITRUM_ONE = Web3Network(
            name = "Arbitrum One",
            chainId = 42161L,
            currencySymbol = "ETH",
            rpcUrls = listOf(
                "https://arb1.arbitrum.io/rpc",
                "https://arbitrum.llamarpc.com",
                "https://rpc.ankr.com/arbitrum"
            ),
            explorerUrl = "https://arbiscan.io"
        )

        val OPTIMISM = Web3Network(
            name = "Optimism",
            chainId = 10L,
            currencySymbol = "ETH",
            rpcUrls = listOf(
                "https://mainnet.optimism.io",
                "https://optimism.llamarpc.com",
                "https://rpc.ankr.com/optimism"
            ),
            explorerUrl = "https://optimistic.etherscan.io"
        )

        val BASE = Web3Network(
            name = "Base",
            chainId = 8453L,
            currencySymbol = "ETH",
            rpcUrls = listOf(
                "https://mainnet.base.org",
                "https://base.llamarpc.com",
                "https://base-rpc.publicnode.com"
            ),
            explorerUrl = "https://basescan.org"
        )

        val POLYGON = Web3Network(
            name = "Polygon PoS",
            chainId = 137L,
            currencySymbol = "MATIC",
            rpcUrls = listOf(
                "https://polygon-rpc.com",
                "https://polygon.llamarpc.com",
                "https://rpc.ankr.com/polygon"
            ),
            explorerUrl = "https://polygonscan.com"
        )

        val BSC = Web3Network(
            name = "BNB Smart Chain",
            chainId = 56L,
            currencySymbol = "BNB",
            rpcUrls = listOf(
                "https://bsc-dataseed.binance.org",
                "https://binance.llamarpc.com",
                "https://rpc.ankr.com/bsc"
            ),
            explorerUrl = "https://bscscan.com"
        )

        val SEPOLIA = Web3Network(
            name = "Sepolia Testnet",
            chainId = 11155111L,
            currencySymbol = "SepoliaETH",
            rpcUrls = listOf(
                "https://rpc.sepolia.org",
                "https://ethereum-sepolia.publicnode.com",
                "https://sepolia.gateway.tenderly.co"
            ),
            explorerUrl = "https://sepolia.etherscan.io",
            isTestnet = true
        )

        val ALL_SUPPORTED_NETWORKS = listOf(
            ETHEREUM_MAINNET,
            ARBITRUM_ONE,
            OPTIMISM,
            BASE,
            POLYGON,
            BSC,
            SEPOLIA
        )

        fun findNetworkByName(name: String): Web3Network {
            val lower = name.lowercase()
            return when {
                lower.contains("arbitrum") -> ARBITRUM_ONE
                lower.contains("optimism") -> OPTIMISM
                lower.contains("base") -> BASE
                lower.contains("polygon") -> POLYGON
                lower.contains("bsc") || lower.contains("binance") -> BSC
                lower.contains("sepolia") || lower.contains("testnet") -> SEPOLIA
                else -> ETHEREUM_MAINNET
            }
        }
    }

    /**
     * Executes a raw JSON-RPC call across available node endpoints with automatic failover.
     */
    private suspend fun executeRpc(
        network: Web3Network,
        customRpcUrl: String? = null,
        method: String,
        params: JSONArray = JSONArray()
    ): String? = withContext(Dispatchers.IO) {
        val urlsToTry = mutableListOf<String>()
        if (!customRpcUrl.isNullOrBlank() && customRpcUrl.startsWith("http") && !customRpcUrl.contains("your_key")) {
            urlsToTry.add(customRpcUrl)
        }
        urlsToTry.addAll(network.rpcUrls)

        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", method)
            put("params", params)
            put("id", System.currentTimeMillis() % 100000)
        }.toString()

        for (url in urlsToTry) {
            try {
                val reqBody = payload.toRequestBody(mediaType)
                val request = Request.Builder().url(url).post(reqBody).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val json = JSONObject(bodyString)
                        if (json.has("result") && !json.isNull("result")) {
                            return@withContext json.optString("result")
                        }
                    }
                }
            } catch (_: Exception) {
                // Failover to next RPC node in list
            }
        }
        return@withContext null
    }

    /**
     * Measure ping latency and query current block height of an RPC node.
     */
    suspend fun pingNode(rpcUrl: String): NodeHealth = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val payload = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}".toRequestBody(mediaType)
            val req = Request.Builder().url(rpcUrl).post(payload).build()
            httpClient.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val hex = json.optString("result")
                    val block = if (hex.startsWith("0x")) java.lang.Long.decode(hex) else 0L
                    NodeHealth(rpcUrl, latency, block, isHealthy = true)
                } else {
                    NodeHealth(rpcUrl, latency, 0L, isHealthy = false)
                }
            }
        } catch (_: Exception) {
            val latency = System.currentTimeMillis() - start
            NodeHealth(rpcUrl, latency, 0L, isHealthy = false)
        }
    }

    /**
     * Queries the latest block number on the selected chain.
     */
    suspend fun getBlockNumber(network: Web3Network, customRpcUrl: String? = null): Long? {
        val result = executeRpc(network, customRpcUrl, "eth_blockNumber")
        return if (result != null && result.startsWith("0x")) {
            try { java.lang.Long.decode(result) } catch (_: Exception) { null }
        } else null
    }

    /**
     * Queries the balance of an address in native token (ETH, MATIC, BNB).
     */
    suspend fun getBalance(address: String, network: Web3Network, customRpcUrl: String? = null): Double? {
        val wei = getBalanceWei(address, network, customRpcUrl) ?: return null
        return wei.toDouble() / 1_000_000_000_000_000_000.0
    }

    /**
     * Queries the balance of an address in Wei (BigInteger).
     */
    suspend fun getBalanceWei(address: String, network: Web3Network, customRpcUrl: String? = null): BigInteger? {
        val params = JSONArray().apply {
            put(address)
            put("latest")
        }
        val result = executeRpc(network, customRpcUrl, "eth_getBalance", params)
        return if (result != null && result.startsWith("0x")) {
            try { BigInteger(result.removePrefix("0x"), 16) } catch (_: Exception) { null }
        } else null
    }

    /**
     * Queries the transaction count (nonce) of an account.
     */
    suspend fun getTransactionCount(address: String, network: Web3Network, customRpcUrl: String? = null): Long? {
        val params = JSONArray().apply {
            put(address)
            put("pending")
        }
        val result = executeRpc(network, customRpcUrl, "eth_getTransactionCount", params)
        return if (result != null && result.startsWith("0x")) {
            try { java.lang.Long.decode(result) } catch (_: Exception) { null }
        } else null
    }

    /**
     * Queries the current base gas price in Gwei.
     */
    suspend fun getGasPriceGwei(network: Web3Network, customRpcUrl: String? = null): Double? {
        val result = executeRpc(network, customRpcUrl, "eth_gasPrice")
        return if (result != null && result.startsWith("0x")) {
            try {
                val wei = BigInteger(result.removePrefix("0x"), 16)
                wei.toDouble() / 1_000_000_000.0
            } catch (_: Exception) { null }
        } else null
    }

    /**
     * Queries the current max priority fee per gas (tip) in Gwei.
     */
    suspend fun getMaxPriorityFeePerGasGwei(network: Web3Network, customRpcUrl: String? = null): Double? {
        val result = executeRpc(network, customRpcUrl, "eth_maxPriorityFeePerGas")
        return if (result != null && result.startsWith("0x")) {
            try {
                val wei = BigInteger(result.removePrefix("0x"), 16)
                wei.toDouble() / 1_000_000_000.0
            } catch (_: Exception) { null }
        } else null
    }

    /**
     * Estimates gas needed for a given transaction call.
     */
    suspend fun estimateGas(
        from: String,
        to: String,
        data: String,
        valueWei: BigInteger = BigInteger.ZERO,
        network: Web3Network,
        customRpcUrl: String? = null
    ): Long? {
        val txObj = JSONObject().apply {
            put("from", from)
            put("to", to)
            put("data", data)
            if (valueWei > BigInteger.ZERO) {
                put("value", "0x" + valueWei.toString(16))
            }
        }
        val params = JSONArray().apply { put(txObj) }
        val result = executeRpc(network, customRpcUrl, "eth_estimateGas", params)
        return if (result != null && result.startsWith("0x")) {
            try { java.lang.Long.decode(result) } catch (_: Exception) { null }
        } else null
    }

    /**
     * Executes a read-only smart contract view function via `eth_call`.
     */
    suspend fun callViewFunction(
        to: String,
        data: String,
        from: String? = null,
        network: Web3Network,
        customRpcUrl: String? = null
    ): String? {
        val callObj = JSONObject().apply {
            if (!from.isNullOrBlank()) put("from", from)
            put("to", to)
            put("data", data)
        }
        val params = JSONArray().apply {
            put(callObj)
            put("latest")
        }
        return executeRpc(network, customRpcUrl, "eth_call", params)
    }

    /**
     * Verifies if a contract is deployed on-chain by fetching its bytecode via `eth_getCode`.
     */
    suspend fun getCode(contractAddress: String, network: Web3Network, customRpcUrl: String? = null): String? {
        val params = JSONArray().apply {
            put(contractAddress)
            put("latest")
        }
        return executeRpc(network, customRpcUrl, "eth_getCode", params)
    }

    /**
     * Broadcasts a cryptographically signed raw transaction to the network mempool.
     * Returns the 32-byte Transaction Hash if accepted.
     */
    suspend fun sendRawTransaction(signedTxHex: String, network: Web3Network, customRpcUrl: String? = null): String? {
        val cleanHex = if (signedTxHex.startsWith("0x")) signedTxHex else "0x$signedTxHex"
        val params = JSONArray().apply { put(cleanHex) }
        return executeRpc(network, customRpcUrl, "eth_sendRawTransaction", params)
    }

    /**
     * Queries the transaction receipt to check inclusion status, block number, and gas used.
     */
    suspend fun getTransactionReceipt(txHash: String, network: Web3Network, customRpcUrl: String? = null): TransactionReceipt? = withContext(Dispatchers.IO) {
        val urlsToTry = mutableListOf<String>()
        if (!customRpcUrl.isNullOrBlank() && customRpcUrl.startsWith("http") && !customRpcUrl.contains("your_key")) {
            urlsToTry.add(customRpcUrl)
        }
        urlsToTry.addAll(network.rpcUrls)

        val cleanHash = if (txHash.startsWith("0x")) txHash else "0x$txHash"
        val payload = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", "eth_getTransactionReceipt")
            put("params", JSONArray().apply { put(cleanHash) })
            put("id", 1)
        }.toString()

        for (url in urlsToTry) {
            try {
                val reqBody = payload.toRequestBody(mediaType)
                val request = Request.Builder().url(url).post(reqBody).build()
                httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val result = json.optJSONObject("result")
                        if (result != null) {
                            val statusHex = result.optString("status", "0x1")
                            val isSuccess = statusHex == "0x1" || statusHex == "1"
                            val blockHex = result.optString("blockNumber", "0x0")
                            val blockNum = if (blockHex.startsWith("0x")) java.lang.Long.decode(blockHex) else 0L
                            val gasHex = result.optString("gasUsed", "0x0")
                            val gasUsed = if (gasHex.startsWith("0x")) java.lang.Long.decode(gasHex) else 0L
                            val contractAddr = result.optString("contractAddress").ifBlank { null }
                            
                            val logArray = result.optJSONArray("logs")
                            val logs = mutableListOf<String>()
                            if (logArray != null) {
                                for (i in 0 until logArray.length()) {
                                    val logObj = logArray.getJSONObject(i)
                                    logs.add(logObj.toString())
                                }
                            }

                            return@withContext TransactionReceipt(
                                txHash = cleanHash,
                                status = isSuccess,
                                blockNumber = blockNum,
                                gasUsed = gasUsed,
                                contractAddress = contractAddr,
                                logs = logs
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next RPC
            }
        }
        return@withContext null
    }
}
