package com.gamevault.android.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.gamevault.android.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object DownloadHelper {

    /**
     * Download a game file from the server and write it directly to the
     * correct EmulationStation subfolder using SAF (Storage Access Framework).
     *
     * Streams via OkHttp so there's no intermediate copy and no DownloadManager
     * content:// URI incompatibility.
     *
     * @param onProgress 0–100 percent, or -1 if content-length unknown
     */
    suspend fun download(
        context: Context,
        serverBaseUrl: String,
        filePath: String,
        platformId: String,
        romsRootUri: String,
        onProgress: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val fileName   = filePath.substringAfterLast("/")
        val folderName = PlatformMapper.getFolderName(platformId)

        val root = DocumentFile.fromTreeUri(context, Uri.parse(romsRootUri))
            ?: throw Exception("Cannot access ROMs folder. Re-pick it in Settings.")

        val platformDir = root.findFile(folderName)
            ?: root.createDirectory(folderName)
            ?: throw Exception("Cannot create folder: $folderName")

        // Remove existing file so we start fresh
        platformDir.findFile(fileName)?.delete()

        val destFile = platformDir.createFile("application/octet-stream", fileName)
            ?: throw Exception("Cannot create file: $fileName")

        val url = "${serverBaseUrl.trimEnd('/')}/download?path=${Uri.encode(filePath)}"
        val request = Request.Builder().url(url).build()

        val response = ApiClient.downloadClient.newCall(request).execute()
        if (!response.isSuccessful) {
            destFile.delete()
            throw Exception("Server returned ${response.code}")
        }

        val body = response.body ?: throw Exception("Empty response from server")
        val totalBytes = body.contentLength()

        context.contentResolver.openOutputStream(destFile.uri)?.use { out ->
            body.byteStream().use { input ->
                val buffer = ByteArray(32 * 1024)
                var downloaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    onProgress(
                        if (totalBytes > 0) (downloaded * 100 / totalBytes).toInt() else -1
                    )
                }
            }
        } ?: run {
            destFile.delete()
            throw Exception("Cannot open output stream")
        }
    }
}
