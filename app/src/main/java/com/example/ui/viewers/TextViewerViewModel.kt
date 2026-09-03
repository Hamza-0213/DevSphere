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

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _editedText = MutableStateFlow("")
    val editedText: StateFlow<String> = _editedText.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var originalContent: String = ""

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
                val doc = result.getOrThrow()
                originalContent = doc.content
                if (!_hasUnsavedChanges.value) {
                    _editedText.value = doc.content
                }
                _uiState.value = TextUiState.Success(doc)
            } else {
                _uiState.value = TextUiState.Error(result.exceptionOrNull()?.message ?: "Unable to read text document")
            }
        }
    }

    fun toggleEditMode() {
        if (!_isEditMode.value) {
            // Entering edit mode
            if (!_hasUnsavedChanges.value) {
                _editedText.value = originalContent
            }
            _isEditMode.value = true
        } else {
            // Exiting edit mode
            _isEditMode.value = false
        }
    }

    fun setEditMode(enabled: Boolean) {
        _isEditMode.value = enabled
    }

    fun onTextChanged(newText: String) {
        _editedText.value = newText
        _hasUnsavedChanges.value = (newText != originalContent)
    }

    fun revertChanges() {
        _editedText.value = originalContent
        _hasUnsavedChanges.value = false
    }

    fun insertSnippet(snippet: String) {
        val current = _editedText.value
        val updated = if (current.isEmpty()) snippet else "$current\n$snippet"
        onTextChanged(updated)
    }

    fun replaceAll(find: String, replaceWith: String) {
        if (find.isEmpty()) return
        val current = _editedText.value
        val updated = current.replace(find, replaceWith)
        onTextChanged(updated)
    }

    fun saveChanges(uriString: String, onResult: (Boolean, String) -> Unit) {
        val uri = Uri.parse(uriString)
        val textToSave = _editedText.value
        _isSaving.value = true

        viewModelScope.launch {
            val saveResult = textEngine.saveText(uri, textToSave)
            if (saveResult.isSuccess) {
                originalContent = textToSave
                _hasUnsavedChanges.value = false
                val bytes = textToSave.toByteArray(java.nio.charset.StandardCharsets.UTF_8).size.toLong()
                repository.updateDocumentStats(uriString, bytes)
                // Reload state
                val lines = textToSave.split("\n").map { it.trimEnd('\r') }
                val wordCount = textToSave.split("\\s+".toRegex()).count { it.isNotBlank() }
                val currentDoc = (_uiState.value as? TextUiState.Success)?.document
                if (currentDoc != null) {
                    _uiState.value = TextUiState.Success(
                        currentDoc.copy(
                            content = textToSave,
                            lines = lines,
                            lineCount = lines.size,
                            characterCount = textToSave.length,
                            wordCount = wordCount
                        )
                    )
                }
                _isSaving.value = false
                onResult(true, "Document saved successfully ($bytes bytes)")
            } else {
                _isSaving.value = false
                val errorMsg = saveResult.exceptionOrNull()?.localizedMessage ?: "Failed to save document"
                onResult(false, errorMsg)
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
