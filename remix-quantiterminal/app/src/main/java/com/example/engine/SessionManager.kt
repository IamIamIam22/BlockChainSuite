package com.example.engine

import android.util.Log
import com.example.data.firebase.FirebaseCloudService
import com.example.data.local.TerminalSessionDao
import com.example.data.local.TerminalSessionEntity
import com.example.data.model.CloudSyncStatus
import com.example.data.model.SessionStateSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class SessionManager(
    private val sessionDao: TerminalSessionDao,
    private val firebaseCloudService: FirebaseCloudService,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "SessionManager"

    private val _currentSessionState = MutableStateFlow(
        SessionStateSnapshot(
            vmId = "vm-1",
            workingDirectory = "/home/clouduser",
            envVars = emptyMap(),
            cloudSyncStatus = CloudSyncStatus.LOCAL_SAVED
        )
    )
    val currentSessionState: StateFlow<SessionStateSnapshot> = _currentSessionState.asStateFlow()

    private var autoSaveJob: Job? = null

    /**
     * Restore session from Room database or Cloud Firestore when VM loads
     */
    suspend fun restoreSessionForVm(
        vmId: String,
        onSessionRestored: (workingDirectory: String, envVars: Map<String, String>, isBypass: Boolean) -> Unit
    ) {
        try {
            // First check local Room Database
            val localSession = sessionDao.getSession(vmId)

            if (localSession != null) {
                val envMap = parseJsonToMap(localSession.envVarsJson)
                _currentSessionState.value = SessionStateSnapshot(
                    vmId = vmId,
                    workingDirectory = localSession.currentDirectory,
                    envVars = envMap,
                    lastUpdated = localSession.lastUpdated,
                    cloudSyncStatus = CloudSyncStatus.LOCAL_SAVED
                )
                onSessionRestored(localSession.currentDirectory, envMap, localSession.isBypassTargeting)
                Log.d(TAG, "Restored local session for VM $vmId: dir=${localSession.currentDirectory}, ${envMap.size} env vars")
            } else {
                Log.d(TAG, "No local session for $vmId, using defaults.")
            }

            // Check if Cloud Firestore has a newer backup
            coroutineScope.launch(Dispatchers.IO) {
                val cloudData = firebaseCloudService.loadSessionFromFirestore(vmId)
                if (cloudData != null && (localSession == null || cloudData.lastUpdated > localSession.lastUpdated)) {
                    _currentSessionState.value = SessionStateSnapshot(
                        vmId = vmId,
                        workingDirectory = cloudData.currentDirectory,
                        envVars = cloudData.envVars,
                        lastUpdated = cloudData.lastUpdated,
                        cloudSyncStatus = CloudSyncStatus.CLOUD_SYNCED
                    )
                    // Update local Room
                    sessionDao.saveSession(
                        TerminalSessionEntity(
                            vmId = vmId,
                            currentDirectory = cloudData.currentDirectory,
                            envVarsJson = mapToJson(cloudData.envVars),
                            isBypassTargeting = cloudData.isBypassTargeting,
                            lastUpdated = cloudData.lastUpdated
                        )
                    )
                    onSessionRestored(cloudData.currentDirectory, cloudData.envVars, cloudData.isBypassTargeting)
                    Log.d(TAG, "Applied newer Cloud Firestore session for VM $vmId: dir=${cloudData.currentDirectory}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring session: ${e.message}", e)
        }
    }

    /**
     * Notify session manager that directory or environment variables changed.
     * Automatically debounces and saves to Room & Firestore.
     */
    fun notifyStateChanged(
        vmId: String,
        currentDirectory: String,
        envVars: Map<String, String>,
        isBypass: Boolean = true,
        lastCommand: String = ""
    ) {
        _currentSessionState.value = _currentSessionState.value.copy(
            vmId = vmId,
            workingDirectory = currentDirectory,
            envVars = envVars,
            lastUpdated = System.currentTimeMillis(),
            cloudSyncStatus = CloudSyncStatus.SYNCING
        )

        autoSaveJob?.cancel()
        autoSaveJob = coroutineScope.launch(Dispatchers.IO) {
            delay(400) // Debounce rapid keystrokes/directory hops
            saveSessionImmediately(vmId, currentDirectory, envVars, isBypass, lastCommand)
        }
    }

    suspend fun saveSessionImmediately(
        vmId: String,
        currentDirectory: String,
        envVars: Map<String, String>,
        isBypass: Boolean = true,
        lastCommand: String = ""
    ) {
        try {
            // 1. Save to local SQLite via Room
            val entity = TerminalSessionEntity(
                vmId = vmId,
                currentDirectory = currentDirectory,
                envVarsJson = mapToJson(envVars),
                lastCommand = lastCommand,
                lastUpdated = System.currentTimeMillis(),
                isBypassTargeting = isBypass
            )
            sessionDao.saveSession(entity)

            // 2. Sync to Cloud Firestore if user is authenticated
            val cloudSynced = firebaseCloudService.saveSessionToFirestore(
                vmId = vmId,
                workingDirectory = currentDirectory,
                envVars = envVars,
                isBypass = isBypass
            )

            _currentSessionState.value = _currentSessionState.value.copy(
                cloudSyncStatus = if (cloudSynced) CloudSyncStatus.CLOUD_SYNCED else CloudSyncStatus.LOCAL_SAVED
            )
            Log.d(TAG, "Saved session for $vmId (Local: OK, Cloud: $cloudSynced)")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session immediately: ${e.message}", e)
            _currentSessionState.value = _currentSessionState.value.copy(
                cloudSyncStatus = CloudSyncStatus.ERROR
            )
        }
    }

    suspend fun forceCloudSync(
        vmId: String,
        currentDirectory: String,
        envVars: Map<String, String>,
        isBypass: Boolean
    ): Boolean {
        _currentSessionState.value = _currentSessionState.value.copy(cloudSyncStatus = CloudSyncStatus.SYNCING)
        val result = firebaseCloudService.saveSessionToFirestore(vmId, currentDirectory, envVars, isBypass)
        _currentSessionState.value = _currentSessionState.value.copy(
            cloudSyncStatus = if (result) CloudSyncStatus.CLOUD_SYNCED else CloudSyncStatus.ERROR
        )
        return result
    }

    companion object {
        fun mapToJson(map: Map<String, String>): String {
            val json = JSONObject()
            map.forEach { (k, v) -> json.put(k, v) }
            return json.toString()
        }

        fun parseJsonToMap(jsonStr: String?): Map<String, String> {
            if (jsonStr.isNullOrBlank()) return emptyMap()
            return try {
                val json = JSONObject(jsonStr)
                val map = mutableMapOf<String, String>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.optString(key, "")
                }
                map
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}
