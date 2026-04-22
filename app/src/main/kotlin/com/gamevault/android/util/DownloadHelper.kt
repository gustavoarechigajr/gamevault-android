package com.gamevault.android.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object DownloadHelper {

    /**
     * Enqueue a game file download using Android's DownloadManager.
     * The file lands in {romsRootUri}/{esFolderName}/{filename}.
     *
     * Returns the DownloadManager enqueue ID, or -1 on failure.
     */
    fun enqueue(
        context: Context,
        serverBaseUrl: String,
        filePath: String,
        platformId: String,
        romsRootUri: String,
        sessionCookie: String,
    ): Long {
        val fileName = filePath.substringAfterLast("/")
        val folderName = PlatformMapper.getFolderName(platformId)

        val rootDoc = DocumentFile.fromTreeUri(context, Uri.parse(romsRootUri))
            ?: return -1L

        val platformDir = rootDoc.findFile(folderName)
            ?: rootDoc.createDirectory(folderName)
            ?: return -1L

        val destUri = platformDir.uri

        val downloadUrl = "${serverBaseUrl.trimEnd('/')}/download?path=${Uri.encode(filePath)}"

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(fileName)
            .setDescription("Downloading via GameVault")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .addRequestHeader("Cookie", sessionCookie)
            .setDestinationUri(Uri.withAppendedPath(destUri, fileName))

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
}
