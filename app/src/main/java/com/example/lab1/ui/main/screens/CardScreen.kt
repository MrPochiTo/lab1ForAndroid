package com.example.lab1.ui.main.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lab1.data.mockCards
import com.example.lab1.data.mockCategories
import com.example.lab1.model.DiscountCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen() {
    // useState в React: строка поиска и выбранная категория
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Int?>(null) }

    // useMemo в React: пересчёт отфильтрованного списка при изменении query/category
    val visibleCards = remember(query, selectedCategoryId) {
        mockCards
            .filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
            .filter {
                query.isBlank() ||
                        it.shopName.contains(query, ignoreCase = true) ||
                        it.barcodeNumber.contains(query)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои карты", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Поле поиска
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск по магазину или номеру карты") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Чипы категорий (фильтр)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("Все") }
                    )
                }
                items(mockCategories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            // Список карт или пустое состояние
            if (visibleCards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Карт не найдено", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Попробуйте изменить запрос или категорию",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleCards, key = { it.id }) { card ->
                        CardItem(card)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardItem(card: DiscountCard) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Цветной квадрат вместо иконки
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(card.color)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    card.barcodeNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val catName = mockCategories.firstOrNull { it.id == card.categoryId }?.name ?: ""
            Text(
                catName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}