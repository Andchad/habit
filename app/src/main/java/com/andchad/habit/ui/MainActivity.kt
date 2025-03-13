package com.andchad.habit.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.andchad.habit.ui.screens.ManageHabitsScreen
import com.andchad.habit.ui.theme.HabitTheme
import com.andchad.habit.utils.AdManager
import com.andchad.habit.utils.HabitBroadcastManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import javax.inject.Inject
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.launch
import android.provider.Settings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings

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
    object Manage : Screen(
        "manage_habits",
        "Manage",
        { Icon(Icons.Filled.Settings, contentDescription = "Manage Habits") },
        { Icon(Icons.Outlined.Settings, contentDescription = "Manage Habits") }
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

    // Broadcast receiver for habit updates
    private val habitUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                HabitBroadcastManager.ACTION_ALARM_DISMISSED -> {
                    val habitId = intent.getStringExtra(HabitBroadcastManager.EXTRA_HABIT_ID) ?: return
                    val habitName = intent.getStringExtra(HabitBroadcastManager.EXTRA_HABIT_NAME) ?: "Unknown"

                    // Mark the habit as missed (dismissed) in the database and update UI
                    viewModel.dismissHabit(habitId)

                    // Show user feedback
                    Toast.makeText(
                        this@MainActivity,
                        "Habit '$habitName' skipped for today",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                HabitBroadcastManager.ACTION_ALARM_COMPLETED -> {
                    val habitId = intent.getStringExtra(HabitBroadcastManager.EXTRA_HABIT_ID) ?: return
                    val habitName = intent.getStringExtra(HabitBroadcastManager.EXTRA_HABIT_NAME) ?: "Unknown"

                    // Mark the habit as completed in the database and update UI
                    viewModel.completeHabit(habitId, true)

                    // Show user feedback
                    Toast.makeText(
                        this@MainActivity,
                        "Habit '$habitName' completed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                HabitBroadcastManager.ACTION_HABIT_UPDATE -> {
                    // Handle general habit updates if needed
                    val habitId = intent.getStringExtra(HabitBroadcastManager.EXTRA_HABIT_ID) ?: return
                    val actionType = intent.getStringExtra(HabitBroadcastManager.EXTRA_ACTION_TYPE) ?: return

                    // Update UI based on action type if needed
                    Log.d("MainActivity", "Received habit update: $actionType for habit $habitId")
                }
            }
        }
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

    override fun onStart() {
        super.onStart()
        // Register the broadcast receiver
        HabitBroadcastManager.registerReceiver(this, habitUpdateReceiver)
    }

    override fun onStop() {
        super.onStop()
        // Unregister the broadcast receiver
        HabitBroadcastManager.unregisterReceiver(this, habitUpdateReceiver)
    }

    override fun onResume() {
        super.onResume()

        // Check if we need to refresh the UI after alarm activity
        if (AlarmActivity.needsRefresh) {
            Log.d("MainActivity", "Force refreshing UI after alarm activity")
            // Set flag back to false
            AlarmActivity.needsRefresh = false

            // Force refresh the ViewModel data
            viewModel.forceRefreshHabits()
        }

        // Ensure an ad is loaded and ready to show
        adManager.ensureAdLoaded()
    }

    private fun requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
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
    val allHabits by viewModel.allHabits.collectAsState() // Collect all habits for manage screen
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showTodayHabitsOnly by viewModel.showTodayHabitsOnly.collectAsState()
    val habitHistory by viewModel.habitHistory.collectAsState()
    val habitNameMap by viewModel.habitNameMap.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Counter to limit ad frequency
    var adCounter by remember { mutableStateOf(0) }

    // Bottom nav items - reordered with Manage in the middle
    val items = listOf(
        Screen.Habits,
        Screen.Manage,  // Now in the middle position
        Screen.History
    )

    // Get current route for bottom nav selection
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Separate habits into upcoming and past due
    val upcomingHabits = habits.filter { !it.isCompleted && viewModel.isHabitUpcoming(it) }
    val pastDueHabits = habits.filter { !it.isCompleted && !viewModel.isHabitUpcoming(it) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Only show bottom nav on main screens
            val currentRoute = currentDestination?.route
            val showBottomNav = currentRoute == Screen.Habits.route ||
                    currentRoute == Screen.History.route ||
                    currentRoute == Screen.Manage.route

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
                    },
                    onDismissHabit = { id ->
                        viewModel.dismissHabit(id)
                        scope.launch {
                            snackbarHostState.showSnackbar("Habit skipped for today")
                        }
                    },
                    onManageHabits = {
                        // Navigate to the manage habits screen
                        navController.navigate(Screen.Manage.route)
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

            // Manage Habits screen - accessible via middle tab in bottom nav
            composable(Screen.Manage.route) {
                ManageHabitsScreen(
                    habits = allHabits,
                    onBack = { navController.popBackStack() },
                    onEditHabit = { habit ->
                        navController.navigate("edit_habit/${habit.id}")
                    },
                    onDeleteHabit = { habit ->
                        viewModel.deleteHabit(habit)
                        scope.launch {
                            snackbarHostState.showSnackbar("Habit '${habit.name}' deleted")
                        }
                    },
                    onDeleteAllHabits = {
                        // Delete all habits
                        allHabits.forEach { habit ->
                            viewModel.deleteHabit(habit)
                        }
                        scope.launch {
                            snackbarHostState.showSnackbar("All habits deleted")
                        }
                        navController.popBackStack()
                    }
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
                val habit = allHabits.find { it.id == habitId } ?: return@composable

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