package com.example.lab1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lab1.model.DiscountCard
import com.example.lab1.ui.MainViewModel
import com.example.lab1.ui.getCategoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    viewModel: MainViewModel,
    onCardClick: (Int) -> Unit,
    onAddCardClick: () -> Unit,
    onAiClick: () -> Unit
) {
    val cards by viewModel.cards.collectAsState()

    val visibleCards = remember(cards, viewModel.searchQuery, viewModel.selectedCategoryId) {
        cards
            .filter { viewModel.selectedCategoryId == null || it.categoryId == viewModel.selectedCategoryId }
            .filter {
                viewModel.searchQuery.isBlank() ||
                        it.shopName.contains(viewModel.searchQuery, ignoreCase = true) ||
                        it.barcodeNumber.contains(viewModel.searchQuery)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои карты", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onAiClick) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI-проверка коллекции"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCardClick) {
                Icon(Icons.Default.Add, contentDescription = "Сканировать/Добавить карту")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск по магазину или номеру") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = viewModel.selectedCategoryId == null,
                        onClick = { viewModel.selectedCategoryId = null },
                        label = { Text("Все") }
                    )
                }
                items(viewModel.categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = viewModel.selectedCategoryId == cat.id,
                        onClick = { viewModel.selectedCategoryId = cat.id },
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

            if (visibleCards.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Карт не найдено")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleCards, key = { it.id }) { card ->
                        val category = viewModel.getCategory(card.categoryId)
                        val categoryName = if (category != null) category.name else ""
                        CardItem(
                            card = card,
                            categoryName = categoryName,
                            onClick = { onCardClick(card.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardItem(card: DiscountCard, categoryName: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(card.colorHex))
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
                Text(
                    categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
