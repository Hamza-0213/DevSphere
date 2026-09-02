package com.example.filemanager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.local.database.DocumentEntity
import com.example.domain.model.DocumentType
import java.io.File

object DocumentNameResolver {

    /**
     * Resolves a clean, user-friendly file name without ugly URL encodings,
     * provider prefixes, or raw URI paths.
     */
    fun resolveDisplayName(
        uriString: String,
        docEntity: DocumentEntity? = null,
        context: Context? = null
    ): String {
        // 1. If we have a valid entity with a clean name, use it
        val entityName = docEntity?.displayName?.trim()
        if (!entityName.isNullOrEmpty() &&
            !entityName.startsWith("content://", ignoreCase = true) &&
            !entityName.startsWith("file://", ignoreCase = true) &&
            !entityName.startsWith("/data/user/", ignoreCase = true)
        ) {
            return cleanRawFileName(entityName)
        }

        val uri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            null
        } ?: return "Document"

        // 2. Query ContentResolver if context is provided and it's a content:// URI
        if (context != null && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            return cleanRawFileName(name)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore cursor query errors
            }
        }

        // 3. Extract and decode from URI path segments
        val decodedUri = try {
            Uri.decode(uriString)
        } catch (e: Exception) {
            uriString
        }

        val lastSegment = try {
            uri.lastPathSegment?.let { Uri.decode(it) } ?: decodedUri.substringAfterLast("/")
        } catch (e: Exception) {
            decodedUri.substringAfterLast("/")
        }

        val candidate = cleanRawFileName(lastSegment)
        if (candidate.isNotBlank() && candidate != "document" && candidate != "tree") {
            return candidate
        }

        // 4. Fallback based on extension
        val ext = resolveExtension(uriString, docEntity)
        val docType = DocumentType.fromExtension(ext)
        return if (docType != DocumentType.UNKNOWN) "${docType.displayName} Document" else "Document"
    }

    /**
     * Cleans up file names removing prefixes like "raw:", "primary:", URL encoding artifacts, etc.
     */
    fun cleanRawFileName(rawName: String): String {
        var name = rawName.trim()
        if (name.contains("%20")) {
            name = try {
                Uri.decode(name)
            } catch (e: Exception) {
                name.replace("%20", " ")
            }
        }

        // Remove provider prefixes like raw:/storage/emulated/0/... or primary:Documents/...
        if (name.contains("/")) {
            name = name.substringAfterLast("/")
        }
        if (name.contains(":")) {
            name = name.substringAfterLast(":")
        }

        // Remove timestamp prefixes like 1725200000_
        val timestampRegex = Regex("^\\d{10,13}_")
        if (timestampRegex.containsMatchIn(name)) {
            name = name.replaceFirst(timestampRegex, "")
        }

        // If file name has underscores separating words, keep extension clean
        return name.ifBlank { "Document" }
    }

    /**
     * Resolves file extension
     */
    fun resolveExtension(uriString: String, docEntity: DocumentEntity? = null): String {
        if (docEntity != null && docEntity.extension.isNotBlank()) {
            return docEntity.extension.lowercase().removePrefix(".")
        }
        val cleanName = resolveDisplayName(uriString, docEntity)
        val ext = cleanName.substringAfterLast(".", "").lowercase()
        if (ext.isNotBlank() && ext.length <= 5) {
            return ext
        }
        return ""
    }

    /**
     * Resolves document type
     */
    fun resolveDocumentType(uriString: String, docEntity: DocumentEntity? = null): DocumentType {
        if (docEntity != null) {
            return docEntity.toDocumentType()
        }
        val ext = resolveExtension(uriString, docEntity)
        return DocumentType.fromExtension(ext)
    }
}
