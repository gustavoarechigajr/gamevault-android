package com.gamevault.android.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val cookieJar = PersistentCookieJar()

    // Short-timeout client used only for probing reachability (no auth needed)
    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    private val mainClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    // Download client — no read timeout for large files
    val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(0, TimeUnit.SECONDS)
        .build()

    private fun buildApi(baseUrl: String, client: OkHttpClient): GameVaultApi {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GameVaultApi::class.java)
    }

    fun getApi(baseUrl: String): GameVaultApi = buildApi(baseUrl, mainClient)

    /**
     * Probe the local URL with a no-auth HEAD request to /login.
     * Returns (api, chosenBaseUrl). The chosen URL is used by callers
     * to construct asset URLs (cover art, etc.).
     */
    suspend fun getApiSmart(remoteUrl: String, localUrl: String): Pair<GameVaultApi, String> {
        if (localUrl.isNotBlank()) {
            val normalized = if (localUrl.endsWith("/")) localUrl else "$localUrl/"
            try {
                withContext(Dispatchers.IO) {
                    val req = Request.Builder().url("${normalized}login").head().build()
                    probeClient.newCall(req).execute().close()
                }
                // Any response (including 405 Method Not Allowed) means server reachable
                return Pair(buildApi(localUrl, mainClient), localUrl.trimEnd('/'))
            } catch (_: Exception) {
                // Local unreachable, fall through
            }
        }
        return Pair(buildApi(remoteUrl, mainClient), remoteUrl.trimEnd('/'))
    }

    fun clearSession() = cookieJar.clear()
}
