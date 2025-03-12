package com.andchad.habit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a navigation item in the bottom bar.
 *
 * @param route The route for navigation
 * @param title The title text to display
 * @param selectedIcon The icon to display when this item is selected
 * @param unselectedIcon The icon to display when this item is not selected
 */
data class NavigationItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * The main navigation items for the bottom bar.
 */
object NavigationItems {
    val items = listOf(
        NavigationItem(
            route = "habits",
            title = "Habits",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavigationItem(
            route = "history",
            title = "History",
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History
        )
    )
}