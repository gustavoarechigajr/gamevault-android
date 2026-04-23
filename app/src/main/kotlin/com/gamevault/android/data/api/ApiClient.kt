package com.gamevault.android.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ApiClient {
    private val cookieJar = PersistentCookieJar()

    @Volatile private var cachedUrl: String? = null

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

    /**
     * Sends a UDP broadcast on port 7359 asking "who is gamevault?".
     * The Windows PC relay service replies with {"address":"http://192.168.1.x:5555",...}.
     * Returns the local address on success, null if no reply within 2 seconds.
     */
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
     * On each call:
     *  1. If a cached URL exists for this session, return it immediately.
     *  2. Send a UDP broadcast on port 7359 — the Windows PC relay responds with
     *     the local LAN address (http://192.168.1.x:5555) within ~100ms if on the
     *     same network.
     *  3. If no UDP reply in 2 seconds, fall back to the remote DuckDNS URL.
     *
     * Cache is cleared on logout (clearSession) and on network change
     * (invalidateUrlCache, called by ConnectivityManager.NetworkCallback in MainActivity),
     * so the app always picks the right path as you move between home and away.
     */
    suspend fun getApiSmart(remoteUrl: String, localUrl: String): Pair<GameVaultApi, String> {
        val remote = remoteUrl.trim()

        cachedUrl?.let { return Pair(buildApi(it, mainClient), it.trimEnd('/')) }

        // Try explicit local URL first if configured, then UDP broadcast, then remote
        val chosen = if (localUrl.isNotBlank()) {
            localUrl.trim()
        } else {
            discoverViaUdp() ?: remote
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
