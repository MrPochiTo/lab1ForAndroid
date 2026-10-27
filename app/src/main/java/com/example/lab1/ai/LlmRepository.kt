package com.example.lab1.ai

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File
import java.util.concurrent.CancellationException

sealed interface LlmState {
    data object Idle : LlmState
    data object ModelLoading : LlmState
    data object ModelReady : LlmState
    data class Generating(val partialText: String) : LlmState
    data class Success(val rawResponse: String) : LlmState
    data class Error(val message: String) : LlmState
}

data class DeviceCompatibility(
    val isCompatible: Boolean,
    val reason: String? = null
)

data class ModelValidation(
    val isValid: Boolean,
    val message: String? = null
)

data class AiRecommendation(
    val cardId: Int,
    val shopName: String,
    val currentCategoryId: Int,
    val currentCategoryName: String,
    val suggestedCategoryId: Int,
    val suggestedCategoryName: String,
    val reason: String
)

data class CollectionAnalysis(
    val summary: String,
    val recommendations: List<AiRecommendation> = emptyList(),
    val structured: Boolean = true
)

data class AnalysisResult(
    val analysis: CollectionAnalysis,
    val fromCache: Boolean,
    val createdAt: Long
)

interface LlmRepository {
    val state: StateFlow<LlmState>

    fun checkCompatibility(): DeviceCompatibility
    fun availableModelUri(): String?
    fun modelName(uri: String?): String?
    fun selectModel(uri: String): ModelValidation
    fun loadModel(uri: String? = null)
    fun isModelLoaded(): Boolean
    suspend fun generate(prompt: String): String
    fun stop()
    fun release()
}

class LlmRepositoryImpl(context: Context) : LlmRepository {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = MutableSharedFlow<LlamaHelper.LLMEvent>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val helper = LlamaHelper(appContext.contentResolver, scope, events)
    private val generationMutex = Mutex()

    private val _state = MutableStateFlow<LlmState>(LlmState.Idle)
    override val state: StateFlow<LlmState> = _state.asStateFlow()

    @Volatile
    private var modelLoaded = false

    @Volatile
    private var currentModelUri: String? = null

    private var pendingResponse: CompletableDeferred<String>? = null
    private val partialResponse = StringBuilder()

    init {
        scope.launch { events.collect { handleEvent(it) } }
    }

    override fun checkCompatibility(): DeviceCompatibility {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return DeviceCompatibility(false, "Нужен Android 7.0 или новее.")
        }

        val has64BitAbi = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" || it == "x86_64" }
        if (!has64BitAbi) {
            return DeviceCompatibility(false, "Нужна 64-битная архитектура arm64-v8a или x86_64.")
        }

        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return DeviceCompatibility(false, "Не удалось определить объём оперативной памяти.")
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        val totalGb = info.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
        if (totalGb < MIN_RAM_GB) {
            return DeviceCompatibility(false, "Нужно не менее 3 ГБ ОЗУ, доступно %.1f ГБ.".format(totalGb))
        }

        return DeviceCompatibility(true)
    }

    override fun availableModelUri(): String? {
        val saved = preferences.getString(KEY_MODEL_URI, null)
        if (saved != null && canRead(saved)) return saved

        val directories = listOfNotNull(
            File(appContext.filesDir, MODELS_DIRECTORY),
            appContext.getExternalFilesDir(null)?.let { File(it, MODELS_DIRECTORY) }
        )
        val files = directories
            .filter { it.isDirectory }
            .flatMap { it.listFiles()?.toList().orEmpty() }
        return files
            .filter { it.isFile && it.extension.equals("gguf", ignoreCase = true) }
            .maxByOrNull { it.length() }
            ?.let { Uri.fromFile(it).toString() }
    }

    override fun modelName(uri: String?): String? {
        if (uri == null) return null
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "content") {
            runCatching {
                appContext.contentResolver
                    .query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (index >= 0) return cursor.getString(index)
                        }
                    }
            }
        }
        return parsed.lastPathSegment?.substringAfterLast('/') ?: File(uri).name
    }

    override fun selectModel(uri: String): ModelValidation {
        val validation = validateModel(uri)
        if (validation.isValid) {
            preferences.edit().putString(KEY_MODEL_URI, uri).apply()
        }
        return validation
    }

    override fun loadModel(uri: String?) {
        val compatibility = checkCompatibility()
        if (!compatibility.isCompatible) {
            _state.value = LlmState.Error(compatibility.reason ?: "Устройство несовместимо.")
            return
        }

        val modelUri = uri ?: availableModelUri()
        if (modelUri == null) {
            _state.value = LlmState.Error("Модель не найдена. Выберите GGUF-файл.")
            return
        }

        val validation = validateModel(modelUri)
        if (!validation.isValid) {
            _state.value = LlmState.Error(validation.message ?: "Некорректный файл модели.")
            return
        }

        if (modelLoaded && currentModelUri == modelUri) {
            _state.value = LlmState.ModelReady
            return
        }

        stop()
        modelLoaded = false
        currentModelUri = modelUri
        _state.value = LlmState.ModelLoading

        try {
            helper.load(path = modelUri, contextLength = CONTEXT_LENGTH) {
                modelLoaded = true
                _state.value = LlmState.ModelReady
            }
        } catch (exception: Exception) {
            _state.value = LlmState.Error(
                "Не удалось загрузить модель: ${exception.message ?: "неизвестная ошибка"}"
            )
        }
    }

    override fun isModelLoaded(): Boolean = modelLoaded

    override suspend fun generate(prompt: String): String = generationMutex.withLock {
        if (!modelLoaded) error("Модель не загружена.")

        val deferred = CompletableDeferred<String>()
        pendingResponse = deferred
        partialResponse.clear()
        _state.value = LlmState.Generating("")

        try {
            helper.predict(prompt)
            withTimeout(GENERATION_TIMEOUT_MS) { deferred.await() }
        } catch (exception: TimeoutCancellationException) {
            helper.stopPrediction()
            _state.value = LlmState.Error("Генерация превысила допустимое время.")
            throw IllegalStateException("Генерация превысила допустимое время.", exception)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            _state.value = LlmState.Error(exception.message ?: "Ошибка генерации.")
            throw exception
        } finally {
            if (pendingResponse === deferred) pendingResponse = null
        }
    }

    override fun stop() {
        pendingResponse?.cancel(CancellationException("Генерация остановлена."))
        pendingResponse = null
        helper.stopPrediction()
        if (modelLoaded) {
            _state.value = LlmState.ModelReady
        } else if (_state.value is LlmState.Generating) {
            _state.value = LlmState.Idle
        }
    }

    override fun release() {
        stop()
        helper.release()
        modelLoaded = false
        currentModelUri = null
        _state.value = LlmState.Idle
    }

    private fun validateModel(uri: String): ModelValidation {
        val parsed = Uri.parse(uri)
        val descriptor = runCatching {
            appContext.contentResolver.openFileDescriptor(parsed, "r")
        }.getOrNull() ?: return ModelValidation(false, "Не удалось открыть файл модели.")

        descriptor.use { file ->
            val size = file.statSize
            if (size > 0 && size < MIN_MODEL_SIZE_BYTES) {
                return ModelValidation(false, "Файл слишком мал. Выберите GGUF-модель целиком.")
            }

            val header = ByteArray(4)
            val read = runCatching {
                appContext.contentResolver.openInputStream(parsed)?.use { it.read(header) } ?: -1
            }.getOrDefault(-1)
            if (read < 4 || String(header, Charsets.US_ASCII) != "GGUF") {
                return ModelValidation(false, "Файл не является GGUF-моделью.")
            }
        }

        return ModelValidation(true)
    }

    private fun canRead(uri: String): Boolean = runCatching {
        appContext.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    }.getOrDefault(false)

    private fun handleEvent(event: LlamaHelper.LLMEvent) {
        when (event) {
            is LlamaHelper.LLMEvent.Loaded -> {
                modelLoaded = true
                _state.value = LlmState.ModelReady
            }

            is LlamaHelper.LLMEvent.Started -> {
                _state.value = LlmState.Generating("")
            }

            is LlamaHelper.LLMEvent.Ongoing -> {
                if (pendingResponse != null) {
                    partialResponse.append(event.word)
                    _state.value = LlmState.Generating(partialResponse.toString())
                }
            }

            is LlamaHelper.LLMEvent.Done -> {
                val text = event.fullText.ifBlank { partialResponse.toString() }
                pendingResponse?.complete(text)
                pendingResponse = null
                _state.value = LlmState.Success(text)
            }

            is LlamaHelper.LLMEvent.Error -> {
                val pending = pendingResponse
                pendingResponse = null
                if (pending != null) {
                    pending.completeExceptionally(IllegalStateException(event.message))
                } else {
                    _state.value = LlmState.Error(event.message)
                }
            }
        }
    }

    companion object {
        private const val PREFERENCES = "llm_model"
        private const val KEY_MODEL_URI = "model_uri"
        private const val MODELS_DIRECTORY = "models"
        private const val MIN_MODEL_SIZE_BYTES = 100L * 1024L * 1024L
        private const val MIN_RAM_GB = 3.0
        private const val CONTEXT_LENGTH = 2048
        private const val GENERATION_TIMEOUT_MS = 10 * 60 * 1000L
    }
}
