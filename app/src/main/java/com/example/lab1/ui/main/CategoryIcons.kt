package com.example.lab1.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Category
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(name: String): ImageVector {
    return when (name) {
        "shopping_cart" -> Icons.Default.ShoppingBag
        "checkroom"     -> Icons.Default.Store
        "restaurant"    -> Icons.Default.Restaurant
        "local_pharmacy"-> Icons.Default.LocalHospital
        else            -> Icons.Default.Category
    }
}