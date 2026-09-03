package com.example.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY lastModified DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE lastOpenedTimestamp > 0 ORDER BY lastOpenedTimestamp DESC")
    fun getRecentDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavourite = 1 ORDER BY displayName COLLATE NOCASE ASC")
    fun getFavouriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE fileType = :fileType ORDER BY displayName COLLATE NOCASE ASC")
    fun getDocumentsByType(fileType: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE uri = :uri LIMIT 1")
    fun getDocumentByUri(uri: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE uri = :uri LIMIT 1")
    suspend fun getDocumentByUriDirect(uri: String): DocumentEntity?

    @Query("""
        SELECT * FROM documents 
        WHERE displayName LIKE '%' || :query || '%' 
           OR extension LIKE '%' || :query || '%'
        ORDER BY 
           CASE WHEN displayName LIKE :query || '%' THEN 0 ELSE 1 END,
           lastModified DESC
    """)
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("UPDATE documents SET isFavourite = :isFav WHERE uri = :uri")
    suspend fun setFavourite(uri: String, isFav: Boolean)

    @Query("""
        UPDATE documents 
        SET lastOpenedTimestamp = :timestamp, 
            lastReadingPosition = :position, 
            readingProgressPercent = :progress
        WHERE uri = :uri
    """)
    suspend fun updateLastOpened(uri: String, timestamp: Long, position: Int, progress: Float)

    @Query("UPDATE documents SET pageCount = :pageCount WHERE uri = :uri")
    suspend fun updatePageCount(uri: String, pageCount: Int)

    @Query("UPDATE documents SET sizeBytes = :sizeBytes, lastModified = :lastModified WHERE uri = :uri")
    suspend fun updateDocumentStats(uri: String, sizeBytes: Long, lastModified: Long)

    @Query("UPDATE documents SET displayName = :newDisplayName WHERE uri = :uri")
    suspend fun updateDisplayName(uri: String, newDisplayName: String)

    @Query("UPDATE documents SET uri = :newUri, displayName = :newDisplayName, lastModified = :lastModified WHERE uri = :oldUri")
    suspend fun updateDocumentUriAndName(oldUri: String, newUri: String, newDisplayName: String, lastModified: Long)

    @Query("DELETE FROM documents WHERE uri = :uri")
    suspend fun deleteDocumentByUri(uri: String)

    @Query("UPDATE documents SET lastOpenedTimestamp = 0, lastReadingPosition = 0, readingProgressPercent = 0 WHERE lastOpenedTimestamp > 0")
    suspend fun clearRecentHistory()

    @Query("SELECT COUNT(*) FROM documents")
    fun getDocumentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE isFavourite = 1")
    fun getFavouriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE lastOpenedTimestamp > 0")
    fun getRecentCount(): Flow<Int>

    @Query("SELECT SUM(sizeBytes) FROM documents")
    fun getTotalSizeBytes(): Flow<Long?>

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsDirect(): List<DocumentEntity>
}
