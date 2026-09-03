package com.example.document.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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

    suspend fun saveEditedImage(
        uri: Uri,
        rotationDegrees: Float,
        flipH: Boolean,
        flipV: Boolean,
        filter: String,
        brightness: Float = 0f
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val inputStream = if (uri.scheme == "file") {
                FileInputStream(File(uri.path ?: ""))
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return@withContext Result.failure(Exception("Cannot open source image"))

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                return@withContext Result.failure(Exception("Unable to decode source bitmap"))
            }

            val matrix = Matrix().apply {
                if (rotationDegrees != 0f) {
                    postRotate(rotationDegrees)
                }
                val sx = if (flipH) -1f else 1f
                val sy = if (flipV) -1f else 1f
                if (flipH || flipV) {
                    postScale(sx, sy)
                }
            }

            val transformedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )

            val finalBitmap = if (filter != "NORMAL" || brightness != 0f) {
                val resultBitmap = Bitmap.createBitmap(
                    transformedBitmap.width,
                    transformedBitmap.height,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(resultBitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)

                val cm = ColorMatrix()
                when (filter) {
                    "GRAYSCALE" -> cm.setSaturation(0f)
                    "SEPIA" -> {
                        val sepiaMatrix = ColorMatrix().apply {
                            setScale(1f, 0.95f, 0.82f, 1.0f)
                        }
                        cm.set(sepiaMatrix)
                    }
                    "INVERT" -> {
                        cm.set(floatArrayOf(
                            -1f, 0f, 0f, 0f, 255f,
                            0f, -1f, 0f, 0f, 255f,
                            0f, 0f, -1f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                    "WARM" -> {
                        cm.set(floatArrayOf(
                            1.15f, 0f, 0f, 0f, 0f,
                            0f, 1.05f, 0f, 0f, 0f,
                            0f, 0f, 0.9f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                }

                if (brightness != 0f) {
                    val brightMatrix = ColorMatrix().apply {
                        set(floatArrayOf(
                            1f, 0f, 0f, 0f, brightness,
                            0f, 1f, 0f, 0f, brightness,
                            0f, 0f, 1f, 0f, brightness,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                    cm.postConcat(brightMatrix)
                }

                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(transformedBitmap, 0f, 0f, paint)
                resultBitmap
            } else {
                transformedBitmap
            }

            val imagesDir = File(context.filesDir, "edited_images").apply { mkdirs() }
            val fileName = "DocSphere_Edit_${System.currentTimeMillis()}.jpg"
            val destFile = File(imagesDir, fileName)

            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }

            Result.success(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
