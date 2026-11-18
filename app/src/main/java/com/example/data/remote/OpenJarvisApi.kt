package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenAiChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Float = 0.6f,
    @Json(name = "max_tokens") val maxTokens: Int = 1024,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatChoice> = emptyList(),
    val usage: TokenUsage? = null
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    val index: Int = 0,
    val message: OpenAiChatMessage? = null,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenUsage(
    @Json(name = "prompt_tokens") val promptTokens: Int = 0,
    @Json(name = "completion_tokens") val completionTokens: Int = 0,
    @Json(name = "total_tokens") val totalTokens: Int = 0
)

@JsonClass(generateAdapter = true)
data class ModelsListResponse(
    val data: List<RemoteModelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RemoteModelItem(
    val id: String
)

interface OpenJarvisServerApi {
    @GET("models")
    suspend fun listModels(): ModelsListResponse

    @POST("chat/completions")
    suspend fun chatCompletions(@Body request: ChatCompletionRequest): ChatCompletionResponse
}

object OpenJarvisApiFactory {
    private val sharedClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun create(baseUrl: String): OpenJarvisServerApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(sharedClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenJarvisServerApi::class.java)
    }
}
