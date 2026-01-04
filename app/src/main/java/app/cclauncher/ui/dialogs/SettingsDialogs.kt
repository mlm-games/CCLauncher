package app.cclauncher.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cclauncher.ui.viewmodels.ImportExportState
import io.github.mlmgames.settings.core.backup.ValidationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog for slider-based settings
 */
@Composable
fun SliderSettingDialog(
    title: String,
    currentValue: Float,
    min: Float,
    max: Float,
    step: Float,
    onDismiss: () -> Unit,
    onValueSelected: (Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(String.format(Locale.getDefault(), "%.1f", sliderValue))
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        // Round to nearest step
                        val steps = ((it - min) / step).toInt()
                        sliderValue = min + (steps * step)
                    },
                    valueRange = min..max,
                    steps = ((max - min) / step).toInt() - 1
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueSelected(sliderValue)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for dropdown/selection settings
 */
@Composable
fun DropdownSettingDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = index }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { selected = index }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(options[index])
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOptionSelected(selected)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportExportResultDialog(
    state: ImportExportState,
    onDismiss: () -> Unit
) {
    when (state) {
        is ImportExportState.Loading -> {
            AlertDialog(
                onDismissRequest = { /* Don't dismiss while loading */ },
                title = { Text("Please wait...") },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = { }
            )
        }

        is ImportExportState.ExportSuccess -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Export Successful") },
                text = { Text("Your settings have been exported successfully.") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }

        is ImportExportState.ImportSuccess -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Import Successful") },
                text = {
                    Column {
                        Text("Settings imported successfully!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Applied: ${state.appliedCount} settings",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.skippedCount > 0) {
                            Text(
                                "Skipped: ${state.skippedCount} settings (unknown or incompatible)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (state.errors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Errors:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            state.errors.take(5).forEach { (key, error) ->
                                Text(
                                    "• $key: $error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (state.errors.size > 5) {
                                Text(
                                    "... and ${state.errors.size - 5} more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }

        is ImportExportState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Error") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }

        ImportExportState.Idle -> { /* No dialog */ }
    }
}

@Composable
fun ImportValidationDialog(
    validationResult: ValidationResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val exportDate = remember(validationResult.exportedAt) {
        if (validationResult.exportedAt > 0) {
            dateFormat.format(Date(validationResult.exportedAt))
        } else {
            "Unknown"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Settings") },
        text = {
            Column {
                Text(
                    "Backup Details:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "• Settings: ${validationResult.settingsCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Schema version: ${validationResult.schemaVersion}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Exported: $exportDate",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (validationResult.issues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Warnings:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    validationResult.issues.forEach { issue ->
                        Text(
                            "• $issue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Do you want to proceed? This will overwrite your current settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
