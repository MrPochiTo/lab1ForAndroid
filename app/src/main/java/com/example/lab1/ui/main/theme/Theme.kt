package com.example.lab1.ui.main.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


@Composable
fun DiscountCardsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF4A5C92),
            background = Color(0xFFF6F4FB)
        ),
        content = content
    )
}