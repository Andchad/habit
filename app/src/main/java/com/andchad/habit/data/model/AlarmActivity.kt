package com.andchad.habit.ui

import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andchad.habit.data.model.HabitHistory
import com.andchad.habit.data.model.HabitStatus
import com.andchad.habit.ui.theme.HabitTheme
import com.andchad.habit.utils.AlarmUtils
import com.andchad.habit.utils.HabitBroadcastManager
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    @Inject
    lateinit var alarmUtils: AlarmUtils

    // Use the shared ViewModel to directly update habit states
    private val viewModel: HabitViewModel by viewModels()

    private var mediaPlayer: MediaPlayer? = null
    private val TAG = "AlarmActivity"

    companion object {
        // Static variable to track if this activity is in the foreground
        private var isActivityVisible = false

        // New flag to track if the UI needs to be refreshed
        var needsRefresh = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and show above lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Get data from intent
        val habitId = intent.getStringExtra(AlarmUtils.KEY_HABIT_ID) ?: ""
        val habitName = intent.getStringExtra(AlarmUtils.KEY_HABIT_NAME) ?: "Habit Reminder"
        val snoozeEnabled = intent.getBooleanExtra(AlarmUtils.KEY_SNOOZE_ENABLED, false)
        val vibrationEnabled = intent.getBooleanExtra(AlarmUtils.KEY_VIBRATION_ENABLED, false)

        // Start playing alarm sound
        playAlarmSound()

        setContent {
            HabitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmScreen(
                        habitName = habitName,
                        snoozeEnabled = snoozeEnabled,
                        onDismiss = {
                            // Set the needs refresh flag
                            needsRefresh = true

                            // Directly call viewModel to dismiss habit
                            Log.d(TAG, "Dismissing habit: $habitId ($habitName)")
                            viewModel.dismissHabit(habitId)

                            // Stop alarm and close activity
                            stopAlarmSound()
                            finish()
                        },
                        onComplete = {
                            // Set the needs refresh flag
                            needsRefresh = true

                            // Directly call viewModel to complete habit
                            Log.d(TAG, "Completing habit: $habitId ($habitName)")
                            viewModel.completeHabit(habitId, true)

                            // Stop alarm and close activity
                            stopAlarmSound()
                            finish()
                        },
                        onSnooze = {
                            stopAlarmSound()
                            alarmUtils.scheduleSnoozeAlarm(habitId, habitName, vibrationEnabled)
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
    }

    private fun playAlarmSound() {
        try {
            // Make sure we're not creating multiple media players
            if (mediaPlayer != null) {
                stopAlarmSound()
            }

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, alarmSound)
                setLooping(true)
                prepare()
                start()
            }
            Log.d(TAG, "Started playing alarm sound")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                    Log.d(TAG, "Stopped alarm sound")
                }
                reset()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm sound: ${e.message}")
        }
    }

    override fun onDestroy() {
        stopAlarmSound()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Stop sound and finish when back is pressed
        stopAlarmSound()
        super.onBackPressed()
    }
}

@Composable
fun AlarmScreen(
    habitName: String,
    snoozeEnabled: Boolean,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,  // Added new callback for completion
    onSnooze: () -> Unit
) {
    // Clean up resources when leaving the composition
    DisposableEffect(Unit) {
        onDispose {
            // Clean up any resources
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Alarm",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Time for your habit",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = habitName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Complete habit button
            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Mark as Completed",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Changed to OutlinedButton for visual distinction
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Skip",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Skip For Today",
                    fontSize = 18.sp
                )
            }

            if (snoozeEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = "Snooze for ${AlarmUtils.SNOOZE_TIME_MINUTES} minutes",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}