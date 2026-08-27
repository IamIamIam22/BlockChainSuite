package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetworkExecutor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun executeCurl(url: String, method: String = "GET", headers: Map<String, String> = emptyMap()): String {
        return withContext(Dispatchers.IO) {
            try {
                val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else {
                    url
                }

                val requestBuilder = Request.Builder()
                    .url(formattedUrl)
                    .header("User-Agent", "curl/8.5.0 CloudTerm-VM/1.0")

                headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

                val response = client.newCall(requestBuilder.build()).execute()
                val code = response.code
                val message = response.message
                val body = response.body?.string() ?: ""

                buildString {
                    appendLine("< HTTP/2 $code $message")
                    response.headers.forEach { (name, value) ->
                        appendLine("< $name: $value")
                    }
                    appendLine("<")
                    append(body.take(4000))
                    if (body.length > 4000) {
                        appendLine("\n... [Output truncated. Total size: ${body.length} bytes]")
                    }
                }
            } catch (e: Exception) {
                "curl: (7) Failed to connect to host: ${e.localizedMessage ?: "Unknown network error"}"
            }
        }
    }

    suspend fun executePing(host: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val cleanHost = host.removePrefix("https://").removePrefix("http://").split("/").first()
                val startTime = System.currentTimeMillis()
                val formattedUrl = "https://$cleanHost"
                
                val req = Request.Builder().url(formattedUrl).head().build()
                val resp = client.newCall(req).execute()
                val rtt = System.currentTimeMillis() - startTime

                """
                PING $cleanHost ($cleanHost): 56 data bytes
                64 bytes from $cleanHost: icmp_seq=1 ttl=56 time=${rtt}.2 ms
                64 bytes from $cleanHost: icmp_seq=2 ttl=56 time=${(rtt + 2)}.1 ms
                64 bytes from $cleanHost: icmp_seq=3 ttl=56 time=${(rtt - 1).coerceAtLeast(1)}.8 ms

                --- $cleanHost ping statistics ---
                3 packets transmitted, 3 packets received, 0.0% packet loss
                round-trip min/avg/max/stddev = ${(rtt - 1).coerceAtLeast(1)}/${rtt}/${rtt + 2}/1.2 ms
                """.trimIndent()
            } catch (e: Exception) {
                """
                PING $host: 56 data bytes
                ping: cannot resolve $host: Unknown host or offline (${e.message})
                """.trimIndent()
            }
        }
    }
}
