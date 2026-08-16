package com.remink.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.remink.ui.reminders.AddReminderScreen
import com.remink.ui.reminders.ReminderDetailScreen
import com.remink.ui.reminders.ReminderListScreen

object Routes {
    const val LIST = "list"
    const val ADD = "add"
    const val DETAIL = "detail/{reminderId}"

    fun detail(reminderId: Long) = "detail/$reminderId"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIST,
    ) {
        composable(Routes.LIST) {
            ReminderListScreen(
                onAddClick = { navController.navigate(Routes.ADD) },
                onRowClick = { reminderId -> navController.navigate(Routes.detail(reminderId)) },
            )
        }

        composable(Routes.ADD) {
            AddReminderScreen(
                onNavigateUp = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("reminderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: return@composable
            ReminderDetailScreen(
                reminderId = reminderId,
                onNavigateUp = { navController.popBackStack() },
            )
        }
    }
}
