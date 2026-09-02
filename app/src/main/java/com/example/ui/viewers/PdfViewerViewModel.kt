package com.example.ui.viewers

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.BookmarkEntity
import com.example.data.local.database.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.document.pdf.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class PdfUiState {
    object Loading : PdfUiState()
    data class Success(val pageCount: Int) : PdfUiState()
    data class PasswordRequired(val message: String) : PdfUiState()
    data class Error(val message: String) : PdfUiState()
}

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)
    val pdfEngine = PdfEngine(application)

    private val _uiState = MutableStateFlow<PdfUiState>(PdfUiState.Loading)
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    private val _documentEntity = MutableStateFlow<DocumentEntity?>(null)
    val documentEntity: StateFlow<DocumentEntity?> = _documentEntity.asStateFlow()

    private val _currentPage = MutableStateFlow(0) // 0-indexed
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // High performance in-memory LRU cache for rendered bitmaps
    private val bitmapCache = object : LruCache<Int, Bitmap>(24) {}

    private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private var currentUri: Uri? = null

    fun loadPdf(uriString: String, password: String? = null) {
        val uri = Uri.parse(uriString)
        currentUri = uri
        _uiState.value = PdfUiState.Loading

        viewModelScope.launch {
            // Load DB metadata and bookmarks
            repository.getDocument(uriString).collect { doc ->
                _documentEntity.value = doc
                if (doc != null && _currentPage.value == 0 && doc.lastReadingPosition > 0) {
                    _currentPage.value = doc.lastReadingPosition.coerceAtLeast(0)
                }
            }
        }

        viewModelScope.launch {
            repository.getBookmarksForDocument(uriString).collect { bList ->
                _bookmarks.value = bList
            }
        }

        viewModelScope.launch {
            repository.recordDocumentOpened(uriString)
            val result = pdfEngine.openPdf(uri, password)
            if (result.isSuccess) {
                val pages = result.getOrNull() ?: 0
                _uiState.value = PdfUiState.Success(pages)
                // Render initial pages
                renderPageAndNeighbors(_currentPage.value)
            } else {
                val error = result.exceptionOrNull()
                if (pdfEngine.isPasswordProtected) {
                    _uiState.value = PdfUiState.PasswordRequired(error?.message ?: "Password required")
                } else {
                    _uiState.value = PdfUiState.Error(error?.message ?: "Failed to open PDF")
                }
            }
        }
    }

    fun renderPageAndNeighbors(targetPage: Int) {
        if (!pdfEngine.isLoaded) return
        val total = pdfEngine.pageCount
        val safePage = targetPage.coerceIn(0, (total - 1).coerceAtLeast(0))
        _currentPage.value = safePage

        // Check if page already in cache
        val cached = bitmapCache.get(safePage)
        if (cached != null && !_pageBitmaps.value.containsKey(safePage)) {
            val updated = _pageBitmaps.value.toMutableMap()
            updated[safePage] = cached
            _pageBitmaps.value = updated
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Update reading position in DB asynchronously
            currentUri?.let { uri ->
                val progress = if (total > 0) (safePage + 1).toFloat() / total.toFloat() else 0f
                repository.updateReadingPosition(uri.toString(), safePage, progress, total)
            }

            // Render current page if not cached
            var currentBmp = bitmapCache.get(safePage)
            if (currentBmp == null) {
                currentBmp = pdfEngine.renderPage(safePage)
                if (currentBmp != null) {
                    bitmapCache.put(safePage, currentBmp)
                }
            }

            if (currentBmp != null) {
                withContext(Dispatchers.Main) {
                    val updatedMap = _pageBitmaps.value.toMutableMap()
                    updatedMap[safePage] = currentBmp
                    _pageBitmaps.value = updatedMap
                }
            }

            // Pre-fetch next page in background for smooth, latency-free navigation
            if (safePage + 1 < total && bitmapCache.get(safePage + 1) == null) {
                val nextBmp = pdfEngine.renderPage(safePage + 1)
                if (nextBmp != null) {
                    bitmapCache.put(safePage + 1, nextBmp)
                    withContext(Dispatchers.Main) {
                        val map = _pageBitmaps.value.toMutableMap()
                        map[safePage + 1] = nextBmp
                        _pageBitmaps.value = map
                    }
                }
            }

            // Pre-fetch previous page
            if (safePage - 1 >= 0 && bitmapCache.get(safePage - 1) == null) {
                val prevBmp = pdfEngine.renderPage(safePage - 1)
                if (prevBmp != null) {
                    bitmapCache.put(safePage - 1, prevBmp)
                    withContext(Dispatchers.Main) {
                        val map = _pageBitmaps.value.toMutableMap()
                        map[safePage - 1] = prevBmp
                        _pageBitmaps.value = map
                    }
                }
            }
        }
    }

    fun jumpToPage(pageNumber: Int) { // 1-indexed from UI
        val pageIdx = (pageNumber - 1).coerceIn(0, (pdfEngine.pageCount - 1).coerceAtLeast(0))
        renderPageAndNeighbors(pageIdx)
    }

    fun toggleBookmarkCurrentPage(title: String? = null) {
        val uri = currentUri?.toString() ?: return
        val pageNum = _currentPage.value + 1
        val existing = _bookmarks.value.firstOrNull { it.pageNumber == pageNum }

        viewModelScope.launch {
            if (existing != null) {
                repository.removeBookmark(existing.id)
            } else {
                repository.addBookmark(uri, pageNum, title ?: "Bookmark - Page $pageNum")
            }
        }
    }

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
    }

    fun toggleFavourite() {
        val doc = _documentEntity.value ?: return
        viewModelScope.launch {
            repository.toggleFavourite(doc.uri, doc.isFavourite)
        }
    }

    override fun onCleared() {
        super.onCleared()
        bitmapCache.evictAll()
        pdfEngine.close()
    }
}
