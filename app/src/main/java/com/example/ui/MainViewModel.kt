package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.DocumentEntity
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.SortOption
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.DocumentRepository
import com.example.domain.model.DocumentType
import com.example.filemanager.FileItem
import com.example.filemanager.StorageExplorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DocumentRepository.getInstance(application)
    val preferences = UserPreferencesRepository(application)
    val storageExplorer = StorageExplorer(application, com.example.data.local.database.DocSphereDatabase.getDatabase(application).documentDao())

    val userSettings: StateFlow<UserSettings> = preferences.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    // Raw streams
    private val _rawDocuments = repository.allDocuments
    private val _rawRecents = repository.recentDocuments
    private val _rawFavourites = repository.favouriteDocuments

    // Sorted and Filtered Streams
    val allDocuments: StateFlow<List<DocumentEntity>> = combine(_rawDocuments, userSettings) { docs, settings ->
        sortDocuments(docs, settings.sortBy, settings.sortAscending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<DocumentEntity>> = combine(_rawRecents, userSettings) { docs, _ ->
        // Recents always strictly sorted by last opened time descending
        docs.sortedByDescending { it.lastOpenedTimestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favouriteDocuments: StateFlow<List<DocumentEntity>> = combine(_rawFavourites, userSettings) { docs, settings ->
        sortDocuments(docs, settings.sortBy, settings.sortAscending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query & results
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val searchResults: StateFlow<List<DocumentEntity>> = _searchResults.asStateFlow()

    // File Browser Navigation State
    private val _currentBrowserPath = MutableStateFlow<File?>(null)
    val currentBrowserPath: StateFlow<File?> = _currentBrowserPath.asStateFlow()

    private val _browserItems = MutableStateFlow<List<FileItem>>(emptyList())
    val browserItems: StateFlow<List<FileItem>> = _browserItems.asStateFlow()

    private val _isBrowserLoading = MutableStateFlow(false)
    val isBrowserLoading: StateFlow<Boolean> = _isBrowserLoading.asStateFlow()

    // Selected Category State
    private val _selectedCategory = MutableStateFlow<DocumentType?>(null)
    val selectedCategory: StateFlow<DocumentType?> = _selectedCategory.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeSamplesIfNeeded()
            repository.cleanStaleEntries()
            loadRootBrowserLocations()
        }
    }

    fun sortDocuments(docs: List<DocumentEntity>, sortBy: SortOption, ascending: Boolean): List<DocumentEntity> {
        val sorted = when (sortBy) {
            SortOption.NAME -> docs.sortedBy { it.displayName.lowercase() }
            SortOption.DATE -> docs.sortedBy { it.lastModified }
            SortOption.SIZE -> docs.sortedBy { it.sizeBytes }
            SortOption.TYPE -> docs.sortedBy { it.fileType }
        }
        return if (ascending) sorted else sorted.reversed()
    }

    fun sortFileItems(items: List<FileItem>, sortBy: SortOption, ascending: Boolean): List<FileItem> {
        val directories = items.filter { it.isDirectory }.sortedBy { it.name.lowercase() }
        val files = items.filter { !it.isDirectory }
        val sortedFiles = when (sortBy) {
            SortOption.NAME -> files.sortedBy { it.name.lowercase() }
            SortOption.DATE -> files.sortedBy { it.lastModified }
            SortOption.SIZE -> files.sortedBy { it.sizeBytes }
            SortOption.TYPE -> files.sortedBy { it.documentType.displayName }
        }
        val finalFiles = if (ascending) sortedFiles else sortedFiles.reversed()
        return directories + finalFiles
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                repository.searchDocuments(query).collect { results ->
                    _searchResults.value = results
                }
            }
        }
    }

    fun toggleFavourite(document: DocumentEntity) {
        viewModelScope.launch {
            repository.toggleFavourite(document.uri, document.isFavourite)
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            repository.clearRecentHistory()
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document.uri)
            // If it's a physical file in cache or filesDir, attempt delete
            val uri = Uri.parse(document.uri)
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) f.delete()
            }
        }
    }

    fun deleteDocumentByUri(uriStr: String) {
        viewModelScope.launch {
            repository.deleteDocument(uriStr)
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) f.delete()
            }
        }
    }

    fun restoreSampleDocuments() {
        viewModelScope.launch {
            repository.reloadSamples()
        }
    }

    fun onDocumentPicked(uri: Uri, onIndexed: (DocumentEntity) -> Unit) {
        viewModelScope.launch {
            val doc = repository.indexPickedDocument(uri)
            onIndexed(doc)
        }
    }

    fun renameDocument(uriStr: String, newName: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.renameDocument(uriStr, newName)
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun createNewTextDocument(fileName: String, content: String = "", onCreated: (DocumentEntity) -> Unit) {
        viewModelScope.launch {
            val result = repository.createNewTextDocument(fileName, content)
            if (result.isSuccess) {
                onCreated(result.getOrThrow())
            }
        }
    }

    // Browser navigation
    fun loadRootBrowserLocations() {
        _currentBrowserPath.value = null
        _browserItems.value = storageExplorer.getRootLocations()
    }

    fun navigateToDirectory(directory: File) {
        _currentBrowserPath.value = directory
        viewModelScope.launch {
            _isBrowserLoading.value = true
            val items = storageExplorer.listDirectory(directory)
            _browserItems.value = items
            _isBrowserLoading.value = false
        }
    }

    fun navigateUp(): Boolean {
        val current = _currentBrowserPath.value ?: return false
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            navigateToDirectory(parent)
            return true
        } else {
            loadRootBrowserLocations()
            return true
        }
    }

    // Settings actions
    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setSortOption(option: SortOption, ascending: Boolean) {
        viewModelScope.launch { preferences.setSortOption(option, ascending) }
    }

    fun setEnableAnimations(enabled: Boolean) {
        viewModelScope.launch { preferences.setEnableAnimations(enabled) }
    }

    fun clearCache() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            preferences.resetAllSettings()
        }
    }

    fun setSelectedCategory(type: DocumentType?) {
        _selectedCategory.value = type
    }
}
