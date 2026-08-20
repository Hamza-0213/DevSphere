package com.example.document.image

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class ImageMetadata(
    val title: String,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val sizeBytes: Long,
    val orientationDegrees: Int
)

class ImageEngine(private val context: Context) {

    suspend fun getImageMetadata(uri: Uri): Result<ImageMetadata> = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = if (uri.scheme == "file") {
                FileInputStream(File(uri.path ?: ""))
            } else {
                context.contentResolver.openInputStream(uri)
            }

            if (inputStream == null) {
                return@withContext Result.failure(Exception("Cannot open image file"))
            }

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Re-open for Exif
            var orientationDegrees = 0
            try {
                val exifStream = if (uri.scheme == "file") {
                    FileInputStream(File(uri.path ?: ""))
                } else {
                    context.contentResolver.openInputStream(uri)
                }
                if (exifStream != null) {
                    val exif = ExifInterface(exifStream)
                    orientationDegrees = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                    exifStream.close()
                }
            } catch (e: Exception) {
                // Ignore exif failure
            }

            val title = uri.lastPathSegment ?: "Image"
            val width = if (orientationDegrees == 90 || orientationDegrees == 270) options.outHeight else options.outWidth
            val height = if (orientationDegrees == 90 || orientationDegrees == 270) options.outWidth else options.outHeight

            Result.success(
                ImageMetadata(
                    title = title,
                    width = width,
                    height = height,
                    mimeType = options.outMimeType ?: "image/*",
                    sizeBytes = 0L,
                    orientationDegrees = orientationDegrees
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to read image metadata: ${e.localizedMessage}"))
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
