package com.example.lab1.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.lab1.data.CardRepository
import com.example.lab1.model.DiscountCard
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(private val repository: CardRepository) : ViewModel() {

    val cards: StateFlow<List<DiscountCard>> = repository.cards

    var searchQuery by mutableStateOf("")
    var selectedCategoryId by mutableStateOf<Int?>(null)

    fun getCard(id: Int): DiscountCard? {
        return repository.getCardById(id)
    }

    fun updateCardColor(cardId: Int, newColorHex: Long) {
        repository.updateCardColor(cardId, newColorHex)
    }

    fun addCard(shopName: String, barcode: String, categoryId: Int, colorHex: Long) {
        repository.addCard(shopName, barcode, categoryId, colorHex)
    }
}