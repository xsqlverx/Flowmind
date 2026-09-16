package com.example.data.nlp

import com.example.data.model.EntityType
import com.example.data.model.ParsedEntity
import com.example.data.model.ParsedReminderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object LlmReminderClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Resolves the default model name for any provider if user hasn't supplied a custom one.
     */
    fun resolveDefaultModel(provider: String, customModel: String): String {
        if (customModel.isNotBlank()) return customModel.trim()
        return when (provider.lowercase(Locale.ROOT)) {
            "nvidia", "nvidia nim" -> "meta/llama-3.3-70b-instruct"
            "openrouter" -> "meta-llama/llama-3.3-70b-instruct"
            "groq" -> "llama-3.3-70b-versatile"
            "google gemini", "gemini" -> "gemini-2.5-flash"
            "anthropic", "claude" -> "claude-3-5-haiku-20241022"
            "mistral", "mistral ai" -> "mistral-small-latest"
            "openai" -> "gpt-4o-mini"
            "local ollama", "ollama" -> "llama3.2"
            else -> "gpt-4o-mini"
        }
    }

    /**
     * Resolves the default API endpoint URL for any provider if user hasn't specified custom.
     */
    fun resolveDefaultEndpoint(provider: String, customEndpoint: String): String {
        if (customEndpoint.isNotBlank()) return customEndpoint.trim()
        return when (provider.lowercase(Locale.ROOT)) {
            "nvidia", "nvidia nim" -> "https://integrate.api.nvidia.com/v1/chat/completions"
            "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "groq" -> "https://api.groq.com/openai/v1/chat/completions"
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "anthropic", "claude" -> "https://api.anthropic.com/v1/messages"
            "mistral", "mistral ai" -> "https://api.mistral.ai/v1/chat/completions"
            "local ollama", "ollama" -> "http://10.0.2.2:11434/v1/chat/completions"
            "google gemini", "gemini" -> "https://generativelanguage.googleapis.com/v1beta"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }

    /**
     * Calls any LLM provider API (NVIDIA, OpenRouter, Groq, Gemini, OpenAI, Anthropic, or Custom)
     * with structured JSON output. Accepts ANY user-supplied API key.
     */
    suspend fun parseWithLlm(
        input: String,
        provider: String,
        apiKey: String,
        customEndpointUrl: String = "",
        customModelName: String = ""
    ): Result<ParsedReminderResult> = withContext(Dispatchers.IO) {
        try {
            val trimmedKey = apiKey.trim()
            val isLocal = provider.lowercase(Locale.ROOT).contains("local") ||
                    provider.lowercase(Locale.ROOT).contains("ollama") ||
                    customEndpointUrl.contains("localhost") ||
                    customEndpointUrl.contains("10.0.2.2")

            // Require an API key for cloud providers
            if (trimmedKey.isEmpty() && !isLocal) {
                return@withContext Result.failure(
                    IllegalArgumentException("Please enter your $provider API key in Settings")
                )
            }

            val currentTimeStr = SimpleDateFormat("EEEE, MMMM d, yyyy h:mm a", Locale.getDefault()).format(Date())

            val systemInstruction = """
                You are FlowMind, an intelligent assistant that parses conversational reminder requests into structured data.
                Current reference time: $currentTimeStr.
                Extract the reminder details and return ONLY a valid JSON object (no markdown quotes, no explanations) with these exact keys:
                - cleanTitle: string (short, concise action title, e.g. "Buy groceries", "Schedule sync")
                - category: string (one of: Work, Personal, Finance, Health, Dev, Study, Errands, General)
                - tag: string (e.g. #work, #health, #finance, #dev, #personal)
                - priority: string (HIGH, NORMAL, LOW)
                - dueTimeString: string (human readable schedule, e.g. "Tomorrow, 3:00 PM" or "Friday · 10:00 AM")
                - dueOffsetMinutes: number (approximate minutes from now when the reminder is due)
                - participant: string (person or group, or empty string "")
                - recurrence: string (NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY)
                - notes: string (short context notes)
            """.trimIndent()

            val effectiveModel = resolveDefaultModel(provider, customModelName)
            val effectiveEndpoint = resolveDefaultEndpoint(provider, customEndpointUrl)

            val jsonResponseText: String = when (provider.lowercase(Locale.ROOT)) {
                "google gemini", "gemini" -> callGemini(input, systemInstruction, effectiveModel, trimmedKey)
                "anthropic", "claude" -> callAnthropic(input, systemInstruction, effectiveModel, effectiveEndpoint, trimmedKey)
                else -> callOpenAiCompatible(
                    input = input,
                    systemInstruction = systemInstruction,
                    provider = provider,
                    endpointUrl = effectiveEndpoint,
                    model = effectiveModel,
                    apiKey = trimmedKey
                )
            }

            val jsonObject = extractJsonObject(jsonResponseText)

            val cleanTitle = jsonObject.optString("cleanTitle", input.take(40))
            val category = jsonObject.optString("category", "General")
            val tag = jsonObject.optString("tag", "#general")
            val priority = jsonObject.optString("priority", "NORMAL").uppercase(Locale.ROOT)
            val dueTimeString = jsonObject.optString("dueTimeString", "Today · 5:00 PM")
            val dueOffsetMinutes = jsonObject.optLong("dueOffsetMinutes", 120L)
            val participant = jsonObject.optString("participant", "")
            val recurrence = jsonObject.optString("recurrence", "NONE").uppercase(Locale.ROOT)
            val notes = jsonObject.optString("notes", "Synthesized with $provider ($effectiveModel)")

            val dueDateMillis = System.currentTimeMillis() + (dueOffsetMinutes * 60 * 1000L)

            // Dynamic entity chips
            val entities = mutableListOf<ParsedEntity>()
            entities.add(ParsedEntity(EntityType.DATE_TIME, dueTimeString, "schedule"))
            entities.add(ParsedEntity(EntityType.CATEGORY, category, "label"))
            if (priority == "HIGH") {
                entities.add(ParsedEntity(EntityType.PRIORITY, "High Priority", "priority_high"))
            } else if (priority == "LOW") {
                entities.add(ParsedEntity(EntityType.PRIORITY, "Low Priority", "low_priority"))
            }
            if (participant.isNotEmpty()) {
                entities.add(ParsedEntity(EntityType.PARTICIPANT, participant, "person"))
            }
            if (recurrence != "NONE") {
                entities.add(ParsedEntity(EntityType.RECURRENCE, "Repeat: $recurrence", "repeat"))
            }

            val parsedResult = ParsedReminderResult(
                cleanTitle = cleanTitle,
                rawInput = input,
                entities = entities,
                category = category,
                tag = tag,
                priority = priority,
                dueTimeString = dueTimeString,
                dueDateMillis = dueDateMillis,
                participant = participant,
                recurrence = recurrence,
                parsingEngine = "$provider Neural Engine",
                needsLlmAssistance = false,
                notes = notes
            )

            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Universal OpenAI-compatible Chat Completion Caller.
     * Works with: NVIDIA NIM, OpenRouter, Groq, OpenAI, Mistral, Together AI, Perplexity, DeepSeek, Ollama, etc.
     */
    private fun callOpenAiCompatible(
        input: String,
        systemInstruction: String,
        provider: String,
        endpointUrl: String,
        model: String,
        apiKey: String
    ): String {
        val rootJson = JSONObject().apply {
            put("model", model)
            // Some providers support response_format json_object
            put("response_format", JSONObject().apply { put("type", "json_object") })
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", input)
                })
            })
            put("temperature", 0.1)
        }

        val requestBuilder = Request.Builder()
            .url(endpointUrl)
            .post(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))

        if (apiKey.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        // OpenRouter specific headers
        if (provider.lowercase(Locale.ROOT).contains("openrouter")) {
            requestBuilder.header("HTTP-Referer", "https://flowmind.ai")
            requestBuilder.header("X-Title", "FlowMind Assistant")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val code = response.code
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                // If response_format error on a provider that doesn't support json_object, retry without it
                if (code == 400 && body.contains("response_format", ignoreCase = true)) {
                    return retryWithoutResponseFormat(input, systemInstruction, endpointUrl, model, apiKey, provider)
                }
                throw IllegalStateException("$provider API Error (HTTP $code): ${extractErrorMessage(body)}")
            }

            if (body.isEmpty()) {
                throw IllegalStateException("Empty response body from $provider")
            }

            val respJson = JSONObject(body)
            val choices = respJson.getJSONArray("choices")
            val msg = choices.getJSONObject(0).getJSONObject("message")
            return msg.getString("content")
        }
    }

    private fun retryWithoutResponseFormat(
        input: String,
        systemInstruction: String,
        endpointUrl: String,
        model: String,
        apiKey: String,
        provider: String
    ): String {
        val rootJson = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction + "\nRespond with RAW JSON ONLY. No markdown, no quotes.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", input)
                })
            })
            put("temperature", 0.1)
        }

        val req = Request.Builder()
            .url(endpointUrl)
            .apply {
                if (apiKey.isNotEmpty()) header("Authorization", "Bearer $apiKey")
            }
            .post(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(req).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("$provider API Error (HTTP ${response.code}): ${extractErrorMessage(body)}")
            }
            val respJson = JSONObject(body)
            val choices = respJson.getJSONArray("choices")
            return choices.getJSONObject(0).getJSONObject("message").getString("content")
        }
    }

    private fun callGemini(input: String, systemInstruction: String, model: String, apiKey: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemInstruction\n\nUser reminder: \"$input\"")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Google Gemini Error (HTTP ${response.code}): ${extractErrorMessage(body)}")
            }
            val respJson = JSONObject(body)
            val candidates = respJson.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }

    private fun callAnthropic(
        input: String,
        systemInstruction: String,
        model: String,
        endpointUrl: String,
        apiKey: String
    ): String {
        val url = if (endpointUrl.contains("anthropic.com")) endpointUrl else "https://api.anthropic.com/v1/messages"

        val rootJson = JSONObject().apply {
            put("model", model)
            put("max_tokens", 512)
            put("system", systemInstruction + "\nOutput strictly pure JSON with no markdown wrapping.")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", input)
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(rootJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Anthropic Error (HTTP ${response.code}): ${extractErrorMessage(body)}")
            }
            val respJson = JSONObject(body)
            val contentArr = respJson.getJSONArray("content")
            return contentArr.getJSONObject(0).getString("text")
        }
    }

    /**
     * Robust JSON extractor that recovers structured data even if the LLM adds markdown or chat tokens.
     */
    private fun extractJsonObject(rawText: String): JSONObject {
        val clean = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            JSONObject(clean)
        } catch (e: Exception) {
            val firstBrace = clean.indexOf('{')
            val lastBrace = clean.lastIndexOf('}')
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                JSONObject(clean.substring(firstBrace, lastBrace + 1))
            } else {
                throw IllegalStateException("Could not parse JSON response from LLM: $clean")
            }
        }
    }

    private fun extractErrorMessage(body: String): String {
        return try {
            val obj = JSONObject(body)
            if (obj.has("error")) {
                val errObj = obj.get("error")
                if (errObj is JSONObject) {
                    errObj.optString("message", errObj.toString())
                } else {
                    errObj.toString()
                }
            } else {
                body.take(160)
            }
        } catch (e: Exception) {
            body.take(160)
        }
    }

    /**
     * Performs a real network ping against ANY provider's endpoint with the user's API key.
     */
    suspend fun pingProvider(
        provider: String,
        apiKey: String,
        customEndpointUrl: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val trimmedKey = apiKey.trim()
            val pingUrl: String = when (provider.lowercase(Locale.ROOT)) {
                "nvidia", "nvidia nim" -> {
                    if (customEndpointUrl.isNotBlank()) customEndpointUrl else "https://integrate.api.nvidia.com/v1/models"
                }
                "openrouter" -> {
                    if (customEndpointUrl.isNotBlank()) customEndpointUrl else "https://openrouter.ai/api/v1/models"
                }
                "groq" -> "https://api.groq.com/openai/v1/models"
                "google gemini", "gemini" -> {
                    "https://generativelanguage.googleapis.com/v1beta/models?key=$trimmedKey"
                }
                "anthropic", "claude" -> "https://api.anthropic.com/v1/messages"
                "mistral", "mistral ai" -> "https://api.mistral.ai/v1/models"
                else -> {
                    if (customEndpointUrl.isNotBlank()) customEndpointUrl else "https://api.openai.com/v1/models"
                }
            }

            val requestBuilder = Request.Builder().url(pingUrl)

            if (provider.lowercase(Locale.ROOT) != "google gemini" && provider.lowercase(Locale.ROOT) != "gemini") {
                if (trimmedKey.isNotEmpty()) {
                    if (provider.lowercase(Locale.ROOT) == "anthropic" || provider.lowercase(Locale.ROOT) == "claude") {
                        requestBuilder.header("x-api-key", trimmedKey)
                        requestBuilder.header("anthropic-version", "2023-06-01")
                    } else {
                        requestBuilder.header("Authorization", "Bearer $trimmedKey")
                    }
                }
            }

            if (provider.lowercase(Locale.ROOT).contains("openrouter")) {
                requestBuilder.header("HTTP-Referer", "https://flowmind.ai")
                requestBuilder.header("X-Title", "FlowMind Assistant")
            }

            client.newCall(requestBuilder.get().build()).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                // If response is returned (HTTP 200, 400, 401, 403, 404, etc.), network round-trip succeeded!
                if (response.code == 401) {
                    Result.failure(IllegalStateException("HTTP 401: Invalid or unauthorized API key (${elapsed}ms)"))
                } else {
                    Result.success(elapsed)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
