package com.example.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class WebIntegrationServer(
    private val context: Context,
    private val repository: SentinelRepository,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isRunning = mutableStateOf(false)
    val serverUrl = mutableStateOf("http://localhost:$port")

    fun startServer() {
        if (isRunning.value) return

        try {
            serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            isRunning.value = true
            val ipList = getAllIpAddresses()
            val primaryIp = ipList.firstOrNull() ?: "127.0.0.1"
            serverUrl.value = "http://$primaryIp:$port"

            serverJob = scope.launch {
                while (isRunning.value && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        handleClient(clientSocket)
                    } catch (e: Exception) {
                        if (!isRunning.value) break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRunning.value = false
        }
    }

    fun stopServer() {
        isRunning.value = false
        try {
            serverSocket?.close()
            serverSocket = null
            serverJob?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val requestLine = reader.readLine() ?: return@launch
                val path = requestLine.split(" ").getOrNull(1) ?: "/"

                val html = ContractVaultExporter.generateWebDashboardHtml(
                    repository.trackedContracts,
                    repository.smartWallets,
                    repository.config
                )

                val responseBody: String
                val contentType: String

                when {
                    path.startsWith("/api/wallets") -> {
                        contentType = "application/json"
                        responseBody = ContractVaultExporter.generateWalletSecurityConfigJson(
                            repository.smartWallets,
                            repository.config
                        )
                    }
                    path.startsWith("/api/contracts") -> {
                        contentType = "application/json"
                        responseBody = ContractVaultExporter.generateWebDashboardHtml(
                            repository.trackedContracts,
                            repository.smartWallets,
                            repository.config
                        )
                    }
                    else -> {
                        contentType = "text/html; charset=UTF-8"
                        responseBody = html
                    }
                }

                val bodyBytes = responseBody.toByteArray(Charsets.UTF_8)

                writer.print("HTTP/1.1 200 OK\r\n")
                writer.print("Content-Type: $contentType\r\n")
                writer.print("Content-Length: ${bodyBytes.size}\r\n")
                writer.print("Access-Control-Allow-Origin: *\r\n")
                writer.print("Connection: close\r\n\r\n")
                writer.flush()

                socket.getOutputStream().write(bodyBytes)
                socket.getOutputStream().flush()
                socket.close()
            } catch (e: Exception) {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    fun getAllIpAddresses(): List<String> {
        val ipList = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        val hostAddress = address.hostAddress ?: ""
                        if (hostAddress.indexOf(':') < 0 && hostAddress != "127.0.0.1") { // IPv4
                            ipList.add(hostAddress)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        if (ipList.isEmpty()) ipList.add("127.0.0.1")
        return ipList
    }

    fun getLocalIpAddress(): String {
        return getAllIpAddresses().firstOrNull() ?: "127.0.0.1"
    }
}
