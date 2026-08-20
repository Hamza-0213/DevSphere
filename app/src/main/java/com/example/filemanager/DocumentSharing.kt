package com.example.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object DocumentSharing {

    fun shareDocument(context: Context, documentUriStr: String, mimeType: String?, title: String?) {
        try {
            val uri = Uri.parse(documentUriStr)
            val shareUri: Uri = if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                uri
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType ?: "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, title ?: "Document")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Document via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
