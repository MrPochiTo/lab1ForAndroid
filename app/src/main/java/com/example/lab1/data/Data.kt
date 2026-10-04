package com.example.lab1.data

import com.example.lab1.model.Category
import com.example.lab1.model.DiscountCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val mockCategories = listOf(
    Category(1, "Супермаркеты"),
    Category(2, "Одежда"),
    Category(3, "Кафе и рестораны"),
    Category(4, "Аптеки")
)

val initialCards = listOf(
    DiscountCard(1, "Пятёрочка",      "4600123456789", 1, 0xFF2E7D32),
    DiscountCard(2, "Магнит",         "4600987654321", 1, 0xFFC62828),
    DiscountCard(3, "Zara",           "2000000000015", 2, 0xFF37474F),
    DiscountCard(4, "Вкусно и точка", "1234567890123", 3, 0xFFEF6C00),
    DiscountCard(5, "Ригла",          "9876543210987", 4, 0xFF00838F)
)

class CardRepository {
    private val _cards = MutableStateFlow(initialCards)
    val cards: StateFlow<List<DiscountCard>> = _cards.asStateFlow()

    fun getCardById(id: Int): DiscountCard? {
        return _cards.value.find { it.id == id }
    }

    fun addCard(shopName: String, barcode: String, categoryId: Int, colorHex: Long) {
        val newId = (_cards.value.maxOfOrNull { it.id } ?: 0) + 1
        val newCard = DiscountCard(newId, shopName, barcode, categoryId, colorHex)
        _cards.value = _cards.value + newCard
    }

    fun updateCardColor(cardId: Int, newColorHex: Long) {
        _cards.value = _cards.value.map { card ->
            if (card.id == cardId) card.copy(colorHex = newColorHex) else card
        }
    }
}