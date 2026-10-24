package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.LruCache
import com.example.BuildConfig
import com.example.engine.OfflineKnowledgeEngine
import com.example.model.KnowledgeDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

enum class ResponseSource(val label: String) {
    GEMINI_API_CLOUD("Gemini 3.5 Cloud"),
    OFFLINE_CACHE("Cached Offline Memory"),
    OFFLINE_KNOWLEDGE_ENGINE("ChatSobi Offline Engine")
}

data class CodeSnippet(
    val language: String,
    val code: String
)

data class StructuredAiResponse(
    val query: String,
    val mainAnswer: String,
    val mathFormulas: List<String> = emptyList(),
    val codeBlocks: List<CodeSnippet> = emptyList(),
    val stepByStepPoints: List<String> = emptyList(),
    val keyTakeaways: List<String> = emptyList(),
    val suggestedFollowUps: List<String> = emptyList(),
    val source: ResponseSource,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatSobiRoutingService private constructor() {

    companion object {
        @Volatile
        private var instance: ChatSobiRoutingService? = null

        fun getInstance(): ChatSobiRoutingService {
            return instance ?: synchronized(this) {
                instance ?: ChatSobiRoutingService().also { instance = it }
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // 100-entry in-memory LRU Cache for offline fallback
    private val responseCache = LruCache<String, StructuredAiResponse>(100)

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun routeQuery(
        context: Context,
        query: String,
        domain: KnowledgeDomain = KnowledgeDomain.ALL,
        forceOffline: Boolean = false
    ): StructuredAiResponse = withContext(Dispatchers.IO) {
        val normalizedKey = query.trim().lowercase(Locale.ROOT)
        val isOnline = isNetworkAvailable(context)
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = !apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY"

        // 1. Try Gemini API first if network is reachable, key is present, and user hasn't forced offline mode
        if (!forceOffline && isOnline && hasValidKey) {
            try {
                val cloudResponse = callGeminiApi(query, apiKey, domain)
                // Cache successful response for offline resilience
                responseCache.put(normalizedKey, cloudResponse)
                return@withContext cloudResponse
            } catch (e: IOException) {
                // Network unreachable or timeout -> Proceed to fallback
            } catch (e: Exception) {
                // API error -> Proceed to fallback
            }
        }

        // 2. Network is unreachable -> Fallback to Cache or Offline Knowledge Engine
        val cached = responseCache.get(normalizedKey)
        if (cached != null) {
            return@withContext cached.copy(source = ResponseSource.OFFLINE_CACHE)
        }

        // 3. Fallback to Local Offline Knowledge Engine
        val offlineMessage = OfflineKnowledgeEngine.answerQuery(query, domain)
        val structuredOffline = parseStructuredResponse(
            query = query,
            rawText = offlineMessage.text,
            defaultSource = ResponseSource.OFFLINE_KNOWLEDGE_ENGINE,
            forcedFormula = offlineMessage.codeBlock.takeIf {
                offlineMessage.domain == KnowledgeDomain.MATHEMATICS ||
                        offlineMessage.domain == KnowledgeDomain.SCIENCE
            }
        )

        // Store into cache so repeat offline queries are answered instantly
        responseCache.put(normalizedKey, structuredOffline)
        return@withContext structuredOffline
    }

    private fun callGeminiApi(
        query: String,
        apiKey: String,
        domain: KnowledgeDomain
    ): StructuredAiResponse {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemPrompt = """
You are ChatSobi, an ultra-intelligent, multi-modal AI assistant for students and problem solvers.
Provide clear, authoritative, and direct answers without repeating self-introductions (never say 'I am ChatSobi', 'Hello, I am ChatSobi', or 'As an AI' in your answers). Get straight to the point.
When providing mathematical or scientific formulas:
- Wrap display formulas in $$ ... $$ or write clean LaTeX (e.g., $$ x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a} $$).
When providing code:
- Wrap in ```language ... ``` code blocks.
Include key takeaways or step-by-step points where appropriate.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nUser Question: $query")
                        })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyString = response.body?.string() ?: throw IOException("Empty response body from Gemini API")

        if (!response.isSuccessful) {
            throw IOException("Gemini API error code: ${response.code}")
        }

        val jsonResponse = JSONObject(bodyString)
        val candidates = jsonResponse.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text") ?: "No text in Gemini response."

        return parseStructuredResponse(
            query = query,
            rawText = rawText,
            defaultSource = ResponseSource.GEMINI_API_CLOUD
        )
    }

    private fun parseStructuredResponse(
        query: String,
        rawText: String,
        defaultSource: ResponseSource,
        forcedFormula: String? = null
    ): StructuredAiResponse {
        val formulas = mutableListOf<String>()
        val codeSnippets = mutableListOf<CodeSnippet>()
        val stepPoints = mutableListOf<String>()
        val takeaways = mutableListOf<String>()

        if (!forcedFormula.isNullOrBlank()) {
            formulas.add(forcedFormula)
        }

        // 1. Extract LaTeX display math: $$ formula $$ or \[ formula \]
        val mathPattern = Pattern.compile("\\$\\$([\\s\\S]*?)\\$\\$|\\\\\\[([\\s\\S]*?)\\\\\\]")
        val mathMatcher = mathPattern.matcher(rawText)
        while (mathMatcher.find()) {
            val formula = mathMatcher.group(1) ?: mathMatcher.group(2)
            if (!formula.isNullOrBlank()) {
                formulas.add(formula.trim())
            }
        }

        // 2. Extract code blocks: ```lang ... ```
        val codePattern = Pattern.compile("```([a-zA-Z0-9]*)\\n([\\s\\S]*?)```")
        val codeMatcher = codePattern.matcher(rawText)
        while (codeMatcher.find()) {
            val lang = codeMatcher.group(1)?.ifBlank { "code" } ?: "code"
            val code = codeMatcher.group(2) ?: ""
            if (code.isNotBlank()) {
                codeSnippets.add(CodeSnippet(lang, code.trim()))
            }
        }

        // 3. Extract step-by-step lines (lines starting with "1. ", "• ", "- ")
        rawText.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.matches(Regex("^\\d+\\.\\s.*"))) {
                stepPoints.add(trimmed)
            }
        }

        // Clean main text: remove redundant raw $$ blocks if formulas extracted, or keep formatted text
        var cleanedMain = rawText
        if (formulas.isNotEmpty()) {
            cleanedMain = cleanedMain.replace(Regex("\\$\\$[\\s\\S]*?\\$\\$"), "").trim()
        }

        return StructuredAiResponse(
            query = query,
            mainAnswer = cleanedMain.ifBlank { rawText },
            mathFormulas = formulas.distinct(),
            codeBlocks = codeSnippets,
            stepByStepPoints = stepPoints.take(8),
            keyTakeaways = takeaways,
            source = defaultSource
        )
    }
}
