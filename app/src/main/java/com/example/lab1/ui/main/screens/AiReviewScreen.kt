package com.example.lab1.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lab1.ai.AnalysisResult
import com.example.lab1.ai.LlmState
import com.example.lab1.ui.ai.AiReviewUiState
import com.example.lab1.ui.ai.AiReviewViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReviewScreen(
    viewModel: AiReviewViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.selectModel(it.toString())
        }
    }

    val isLoading = state.engineState is LlmState.ModelLoading
    val buttonText = when {
        state.modelUri == null -> "Выбрать модель"
        isLoading -> "Загрузка..."
        !state.modelReady -> "Загрузить модель"
        state.isGenerating -> "Остановить"
        else -> "Проверить карты"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI-проверка") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Qwen 1.5B Q4_K_M · работает без сети",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                state.modelName ?: "Файл модели не выбран",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Button(
                onClick = {
                    when {
                        state.modelUri == null -> modelPicker.launch(arrayOf("*/*"))
                        isLoading -> Unit
                        !state.modelReady -> viewModel.loadModel()
                        state.isGenerating -> viewModel.stopGeneration()
                        else -> viewModel.analyze(forceRefresh = true)
                    }
                },
                enabled = state.compatibility.isCompatible && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }

            if (state.modelUri != null) {
                TextButton(
                    onClick = { modelPicker.launch(arrayOf("*/*")) },
                    enabled = !isLoading && !state.isGenerating
                ) {
                    Text("Сменить модель")
                }
            }

            if (!state.compatibility.isCompatible) {
                Text(
                    state.compatibility.reason ?: "Устройство несовместимо.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            StatusText(state)

            if (isLoading || state.isGenerating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.isGenerating) {
                val partial = (state.engineState as? LlmState.Generating)?.partialText.orEmpty()
                if (partial.isNotBlank()) {
                    Text(
                        partial,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            state.errorMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.result?.let { ResultBlock(it) }
        }
    }
}

@Composable
private fun StatusText(state: AiReviewUiState) {
    val text = when (state.engineState) {
        LlmState.Idle -> if (state.modelUri == null) "Выберите файл модели." else "Модель не загружена."
        LlmState.ModelLoading -> "Загрузка модели в память..."
        LlmState.ModelReady -> "Модель готова."
        is LlmState.Generating -> "Генерация ответа..."
        is LlmState.Success -> "Готово."
        is LlmState.Error -> null
    }

    if (text != null) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResultBlock(result: AnalysisResult) {
    Spacer(Modifier.height(4.dp))
    Text("Результат", style = MaterialTheme.typography.titleMedium)

    if (result.fromCache) {
        Text(
            "Из кэша · ${formatDate(result.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Text(result.analysis.summary, style = MaterialTheme.typography.bodyMedium)

    if (!result.analysis.structured) {
        Text(
            "Ответ модели не удалось разобрать как JSON.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    result.analysis.recommendations.forEach { recommendation ->
        Spacer(Modifier.height(6.dp))
        Text(
            "${recommendation.shopName}: ${recommendation.currentCategoryName} -> " +
                recommendation.suggestedCategoryName,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            recommendation.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (result.analysis.structured && result.analysis.recommendations.isEmpty()) {
        Text(
            "Все карты распределены верно.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
