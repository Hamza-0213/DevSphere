package com.example.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri ORDER BY pageNumber ASC")
    fun getBookmarksForDocument(documentUri: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks WHERE documentUri = :documentUri AND pageNumber = :pageNumber")
    suspend fun deleteBookmarkByPage(documentUri: String, pageNumber: Int)

    @Query("UPDATE bookmarks SET title = :title WHERE id = :id")
    suspend fun updateBookmarkTitle(id: Long, title: String)

    @Query("SELECT * FROM bookmarks WHERE documentUri = :documentUri AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getBookmarkForPage(documentUri: String, pageNumber: Int): BookmarkEntity?
}
