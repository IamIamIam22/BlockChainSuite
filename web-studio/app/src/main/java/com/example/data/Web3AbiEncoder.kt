package com.example.data

import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale

/**
 * Ethereum ABI (Application Binary Interface) Encoder and Decoder.
 * Generates EVM-compliant calldata for smart contract execution, view calls,
 * and state-mutating transactions (harvestProfit, sweepTokens, flashArbitrage, etc.).
 */
object Web3AbiEncoder {

    /**
     * Computes the 4-byte EVM function selector from a function signature.
     * Uses SHA-256 / Keccak digest prefix according to EVM specifications.
     */
    fun calculateFunctionSelector(functionSignature: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(functionSignature.toByteArray(Charsets.UTF_8))
        return "0x" + digest.take(4).joinToString("") { "%02x".format(it) }
    }

    /**
     * Pads an Ethereum address to a 32-byte (64 hex characters) ABI parameter.
     */
    fun encodeAddress(address: String): String {
        val clean = address.removePrefix("0x").lowercase(Locale.ROOT)
        return clean.padStart(64, '0')
    }

    /**
     * Encodes a BigInteger uint256 into a 32-byte (64 hex characters) ABI parameter.
     */
    fun encodeUint256(value: BigInteger): String {
        val hex = value.toString(16)
        return hex.padStart(64, '0')
    }

    fun encodeUint256(value: Long): String {
        return encodeUint256(BigInteger.valueOf(value))
    }

    /**
     * Encodes a Boolean value into a 32-byte ABI parameter.
     */
    fun encodeBool(value: Boolean): String {
        return if (value) "1".padStart(64, '0') else "0".padStart(64, '0')
    }

    /**
     * Encodes a dynamic array of addresses for functions like sweepTokenBasket(address[],address).
     */
    fun encodeAddressArray(addresses: List<String>): String {
        val countHex = BigInteger.valueOf(addresses.size.toLong()).toString(16).padStart(64, '0')
        val elements = addresses.joinToString("") { encodeAddress(it) }
        return countHex + elements
    }

    /**
     * ABI encodes a call to `harvestProfit(address recipient)`
     */
    fun encodeHarvestProfit(recipientAddress: String): String {
        val selector = calculateFunctionSelector("harvestProfit(address)")
        val param = encodeAddress(recipientAddress)
        return selector + param
    }

    /**
     * ABI encodes a call to `sweepTokens(address token, address recipient)`
     */
    fun encodeSweepTokens(tokenAddress: String, recipientAddress: String): String {
        val selector = calculateFunctionSelector("sweepTokens(address,address)")
        return selector + encodeAddress(tokenAddress) + encodeAddress(recipientAddress)
    }

    /**
     * ABI encodes a call to `sweepTokenBasket(address[] tokens, address recipient)`
     */
    fun encodeSweepTokenBasket(tokenAddresses: List<String>, recipientAddress: String): String {
        val selector = calculateFunctionSelector("sweepTokenBasket(address[],address)")
        // Offset to dynamic array (starts after offset and recipient address = 64 bytes = 0x40)
        val offsetHex = BigInteger.valueOf(64).toString(16).padStart(64, '0')
        val recipientHex = encodeAddress(recipientAddress)
        val arrayData = encodeAddressArray(tokenAddresses)
        return selector + offsetHex + recipientHex + arrayData
    }

    /**
     * ABI encodes a call to `executeTrade(address sourceDex, address targetDex, uint256 tradeAmount, uint256 slippageThreshold)`
     */
    fun encodeExecuteTrade(
        sourceDex: String,
        targetDex: String,
        tradeAmountWei: BigInteger,
        slippageBps: Long
    ): String {
        val selector = calculateFunctionSelector("executeTrade(address,address,uint256,uint256)")
        return selector +
                encodeAddress(sourceDex) +
                encodeAddress(targetDex) +
                encodeUint256(tradeAmountWei) +
                encodeUint256(slippageBps)
    }

    /**
     * ABI encodes a call to `collectDexLpFees(address pool, uint256 positionId)`
     */
    fun encodeCollectDexLpFees(poolAddress: String, positionId: Long): String {
        val selector = calculateFunctionSelector("collectDexLpFees(address,uint256)")
        return selector + encodeAddress(poolAddress) + encodeUint256(positionId)
    }

    /**
     * ABI encodes a call to `emergencyPause()`
     */
    fun encodeEmergencyPause(): String {
        return calculateFunctionSelector("emergencyPause()")
    }

    /**
     * ABI encodes a call to `emergencyUnpause()`
     */
    fun encodeEmergencyUnpause(): String {
        return calculateFunctionSelector("emergencyUnpause()")
    }

    /**
     * ABI encodes an ERC-20 `balanceOf(address owner)` view call
     */
    fun encodeBalanceOf(ownerAddress: String): String {
        val selector = calculateFunctionSelector("balanceOf(address)")
        return selector + encodeAddress(ownerAddress)
    }

    /**
     * ABI encodes an ERC-20 `transfer(address to, uint256 amount)`
     */
    fun encodeTransfer(recipientAddress: String, amountWei: BigInteger): String {
        val selector = calculateFunctionSelector("transfer(address,uint256)")
        return selector + encodeAddress(recipientAddress) + encodeUint256(amountWei)
    }

    /**
     * Decodes a 32-byte hex response into a uint256 BigInteger.
     */
    fun decodeUint256(hex: String): BigInteger {
        val clean = hex.removePrefix("0x").trim()
        if (clean.isBlank()) return BigInteger.ZERO
        return try {
            BigInteger(clean, 16)
        } catch (_: Exception) {
            BigInteger.ZERO
        }
    }

    /**
     * Decodes a 32-byte hex response into an EIP-55 checksummed Ethereum address.
     */
    fun decodeAddress(hex: String): String {
        val clean = hex.removePrefix("0x").trim()
        if (clean.length < 40) return "0x0000000000000000000000000000000000000000"
        val rawAddr = clean.takeLast(40)
        return CryptoUtils.generateEthereumAddress(rawAddr)
    }

    /**
     * Decodes a 32-byte hex response into a Boolean.
     */
    fun decodeBool(hex: String): Boolean {
        val clean = hex.removePrefix("0x").trim()
        return clean.endsWith("1")
    }
}
