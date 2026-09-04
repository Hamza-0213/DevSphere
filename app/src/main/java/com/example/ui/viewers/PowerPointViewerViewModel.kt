package com.example.ui.viewers

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.document.powerpoint.PptxEngine
import com.example.document.powerpoint.PptxPresentation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PptViewMode {
    SLIDE_CANVAS,
    OUTLINE
}

sealed class PptUiState {
    object Loading : PptUiState()
    data class Success(val presentation: PptxPresentation) : PptUiState()
    data class Error(val message: String) : PptUiState()
}

class PowerPointViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository.getInstance(application)
    private val pptxEngine = PptxEngine(application)

    private val _uiState = MutableStateFlow<PptUiState>(PptUiState.Loading)
    val uiState: StateFlow<PptUiState> = _uiState.asStateFlow()

    private val _documentEntity = MutableStateFlow<DocumentEntity?>(null)
    val documentEntity: StateFlow<DocumentEntity?> = _documentEntity.asStateFlow()

    private val _currentSlideIndex = MutableStateFlow(0)
    val currentSlideIndex: StateFlow<Int> = _currentSlideIndex.asStateFlow()

    private val _viewMode = MutableStateFlow(PptViewMode.SLIDE_CANVAS)
    val viewMode: StateFlow<PptViewMode> = _viewMode.asStateFlow()

    private val _isFullscreenPresentation = MutableStateFlow(false)
    val isFullscreenPresentation: StateFlow<Boolean> = _isFullscreenPresentation.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(1.0f)
    val fontSizeScale: StateFlow<Float> = _fontSizeScale.asStateFlow()

    private val _showSpeakerNotes = MutableStateFlow(true)
    val showSpeakerNotes: StateFlow<Boolean> = _showSpeakerNotes.asStateFlow()

    // Slide Sorter / Grid view mode
    private val _isSlideSorterOpen = MutableStateFlow(false)
    val isSlideSorterOpen: StateFlow<Boolean> = _isSlideSorterOpen.asStateFlow()

    // Presenter Timer (in seconds) & running state
    private val _isPresenterTimerRunning = MutableStateFlow(false)
    val isPresenterTimerRunning: StateFlow<Boolean> = _isPresenterTimerRunning.asStateFlow()
    private val _presenterElapsedSeconds = MutableStateFlow(0)
    val presenterElapsedSeconds: StateFlow<Int> = _presenterElapsedSeconds.asStateFlow()

    // Auto-advance slideshow
    private val _isAutoPlayRunning = MutableStateFlow(false)
    val isAutoPlayRunning: StateFlow<Boolean> = _isAutoPlayRunning.asStateFlow()
    private val _autoPlayIntervalSeconds = MutableStateFlow(4)
    val autoPlayIntervalSeconds: StateFlow<Int> = _autoPlayIntervalSeconds.asStateFlow()

    // PowerPoint Drawing & Presenter Tools (Pen, Highlighter, Laser pointer)
    private val _activeToolMode = MutableStateFlow(PptToolMode.NONE)
    val activeToolMode: StateFlow<PptToolMode> = _activeToolMode.asStateFlow()

    private val _penColor = MutableStateFlow(Color(0xFFEF4444)) // Default red pen
    val penColor: StateFlow<Color> = _penColor.asStateFlow()

    // Slide Index -> List of DrawnStrokes
    private val _slideAnnotations = MutableStateFlow<Map<Int, List<DrawnStroke>>>(emptyMap())
    val slideAnnotations: StateFlow<Map<Int, List<DrawnStroke>>> = _slideAnnotations.asStateFlow()

    // Laser pointer position (ratio 0..1, null when not pressing/moving)
    private val _laserPosition = MutableStateFlow<Pair<Float, Float>?>(null)
    val laserPosition: StateFlow<Pair<Float, Float>?> = _laserPosition.asStateFlow()

    // Transition effect type: "FADE", "SLIDE", "ZOOM", "NONE"
    private val _transitionStyle = MutableStateFlow("SLIDE")
    val transitionStyle: StateFlow<String> = _transitionStyle.asStateFlow()

    init {
        // Coroutine for Presenter Timer and Auto-advance
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (_isPresenterTimerRunning.value) {
                    _presenterElapsedSeconds.value++
                }
                if (_isAutoPlayRunning.value) {
                    val currentSec = _presenterElapsedSeconds.value
                    if (currentSec > 0 && currentSec % _autoPlayIntervalSeconds.value == 0) {
                        val state = _uiState.value as? PptUiState.Success
                        if (state != null) {
                            if (_currentSlideIndex.value < state.presentation.slides.size - 1) {
                                nextSlide()
                            } else {
                                // Loop or stop
                                goToSlide(0)
                            }
                        }
                    }
                }
            }
        }
    }

    val matchingSlideIndices: StateFlow<List<Int>> = combine(_uiState, _searchQuery) { state, query ->
        if (state !is PptUiState.Success || query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            state.presentation.slides.mapIndexedNotNull { index, slide ->
                val matchesTitle = slide.title.lowercase().contains(q)
                val matchesText = slide.textBlocks.any { it.lowercase().contains(q) }
                val matchesBullets = slide.bulletPoints.any { it.lowercase().contains(q) }
                val matchesNotes = slide.notes?.lowercase()?.contains(q) == true
                if (matchesTitle || matchesText || matchesBullets || matchesNotes) index else null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadPresentation(uriString: String) {
        val uri = Uri.parse(uriString)
        _uiState.value = PptUiState.Loading

        viewModelScope.launch {
            repository.getDocument(uriString).collect { doc ->
                _documentEntity.value = doc
                if (doc != null && _currentSlideIndex.value == 0 && doc.lastReadingPosition > 0) {
                    _currentSlideIndex.value = doc.lastReadingPosition
                }
            }
        }

        viewModelScope.launch {
            repository.recordDocumentOpened(uriString)
            val result = pptxEngine.parsePptx(uri)
            if (result.isSuccess) {
                _uiState.value = PptUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = PptUiState.Error(
                    result.exceptionOrNull()?.message ?: "Unable to load PowerPoint presentation"
                )
            }
        }
    }

    fun nextSlide() {
        val state = _uiState.value as? PptUiState.Success ?: return
        if (_currentSlideIndex.value < state.presentation.slides.size - 1) {
            _currentSlideIndex.value++
            saveSlidePosition()
        }
    }

    fun previousSlide() {
        if (_currentSlideIndex.value > 0) {
            _currentSlideIndex.value--
            saveSlidePosition()
        }
    }

    fun goToSlide(index: Int) {
        val state = _uiState.value as? PptUiState.Success ?: return
        if (index in state.presentation.slides.indices) {
            _currentSlideIndex.value = index
            saveSlidePosition()
        }
    }

    private fun saveSlidePosition() {
        val doc = _documentEntity.value ?: return
        val state = _uiState.value as? PptUiState.Success ?: return
        val total = state.presentation.slides.size
        val progress = (currentSlideIndex.value + 1).toFloat() / total.toFloat()
        viewModelScope.launch {
            repository.updateReadingPosition(doc.uri, _currentSlideIndex.value, progress, total)
        }
    }

    fun toggleFullscreen() {
        _isFullscreenPresentation.value = !_isFullscreenPresentation.value
    }

    fun toggleFavourite() {
        val doc = _documentEntity.value ?: return
        viewModelScope.launch {
            repository.toggleFavourite(doc.uri, doc.isFavourite)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            _searchQuery.value = ""
        }
    }

    fun toggleSpeakerNotes() {
        _showSpeakerNotes.value = !_showSpeakerNotes.value
    }

    fun setViewMode(mode: PptViewMode) {
        _viewMode.value = mode
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == PptViewMode.SLIDE_CANVAS) PptViewMode.OUTLINE else PptViewMode.SLIDE_CANVAS
    }

    fun zoomIn() {
        if (_fontSizeScale.value < 1.6f) {
            _fontSizeScale.value = (_fontSizeScale.value + 0.15f).coerceAtMost(1.6f)
        }
    }

    fun zoomOut() {
        if (_fontSizeScale.value > 0.7f) {
            _fontSizeScale.value = (_fontSizeScale.value - 0.15f).coerceAtLeast(0.7f)
        }
    }

    fun resetZoom() {
        _fontSizeScale.value = 1.0f
    }

    // Slide Sorter / Grid view
    fun toggleSlideSorter() {
        _isSlideSorterOpen.value = !_isSlideSorterOpen.value
    }

    fun setSlideSorterOpen(open: Boolean) {
        _isSlideSorterOpen.value = open
    }

    // Presenter Timer
    fun togglePresenterTimer() {
        _isPresenterTimerRunning.value = !_isPresenterTimerRunning.value
    }

    fun resetPresenterTimer() {
        _presenterElapsedSeconds.value = 0
        _isPresenterTimerRunning.value = false
    }

    // Auto Play Slideshow
    fun toggleAutoPlay() {
        _isAutoPlayRunning.value = !_isAutoPlayRunning.value
        if (_isAutoPlayRunning.value) {
            _isPresenterTimerRunning.value = true
        }
    }

    fun setAutoPlayInterval(seconds: Int) {
        _autoPlayIntervalSeconds.value = seconds
    }

    // Transition style
    fun setTransitionStyle(style: String) {
        _transitionStyle.value = style
    }

    // Annotation and Drawing tools
    fun setToolMode(mode: PptToolMode) {
        _activeToolMode.value = mode
        if (mode != PptToolMode.LASER_POINTER) {
            _laserPosition.value = null
        }
    }

    fun setPenColor(color: Color) {
        _penColor.value = color
    }

    fun addStroke(slideIndex: Int, stroke: DrawnStroke) {
        val currentMap = _slideAnnotations.value.toMutableMap()
        val list = currentMap[slideIndex]?.toMutableList() ?: mutableListOf()
        list.add(stroke)
        currentMap[slideIndex] = list
        _slideAnnotations.value = currentMap
    }

    fun clearAnnotations(slideIndex: Int) {
        val currentMap = _slideAnnotations.value.toMutableMap()
        currentMap.remove(slideIndex)
        _slideAnnotations.value = currentMap
    }

    fun undoLastStroke(slideIndex: Int) {
        val currentMap = _slideAnnotations.value.toMutableMap()
        val list = currentMap[slideIndex]?.toMutableList() ?: return
        if (list.isNotEmpty()) {
            list.removeAt(list.size - 1)
            currentMap[slideIndex] = list
            _slideAnnotations.value = currentMap
        }
    }

    fun setLaserPosition(pos: Pair<Float, Float>?) {
        _laserPosition.value = pos
    }
}
