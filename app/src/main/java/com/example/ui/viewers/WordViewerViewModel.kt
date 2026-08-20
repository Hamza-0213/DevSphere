package com.example.ui.viewers

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.document.word.DocxDocument
import com.example.document.word.DocxEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WordUiState {
    object Loading : WordUiState()
    data class Success(val document: DocxDocument) : WordUiState()
    data class Error(val message: String) : WordUiState()
}

class WordViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)
    private val docxEngine = DocxEngine(application)

    private val _uiState = MutableStateFlow<WordUiState>(WordUiState.Loading)
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    private val _documentEntity = MutableStateFlow<DocumentEntity?>(null)
    val documentEntity: StateFlow<DocumentEntity?> = _documentEntity.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun loadWordDocument(uriString: String) {
        val uri = Uri.parse(uriString)
        _uiState.value = WordUiState.Loading

        viewModelScope.launch {
            repository.getDocument(uriString).collect {
                _documentEntity.value = it
            }
        }

        viewModelScope.launch {
            repository.recordDocumentOpened(uriString)
            val result = docxEngine.parseDocx(uri)
            if (result.isSuccess) {
                _uiState.value = WordUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = WordUiState.Error(result.exceptionOrNull()?.message ?: "Unable to read Word document")
            }
        }
    }

    fun increaseFont() {
        if (_fontScale.value < 2.0f) _fontScale.value += 0.15f
    }

    fun decreaseFont() {
        if (_fontScale.value > 0.7f) _fontScale.value -= 0.15f
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
