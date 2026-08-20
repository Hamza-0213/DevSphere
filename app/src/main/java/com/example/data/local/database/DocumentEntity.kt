package com.example.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DocumentType

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["lastOpenedTimestamp"]),
        Index(value = ["isFavourite"]),
        Index(value = ["fileType"])
    ]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val displayName: String,
    val extension: String,
    val fileType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val lastOpenedTimestamp: Long = 0,
    val isFavourite: Boolean = false,
    val lastReadingPosition: Int = 0, // Page index or line index
    val readingProgressPercent: Float = 0f,
    val pageCount: Int = 0,
    val isSample: Boolean = false
) {
    fun toDocumentType(): DocumentType {
        return try {
            DocumentType.valueOf(fileType)
        } catch (e: Exception) {
            DocumentType.fromExtension(extension)
        }
    }
}
