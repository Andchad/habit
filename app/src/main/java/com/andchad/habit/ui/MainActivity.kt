package com.andchad.habit.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: HabitViewModel by viewModels()

    @Inject
    lateinit var adManager: AdManager

    // Request permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied - you might want to show an explanation
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Apply the splash screen theme
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Check and request notification permission for Android 13+
        requestNotificationPermission()

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            HabitTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HabitApp(
                        viewModel = viewModel,
                        onShowAd = { adManager.showInterstitialAd(this) }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show a rationale to the user before requesting the permission
                    // You could display a dialog here explaining why the permission is needed
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
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
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onAddHabit = {
                    // Show ad occasionally when navigating to create a habit
                    adCounter++
                    if (adCounter >= 3) { // Show ad every 3 actions
                        onShowAd()
                        adCounter = 0
                    }
                    navController.navigate("add_habit") {
                        // Navigation options for stability
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
                onToggleCompleteHabit = { id, isCompleted ->
                    viewModel.completeHabit(id, isCompleted)
                }
            )
        }

        composable(route = "add_habit") {
            AddEditHabitScreen(
                habit = null,
                viewModel = viewModel,
                onSave = { name, reminderTime ->
                    viewModel.createHabit(name, reminderTime)
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
                onSave = { name, reminderTime ->
                    viewModel.updateHabit(habit.id, name, reminderTime)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}