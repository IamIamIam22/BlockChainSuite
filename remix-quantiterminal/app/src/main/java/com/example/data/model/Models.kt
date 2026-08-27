package com.example.data.model

data class VirtualMachine(
    val id: String,
    val name: String,
    val distro: String, // "Ubuntu 24.04 LTS", "Debian 12 Bookworm", "Alpine Linux 3.20", "Arch Linux Rolling", "Fedora 40"
    val region: String, // "us-east-1 (N. Virginia)", "eu-central-1 (Frankfurt)", "ap-northeast-1 (Tokyo)"
    val ipAddress: String,
    val publicIp: String,
    val isRunning: Boolean,
    val cpuCores: Int,
    val ramMb: Int,
    val diskGb: Int,
    val cpuUsagePercent: Float = 12f,
    val ramUsageMb: Int = 1420,
    val uptimeSeconds: Long = 3600L,
    val installedPackageCount: Int = 14
)

data class TerminalLine(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val type: LineType = LineType.OUTPUT,
    val timestamp: Long = System.currentTimeMillis()
)

enum class LineType {
    INPUT,
    OUTPUT,
    SYSTEM_INFO,
    SUCCESS,
    WARNING,
    ERROR,
    CODE,
    ACCENT
}

data class VirtualFile(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val content: String = "",
    val sizeBytes: Long = 0L,
    val permissions: String = "-rw-r--r--",
    val modifiedAt: Long = System.currentTimeMillis(),
    val language: String = "plaintext"
)

data class SoftwareItem(
    val id: String,
    val name: String,
    val toolType: ToolType, // NPM, PYTHON, RUBY, GO, RUST, DOCKER, SYSTEM, AI_LLM
    val version: String,
    val description: String,
    val category: String,
    val commandInstall: String,
    val launchCommand: String,
    val isInstalled: Boolean = false,
    val stars: String = "4.9k",
    val releaseDate: String = "Latest 2026",
    val officialUrl: String = "",
    val sampleCode: String = ""
)

enum class ToolType {
    NPM,
    PYTHON,
    RUBY,
    GO,
    RUST,
    DOCKER,
    AI_LLM,
    DEV_OPS,
    SYSTEM
}

data class ProcessInfo(
    val pid: Int,
    val user: String,
    val cpu: Float,
    val mem: Float,
    val command: String
)

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false
)

data class SessionStateSnapshot(
    val vmId: String,
    val workingDirectory: String,
    val envVars: Map<String, String>,
    val lastUpdated: Long = System.currentTimeMillis(),
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus.LOCAL_SAVED
)

enum class CloudSyncStatus {
    LOCAL_SAVED,
    SYNCING,
    CLOUD_SYNCED,
    OFFLINE_PENDING,
    ERROR
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val codeBlocks: List<String> = emptyList(),
    val modelName: String = "gemini-3.5-flash"
)

enum class ChatRole {
    USER,
    MODEL,
    SYSTEM
}
