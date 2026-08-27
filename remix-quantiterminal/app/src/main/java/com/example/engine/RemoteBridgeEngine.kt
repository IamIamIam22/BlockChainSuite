package com.example.engine

import com.example.data.model.LineType
import com.example.data.model.TerminalLine
import com.example.data.model.VirtualMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RemoteRequestLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val clientSource: String, // e.g. "Windows PowerShell", "Windows Terminal", "cURL", "Web Client"
    val command: String,
    val outputSummary: String,
    val latencyMs: Long,
    val statusCode: Int = 200,
    val bypassTargetingApplied: Boolean = true
)

data class RemoteBridgeState(
    val isRunning: Boolean = true,
    val port: Int = 9090,
    val hostAddress: String = "127.0.0.1",
    val isBypassTargetingActive: Boolean = true,
    val sessionToken: String = "ct-bypass-stream-token",
    val totalRequestsReceived: Int = 0,
    val activeConnectionsCount: Int = 0,
    val lastRequestTime: Long? = null,
    val requestLogs: List<RemoteRequestLog> = emptyList()
)

class RemoteBridgeEngine(
    private val terminalEngine: TerminalEngine,
    private val scope: CoroutineScope
) {
    private val _bridgeState = MutableStateFlow(RemoteBridgeState())
    val bridgeState: StateFlow<RemoteBridgeState> = _bridgeState.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    // Callback when a remote command runs so UI ViewModel can update terminal state
    var onRemoteCommandExecuted: ((command: String, output: String, source: String, bypass: Boolean) -> Unit)? = null
    var getActiveVm: (() -> VirtualMachine?)? = null

    init {
        startServer()
    }

    fun toggleBypassTargeting() {
        val next = !_bridgeState.value.isBypassTargetingActive
        _bridgeState.value = _bridgeState.value.copy(isBypassTargetingActive = next)
    }

    fun setBypassTargeting(enabled: Boolean) {
        _bridgeState.value = _bridgeState.value.copy(isBypassTargetingActive = enabled)
    }

    fun setPort(port: Int) {
        if (port != _bridgeState.value.port) {
            stopServer()
            _bridgeState.value = _bridgeState.value.copy(port = port)
            startServer()
        }
    }

    fun setSessionToken(token: String) {
        _bridgeState.value = _bridgeState.value.copy(sessionToken = token.trim())
    }

    fun startServer() {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch(Dispatchers.IO) {
            val port = _bridgeState.value.port
            try {
                serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
                _bridgeState.value = _bridgeState.value.copy(
                    isRunning = true,
                    hostAddress = "0.0.0.0:$port"
                )

                while (isActive && serverSocket?.isClosed == false) {
                    val socket = try {
                        serverSocket?.accept() ?: break
                    } catch (e: Exception) {
                        break
                    }
                    scope.launch(Dispatchers.IO) {
                        handleClientSocket(socket)
                    }
                }
            } catch (e: Exception) {
                _bridgeState.value = _bridgeState.value.copy(isRunning = false)
            }
        }
    }

    fun stopServer() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverJob = null
        _bridgeState.value = _bridgeState.value.copy(isRunning = false)
    }

    private suspend fun handleClientSocket(socket: Socket) {
        val startTime = System.currentTimeMillis()
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Read request line
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase(Locale.US)
            val path = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line: String? = reader.readLine()
            var contentLength = 0
            while (!line.isNullOrEmpty()) {
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val k = line.substring(0, colonIdx).trim().lowercase(Locale.US)
                    val v = line.substring(colonIdx + 1).trim()
                    headers[k] = v
                    if (k == "content-length") {
                        contentLength = v.toIntOrNull() ?: 0
                    }
                }
                line = reader.readLine()
            }

            // Read body if present
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val charBuf = CharArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val r = reader.read(charBuf, readTotal, contentLength - readTotal)
                    if (r == -1) break
                    readTotal += r
                }
                bodyBuilder.append(charBuf, 0, readTotal)
            }
            val body = bodyBuilder.toString()

            val userAgent = headers["user-agent"] ?: "Windows Terminal / Remote Client"
            val clientSource = when {
                userAgent.contains("PowerShell", ignoreCase = true) -> "Windows PowerShell"
                userAgent.contains("curl", ignoreCase = true) -> "Windows cURL"
                userAgent.contains("Wget", ignoreCase = true) -> "Windows Wget"
                userAgent.contains("Windows", ignoreCase = true) -> "Windows Client"
                else -> "Remote Bridge Client"
            }

            // Handle OPTIONS CORS preflight
            if (method == "OPTIONS") {
                sendCorsResponse(writer, 204, "No Content", "")
                return
            }

            when {
                path == "/api/status" || path == "/" -> {
                    val activeVm = getActiveVm?.invoke()
                    val respJson = JSONObject().apply {
                        put("status", "online")
                        put("engine", "CloudTerm Virtual Machine Engine")
                        put("activeVm", activeVm?.name ?: "Default Cloud Node")
                        put("distro", activeVm?.distro ?: "Ubuntu 24.04 LTS")
                        put("ip", activeVm?.ipAddress ?: "10.240.0.42")
                        put("bypassTargetingActive", _bridgeState.value.isBypassTargetingActive)
                        put("streamSandbox", "SECURE_BROWSER_STREAM")
                        put("serverPort", _bridgeState.value.port)
                        put("sessionToken", _bridgeState.value.sessionToken)
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendJsonResponse(writer, 200, respJson.toString())
                }

                path.startsWith("/api/exec") || path.startsWith("/api/command") -> {
                    var commandToRun = ""
                    var bypassOverride = _bridgeState.value.isBypassTargetingActive

                    if (method == "POST") {
                        try {
                            if (body.startsWith("{")) {
                                val json = JSONObject(body)
                                commandToRun = json.optString("cmd", json.optString("command", ""))
                                if (json.has("bypassTargeting")) {
                                    bypassOverride = json.getBoolean("bypassTargeting")
                                }
                            } else {
                                commandToRun = body.trim()
                            }
                        } catch (_: Exception) {
                            commandToRun = body.trim()
                        }
                    } else if (method == "GET") {
                        // Support GET /api/exec?cmd=ls
                        val queryIdx = path.indexOf('?')
                        if (queryIdx != -1) {
                            val query = path.substring(queryIdx + 1)
                            for (param in query.split("&")) {
                                val kv = param.split("=")
                                if (kv.size == 2 && (kv[0] == "cmd" || kv[0] == "command")) {
                                    commandToRun = java.net.URLDecoder.decode(kv[1], "UTF-8")
                                }
                            }
                        }
                    }

                    if (commandToRun.isEmpty()) {
                        sendJsonResponse(
                            writer,
                            400,
                            JSONObject().apply {
                                put("error", "Missing 'cmd' parameter in request")
                                put("usage", "POST /api/exec with JSON {\"cmd\": \"ls -la\"}")
                            }.toString()
                        )
                        return
                    }

                    // Execute command through terminal engine
                    val activeVm = getActiveVm?.invoke()
                    if (activeVm == null) {
                        sendJsonResponse(
                            writer,
                            503,
                            JSONObject().apply {
                                put("error", "No active VM online in cloud stream")
                            }.toString()
                        )
                        return
                    }

                    val outputBuffer = mutableListOf<String>()
                    terminalEngine.execute(commandToRun, activeVm) { line ->
                        outputBuffer.add(line.text)
                    }

                    val fullOutput = outputBuffer.joinToString("\n")
                    val latency = System.currentTimeMillis() - startTime

                    // Notify UI and terminal lines
                    onRemoteCommandExecuted?.invoke(commandToRun, fullOutput, clientSource, bypassOverride)

                    // Log request
                    val log = RemoteRequestLog(
                        method = method,
                        path = path,
                        clientSource = clientSource,
                        command = commandToRun,
                        outputSummary = fullOutput.take(150),
                        latencyMs = latency,
                        statusCode = 200,
                        bypassTargetingApplied = bypassOverride
                    )
                    addLog(log)

                    val respJson = JSONObject().apply {
                        put("status", "success")
                        put("command", commandToRun)
                        put("output", fullOutput)
                        put("exitCode", 0)
                        put("bypassTargetingApplied", bypassOverride)
                        put("executionEnv", "Secure Cloud Stream (Bypass Target Active)")
                        put("vm", activeVm.name)
                        put("latencyMs", latency)
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendJsonResponse(writer, 200, respJson.toString())
                }

                path == "/api/bypass" -> {
                    if (method == "POST") {
                        try {
                            val json = JSONObject(body)
                            if (json.has("enabled")) {
                                setBypassTargeting(json.getBoolean("enabled"))
                            } else {
                                toggleBypassTargeting()
                            }
                        } catch (_: Exception) {
                            toggleBypassTargeting()
                        }
                    }
                    val resp = JSONObject().apply {
                        put("bypassTargetingActive", _bridgeState.value.isBypassTargetingActive)
                        put("message", "Targeting bypassed. Tasks run entirely inside secure browser cloud stream.")
                    }
                    sendJsonResponse(writer, 200, resp.toString())
                }

                else -> {
                    sendJsonResponse(
                        writer,
                        404,
                        JSONObject().apply {
                            put("error", "Not Found")
                            put("availableEndpoints", JSONArray().apply {
                                put("GET /api/status")
                                put("POST /api/exec (payload: {\"cmd\": \"python3 main.py\"})")
                                put("POST /api/bypass")
                            })
                        }.toString()
                    )
                }
            }
        } catch (e: Exception) {
            // Handle client disconnect gracefully
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendJsonResponse(writer: PrintWriter, code: Int, json: String) {
        val statusText = when (code) {
            200 -> "OK"
            204 -> "No Content"
            400 -> "Bad Request"
            404 -> "Not Found"
            503 -> "Service Unavailable"
            else -> "OK"
        }
        val bytes = json.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $code $statusText\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
        writer.print("Access-Control-Allow-Headers: *\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(json)
        writer.flush()
    }

    private fun sendCorsResponse(writer: PrintWriter, code: Int, statusText: String, body: String) {
        writer.print("HTTP/1.1 $code $statusText\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n")
        writer.print("Access-Control-Allow-Headers: *\r\n")
        writer.print("Content-Length: 0\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
    }

    private fun addLog(log: RemoteRequestLog) {
        val currentLogs = _bridgeState.value.requestLogs
        val updated = (listOf(log) + currentLogs).take(50)
        _bridgeState.value = _bridgeState.value.copy(
            requestLogs = updated,
            totalRequestsReceived = _bridgeState.value.totalRequestsReceived + 1,
            lastRequestTime = System.currentTimeMillis()
        )
    }

    /**
     * Executes a command via the simulated / internal bridge as if sent from Windows
     */
    suspend fun executeDirectFromWindows(
        command: String,
        source: String = "Windows PowerShell",
        bypassTargeting: Boolean = true
    ): Pair<String, Long> {
        val startTime = System.currentTimeMillis()
        val activeVm = getActiveVm?.invoke() ?: return Pair("Error: No active VM running in cloud stream.", 0L)

        val outputBuffer = mutableListOf<String>()
        terminalEngine.execute(command, activeVm) { line ->
            outputBuffer.add(line.text)
        }
        val output = outputBuffer.joinToString("\n")
        val latency = System.currentTimeMillis() - startTime

        onRemoteCommandExecuted?.invoke(command, output, source, bypassTargeting)

        val log = RemoteRequestLog(
            method = "DIRECT_BRIDGE",
            path = "/api/exec",
            clientSource = source,
            command = command,
            outputSummary = output.take(150),
            latencyMs = latency,
            statusCode = 200,
            bypassTargetingApplied = bypassTargeting
        )
        addLog(log)

        return Pair(output, latency)
    }
}
