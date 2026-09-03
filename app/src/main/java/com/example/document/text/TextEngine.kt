package com.example.document.text

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class TextDocument(
    val title: String,
    val content: String,
    val lines: List<String>,
    val lineCount: Int,
    val characterCount: Int,
    val wordCount: Int,
    val encoding: String,
    val isCsv: Boolean
)

class TextEngine(private val context: Context) {

    suspend fun parseText(uri: Uri): Result<TextDocument> = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = if (uri.scheme == "file") {
                FileInputStream(File(uri.path ?: ""))
            } else {
                context.contentResolver.openInputStream(uri)
            }

            if (inputStream == null) {
                return@withContext Result.failure(Exception("Cannot open text file stream"))
            }

            val rawBytes = inputStream.readBytes()
            val charset = detectCharset(rawBytes)
            val fullText = String(rawBytes, charset)

            val lines = fullText.split("\n").map { it.trimEnd('\r') }
            val charCount = fullText.length
            val wordCount = fullText.split("\\s+".toRegex()).count { it.isNotBlank() }

            val fileName = uri.lastPathSegment ?: "Text Document"
            val isCsv = fileName.endsWith(".csv", ignoreCase = true)

            Result.success(
                TextDocument(
                    title = fileName,
                    content = fullText,
                    lines = lines,
                    lineCount = lines.size,
                    characterCount = charCount,
                    wordCount = wordCount,
                    encoding = charset.displayName(),
                    isCsv = isCsv
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to read text file: ${e.localizedMessage}"))
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun saveText(uri: Uri, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val outputStream = if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (!f.exists()) {
                    f.parentFile?.mkdirs()
                    f.createNewFile()
                }
                java.io.FileOutputStream(f)
            } else {
                context.contentResolver.openOutputStream(uri, "wt")
                    ?: context.contentResolver.openOutputStream(uri)
            }

            if (outputStream == null) {
                return@withContext Result.failure(Exception("Cannot open file for writing"))
            }

            outputStream.use { os ->
                os.write(text.toByteArray(StandardCharsets.UTF_8))
                os.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createNewTextFile(fileName: String, initialContent: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
            val cleanName = if (fileName.endsWith(".txt", ignoreCase = true)) fileName else "$fileName.txt"
            val file = File(docsDir, cleanName)
            file.writeText(initialContent, StandardCharsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun detectCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return StandardCharsets.UTF_8
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return StandardCharsets.UTF_16BE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return StandardCharsets.UTF_16LE
        }
        // Default to UTF-8
        return StandardCharsets.UTF_8
    }
}
