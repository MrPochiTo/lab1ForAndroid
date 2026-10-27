package com.example.lab1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lab1.ui.MainViewModel
import com.example.lab1.ui.ai.AiReviewViewModel
import com.example.lab1.ui.screens.AiReviewScreen
import com.example.lab1.ui.screens.CardDetailScreen
import com.example.lab1.ui.screens.CardsScreen
import com.example.lab1.ui.screens.EditCardScreen
import com.example.lab1.ui.screens.ScanCardScreen
import com.example.lab1.ui.main.theme.DiscountCardsTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiscountCardsTheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = koinViewModel()

                NavHost(navController = navController, startDestination = "cards") {
                    composable("cards") {
                        CardsScreen(
                            viewModel = mainViewModel,
                            onCardClick = { cardId -> navController.navigate("detail/$cardId") },
                            onAddCardClick = { navController.navigate("scan") },
                            onAiClick = { navController.navigate("ai") }
                        )
                    }
                    composable(
                        route = "detail/{cardId}",
                        arguments = listOf(navArgument("cardId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val cardId = backStackEntry.arguments?.getInt("cardId") ?: 0
                        CardDetailScreen(
                            cardId = cardId,
                            viewModel = mainViewModel,
                            onBackClick = { navController.popBackStack() },
                            onEditClick = { navController.navigate("edit/$cardId") }
                        )
                    }
                    composable(
                        route = "edit/{cardId}",
                        arguments = listOf(navArgument("cardId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val cardId = backStackEntry.arguments?.getInt("cardId") ?: 0
                        EditCardScreen(
                            cardId = cardId,
                            viewModel = mainViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable("scan") {
                        ScanCardScreen(
                            viewModel = mainViewModel,
                            onCardAdded = { navController.popBackStack() }
                        )
                    }
                    composable("ai") {
                        val aiViewModel: AiReviewViewModel = koinViewModel()
                        AiReviewScreen(
                            viewModel = aiViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}