package com.example.document.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

class PdfEngine(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempCachedFile: File? = null

    var pageCount: Int = 0
        private set

    var isLoaded: Boolean = false
        private set

    var isPasswordProtected: Boolean = false
        private set

    /**
     * Open a PDF file from URI
     */
    suspend fun openPdf(uri: Uri, password: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        close()
        try {
            // Obtain a guaranteed seekable ParcelFileDescriptor from local file or cache
            val fileToRender: File = if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (!f.exists()) {
                    return@withContext Result.failure(Exception("File does not exist: ${f.absolutePath}"))
                }
                f
            } else {
                // For content:// or other URIs, always create a local seekable cache file
                val cached = copyToCache(uri)
                if (cached == null || !cached.exists()) {
                    return@withContext Result.failure(Exception("Unable to read file stream from $uri"))
                }
                cached
            }

            val pfd = ParcelFileDescriptor.open(fileToRender, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor = pfd

            try {
                pdfRenderer = PdfRenderer(pfd)
                pageCount = pdfRenderer?.pageCount ?: 0
                isLoaded = true
                isPasswordProtected = false
                Result.success(pageCount)
            } catch (e: Exception) {
                // Check if the PDF is genuinely encrypted with a password
                val isEncrypted = checkIfPdfIsEncrypted(fileToRender)
                if (isEncrypted) {
                    isPasswordProtected = true
                    Result.failure(Exception("This document is password protected."))
                } else {
                    isPasswordProtected = false
                    val errorMsg = e.localizedMessage ?: "Invalid or unsupported PDF format."
                    Result.failure(Exception("Unable to display PDF: $errorMsg"))
                }
            }
        } catch (e: Exception) {
            isPasswordProtected = false
            Result.failure(Exception("Error opening document: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    /**
     * Inspect PDF bytes for /Encrypt dictionary to determine true encryption
     */
    private fun checkIfPdfIsEncrypted(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() == 0L) return false
            val buffer = ByteArray(minOf(file.length().toInt(), 65536))
            FileInputStream(file).use { input ->
                input.read(buffer)
            }
            val content = String(buffer, Charsets.ISO_8859_1)
            content.contains("/Encrypt", ignoreCase = false)
        } catch (e: Exception) {
            false
        }
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val cacheFile = File(context.cacheDir, "pdf_render_${System.currentTimeMillis()}.tmp")
            tempCachedFile = cacheFile
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Render a specific page (0-indexed) into a high quality Bitmap
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        var page: PdfRenderer.Page? = null
        try {
            page = renderer.openPage(pageIndex)
            val originalWidth = page.width
            val originalHeight = page.height

            val scale = (targetWidth.toFloat() / originalWidth.toFloat()).coerceAtLeast(1.0f)
            val renderWidth = (originalWidth * scale).toInt().coerceAtMost(2400)
            val renderHeight = (originalHeight * scale).toInt().coerceAtMost(3600)

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // Ensure pure white background

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                page?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun close() {
        try {
            pdfRenderer?.close()
        } catch (e: Exception) {
            // Ignore
        }
        pdfRenderer = null

        try {
            fileDescriptor?.close()
        } catch (e: Exception) {
            // Ignore
        }
        fileDescriptor = null

        try {
            tempCachedFile?.delete()
        } catch (e: Exception) {
            // Ignore
        }
        tempCachedFile = null
        isLoaded = false
        pageCount = 0
    }
}

