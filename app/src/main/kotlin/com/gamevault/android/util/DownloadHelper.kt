package com.gamevault.android.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.gamevault.android.data.api.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.coroutineContext

object DownloadHelper {

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

        platformDir.findFile(fileName)?.delete()

        val destFile = platformDir.createFile("application/octet-stream", fileName)
            ?: throw Exception("Cannot create file: $fileName")

        val request = Request.Builder()
            .url("${serverBaseUrl.trimEnd('/')}/download?path=${Uri.encode(filePath)}")
            .build()

        val call = ApiClient.downloadClient.newCall(request)

        // When the coroutine is cancelled, interrupt the blocking OkHttp I/O immediately
        coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause != null) call.cancel()
        }

        val response = try {
            call.execute()
        } catch (e: IOException) {
            destFile.delete()
            // If the coroutine was cancelled, ensureActive() converts this to CancellationException
            ensureActive()
            throw e
        }

        if (!response.isSuccessful) {
            destFile.delete()
            throw Exception("Server returned ${response.code}")
        }

        val body = response.body ?: run {
            destFile.delete()
            throw Exception("Empty response from server")
        }
        val totalBytes = body.contentLength()

        try {
            context.contentResolver.openOutputStream(destFile.uri)?.use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(32 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        ensureActive() // honour cancellation between chunks
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
        } catch (e: CancellationException) {
            destFile.delete()
            throw e
        } catch (e: IOException) {
            destFile.delete()
            ensureActive() // treat OkHttp IOException from call.cancel() as cancellation
            throw e
        }
    }
}
