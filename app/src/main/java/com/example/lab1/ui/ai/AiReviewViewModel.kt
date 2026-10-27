package com.example.lab1.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab1.ai.AnalysisResult
import com.example.lab1.ai.AnalyzeCollectionUseCase
import com.example.lab1.ai.DeviceCompatibility
import com.example.lab1.ai.LlmRepository
import com.example.lab1.ai.LlmState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiReviewUiState(
    val compatibility: DeviceCompatibility,
    val engineState: LlmState = LlmState.Idle,
    val modelUri: String? = null,
    val modelName: String? = null,
    val result: AnalysisResult? = null,
    val isAnalyzing: Boolean = false,
    val modelReady: Boolean = false,
    val errorMessage: String? = null
) {
    val isGenerating: Boolean
        get() = isAnalyzing || engineState is LlmState.Generating
}

class AiReviewViewModel(
    private val analyzeCollection: AnalyzeCollectionUseCase,
    private val llmRepository: LlmRepository
) : ViewModel() {

    private val compatibility = llmRepository.checkCompatibility()
    private val initialModelUri = llmRepository.availableModelUri()

    private val _uiState = MutableStateFlow(
        AiReviewUiState(
            compatibility = compatibility,
            modelUri = initialModelUri,
            modelName = llmRepository.modelName(initialModelUri)
        )
    )
    val uiState: StateFlow<AiReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            llmRepository.state.collect { engineState ->
                _uiState.update { current ->
                    current.copy(
                        engineState = engineState,
                        modelReady = when (engineState) {
                            LlmState.ModelReady,
                            is LlmState.Generating,
                            is LlmState.Success -> true

                            LlmState.ModelLoading,
                            LlmState.Idle -> false

                            is LlmState.Error -> llmRepository.isModelLoaded()
                        },
                        errorMessage = when (engineState) {
                            is LlmState.Error -> engineState.message
                            LlmState.ModelLoading, LlmState.ModelReady -> null
                            else -> current.errorMessage
                        }
                    )
                }
            }
        }

        if (compatibility.isCompatible && initialModelUri != null) {
            viewModelScope.launch(Dispatchers.IO) { llmRepository.loadModel(initialModelUri) }
        }

        viewModelScope.launch {
            runCatching { analyzeCollection.getCached() }.getOrNull()?.let(::showResult)
        }
    }

    fun selectModel(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            val validation = withContext(Dispatchers.IO) { llmRepository.selectModel(uri) }
            if (!validation.isValid) {
                _uiState.update {
                    it.copy(errorMessage = validation.message ?: "Некорректный файл модели.")
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    modelUri = uri,
                    modelName = llmRepository.modelName(uri),
                    errorMessage = null
                )
            }
            withContext(Dispatchers.IO) { llmRepository.loadModel(uri) }
        }
    }

    fun loadModel() {
        val uri = _uiState.value.modelUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Сначала выберите GGUF-файл.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) { llmRepository.loadModel(uri) }
    }

    fun analyze(forceRefresh: Boolean = false) {
        if (_uiState.value.isAnalyzing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            try {
                showResult(analyzeCollection.execute(forceRefresh))
            } catch (_: CancellationException) {
                // Остановка генерации не является ошибкой.
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Не удалось выполнить AI-проверку.")
                }
            } finally {
                _uiState.update { it.copy(isAnalyzing = false) }
            }
        }
    }

    fun stopGeneration() {
        llmRepository.stop()
        _uiState.update { it.copy(isAnalyzing = false) }
    }

    private fun showResult(result: AnalysisResult) {
        _uiState.update { it.copy(result = result, errorMessage = null) }
    }
}
