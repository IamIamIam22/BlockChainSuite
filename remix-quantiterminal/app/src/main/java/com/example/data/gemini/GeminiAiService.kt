package com.example.data.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.GeminiChatDao
import com.example.data.local.GeminiChatMessageEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiAiService(
    private val chatDao: GeminiChatDao
) {
    private val TAG = "GeminiAiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val persistedMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages().map { entities ->
        entities.map { entity ->
            ChatMessage(
                id = entity.id,
                role = if (entity.role == "user") ChatRole.USER else ChatRole.MODEL,
                text = entity.message,
                timestamp = entity.timestamp,
                codeBlocks = extractCodeSnippets(entity.message)
            )
        }
    }

    suspend fun sendMessage(
        userPrompt: String,
        modelName: String = "gemini-3.5-flash",
        activeVmContext: String = ""
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        val userMsgId = UUID.randomUUID().toString()
        val userEntity = GeminiChatMessageEntity(
            id = userMsgId,
            role = "user",
            message = userPrompt,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userEntity)

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank()) {
            val offlineResponse = "⚠️ **Gemini API Key is not set.**\n\nTo enable full AI Copilot responses, configure your Gemini API Key in the Secrets Panel of AI Studio or `.env` file.\n\n💡 *Offline suggestion for \"$userPrompt\":*\n```bash\n# Check system status or package manager\nneofetch\napt list --installed\n```"
            val fallbackEntity = GeminiChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = "model",
                message = offlineResponse,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(fallbackEntity)
            return@withContext Result.success(
                ChatMessage(
                    id = fallbackEntity.id,
                    role = ChatRole.MODEL,
                    text = fallbackEntity.message,
                    codeBlocks = extractCodeSnippets(fallbackEntity.message),
                    modelName = modelName
                )
            )
        }

        try {
            // Build conversation history from database
            val history = mutableListOf<JSONObject>()

            val requestJson = JSONObject()

            // System Instruction
            val systemInstruction = JSONObject()
            val sysParts = JSONArray()
            val sysText = JSONObject().put(
                "text",
                """You are CloudTerm AI Copilot, a Linux Terminal, Cloud Architecture, and DevOps Assistant built into an interactive Cloud Virtual Machine.
                Active Environment Context: $activeVmContext.
                Always provide helpful terminal commands, bash scripts, Python/Node/Go snippets, or environment configuration instructions. Format commands inside markdown code blocks (```bash ... ```) so users can tap to execute them in their VM or Windows Bridge."""
            )
            sysParts.put(sysText)
            systemInstruction.put("parts", sysParts)
            requestJson.put("systemInstruction", systemInstruction)

            // Multi-turn contents
            val contentsArray = JSONArray()

            // Add user's current message
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val partsArr = JSONArray()
            partsArr.put(JSONObject().put("text", userPrompt))
            currentTurn.put("parts", partsArr)
            contentsArray.put(currentTurn)

            requestJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("topP", 0.95)
            requestJson.put("generationConfig", genConfig)

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API Error: ${response.code} $responseBody")
                val errorMsg = "❌ Gemini API Error (${response.code}): ${parseErrorMessage(responseBody)}"
                val errEntity = GeminiChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    role = "model",
                    message = errorMsg,
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(errEntity)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val responseText = parts?.optJSONObject(0)?.optString("text")
                ?: "I have processed your request, but no text output was generated."

            val modelEntity = GeminiChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = "model",
                message = responseText,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(modelEntity)

            val chatMsg = ChatMessage(
                id = modelEntity.id,
                role = ChatRole.MODEL,
                text = responseText,
                timestamp = modelEntity.timestamp,
                codeBlocks = extractCodeSnippets(responseText),
                modelName = modelName
            )
            Result.success(chatMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API: ${e.message}", e)
            val errorMsg = "⚠️ Connection error: ${e.localizedMessage ?: "Failed to reach Gemini API"}"
            val errEntity = GeminiChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = "model",
                message = errorMsg,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(errEntity)
            Result.failure(e)
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatDao.clearChat()
    }

    companion object {
        fun extractCodeSnippets(markdown: String): List<String> {
            val snippets = mutableListOf<String>()
            val regex = "```(?:[a-zA-Z0-9_-]+)?\\s*\\n?([\\s\\S]*?)```".toRegex()
            regex.findAll(markdown).forEach { match ->
                val code = match.groupValues[1].trim()
                if (code.isNotEmpty()) {
                    snippets.add(code)
                }
            }
            return snippets
        }

        private fun parseErrorMessage(jsonStr: String): String {
            return try {
                val json = JSONObject(jsonStr)
                json.optJSONObject("error")?.optString("message") ?: jsonStr
            } catch (e: Exception) {
                jsonStr
            }
        }
    }
}
