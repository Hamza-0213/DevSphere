package com.example.ui.viewers

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.document.image.ImageEngine
import com.example.document.image.ImageMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImageUiState {
    object Loading : ImageUiState()
    data class Success(val metadata: ImageMetadata) : ImageUiState()
    data class Error(val message: String) : ImageUiState()
}

class ImageViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)
    private val imageEngine = ImageEngine(application)

    private val _uiState = MutableStateFlow<ImageUiState>(ImageUiState.Loading)
    val uiState: StateFlow<ImageUiState> = _uiState.asStateFlow()

    private val _documentEntity = MutableStateFlow<DocumentEntity?>(null)
    val documentEntity: StateFlow<DocumentEntity?> = _documentEntity.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    fun loadImage(uriString: String) {
        val uri = Uri.parse(uriString)
        _uiState.value = ImageUiState.Loading

        viewModelScope.launch {
            repository.getDocument(uriString).collect {
                _documentEntity.value = it
            }
        }

        viewModelScope.launch {
            repository.recordDocumentOpened(uriString)
            val result = imageEngine.getImageMetadata(uri)
            if (result.isSuccess) {
                val meta = result.getOrThrow()
                _rotationDegrees.value = meta.orientationDegrees.toFloat()
                _uiState.value = ImageUiState.Success(meta)
            } else {
                _uiState.value = ImageUiState.Error(result.exceptionOrNull()?.message ?: "Unable to read image")
            }
        }
    }

    fun rotate90() {
        _rotationDegrees.value = (_rotationDegrees.value + 90f) % 360f
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
}
