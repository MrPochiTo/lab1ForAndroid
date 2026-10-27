package com.example.lab1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lab1.ui.MainViewModel
import com.example.lab1.ui.getCategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardScreen(
    cardId: Int,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()
    val card = cards.find { it.id == cardId } ?: return

    var shopName by remember { mutableStateOf(card.shopName) }
    var categoryId by remember { mutableStateOf(card.categoryId) }
    var colorHex by remember { mutableStateOf(card.colorHex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = shopName,
                onValueChange = { shopName = it },
                label = { Text("Название магазина") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text(text = "Категория:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(cat.icon),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(text = "Цвет карты:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(availableColors) { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = if (colorHex == color) 3.dp else 0.dp,
                                color = if (colorHex == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { colorHex = color }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                enabled = shopName.isNotBlank(),
                onClick = {
                    val newCard = card.copy(
                        shopName = shopName.trim(),
                        categoryId = categoryId,
                        colorHex = colorHex
                    )
                    viewModel.updateCard(newCard)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить")
            }
        }
    }
}
