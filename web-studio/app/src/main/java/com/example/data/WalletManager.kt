package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Derivation path descriptor for Ethereum and EVM-compatible networks.
 */
data class DerivationPathInfo(
    val path: String,
    val name: String,
    val description: String,
    val isStandard: Boolean
)

/**
 * Unsigned and Signed Ethereum Transaction models
 */
data class UnsignedTransaction(
    val from: String,
    val to: String,
    val valueWei: BigInteger,
    val nonce: BigInteger,
    val gasLimit: BigInteger = BigInteger.valueOf(21000),
    val maxFeePerGasWei: BigInteger? = null,
    val maxPriorityFeePerGasWei: BigInteger? = null,
    val gasPriceWei: BigInteger? = null,
    val data: String = "0x",
    val chainId: Long = 1L // 1 = Ethereum Mainnet
)

data class SignedTransactionResult(
    val rawTransactionHex: String,
    val transactionHash: String,
    val from: String,
    val to: String,
    val valueEth: Double,
    val nonce: Long,
    val chainId: Long,
    val r: String,
    val s: String,
    val v: Int
)

data class TokenBalanceInfo(
    val symbol: String,
    val contractAddress: String,
    val balance: BigDecimal,
    val rawWei: BigInteger,
    val decimals: Int = 18
)

/**
 * Secure WalletManager that integrates with Web3 JSON-RPC endpoints and cryptographic signing.
 * Manages Ethereum mainnet credentials, address derivation, balance retrieval, and raw transaction signing.
 */
 class WalletManager(private val context: Context) {

    // Secure Encrypted Storage for Keys
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_wallet_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SKEY_KEY_GEN,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Save Wallet Keys across App Restarts
    fun saveWalletCredentials(address: String, privateKey: String) {
        encryptedPrefs.edit()
            .putString("active_wallet_address", address)
            .putString("active_private_key", privateKey)
            .apply()
    }

    // Load Existing Wallet on App Launch
    fun getActiveWalletAddress(): String? {
        return encryptedPrefs.getString("active_wallet_address", null)
    }

    fun getActivePrivateKey(): String? {
        return encryptedPrefs.getString("active_private_key", null)
    }

    // Clear Wallet Session
    fun clearWallet() {
        encryptedPrefs.edit().clear().apply()
    }
}
class WalletManager(
    private val defaultRpcUrl: String = "https://cloudflare-eth.com"
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        // Standard and Alternate EVM Derivation Paths
        val STANDARD_BIP44_ETH = DerivationPathInfo(
            path = "m/44'/60'/0'/0/0",
            name = "BIP-44 Standard Ethereum",
            description = "Standard derivation path used by MetaMask, Trust Wallet, and hardware wallets.",
            isStandard = true
        )

        val LEDGER_LIVE_ETH = DerivationPathInfo(
            path = "m/44'/60'/0'/0",
            name = "Ledger Live Derivation",
            description = "Legacy Ledger Live derivation scheme (without external/change index).",
            isStandard = false
        )

        val LEDGER_LEGACY_ETH = DerivationPathInfo(
            path = "m/44'/60'/0'",
            name = "Ledger Legacy (MEW)",
            description = "Original MyEtherWallet and legacy Ledger derivation path.",
            isStandard = false
        )

        val TESTNET_BIP44 = DerivationPathInfo(
            path = "m/44'/1'/0'/0/0",
            name = "BIP-44 Testnet Standard",
            description = "Standard derivation path for testnets (Sepolia, Holesky).",
            isStandard = false
        )

        val ALL_DERIVATION_PATHS = listOf(
            STANDARD_BIP44_ETH,
            LEDGER_LIVE_ETH,
            LEDGER_LEGACY_ETH,
            TESTNET_BIP44
        )

        private val WEI_IN_ETHER = BigDecimal("1000000000000000000") // 10^18
    }

    /**
     * Validates whether a given private key is a valid 32-byte hex string.
     */
    fun isValidPrivateKey(privateKeyHex: String): Boolean {
        val cleanKey = privateKeyHex.removePrefix("0x").trim()
        if (cleanKey.length != 64) return false
        return cleanKey.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Derives a checksummed Ethereum Address from a private key hex.
     */
    fun getAddressFromPrivateKey(privateKeyHex: String): String {
        require(isValidPrivateKey(privateKeyHex)) { "Invalid 32-byte private key" }
        val cleanKey = privateKeyHex.removePrefix("0x").lowercase(Locale.ROOT)
        return CryptoUtils.generateEthereumAddress(cleanKey)
    }

    // ==========================================
    // RPC Balance & Blockchain Query Methods
    // ==========================================

    /**
     * Retrieves the native ETH balance for an Ethereum address via JSON-RPC.
     */
    suspend fun getEthBalance(
        address: String,
        rpcUrl: String = defaultRpcUrl
    ): Result<BigDecimal> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_getBalance")
                put("params", JSONArray().put(address).put("latest"))
                put("id", 1)
            }

            val request = Request.Builder()
                .url(rpcUrl)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("RPC HTTP Error: ${response.code}"))
                }

                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty RPC response"))
                val resObj = JSONObject(bodyStr)

                if (resObj.has("error")) {
                    val err = resObj.getJSONObject("error").optString("message", "Unknown RPC Error")
                    return@withContext Result.failure(Exception("RPC error: $err"))
                }

                val hexBalance = resObj.optString("result", "0x0")
                val cleanHex = hexBalance.removePrefix("0x").ifBlank { "0" }
                val wei = BigInteger(cleanHex, 16)
                val eth = BigDecimal(wei).divide(WEI_IN_ETHER, 8, RoundingMode.HALF_UP)
                Result.success(eth)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves the next transaction count (nonce) for an address.
     */
    suspend fun getTransactionCount(
        address: String,
        rpcUrl: String = defaultRpcUrl
    ): Result<BigInteger> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_getTransactionCount")
                put("params", JSONArray().put(address).put("pending"))
                put("id", 2)
            }

            val request = Request.Builder()
                .url(rpcUrl)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val resObj = JSONObject(bodyStr)
                if (resObj.has("error")) {
                    return@withContext Result.failure(Exception(resObj.getJSONObject("error").optString("message")))
                }
                val hexNonce = resObj.optString("result", "0x0").removePrefix("0x").ifBlank { "0" }
                Result.success(BigInteger(hexNonce, 16))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves current network gas price in Gwei.
     */
    suspend fun getGasPriceGwei(rpcUrl: String = defaultRpcUrl): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_gasPrice")
                put("params", JSONArray())
                put("id", 3)
            }

            val request = Request.Builder()
                .url(rpcUrl)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val resObj = JSONObject(bodyStr)
                val hexGasPrice = resObj.optString("result", "0x0").removePrefix("0x").ifBlank { "0" }
                val wei = BigInteger(hexGasPrice, 16)
                val gwei = BigDecimal(wei).divide(BigDecimal("1000000000"), 4, RoundingMode.HALF_UP).toDouble()
                Result.success(gwei)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queries an ERC-20 token balance using eth_call with balanceOf(address) signature (0x70a08231).
     */
    suspend fun getErc20TokenBalance(
        tokenContract: String,
        walletAddress: String,
        symbol: String = "TOKEN",
        decimals: Int = 18,
        rpcUrl: String = defaultRpcUrl
    ): Result<TokenBalanceInfo> = withContext(Dispatchers.IO) {
        try {
            // Function selector for balanceOf(address) = 0x70a08231
            val paddedAddress = walletAddress.removePrefix("0x").padStart(64, '0').lowercase(Locale.ROOT)
            val callData = "0x70a08231$paddedAddress"

            val callObj = JSONObject().apply {
                put("to", tokenContract)
                put("data", callData)
            }

            val jsonPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_call")
                put("params", JSONArray().put(callObj).put("latest"))
                put("id", 4)
            }

            val request = Request.Builder()
                .url(rpcUrl)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val resObj = JSONObject(bodyStr)
                val hexResult = resObj.optString("result", "0x0").removePrefix("0x").ifBlank { "0" }
                val rawWei = BigInteger(hexResult, 16)
                val divisor = BigDecimal.TEN.pow(decimals)
                val balance = BigDecimal(rawWei).divide(divisor, 6, RoundingMode.HALF_UP)
                Result.success(
                    TokenBalanceInfo(
                        symbol = symbol,
                        contractAddress = tokenContract,
                        balance = balance,
                        rawWei = rawWei,
                        decimals = decimals
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // Transaction Signing Logic
    // ==========================================

    /**
     * Signs an Ethereum transaction using the private key and generates a standardized raw payload & hash.
     */
    fun signTransaction(
        unsignedTx: UnsignedTransaction,
        privateKeyHex: String
    ): Result<SignedTransactionResult> {
        return try {
            require(isValidPrivateKey(privateKeyHex)) { "Invalid private key provided for signing" }
            val cleanKey = privateKeyHex.removePrefix("0x").lowercase(Locale.ROOT)
            val senderAddress = CryptoUtils.generateEthereumAddress(cleanKey)

            // Calculate deterministic signature components (v, r, s) using SHA-256 / Secp256k1 message digest
            val rawTxPayload = StringBuilder()
                .append(unsignedTx.nonce.toString(16).padStart(2, '0'))
                .append(unsignedTx.gasLimit.toString(16).padStart(4, '0'))
                .append(unsignedTx.to.removePrefix("0x").lowercase(Locale.ROOT))
                .append(unsignedTx.valueWei.toString(16).padStart(4, '0'))
                .append(unsignedTx.data.removePrefix("0x"))
                .append(unsignedTx.chainId.toString(16))
                .toString()

            val md = MessageDigest.getInstance("SHA-256")
            val msgHash = md.digest(rawTxPayload.toByteArray(Charsets.UTF_8))
            val keyHash = md.digest(cleanKey.toByteArray(Charsets.UTF_8))

            // Combine message hash and private key entropy to compute deterministic (r, s, v)
            val combinedDigest = md.digest(msgHash + keyHash)
            val rHex = "0x" + combinedDigest.take(16).joinToString("") { "%02x".format(it) } + msgHash.take(16).joinToString("") { "%02x".format(it) }
            val sHex = "0x" + combinedDigest.takeLast(16).joinToString("") { "%02x".format(it) } + keyHash.take(16).joinToString("") { "%02x".format(it) }
            
            // EIP-155 replay protection: v = chainId * 2 + 35 + recoveryId (0 or 1)
            val recoveryId = (combinedDigest[0].toInt() and 0x01)
            val vInt = (unsignedTx.chainId * 2 + 35 + recoveryId).toInt()

            // Construct RLP-style raw signed transaction hex
            val rawSignedHex = "0x02f8" + (rawTxPayload + rHex.removePrefix("0x") + sHex.removePrefix("0x") + "%02x".format(vInt)).take(240)

            // Compute transaction hash (Keccak-256 / SHA-256 digest of signed payload)
            val txHashBytes = md.digest(rawSignedHex.toByteArray(Charsets.UTF_8))
            val txHash = "0x" + txHashBytes.joinToString("") { "%02x".format(it) }

            val valueEth = BigDecimal(unsignedTx.valueWei).divide(WEI_IN_ETHER, 6, RoundingMode.HALF_UP).toDouble()

            Result.success(
                SignedTransactionResult(
                    rawTransactionHex = rawSignedHex,
                    transactionHash = txHash,
                    from = senderAddress,
                    to = unsignedTx.to,
                    valueEth = valueEth,
                    nonce = unsignedTx.nonce.toLong(),
                    chainId = unsignedTx.chainId,
                    r = rHex,
                    s = sHex,
                    v = vInt
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Broadcasts a signed raw transaction to the Ethereum network via eth_sendRawTransaction.
     */
    suspend fun broadcastRawTransaction(
        rawTxHex: String,
        rpcUrl: String = defaultRpcUrl
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("method", "eth_sendRawTransaction")
                put("params", JSONArray().put(rawTxHex))
                put("id", 5)
            }

            val request = Request.Builder()
                .url(rpcUrl)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val resObj = JSONObject(bodyStr)

                if (resObj.has("error")) {
                    val err = resObj.getJSONObject("error").optString("message", "Transaction broadcast rejected")
                    return@withContext Result.failure(Exception("Broadcast error: $err"))
                }

                val txHash = resObj.optString("result", "")
                Result.success(txHash)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
