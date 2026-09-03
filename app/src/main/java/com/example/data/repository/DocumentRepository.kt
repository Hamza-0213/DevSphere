package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.database.BookmarkDao
import com.example.data.local.database.BookmarkEntity
import com.example.data.local.database.DocSphereDatabase
import com.example.data.local.database.DocumentDao
import com.example.data.local.database.DocumentEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.DocumentType
import com.example.filemanager.StorageExplorer
import com.example.sample.SampleDocumentGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val bookmarkDao: BookmarkDao,
    private val userPreferences: UserPreferencesRepository,
    private val storageExplorer: StorageExplorer
) {

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val recentDocuments: Flow<List<DocumentEntity>> = documentDao.getRecentDocuments()
    val favouriteDocuments: Flow<List<DocumentEntity>> = documentDao.getFavouriteDocuments()

    fun getDocumentsByType(type: DocumentType): Flow<List<DocumentEntity>> {
        return documentDao.getDocumentsByType(type.name)
    }

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> {
        return documentDao.searchDocuments(query.trim())
    }

    fun getDocument(uri: String): Flow<DocumentEntity?> {
        return documentDao.getDocumentByUri(uri)
    }

    suspend fun getDocumentDirect(uri: String): DocumentEntity? = withContext(Dispatchers.IO) {
        documentDao.getDocumentByUriDirect(uri)
    }

    suspend fun toggleFavourite(uri: String, currentIsFav: Boolean) = withContext(Dispatchers.IO) {
        documentDao.setFavourite(uri, !currentIsFav)
    }

    suspend fun setFavourite(uri: String, isFav: Boolean) = withContext(Dispatchers.IO) {
        documentDao.setFavourite(uri, isFav)
    }

    suspend fun recordDocumentOpened(uri: String, initialPage: Int = 0) = withContext(Dispatchers.IO) {
        val existing = documentDao.getDocumentByUriDirect(uri)
        if (existing != null) {
            documentDao.updateLastOpened(
                uri = uri,
                timestamp = System.currentTimeMillis(),
                position = existing.lastReadingPosition.coerceAtLeast(initialPage),
                progress = existing.readingProgressPercent
            )
        }
    }

    suspend fun updateReadingPosition(uri: String, position: Int, progress: Float, pageCount: Int = 0) = withContext(Dispatchers.IO) {
        val existing = documentDao.getDocumentByUriDirect(uri)
        if (existing != null) {
            documentDao.updateLastOpened(
                uri = uri,
                timestamp = System.currentTimeMillis(),
                position = position,
                progress = progress
            )
            if (pageCount > 0) {
                documentDao.updatePageCount(uri, pageCount)
            }
        }
    }

    suspend fun clearRecentHistory() = withContext(Dispatchers.IO) {
        documentDao.clearRecentHistory()
    }

    suspend fun deleteDocument(uri: String) = withContext(Dispatchers.IO) {
        documentDao.deleteDocumentByUri(uri)
    }

    suspend fun updateDocumentStats(uri: String, sizeBytes: Long) = withContext(Dispatchers.IO) {
        documentDao.updateDocumentStats(uri, sizeBytes, System.currentTimeMillis())
    }

    suspend fun renameDocument(oldUriStr: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val oldDoc = documentDao.getDocumentByUriDirect(oldUriStr)
                ?: return@withContext Result.failure(Exception("Document not found"))

            val cleanNewName = newName.trim()
            if (cleanNewName.isBlank()) {
                return@withContext Result.failure(Exception("Name cannot be empty"))
            }

            val oldUri = Uri.parse(oldUriStr)
            if (oldUri.scheme == "file" || oldUriStr.startsWith("/")) {
                val oldFile = if (oldUriStr.startsWith("/")) File(oldUriStr) else File(oldUri.path ?: "")
                if (oldFile.exists()) {
                    val parent = oldFile.parentFile ?: context.filesDir
                    val newFile = File(parent, cleanNewName)
                    val renamed = oldFile.renameTo(newFile)
                    if (renamed) {
                        val newUriStr = Uri.fromFile(newFile).toString()
                        documentDao.updateDocumentUriAndName(
                            oldUri = oldUriStr,
                            newUri = newUriStr,
                            newDisplayName = cleanNewName,
                            lastModified = System.currentTimeMillis()
                        )
                        return@withContext Result.success(newUriStr)
                    }
                }
            }

            // Fallback for SAF URIs or virtual documents: update display name in Room
            documentDao.updateDisplayName(oldUriStr, cleanNewName)
            Result.success(oldUriStr)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun createNewTextDocument(fileName: String, content: String = ""): Result<DocumentEntity> = withContext(Dispatchers.IO) {
        try {
            val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
            val cleanName = if (fileName.endsWith(".txt", ignoreCase = true)) fileName else "$fileName.txt"
            val file = File(docsDir, cleanName)
            file.writeText(content, java.nio.charset.StandardCharsets.UTF_8)

            val uriStr = Uri.fromFile(file).toString()
            val entity = DocumentEntity(
                uri = uriStr,
                displayName = cleanName,
                extension = "txt",
                fileType = DocumentType.TEXT.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                lastOpenedTimestamp = System.currentTimeMillis(),
                isFavourite = false,
                lastReadingPosition = 0,
                readingProgressPercent = 0f,
                pageCount = 1,
                isSample = false
            )
            documentDao.insertDocument(entity)
            Result.success(entity)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun indexLocalFile(file: File): DocumentEntity = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        val docType = DocumentType.fromExtension(extension)
        val entity = DocumentEntity(
            uri = Uri.fromFile(file).toString(),
            displayName = file.name,
            extension = extension,
            fileType = docType.name,
            sizeBytes = file.length(),
            lastModified = file.lastModified(),
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

    // Bookmarks
    fun getBookmarksForDocument(documentUri: String): Flow<List<BookmarkEntity>> {
        return bookmarkDao.getBookmarksForDocument(documentUri)
    }

    suspend fun addBookmark(documentUri: String, pageNumber: Int, title: String) = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkForPage(documentUri, pageNumber)
        if (existing == null) {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    documentUri = documentUri,
                    pageNumber = pageNumber,
                    title = title.ifBlank { "Page $pageNumber" }
                )
            )
        }
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmark(id)
    }

    suspend fun removeBookmarkByPage(documentUri: String, pageNumber: Int) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarkByPage(documentUri, pageNumber)
    }

    suspend fun updateBookmarkTitle(id: Long, title: String) = withContext(Dispatchers.IO) {
        bookmarkDao.updateBookmarkTitle(id, title)
    }

    // Indexing and Initialization
    suspend fun initializeSamplesIfNeeded() = withContext(Dispatchers.IO) {
        val settings = userPreferences.userSettingsFlow.first()
        if (!settings.hasInitializedSamples) {
            val samples = SampleDocumentGenerator.generateInitialSamples(context)
            documentDao.insertDocuments(samples)
            userPreferences.setHasInitializedSamples(true)
        }
    }

    suspend fun reloadSamples() = withContext(Dispatchers.IO) {
        val samples = SampleDocumentGenerator.generateInitialSamples(context)
        documentDao.insertDocuments(samples)
    }

    suspend fun indexPickedDocument(uri: Uri): DocumentEntity = withContext(Dispatchers.IO) {
        storageExplorer.indexPickedDocument(uri)
    }

    suspend fun scanStorageDirectory(dir: File) = withContext(Dispatchers.IO) {
        storageExplorer.indexDirectory(dir)
    }

    suspend fun cleanStaleEntries() = withContext(Dispatchers.IO) {
        storageExplorer.cleanStaleEntries()
    }

    companion object {
        @Volatile
        private var INSTANCE: DocumentRepository? = null

        fun getInstance(context: Context): DocumentRepository {
            return INSTANCE ?: synchronized(this) {
                val db = DocSphereDatabase.getDatabase(context)
                val userPrefs = UserPreferencesRepository(context)
                val explorer = StorageExplorer(context, db.documentDao())
                val repo = DocumentRepository(
                    context = context.applicationContext,
                    documentDao = db.documentDao(),
                    bookmarkDao = db.bookmarkDao(),
                    userPreferences = userPrefs,
                    storageExplorer = explorer
                )
                INSTANCE = repo
                repo
            }
        }
    }
}
