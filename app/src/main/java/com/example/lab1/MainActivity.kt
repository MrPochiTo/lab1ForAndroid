package com.example.lab1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lab1.ui.main.screens.CardsScreen
import com.example.lab1.ui.main.theme.DiscountCardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiscountCardsTheme {
                CardsScreen()
            }
        }
    }
}