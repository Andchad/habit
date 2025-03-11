package com.andchad.habit.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andchad.habit.data.model.Habit
import com.andchad.habit.ui.HabitViewModel
import com.andchad.habit.ui.screens.components.AlarmSettingsComponent
import com.andchad.habit.ui.screens.components.DaySelector
import com.andchad.habit.ui.screens.components.WheelTimePicker
import com.andchad.habit.ui.screens.components.formatTime
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    habit: Habit?,
    viewModel: HabitViewModel,
    onSave: (String, String, List<DayOfWeek>, Boolean, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = habit != null
    val originalName = habit?.name ?: ""
    var name by remember { mutableStateOf(originalName) }
    var showTimePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // For day selection
    var selectedDays by remember {
        mutableStateOf(habit?.scheduledDays ?: emptyList<DayOfWeek>())
    }

    // For alarm settings
    var vibrationEnabled by remember { mutableStateOf(habit?.vibrationEnabled ?: true) }
    var snoozeEnabled by remember { mutableStateOf(habit?.snoozeEnabled ?: true) }

    // Parse the existing reminder time or use current time
    val initialTime = if (habit != null) {
        val time = LocalTime.parse(habit.reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
        time.hour to time.minute
    } else {
        val now = LocalTime.now()
        now.hour to now.minute
    }

    var selectedTime by remember {
        mutableStateOf(formatTime(initialTime.first, initialTime.second))
    }

    if (showTimePicker) {
        WheelTimePicker(
            initialHour = initialTime.first,
            initialMinute = initialTime.second,
            onCancel = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedTime = formatTime(hour, minute)
                showTimePicker = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = if (isEditing) "Edit Habit" else "Create Habit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Habit Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null,
                supportingText = {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = selectedTime,
                onValueChange = { },
                label = { Text("Alarm Time") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Set Time"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Day selector
            DaySelector(
                selectedDays = selectedDays,
                onDaySelected = { day ->
                    selectedDays = if (selectedDays.contains(day)) {
                        selectedDays - day
                    } else {
                        selectedDays + day
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Alarm settings
            AlarmSettingsComponent(
                vibrationEnabled = vibrationEnabled,
                snoozeEnabled = snoozeEnabled,
                onVibrationChange = { vibrationEnabled = it },
                onSnoozeChange = { snoozeEnabled = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Habit name cannot be empty"
                        return@Button
                    }

                    if (selectedDays.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select at least one day for your habit")
                        }
                        return@Button
                    }

                    scope.launch {
                        // Skip duplicate check when editing and name hasn't changed
                        if (isEditing && name == originalName) {
                            onSave(name, selectedTime, selectedDays, vibrationEnabled, snoozeEnabled)
                            return@launch
                        }

                        val nameExists = viewModel.habitNameExists(name)
                        if (nameExists) {
                            errorMessage = "A habit with this name already exists"
                            snackbarHostState.showSnackbar(
                                "A habit with this name already exists. Please use a different name."
                            )
                        } else {
                            onSave(name, selectedTime, selectedDays, vibrationEnabled, snoozeEnabled)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text(text = if (isEditing) "Update Habit" else "Create Habit")
            }
        }
    }
}