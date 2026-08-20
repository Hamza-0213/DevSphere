package com.example.data.local.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["documentUri"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentUri: String,
    val pageNumber: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
