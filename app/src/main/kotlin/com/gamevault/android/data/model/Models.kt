package com.gamevault.android.data.model

import com.google.gson.annotations.SerializedName

data class Platform(
    val id: String,
    val name: String,
    val color: String,
    val folders: List<String>,
    val count: Int = 0,
    val recursive: Boolean = false,
)

data class GameItem(
    val name: String,
    val type: String,
    val size: Long?,
    @SerializedName("size_human") val sizeHuman: String?,
    val path: String,
    val source: String? = null,
    @SerializedName("meta_key") val metaKey: String? = null,
    val meta: GameMeta? = null,
)

data class GameMeta(
    val title: String?,
    val description: String?,
    // Resolved full URL, built from boxArtPath at load time
    val coverUrl: String?,
)

// Mirrors the DB row returned by /api/platform/<id>/metadata
data class MetadataRow(
    val title: String?,
    val description: String?,
    @SerializedName("box_art_path") val boxArtPath: String?,
    val rating: String?,
    val genre: String?,
    val year: String?,
    @SerializedName("platform_id") val platformId: String?,
    val filename: String?,
)

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val ok: Boolean?, val error: String?)
data class MeResponse(val id: Int, val username: String, val role: String)
data class PlatformResponse(val platform: Platform, val items: List<GameItem>)
