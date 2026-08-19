package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "tracked_contracts")
data class TrackedContractEntity(
    @PrimaryKey val id: String,
    val name: String,
    val network: String,
    val status: String,
    val address: String,
    val gasLimit: Long,
    val gasPriceGwei: Double,
    val deployedAt: Long,
    val securityFeatures: String, // Comma separated list
    val soliditySource: String
)

@Entity(tableName = "arbitrage_opportunities")
data class ArbitrageOpportunityEntity(
    @PrimaryKey val id: String,
    val sourceChain: String,
    val targetChain: String,
    val asset: String,
    val buyDex: String,
    val sellDex: String,
    val priceDiffPct: Double,
    val estProfitUsd: Double,
    val requiredGasUsd: Double,
    val status: String
)

@Entity(tableName = "transaction_history")
data class TransactionHistoryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val txHash: String,
    val contractAddress: String,
    val type: String, // "ARBITRAGE", "SANDWICH", "LIQUIDATION"
    val asset: String,
    val profitBtc: Double,
    val profitEth: Double,
    val gasSpent: Long,
    val status: String, // "SUCCESS", "REVERTED"
    val chain: String,
    val blockNumber: Long = 0L,
    val route: String = "",
    val gasPriceGwei: Double = 0.0
)

@Entity(tableName = "security_configurations")
data class SecurityConfigurationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetNetwork: String,
    val transactionType: String,
    val enabledFeatures: String, // Comma separated feature keys
    val reentrancyGuardEnabled: Boolean,
    val accessControlEnabled: Boolean,
    val slippageLockEnabled: Boolean,
    val safeMathEnabled: Boolean,
    val pausableEnabled: Boolean,
    val eip712Enabled: Boolean,
    val oracleTwapGuardEnabled: Boolean,
    val erc4337PaymasterEnabled: Boolean,
    val antiFrontrunningEnabled: Boolean,
    val rateLimiterEnabled: Boolean,
    val customMaxSlippageBps: Int,
    val customMaxRateLimitEth: Double,
    val customTwapWindowSeconds: Int,
    val securityScore: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

// --- DAOs ---

@Dao
interface MevDao {
    // Tracked Contracts
    @Query("SELECT * FROM tracked_contracts ORDER BY deployedAt DESC")
    fun getAllTrackedContractsFlow(): Flow<List<TrackedContractEntity>>

    @Query("SELECT * FROM tracked_contracts ORDER BY deployedAt DESC")
    suspend fun getAllTrackedContracts(): List<TrackedContractEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: TrackedContractEntity)

    @Update
    suspend fun updateContract(contract: TrackedContractEntity)

    @Query("DELETE FROM tracked_contracts WHERE id = :id")
    suspend fun deleteContract(id: String)

    // Arbitrage Opportunities
    @Query("SELECT * FROM arbitrage_opportunities WHERE status = 'READY' ORDER BY priceDiffPct DESC")
    fun getAllActiveOpportunitiesFlow(): Flow<List<ArbitrageOpportunityEntity>>

    @Query("SELECT * FROM arbitrage_opportunities ORDER BY priceDiffPct DESC")
    suspend fun getAllOpportunities(): List<ArbitrageOpportunityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpportunity(opportunity: ArbitrageOpportunityEntity)

    @Update
    suspend fun updateOpportunity(opportunity: ArbitrageOpportunityEntity)

    @Query("DELETE FROM arbitrage_opportunities WHERE id = :id")
    suspend fun deleteOpportunity(id: String)

    @Query("DELETE FROM arbitrage_opportunities")
    suspend fun clearAllOpportunities()

    // Transaction History
    @Query("SELECT * FROM transaction_history ORDER BY timestamp DESC")
    fun getAllTransactionHistoryFlow(): Flow<List<TransactionHistoryEntity>>

    @Query("SELECT * FROM transaction_history ORDER BY timestamp DESC")
    suspend fun getTransactionHistory(): List<TransactionHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionHistoryEntity)

    // Security Configurations
    @Query("SELECT * FROM security_configurations ORDER BY updatedAt DESC")
    fun getAllSecurityConfigurationsFlow(): Flow<List<SecurityConfigurationEntity>>

    @Query("SELECT * FROM security_configurations ORDER BY updatedAt DESC")
    suspend fun getAllSecurityConfigurations(): List<SecurityConfigurationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityConfiguration(config: SecurityConfigurationEntity)

    @Update
    suspend fun updateSecurityConfiguration(config: SecurityConfigurationEntity)

    @Query("DELETE FROM security_configurations WHERE id = :id")
    suspend fun deleteSecurityConfiguration(id: String)
}

// --- App Database ---

@Database(
    entities = [
        TrackedContractEntity::class,
        ArbitrageOpportunityEntity::class,
        TransactionHistoryEntity::class,
        SecurityConfigurationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MevDatabase : RoomDatabase() {
    abstract fun mevDao(): MevDao
}
