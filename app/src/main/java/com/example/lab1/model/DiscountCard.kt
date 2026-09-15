package com.example.lab1.model

import androidx.compose.ui.graphics.Color

data class DiscountCard(
    val id: Int,
    val shopName: String,
    val barcodeNumber: String,
    val categoryId: Int,
    val color: Color
)