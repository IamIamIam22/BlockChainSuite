package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RecommendedFees(
    val fastestFee: Int = 25,
    val halfHourFee: Int = 18,
    val hourFee: Int = 12,
    val minimumFee: Int = 5,
    val economyFee: Int = 8
)

data class BitcoinAddressData(
    val address: String,
    val chainStats: AddressStats,
    val mempoolStats: AddressStats
) {
    val totalReceivedSatoshis: Long get() = chainStats.fundedTxoSum
    val totalSentSatoshis: Long get() = chainStats.spentTxoSum
    val totalSatoshis: Long get() = (chainStats.fundedTxoSum - chainStats.spentTxoSum).coerceAtLeast(0L)
    val btcBalance: Double get() = totalSatoshis.toDouble() / 100_000_000.0
    val fundedTxCount: Int get() = chainStats.fundedTxoCount
    val spentTxCount: Int get() = chainStats.spentTxoCount
    val unspentTxCount: Int get() = (chainStats.fundedTxoCount - chainStats.spentTxoCount).coerceAtLeast(0)
    val pendingSatoshis: Long get() = (mempoolStats.fundedTxoSum - mempoolStats.spentTxoSum)
}

data class AddressStats(
    val fundedTxoCount: Int = 0,
    val fundedTxoSum: Long = 0L,
    val spentTxoCount: Int = 0,
    val spentTxoSum: Long = 0L,
    val txCount: Int = 0
)

data class MempoolStatus(
    val count: Int = 0,
    val vbytes: Long = 0L,
    val totalFeeSat: Long = 0L,
    val mempoolByteSize: Long = 0L
)

data class BitcoinMempoolTx(
    val txid: String,
    val feeSat: Long,
    val vsize: Long,
    val valueSat: Long,
    val isRbf: Boolean = false,
    val time: Long = System.currentTimeMillis()
)

class BitcoinDataService(var network: String = "mainnet") {
    val baseUrl: String
        get() = if (network.equals("testnet", ignoreCase = true)) "https://mempool.space/testnet/api" else "https://mempool.space/api"
    val wsUrl: String
        get() = if (network.equals("testnet", ignoreCase = true)) "wss://mempool.space/testnet/api/v1/ws" else "wss://mempool.space/api/v1/ws"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun getTipBlockHeight(): Long? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/blocks/tip/height").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.string()?.trim()?.toLongOrNull()
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTipBlockHash(): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/blocks/tip/hash").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.string()?.trim()
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecommendedFees(): RecommendedFees? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/v1/fees/recommended").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    RecommendedFees(
                        fastestFee = json.optInt("fastestFee", 25),
                        halfHourFee = json.optInt("halfHourFee", 18),
                        hourFee = json.optInt("hourFee", 12),
                        minimumFee = json.optInt("minimumFee", 5),
                        economyFee = json.optInt("economyFee", 8)
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAddressData(address: String): BitcoinAddressData? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/address/$address").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    val chainObj = json.optJSONObject("chain_stats")
                    val mempoolObj = json.optJSONObject("mempool_stats")
                    val chainStats = if (chainObj != null) AddressStats(
                        fundedTxoCount = chainObj.optInt("funded_txo_count", 0),
                        fundedTxoSum = chainObj.optLong("funded_txo_sum", 0L),
                        spentTxoCount = chainObj.optInt("spent_txo_count", 0),
                        spentTxoSum = chainObj.optLong("spent_txo_sum", 0L),
                        txCount = chainObj.optInt("tx_count", 0)
                    ) else AddressStats()
                    val mempoolStats = if (mempoolObj != null) AddressStats(
                        fundedTxoCount = mempoolObj.optInt("funded_txo_count", 0),
                        fundedTxoSum = mempoolObj.optLong("funded_txo_sum", 0L),
                        spentTxoCount = mempoolObj.optInt("spent_txo_count", 0),
                        spentTxoSum = mempoolObj.optLong("spent_txo_sum", 0L),
                        txCount = mempoolObj.optInt("tx_count", 0)
                    ) else AddressStats()
                    BitcoinAddressData(address, chainStats, mempoolStats)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMempoolStatus(): MempoolStatus? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/mempool").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return@use null
                    val json = JSONObject(body)
                    MempoolStatus(
                        count = json.optInt("count", 0),
                        vbytes = json.optLong("vsize", 0L),
                        totalFeeSat = json.optLong("total_fee", 0L),
                        mempoolByteSize = json.optLong("mempool_byte_size", 0L)
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRecentMempoolTxs(): List<BitcoinMempoolTx> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/mempool/recent").build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: return@use emptyList()
                    val arr = JSONArray(body)
                    val list = mutableListOf<BitcoinMempoolTx>()
                    for (i in 0 until arr.length().coerceAtMost(10)) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            BitcoinMempoolTx(
                                txid = obj.optString("txid"),
                                feeSat = obj.optLong("fee", 0L),
                                vsize = obj.optLong("vsize", 0L),
                                valueSat = obj.optLong("value", 0L),
                                isRbf = obj.optBoolean("rbf", false)
                            )
                        )
                    }
                    list
                } else emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun broadcastBitcoinTransaction(rawTxHex: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/tx")
                .post(rawTxHex.toRequestBody("text/plain".toMediaTypeOrNull()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string()?.trim() ?: ""
                if (resp.isSuccessful) {
                    Result.success(bodyStr)
                } else {
                    Result.failure(Exception("Bitcoin Broadcast Error: HTTP ${resp.code} - $bodyStr"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
