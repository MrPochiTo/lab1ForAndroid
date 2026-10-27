package com.example.lab1.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab1.data.CardRepository
import com.example.lab1.data.CategoryRepository
import com.example.lab1.model.Category
import com.example.lab1.model.DiscountCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val cardRepository: CardRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val cards: StateFlow<List<DiscountCard>> = cardRepository.cards.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val categories: List<Category> = categoryRepository.categories

    var searchQuery by mutableStateOf("")
    var selectedCategoryId by mutableStateOf<Int?>(null)

    init {
        // При создании ViewModel (то есть при старте приложения) синхронизируемся с API
        syncWithApi()
    }

    private fun syncWithApi() {
        viewModelScope.launch {
            cardRepository.syncWithApi()
        }
    }

    fun getCategory(id: Int): Category? {
        return categoryRepository.getById(id)
    }

    fun addCard(shopName: String, barcode: String, categoryId: Int, colorHex: Long) {
        viewModelScope.launch {
            cardRepository.addCard(shopName, barcode, categoryId, colorHex)
        }
    }

    fun updateCard(card: DiscountCard) {
        viewModelScope.launch {
            cardRepository.updateCard(card)
        }
    }

    fun updateCardColor(card: DiscountCard, newColorHex: Long) {
        updateCard(card.copy(colorHex = newColorHex))
    }

    fun deleteCard(card: DiscountCard) {
        viewModelScope.launch {
            cardRepository.deleteCard(card)
        }
    }
}