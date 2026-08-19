package com.example.data

enum class DiagnosticSeverity(val label: String, val level: Int) {
    CRITICAL("CRITICAL", 4),
    ERROR("ERROR", 3),
    WARNING("WARNING", 2),
    INFO("INFO", 1),
    HEALTHY("HEALTHY", 0)
}

enum class DiagnosticCategory(val displayName: String) {
    NETWORK_RPC("RPC & Node Network"),
    SMART_WALLET("Smart Wallet & Signer"),
    MEMPOOL_DAEMON("Mempool Daemon"),
    DATABASE_ROOM("Room SQLite Database"),
    FAUCET_AUTOMATION("Faucet Automator"),
    CONTRACT_LIFECYCLE("Contract Execution"),
    SYSTEM_RUNTIME("System & Memory Runtime")
}

enum class HealthStatus(val label: String) {
    OPERATIONAL("Operational"),
    DEGRADED("Degraded"),
    FAILING("Failing"),
    CHECKING("Checking...")
}

enum class RemediationType(val actionName: String) {
    CYCLE_RPC("Cycle RPC Endpoints & Test Failovers"),
    REPAIR_DATABASE("Verify SQLite Integrity & Re-index"),
    RECALIBRATE_WALLET("Re-sync Smart Wallet Keys & Signers"),
    FLUSH_MEMPOOL("Flush Stale Mempool Queue & Socket"),
    RESET_SLIPPAGE_GUARD("Reset Slippage & Security Policy"),
    CLEAR_CACHE("Purge Ephemeral Cache & Storage"),
    RESTART_DAEMON("Restart Scanning Daemon Thread"),
    TEST_REPAIR("Run Automated Self-Healing Script")
}

data class DiagnosticItem(
    val id: String,
    val timestamp: Long,
    val severity: DiagnosticSeverity,
    val category: DiagnosticCategory,
    val title: String,
    val message: String,
    val stackTrace: String? = null,
    val contextDetails: Map<String, String> = emptyMap(),
    val isResolved: Boolean = false,
    val resolvedAt: Long? = null,
    val recommendedAction: String = "Inspect connection parameters and retry operation.",
    val remediationType: RemediationType = RemediationType.CYCLE_RPC
)

data class SubsystemHealth(
    val id: String,
    val name: String,
    val category: DiagnosticCategory,
    val status: HealthStatus,
    val latencyMs: Long,
    val details: String,
    val lastChecked: Long = System.currentTimeMillis()
)

data class TroubleshootingFaq(
    val id: String,
    val title: String,
    val category: DiagnosticCategory,
    val symptom: String,
    val rootCause: String,
    val resolutionSteps: List<String>,
    val quickActionLabel: String,
    val remediationType: RemediationType
)
