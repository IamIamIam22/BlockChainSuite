package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VirtualMachineDao {
    @Query("SELECT * FROM virtual_machines ORDER BY createdAt ASC")
    fun getAllVms(): Flow<List<VirtualMachineEntity>>

    @Query("SELECT * FROM virtual_machines WHERE id = :id LIMIT 1")
    suspend fun getVmById(id: String): VirtualMachineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVm(vm: VirtualMachineEntity)

    @Update
    suspend fun updateVm(vm: VirtualMachineEntity)

    @Query("DELETE FROM virtual_machines WHERE id = :id")
    suspend fun deleteVm(id: String)
}

@Dao
interface InstalledPackageDao {
    @Query("SELECT * FROM installed_packages WHERE vmId = :vmId ORDER BY installedAt DESC")
    fun getInstalledPackages(vmId: String): Flow<List<InstalledPackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: InstalledPackageEntity)

    @Query("DELETE FROM installed_packages WHERE vmId = :vmId AND name = :name")
    suspend fun deletePackage(vmId: String, name: String)

    @Query("SELECT COUNT(*) FROM installed_packages WHERE vmId = :vmId")
    fun getInstalledCount(vmId: String): Flow<Int>
}

@Dao
interface VirtualFileDao {
    @Query("SELECT * FROM virtual_files WHERE vmId = :vmId ORDER BY isDirectory DESC, name ASC")
    fun getFiles(vmId: String): Flow<List<VirtualFileEntity>>

    @Query("SELECT * FROM virtual_files WHERE vmId = :vmId AND path = :path LIMIT 1")
    suspend fun getFileByPath(vmId: String, path: String): VirtualFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VirtualFileEntity)

    @Query("DELETE FROM virtual_files WHERE vmId = :vmId AND path = :path")
    suspend fun deleteFile(vmId: String, path: String)

    @Query("DELETE FROM virtual_files WHERE vmId = :vmId AND path LIKE :pathPrefix || '%'")
    suspend fun deleteDirectoryRecursive(vmId: String, pathPrefix: String)
}

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history WHERE vmId = :vmId ORDER BY timestamp DESC LIMIT 100")
    fun getHistory(vmId: String): Flow<List<CommandHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CommandHistoryEntity)

    @Query("DELETE FROM command_history WHERE vmId = :vmId")
    suspend fun clearHistory(vmId: String)
}

@Dao
interface TerminalSessionDao {
    @Query("SELECT * FROM terminal_sessions WHERE vmId = :vmId LIMIT 1")
    suspend fun getSession(vmId: String): TerminalSessionEntity?

    @Query("SELECT * FROM terminal_sessions WHERE vmId = :vmId LIMIT 1")
    fun getSessionFlow(vmId: String): Flow<TerminalSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: TerminalSessionEntity)

    @Query("DELETE FROM terminal_sessions WHERE vmId = :vmId")
    suspend fun deleteSession(vmId: String)
}

@Dao
interface GeminiChatDao {
    @Query("SELECT * FROM gemini_chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<GeminiChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: GeminiChatMessageEntity)

    @Query("DELETE FROM gemini_chat_messages")
    suspend fun clearChat()
}

