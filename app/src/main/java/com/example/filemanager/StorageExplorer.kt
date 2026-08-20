package com.example.filemanager

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.data.local.database.DocumentDao
import com.example.data.local.database.DocumentEntity
import com.example.domain.model.DocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class FileItem(
    val file: File? = null,
    val uri: Uri? = null,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val documentType: DocumentType,
    val childCount: Int = 0
)

class StorageExplorer(
    private val context: Context,
    private val documentDao: DocumentDao
) {

    /**
     * Get root storage paths available on the device
     */
    fun getRootLocations(): List<FileItem> {
        val roots = mutableListOf<FileItem>()

        // 1. App Internal Files & Samples
        val appFiles = context.filesDir
        if (appFiles.exists()) {
            roots.add(
                FileItem(
                    file = appFiles,
                    name = "DocSphere Library & Samples",
                    path = appFiles.absolutePath,
                    isDirectory = true,
                    sizeBytes = appFiles.length(),
                    lastModified = appFiles.lastModified(),
                    documentType = DocumentType.UNKNOWN,
                    childCount = appFiles.listFiles()?.size ?: 0
                )
            )
        }

        // 2. Primary External / Emulated Storage
        try {
            val primaryExternal = Environment.getExternalStorageDirectory()
            if (primaryExternal != null && primaryExternal.exists() && primaryExternal.canRead()) {
                roots.add(
                    FileItem(
                        file = primaryExternal,
                        name = "Internal Device Storage",
                        path = primaryExternal.absolutePath,
                        isDirectory = true,
                        sizeBytes = primaryExternal.freeSpace,
                        lastModified = primaryExternal.lastModified(),
                        documentType = DocumentType.UNKNOWN,
                        childCount = primaryExternal.listFiles()?.size ?: 0
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Common Public Media & Document Directories
        val standardDirs = listOf(
            Environment.DIRECTORY_DOCUMENTS to "Documents",
            Environment.DIRECTORY_DOWNLOADS to "Downloads",
            Environment.DIRECTORY_DCIM to "DCIM / Photos",
            Environment.DIRECTORY_PICTURES to "Pictures"
        )

        for ((type, label) in standardDirs) {
            try {
                val dir = Environment.getExternalStoragePublicDirectory(type)
                if (dir != null && dir.exists() && dir.canRead()) {
                    roots.add(
                        FileItem(
                            file = dir,
                            name = label,
                            path = dir.absolutePath,
                            isDirectory = true,
                            sizeBytes = dir.length(),
                            lastModified = dir.lastModified(),
                            documentType = DocumentType.UNKNOWN,
                            childCount = dir.listFiles()?.size ?: 0
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore inaccessible folders
            }
        }

        // 4. Secondary SD Cards / Removable Volumes
        try {
            val extDirs = ContextCompat.getExternalFilesDirs(context, null)
            for (dir in extDirs) {
                if (dir != null && dir.exists() && !dir.absolutePath.contains(context.packageName)) {
                    roots.add(
                        FileItem(
                            file = dir,
                            name = "SD Card / External Volume",
                            path = dir.absolutePath,
                            isDirectory = true,
                            sizeBytes = dir.freeSpace,
                            lastModified = dir.lastModified(),
                            documentType = DocumentType.UNKNOWN,
                            childCount = dir.listFiles()?.size ?: 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        return roots
    }

    /**
     * List contents of a physical directory
     */
    suspend fun listDirectory(directory: File): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        if (!directory.exists() || !directory.canRead()) return@withContext results

        val files = directory.listFiles() ?: return@withContext results

        for (file in files) {
            if (file.isHidden) continue

            if (file.isDirectory) {
                val children = file.listFiles()?.count { !it.isHidden } ?: 0
                results.add(
                    FileItem(
                        file = file,
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = true,
                        sizeBytes = 0L,
                        lastModified = file.lastModified(),
                        documentType = DocumentType.UNKNOWN,
                        childCount = children
                    )
                )
            } else {
                val ext = file.extension.lowercase()
                val docType = DocumentType.fromExtension(ext)
                // Filter to supported document types or display all valid files
                results.add(
                    FileItem(
                        file = file,
                        uri = Uri.fromFile(file),
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        sizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        documentType = docType
                    )
                )
            }
        }

        // Sort: Directories first, then by name
        results.sortedWith(
            compareBy<FileItem> { !it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Index a single picked document from SAF URI
     */
    suspend fun indexPickedDocument(uri: Uri): DocumentEntity = withContext(Dispatchers.IO) {
        // Persist permissions if possible
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // Some URI providers don't support persistable permissions
        }

        var displayName = "Document"
        var sizeBytes = 0L

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: "Document"
                        if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val f = File(uri.path ?: "")
            displayName = f.name
            sizeBytes = f.length()
        }

        val extension = displayName.substringAfterLast(".", "").lowercase()
        val docType = DocumentType.fromExtension(extension)

        val entity = DocumentEntity(
            uri = uri.toString(),
            displayName = displayName,
            extension = extension,
            fileType = docType.name,
            sizeBytes = sizeBytes,
            lastModified = System.currentTimeMillis(),
            lastOpenedTimestamp = System.currentTimeMillis(),
            isFavourite = false,
            lastReadingPosition = 0,
            readingProgressPercent = 0f,
            pageCount = 1,
            isSample = false
        )

        documentDao.insertDocument(entity)
        entity
    }

    /**
     * Clean up stale indexed entries (files moved/deleted on disk)
     */
    suspend fun cleanStaleEntries() = withContext(Dispatchers.IO) {
        try {
            val allIndexed = documentDao.getAllDocumentsDirect()
            for (doc in allIndexed) {
                if (doc.isSample) continue // Samples are preserved

                val uri = Uri.parse(doc.uri)
                if (uri.scheme == ContentResolver.SCHEME_FILE) {
                    val file = File(uri.path ?: "")
                    if (!file.exists()) {
                        documentDao.deleteDocumentByUri(doc.uri)
                    }
                } else if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                    try {
                        context.contentResolver.openInputStream(uri)?.close()
                    } catch (e: Exception) {
                        // File no longer accessible or deleted
                        documentDao.deleteDocumentByUri(doc.uri)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Scan and index a directory recursively in background
     */
    suspend fun indexDirectory(dir: File) = withContext(Dispatchers.IO) {
        if (!dir.exists() || !dir.canRead()) return@withContext
        val filesToIndex = mutableListOf<DocumentEntity>()

        fun scanRecursive(folder: File, depth: Int) {
            if (depth > 4) return // Guard against circular symlinks
            val list = folder.listFiles() ?: return
            for (file in list) {
                if (file.isHidden) continue
                if (file.isDirectory) {
                    scanRecursive(file, depth + 1)
                } else {
                    val ext = file.extension.lowercase()
                    val docType = DocumentType.fromExtension(ext)
                    if (docType != DocumentType.UNKNOWN) {
                        filesToIndex.add(
                            DocumentEntity(
                                uri = Uri.fromFile(file).toString(),
                                displayName = file.name,
                                extension = ext,
                                fileType = docType.name,
                                sizeBytes = file.length(),
                                lastModified = file.lastModified(),
                                lastOpenedTimestamp = 0L,
                                isFavourite = false,
                                lastReadingPosition = 0,
                                readingProgressPercent = 0f,
                                pageCount = 1,
                                isSample = false
                            )
                        )
                    }
                }
            }
        }

        scanRecursive(dir, 0)
        if (filesToIndex.isNotEmpty()) {
            documentDao.insertDocuments(filesToIndex)
        }
    }
}
