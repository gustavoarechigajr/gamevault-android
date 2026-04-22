package com.gamevault.android.data.api

import com.gamevault.android.data.model.GameItem
import com.gamevault.android.data.model.LoginRequest
import com.gamevault.android.data.model.LoginResponse
import com.gamevault.android.data.model.MeResponse
import com.gamevault.android.data.model.Platform
import com.gamevault.android.data.model.PlatformResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GameVaultApi {

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/me")
    suspend fun me(): Response<MeResponse>

    @GET("api/platforms")
    suspend fun getPlatforms(): Response<List<Platform>>

    @GET("api/platform/{id}")
    suspend fun getPlatform(@Path("id") id: String): Response<PlatformResponse>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("platform") platform: String? = null,
    ): Response<List<Any>>

    @GET("download")
    @Streaming
    suspend fun downloadFile(@Query("path") path: String): Response<ResponseBody>

    @GET("logout")
    suspend fun logout(): Response<Unit>
}
