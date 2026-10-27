package com.example.lab1.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class DiscountCard(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val shopName: String,
    val barcodeNumber: String,
    val categoryId: Int,
    val colorHex: Long,
    // true  — карта добавлена пользователем вручную, не трогаем при синхронизации
    // false — карта пришла из API, обновляется при каждом старте
    val isUserAdded: Boolean = false
)