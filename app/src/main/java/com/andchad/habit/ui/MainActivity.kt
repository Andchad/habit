package com.andchad.habit.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.andchad.habit.data.model.Habit
import com.andchad.habit.ui.screens.AddEditHabitScreen
import com.andchad.habit.ui.screens.HabitListScreen
import com.andchad.habit.ui.screens.HistoryScreen
import com.andchad.habit.ui.theme.HabitTheme
import com.andchad.habit.utils.AdManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import javax.inject.Inject
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

// Define navigation destinations
sealed class Screen(val route: String, val title: String, val selectedIcon: (@Composable () -> Unit), val unselectedIcon: (@Composable () -> Unit)) {
    object Habits : Screen(
        "habits",
        "Habits",
        { Icon(Icons.Filled.Home, contentDescription = "Habits") },
        { Icon(Icons.Outlined.Home, contentDescription = "Habits") }
    )
    object History : Screen(
        "history",
        "History",
        { Icon(Icons.Filled.History, contentDescription = "History") },
        { Icon(Icons.Outlined.History, contentDescription = "History") }
    )
    object AddHabit : Screen("add_habit", "Add Habit", { }, { })
    object EditHabit : Screen("edit_habit/{habitId}", "Edit Habit", { }, { })
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels()

    @Inject
    lateinit var adManager: AdManager

    companion object {
        private var hasAppLaunched = false
    }

    // Permission launcher for alarm permission
    private val requestAlarmPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Just check the result, no specific handling needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Only show splash screen on first launch
        if (!hasAppLaunched) {
            installSplashScreen()
            hasAppLaunched = true
        }

        super.onCreate(savedInstanceState)

        // Check and request alarm permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestAlarmPermission()
        }

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            HabitTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create the app with injected dependencies
                    HabitApp(
                        viewModel = viewModel,
                        onShowAd = { adManager.showInterstitialAd(this) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Ensure an ad is loaded and ready to show
        adManager.ensureAdLoaded()
    }

    private fun requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = android.content.Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                requestAlarmPermissionLauncher.launch(intent)
            }
        }
    }
}

@Composable
fun HabitApp(
    viewModel: HabitViewModel,
    onShowAd: () -> Unit
) {
    val navController = rememberNavController()
    val habits by viewModel.habits.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showTodayHabitsOnly by viewModel.showTodayHabitsOnly.collectAsState()
    val habitHistory by viewModel.habitHistory.collectAsState()
    val habitNameMap by viewModel.habitNameMap.collectAsState()

    // Counter to limit ad frequency
    var adCounter by remember { mutableStateOf(0) }

    // Bottom nav items
    val items = listOf(
        Screen.Habits,
        Screen.History
    )

    // Get current route for bottom nav selection
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Separate habits into upcoming and past due
    val upcomingHabits = habits.filter { !it.isCompleted && viewModel.isHabitUpcoming(it) }
    val pastDueHabits = habits.filter { !it.isCompleted && !viewModel.isHabitUpcoming(it) }

    Scaffold(
        bottomBar = {
            // Only show bottom nav on main screens
            val currentRoute = currentDestination?.route
            val showBottomNav = currentRoute == Screen.Habits.route || currentRoute == Screen.History.route

            if (showBottomNav) {
                NavigationBar {
                    items.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

                        NavigationBarItem(
                            icon = {
                                if (selected) screen.selectedIcon() else screen.unselectedIcon()
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Habits.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Habits.route) {
                HabitListScreen(
                    upcomingHabits = upcomingHabits,
                    pastDueHabits = pastDueHabits,
                    isDarkMode = isDarkMode,
                    showTodayHabitsOnly = showTodayHabitsOnly,
                    adCounter = adCounter,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onToggleHabitsFilter = { viewModel.toggleHabitsFilter() },
                    onAddHabit = {
                        // Show ad occasionally when navigating to create a habit
                        adCounter++

                        if (adCounter >= 3) { // Show ad every 3 actions

                            onShowAd()
                            adCounter = 0
                        }

                        navController.navigate(Screen.AddHabit.route)
                    },
                    onEditHabit = { habit ->
                        navController.navigate("edit_habit/${habit.id}")
                    },
                    onDeleteHabit = { habit ->
                        viewModel.deleteHabit(habit)
                    },
                    onToggleCompleteHabit = { id, isCompleted ->
                        viewModel.completeHabit(id, isCompleted)
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    habitHistory = habitHistory,
                    habitNameMap = habitNameMap,
                    viewModel = viewModel
                )
            }

            composable(Screen.AddHabit.route) {
                AddEditHabitScreen(
                    habit = null,
                    viewModel = viewModel,
                    onSave = { name, reminderTime, scheduledDays, vibrationEnabled, snoozeEnabled ->
                        viewModel.createHabit(
                            name,
                            reminderTime,
                            scheduledDays,
                            vibrationEnabled,
                            snoozeEnabled
                        )
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.EditHabit.route,
                arguments = listOf(
                    navArgument("habitId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getString("habitId") ?: return@composable
                val habit = habits.find { it.id == habitId } ?: return@composable

                AddEditHabitScreen(
                    habit = habit,
                    viewModel = viewModel,
                    onSave = { name, reminderTime, scheduledDays, vibrationEnabled, snoozeEnabled ->
                        viewModel.updateHabit(
                            habit.id,
                            name,
                            reminderTime,
                            scheduledDays,
                            vibrationEnabled,
                            snoozeEnabled
                        )
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}