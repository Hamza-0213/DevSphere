package com.example.ui.viewers

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.DocumentEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DocumentRepository
import com.example.document.text.TextDocument
import com.example.document.text.TextEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TextUiState {
    object Loading : TextUiState()
    data class Success(val document: TextDocument) : TextUiState()
    data class Error(val message: String) : TextUiState()
}

class TextViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)
    private val textEngine = TextEngine(application)
    private val preferences = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow<TextUiState>(TextUiState.Loading)
    val uiState: StateFlow<TextUiState> = _uiState.asStateFlow()

    private val _documentEntity = MutableStateFlow<DocumentEntity?>(null)
    val documentEntity: StateFlow<DocumentEntity?> = _documentEntity.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(16)
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private val _showLineNumbers = MutableStateFlow(true)
    val showLineNumbers: StateFlow<Boolean> = _showLineNumbers.asStateFlow()

    private val _readerTheme = MutableStateFlow("LIGHT") // LIGHT, DARK, SEPIA
    val readerTheme: StateFlow<String> = _readerTheme.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.userSettingsFlow.collect { settings ->
                _fontSizeSp.value = settings.textFontSize
                _readerTheme.value = settings.textReaderTheme
            }
        }
    }

    fun loadTextFile(uriString: String) {
        val uri = Uri.parse(uriString)
        _uiState.value = TextUiState.Loading

        viewModelScope.launch {
            repository.getDocument(uriString).collect {
                _documentEntity.value = it
            }
        }

        viewModelScope.launch {
            repository.recordDocumentOpened(uriString)
            val result = textEngine.parseText(uri)
            if (result.isSuccess) {
                _uiState.value = TextUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = TextUiState.Error(result.exceptionOrNull()?.message ?: "Unable to read text document")
            }
        }
    }

    fun toggleLineNumbers() {
        _showLineNumbers.value = !_showLineNumbers.value
    }

    fun setFontSize(size: Int) {
        val safeSize = size.coerceIn(12, 32)
        _fontSizeSp.value = safeSize
        viewModelScope.launch {
            preferences.setTextFontSize(safeSize)
        }
    }

    fun setReaderTheme(theme: String) {
        _readerTheme.value = theme
        viewModelScope.launch {
            preferences.setTextReaderTheme(theme)
        }
    }

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun toggleFavourite() {
        val doc = _documentEntity.value ?: return
        viewModelScope.launch {
            repository.toggleFavourite(doc.uri, doc.isFavourite)
        }
    }
}
