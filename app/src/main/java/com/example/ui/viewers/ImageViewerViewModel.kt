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

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _flipH = MutableStateFlow(false)
    val flipH: StateFlow<Boolean> = _flipH.asStateFlow()

    private val _flipV = MutableStateFlow(false)
    val flipV: StateFlow<Boolean> = _flipV.asStateFlow()

    private val _activeFilter = MutableStateFlow("NORMAL") // NORMAL, GRAYSCALE, SEPIA, INVERT, WARM
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var currentUriString: String = ""

    fun loadImage(uriString: String) {
        currentUriString = uriString
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

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        if (!_isEditMode.value) {
            resetEdits()
        }
    }

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun setBrightness(b: Float) {
        _brightness.value = b
    }

    fun toggleFlipH() {
        _flipH.value = !_flipH.value
    }

    fun toggleFlipV() {
        _flipV.value = !_flipV.value
    }

    fun resetEdits() {
        _flipH.value = false
        _flipV.value = false
        _activeFilter.value = "NORMAL"
        _brightness.value = 0f
    }

    fun saveEditedImage(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val uri = Uri.parse(currentUriString)
        _isSaving.value = true

        viewModelScope.launch {
            val result = imageEngine.saveEditedImage(
                uri = uri,
                rotationDegrees = _rotationDegrees.value,
                flipH = _flipH.value,
                flipV = _flipV.value,
                filter = _activeFilter.value,
                brightness = _brightness.value
            )

            if (result.isSuccess) {
                val savedFile = result.getOrThrow()
                val newEntity = repository.indexLocalFile(savedFile)
                _isSaving.value = false
                _isEditMode.value = false
                resetEdits()
                onSuccess(newEntity.uri)
            } else {
                _isSaving.value = false
                val msg = result.exceptionOrNull()?.localizedMessage ?: "Failed to save edited image"
                onError(msg)
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
