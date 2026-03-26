package com.alvaronolasco.creditcardtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.alvaronolasco.creditcardtracker.ui.dashboard.DashboardScreen
import com.alvaronolasco.creditcardtracker.ui.cards.AddEditCardScreen
import com.alvaronolasco.creditcardtracker.ui.expenses.AddExpenseScreen
import com.alvaronolasco.creditcardtracker.ui.expenses.ExpenseHistoryScreen
import com.alvaronolasco.creditcardtracker.ui.expenses.ExpenseSearchScreen
import com.alvaronolasco.creditcardtracker.ui.income.IncomeSetupScreen
import com.alvaronolasco.creditcardtracker.ui.income.AddEditIncomeScreen
import com.alvaronolasco.creditcardtracker.ui.components.CameraPreviewScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onAddCard = { navController.navigate("add_card") },
                onCardClick = { cardId -> navController.navigate("edit_card/$cardId") },
                onAddExpense = { cardId -> navController.navigate("add_expense/$cardId") },
                onHistoryClick = { cardId -> navController.navigate("expense_history/$cardId") },
                onSettingsClick = { /* Settings later */ },
                onIncomeClick = { navController.navigate("income_setup") },
                onAddIncome = { navController.navigate("add_income") },
                onCameraOpen = { navController.navigate("camera_preview") },
                onSearchClick = { navController.navigate("search_expenses") }
            )
        }

        composable(
            route = "expense_history/{cardId}",
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getInt("cardId") ?: 0
            ExpenseHistoryScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() },
                onExpenseClick = { expenseId -> navController.navigate("edit_expense/$expenseId") }
            )
        }

        composable("add_card") {
            AddEditCardScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_card/{cardId}",
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getInt("cardId")
            AddEditCardScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "add_expense/{cardId}",
            arguments = listOf(navArgument("cardId") { type = NavType.IntType })
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getInt("cardId") ?: 0
            AddExpenseScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate("camera_preview") },
                navController = navController
            )
        }

        composable("camera_preview") {
            CameraPreviewScreen(
                onImageCaptured = { uri ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("captured_image_uri", uri.toString())
                    navController.popBackStack()
                },
                onClose = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_expense/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.IntType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getInt("expenseId") ?: 0
            AddExpenseScreen(
                cardId = 0,
                expenseId = expenseId,
                onBack = { navController.popBackStack() },
                onOpenCamera = { navController.navigate("camera_preview") },
                navController = navController
            )
        }

        composable("search_expenses") {
            ExpenseSearchScreen(
                onBack = { navController.popBackStack() },
                onExpenseClick = { expenseId -> navController.navigate("edit_expense/$expenseId") }
            )
        }

        composable("income_setup") {
            IncomeSetupScreen(
                onBack = { navController.popBackStack() },
                onAddIncome = { navController.navigate("add_income") },
                onEditIncome = { incomeId -> navController.navigate("edit_income/$incomeId") }
            )
        }

        composable("add_income") {
            AddEditIncomeScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit_income/{incomeId}",
            arguments = listOf(navArgument("incomeId") { type = NavType.IntType })
        ) { backStackEntry ->
            val incomeId = backStackEntry.arguments?.getInt("incomeId")
            AddEditIncomeScreen(
                incomeId = incomeId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
