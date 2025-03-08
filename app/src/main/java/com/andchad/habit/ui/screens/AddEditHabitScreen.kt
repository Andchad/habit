package com.andchad.habit.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import com.andchad.habit.ui.screens.components.TimePickerDialog
import com.andchad.habit.ui.screens.components.formatTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitScreen(
    habit: Habit?,
    viewModel: HabitViewModel,
    onSave: (String, String) -> Unit,
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

    // Parse the existing reminder time or use current time
    val initialTime = if (habit != null) {
        val time = LocalTime.parse(habit.reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
        time.hour to time.minute
    } else {
        val now = LocalTime.now()
        now.hour to now.minute
    }

    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.first,
        initialMinute = initialTime.second
    )

    var selectedTime by remember {
        mutableStateOf(formatTime(initialTime.first, initialTime.second))
    }

    if (showTimePicker) {
        TimePickerDialog(
            timePickerState = timePickerState,
            onCancel = { showTimePicker = false },
            onConfirm = {
                selectedTime = formatTime(
                    timePickerState.hour,
                    timePickerState.minute
                )
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
                .padding(16.dp),
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
                label = { Text("Reminder Time") },
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

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Habit name cannot be empty"
                        return@Button
                    }

                    scope.launch {
                        // Skip duplicate check when editing and name hasn't changed
                        if (isEditing && name == originalName) {
                            onSave(name, selectedTime)
                            return@launch
                        }

                        val nameExists = viewModel.habitNameExists(name)
                        if (nameExists) {
                            errorMessage = "A habit with this name already exists"
                            snackbarHostState.showSnackbar(
                                "A habit with this name already exists. Please use a different name."
                            )
                        } else {
                            onSave(name, selectedTime)
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