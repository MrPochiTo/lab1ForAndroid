package com.example.lab1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lab1.ui.MainViewModel
import com.example.lab1.ui.getCategoryIcon
import com.example.lab1.ui.main.BarcodeView

val availableColors = listOf(
    0xFF2E7D32, // Зеленый
    0xFFC62828, // Красный
    0xFF37474F, // Темный
    0xFFEF6C00, // Оранжевый
    0xFF00838F, // Бирюзовый
    0xFF6A1B9A, // Фиолетовый
    0xFF1565C0  // Синий
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Int,
    viewModel: MainViewModel,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val card = cards.find { it.id == cardId } ?: return
    val category = viewModel.getCategory(card.categoryId)

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить карту?") },
            text = { Text("Карта «" + card.shopName + "» будет удалена без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteCard(card)
                    onBackClick()
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.shopName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(card.colorHex),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Карточка с цветом
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(card.colorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.shopName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(24.dp))

            // Штрихкод и номер
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                BarcodeView(barcodeNumber = card.barcodeNumber)
            }

            Spacer(Modifier.height(16.dp))

            // Категория
            if (category != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIcon(category.icon),
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Выбор индивидуального цвета
            Text(
                text = "Выберите цвет карты:",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                availableColors.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .border(
                                width = if (card.colorHex == colorHex) 3.dp else 0.dp,
                                color = if (card.colorHex == colorHex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                viewModel.updateCardColor(card, colorHex)
                            }
                    )
                }
            }
        }
    }
}
