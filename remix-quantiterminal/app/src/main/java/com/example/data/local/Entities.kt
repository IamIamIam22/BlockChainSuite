package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "virtual_machines")
data class VirtualMachineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val distro: String,
    val region: String,
    val ipAddress: String,
    val publicIp: String,
    val isRunning: Boolean,
    val cpuCores: Int,
    val ramMb: Int,
    val diskGb: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "installed_packages")
data class InstalledPackageEntity(
    @PrimaryKey val id: String,
    val vmId: String,
    val name: String,
    val toolType: String,
    val version: String,
    val installedAt: Long = System.currentTimeMillis(),
    val commandRun: String
)

@Entity(tableName = "virtual_files")
data class VirtualFileEntity(
    @PrimaryKey val path: String,
    val vmId: String,
    val name: String,
    val isDirectory: Boolean,
    val content: String,
    val permissions: String,
    val modifiedAt: Long = System.currentTimeMillis(),
    val language: String
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vmId: String,
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val exitCode: Int = 0
)

@Entity(tableName = "terminal_sessions")
data class TerminalSessionEntity(
    @PrimaryKey val vmId: String,
    val currentDirectory: String,
    val envVarsJson: String,
    val lastCommand: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isBypassTargeting: Boolean = true
)

@Entity(tableName = "gemini_chat_messages")
data class GeminiChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String, // "user" or "model"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val codeSnippet: String? = null
)
