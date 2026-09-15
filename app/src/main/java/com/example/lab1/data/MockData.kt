package com.example.lab1.data

import androidx.compose.ui.graphics.Color


import com.example.lab1.model.Category
import com.example.lab1.model.DiscountCard



val mockCategories = listOf(
    Category(1, "Супермаркеты"),
    Category(2, "Одежда"),
    Category(3, "Кафе и рестораны"),
    Category(4, "Аптеки")
)

val mockCards = listOf(
    DiscountCard(1, "Пятёрочка",       "4600123456789", 1, Color(0xFF2E7D32)),
    DiscountCard(2, "Магнит",          "4600987654321", 1, Color(0xFFC62828)),
    DiscountCard(3, "Zara",            "2000000000015", 2, Color(0xFF37474F)),
    DiscountCard(4, "Вкусно и точка",  "1234567890123", 3, Color(0xFFEF6C00)),
    DiscountCard(5, "Ригла",           "9876543210987", 4, Color(0xFF00838F))
)