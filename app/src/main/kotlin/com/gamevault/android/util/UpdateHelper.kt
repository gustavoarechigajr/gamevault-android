package com.gamevault.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class ReleaseInfo(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("assets")   val assets: List<ReleaseAsset>,
)

data class ReleaseAsset(
    @SerializedName("name")                 val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
)

object UpdateHelper {
    private const val RELEASES_API = "https://api.github.com/repos/gustavoarechigajr/gamevault-android/releases/latest"

    private val client = OkHttpClient()

    suspend fun fetchLatestRelease(): ReleaseInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("GitHub API returned ${response.code}")
        val body = response.body?.string() ?: throw Exception("Empty response from GitHub")
        com.google.gson.Gson().fromJson(body, ReleaseInfo::class.java)
    }

    fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val remote  = remoteTag.trimStart('v').toVersionParts() ?: return false
        val current = currentVersion.toVersionParts()           ?: return false
        return remote > current
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val apkFile = File(context.cacheDir, "gamevault-update.apk")
        val request = Request.Builder().url(downloadUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")

        val body = response.body ?: throw Exception("Empty download response")
        val totalBytes = body.contentLength()

        apkFile.outputStream().use { out ->
            body.byteStream().use { input ->
                val buffer = ByteArray(32 * 1024)
                var downloaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    onProgress(if (totalBytes > 0) (downloaded * 100 / totalBytes).toInt() else -1)
                }
            }
        }
        apkFile
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        context.startActivity(intent)
    }

    private fun String.toVersionParts(): List<Int>? =
        split(".").map { it.toIntOrNull() ?: return null }

    private operator fun List<Int>.compareTo(other: List<Int>): Int {
        val len = maxOf(size, other.size)
        for (i in 0 until len) {
            val a = getOrElse(i) { 0 }
            val b = other.getOrElse(i) { 0 }
            if (a != b) return a.compareTo(b)
        }
        return 0
    }
}
