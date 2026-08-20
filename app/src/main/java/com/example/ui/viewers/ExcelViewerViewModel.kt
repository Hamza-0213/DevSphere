package com.example.ui.viewers

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.document.excel.XlsxEngine
import com.example.document.excel.XlsxWorkbook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExcelUiState {
    object Loading : ExcelUiState()
    data class Success(val workbook: XlsxWorkbook) : ExcelUiState()
    data class Error(val message: String) : ExcelUiState()
}

class ExcelViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)
    private val xlsxEngine = XlsxEngine(application)

    private val _uiState = MutableStateFlow<ExcelUiState>(ExcelUiState.Loading)
    val uiState: StateFlow<ExcelUiState> = _uiState.asStateFlow()

    private val _documentEntity = MutableStateFlow<DocumentEntity?>(null)
    val documentEntity: StateFlow<DocumentEntity?> = _documentEntity.asStateFlow()

    private val _activeSheetIndex = MutableStateFlow(0)
    val activeSheetIndex: StateFlow<Int> = _activeSheetIndex.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun loadSpreadsheet(uriString: String) {
        val uri = Uri.parse(uriString)
        _uiState.value = ExcelUiState.Loading

        viewModelScope.launch {
            repository.getDocument(uriString).collect {
                _documentEntity.value = it
            }
        }

        viewModelScope.launch {
            repository.recordDocumentOpened(uriString)
            val result = xlsxEngine.parseXlsx(uri)
            if (result.isSuccess) {
                _uiState.value = ExcelUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = ExcelUiState.Error(result.exceptionOrNull()?.message ?: "Unable to read spreadsheet")
            }
        }
    }

    fun selectSheet(index: Int) {
        _activeSheetIndex.value = index
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
