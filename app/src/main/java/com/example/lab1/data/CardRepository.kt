package com.example.lab1.data

import android.util.Log
import com.example.lab1.model.DiscountCard
import com.example.lab1.network.CardApiService
import com.example.lab1.network.CardDto
import kotlinx.coroutines.flow.Flow

class CardRepository(
    private val cardDao: CardDao,
    private val apiService: CardApiService
) {

    val cards: Flow<List<DiscountCard>> = cardDao.getAll()
    suspend fun syncWithApi() {
        try {
            val dtos: List<CardDto> = apiService.getCards()
            Log.d("SYNC", "Получено карт: ${dtos.size}")
            cardDao.deleteApiCards()
            for (dto in dtos) {
                val card = DiscountCard(
                    id = dto.id,
                    shopName = dto.shopName,
                    barcodeNumber = dto.barcodeNumber,
                    categoryId = dto.categoryId,
                    colorHex = dto.colorHex,
                    isUserAdded = false
                )
                cardDao.insert(card)
            }
Log.d("SYNC", "Синхронизация завершена")
        } catch (e: Exception) {
            Log.e("SYNC", "Сервер недоступен (${e.message}). Использование локальных данных.")
            loadFallbackCardsIfEmpty()
        }


    }

    private suspend fun loadFallbackCardsIfEmpty() {
        if (cardDao.getCount() == 0) {
            Log.d("SYNC", "База данных пуста. Загрузка резервных карт по умолчанию...")
            val defaultCards = listOf(
                DiscountCard(id = 1, shopName = "Пятёрочка", barcodeNumber = "4600123456789", categoryId = 1, colorHex = 4280792370L, isUserAdded = false),
                DiscountCard(id = 2, shopName = "Магнит", barcodeNumber = "4600987654321", categoryId = 1, colorHex = 4291231784L, isUserAdded = false),
                DiscountCard(id = 3, shopName = "Zara", barcodeNumber = "2000000000015", categoryId = 2, colorHex = 4283716687L, isUserAdded = false),
                DiscountCard(id = 4, shopName = "Вкусно и точка", barcodeNumber = "1234567890123", categoryId = 3, colorHex = 4294923264L, isUserAdded = false),
                DiscountCard(id = 5, shopName = "Ригла", barcodeNumber = "9876543210987", categoryId = 4, colorHex = 4278243215L, isUserAdded = false)
            )
            for (card in defaultCards) {
                cardDao.insert(card)
            }
        }
    }

    suspend fun addCard(shopName: String, barcode: String, categoryId: Int, colorHex: Long) {
        val card = DiscountCard(
            shopName = shopName,
            barcodeNumber = barcode,
            categoryId = categoryId,
            colorHex = colorHex,
            isUserAdded = true
        )
        cardDao.insert(card)
    }

    suspend fun updateCard(card: DiscountCard) {
        cardDao.update(card)
    }

    suspend fun deleteCard(card: DiscountCard) {
        cardDao.delete(card)
    }
}