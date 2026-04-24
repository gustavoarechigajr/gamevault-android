package com.gamevault.android.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ApiClient {
    private val cookieJar = PersistentCookieJar()

    @Volatile private var cachedUrl: String? = null

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

    // Returns true if a HEAD /login to the given URL succeeds within 3 seconds.
    private suspend fun probeUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${url.trimEnd('/')}/login")
                .head()
                .build()
            probeClient.newCall(request).execute().use { true }
        } catch (e: Exception) {
            Log.d("GameVaultProbe", "Probe failed for $url: $e")
            false
        }
    }

    private suspend fun discoverViaUdp(): String? = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 2000

            val msg = "who is gamevault?".toByteArray()
            val broadcast = InetAddress.getByName("255.255.255.255")
            Log.d("GameVaultDiscovery", "Sending UDP broadcast to 255.255.255.255:7359")
            socket.send(DatagramPacket(msg, msg.size, broadcast, 7359))
            Log.d("GameVaultDiscovery", "Broadcast sent, waiting for reply...")

            val buf = ByteArray(1024)
            val response = DatagramPacket(buf, buf.size)
            socket.receive(response)

            val raw = String(response.data, 0, response.length)
            Log.d("GameVaultDiscovery", "Got reply from ${response.address}: $raw")
            val json = JSONObject(raw)
            json.getString("address")
        } catch (e: Exception) {
            Log.e("GameVaultDiscovery", "Discovery failed: $e")
            null
        } finally {
            socket?.close()
        }
    }

    /**
     * Returns (api, chosenBaseUrl) using the fastest reachable server.
     *
     * Priority:
     *  1. Cached URL from a previous call this session (cleared on network change / logout).
     *  2. Explicit local URL (Settings → Local URL) — probed with HEAD /login first.
     *     If the probe fails (e.g. user is away from home), falls back to remote.
     *  3. UDP broadcast auto-discovery on port 7359.
     *  4. Remote DuckDNS / public URL.
     */
    suspend fun getApiSmart(remoteUrl: String, localUrl: String): Pair<GameVaultApi, String> {
        val remote = remoteUrl.trim()

        cachedUrl?.let { return Pair(buildApi(it, mainClient), it.trimEnd('/')) }

        val chosen = when {
            localUrl.isNotBlank() -> {
                val trimmed = localUrl.trim()
                if (probeUrl(trimmed)) trimmed else remote
            }
            else -> discoverViaUdp() ?: remote
        }

        cachedUrl = chosen
        return Pair(buildApi(chosen, mainClient), chosen.trimEnd('/'))
    }

    fun invalidateUrlCache() { cachedUrl = null }

    fun clearSession() {
        cookieJar.clear()
        cachedUrl = null
    }
}
