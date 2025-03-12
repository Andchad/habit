package com.andchad.habit.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.andchad.habit.data.model.Habit
import com.andchad.habit.ui.screens.AddEditHabitScreen
import com.andchad.habit.ui.screens.HabitListScreen
import com.andchad.habit.ui.theme.HabitTheme
import com.andchad.habit.utils.AdManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import javax.inject.Inject
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

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

    // Counter to limit ad frequency
    var adCounter by remember { mutableStateOf(0) }

    NavHost(
        navController = navController,
        startDestination = "habits"
    ) {
        composable(route = "habits") {
            HabitListScreen(
                habits = habits,
                isDarkMode = isDarkMode,
                showTodayHabitsOnly = showTodayHabitsOnly,
                adCounter = adCounter,
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onToggleHabitsFilter = { viewModel.toggleHabitsFilter() },
                onAddHabit = {
                    // Show ad occasionally when navigating to create a habit
                    adCounter++
                    Log.d("AdTest", "Action performed. Ad counter: $adCounter/3")

                    if (adCounter >= 3) { // Show ad every 3 actions
                        Log.d("AdTest", "Triggering ad display")
                        onShowAd()
                        adCounter = 0
                    }

                    navController.navigate("add_habit") {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onEditHabit = { habit ->
                    navController.navigate("edit_habit/${habit.id}") {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onDeleteHabit = { habit ->
                    viewModel.deleteHabit(habit)
                },
                onDeleteCompletedHabits = { completedHabits ->
                    viewModel.deleteCompletedHabits(completedHabits)
                },
                onToggleCompleteHabit = { id, isCompleted ->
                    viewModel.completeHabit(id, isCompleted)
                }
            )
        }

        composable(route = "add_habit") {
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
            route = "edit_habit/{habitId}",
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