package com.gamevault.android.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val cookieJar = PersistentCookieJar()

    // Short-timeout client used only for probing the local URL
    private val probeClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
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
     * Try the local URL first (3s timeout). If it's reachable, use it.
     * Otherwise fall back to the remote URL. Completely transparent to the caller.
     */
    suspend fun getApiSmart(remoteUrl: String, localUrl: String): GameVaultApi {
        if (localUrl.isNotBlank()) {
            try {
                val probe = buildApi(localUrl, probeClient)
                probe.getPlatforms() // lightweight ping
                return buildApi(localUrl, mainClient)
            } catch (_: Exception) {
                // local not reachable, fall through to remote
            }
        }
        return buildApi(remoteUrl, mainClient)
    }

    fun clearSession() = cookieJar.clear()
}
