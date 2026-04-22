package com.gamevault.android.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    // Shared cookie jar so session cookie persists across requests
    private val cookieJar = PersistentCookieJar()

    private val httpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    fun getApi(baseUrl: String): GameVaultApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (retrofit == null || normalizedUrl != currentBaseUrl) {
            retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            currentBaseUrl = normalizedUrl
        }
        return retrofit!!.create(GameVaultApi::class.java)
    }

    fun clearSession() {
        cookieJar.clear()
    }
}
