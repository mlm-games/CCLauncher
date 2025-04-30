package app.cclauncher.ui.dialogs

import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.cclauncher.data.Constants

@Composable
fun BaseDialog(
    show: Boolean,
    title: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { content() },
            confirmButton = confirmButton,
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NumberPickerDialog(
    show: Boolean,
    currentValue: Int,
    range: IntRange = 0..16,
    onDismiss: () -> Unit,
    onValueSelected: (Int) -> Unit
) {
    if (show) {
        var selectedValue by remember { mutableIntStateOf(currentValue) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Number of Apps") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    items(range.last) { number ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = number == selectedValue,
                                    onClick = { selectedValue = number },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = number == selectedValue,
                                onClick = null
                            )
                            Text(
                                text = number.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueSelected(selectedValue)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ColumnsPickerDialog(
    show: Boolean,
    currentValue: Int,
    range: IntRange = 1..16,
    onDismiss: () -> Unit,
    onValueSelected: (Int) -> Unit
) {
    if (show) {
        var selectedValue by remember { mutableIntStateOf(currentValue) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Number of Columns") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    items(range.last) { number ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = number == selectedValue,
                                    onClick = { selectedValue = number },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = number == selectedValue,
                                onClick = null
                            )
                            Text(
                                text = number.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueSelected(selectedValue)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ThemePickerDialog(
    show: Boolean,
    currentTheme: Int,
    onDismiss: () -> Unit,
    onThemeSelected: (Int) -> Unit
) {
    if (show) {
        var selectedTheme by remember { mutableIntStateOf(currentTheme) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Theme") },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    // Light theme option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedTheme == AppCompatDelegate.MODE_NIGHT_NO,
                                onClick = { selectedTheme = AppCompatDelegate.MODE_NIGHT_NO },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == AppCompatDelegate.MODE_NIGHT_NO,
                            onClick = null
                        )
                        Text(
                            text = "Light",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Dark theme option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedTheme == AppCompatDelegate.MODE_NIGHT_YES,
                                onClick = { selectedTheme = AppCompatDelegate.MODE_NIGHT_YES },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == AppCompatDelegate.MODE_NIGHT_YES,
                            onClick = null
                        )
                        Text(
                            text = "Dark",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // System theme option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                                onClick = { selectedTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                            onClick = null
                        )
                        Text(
                            text = "System",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onThemeSelected(selectedTheme)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlignmentPickerDialog(
    show: Boolean,
    currentAlignment: Int,
    onDismiss: () -> Unit,
    onAlignmentSelected: (Int) -> Unit
) {
    if (show) {
        var selectedAlignment by remember { mutableIntStateOf(currentAlignment) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Alignment") },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    // Left alignment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAlignment == Gravity.START,
                                onClick = { selectedAlignment = Gravity.START },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAlignment == Gravity.START,
                            onClick = null
                        )
                        Text(
                            text = "Left",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Center alignment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAlignment == Gravity.CENTER,
                                onClick = { selectedAlignment = Gravity.CENTER },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAlignment == Gravity.CENTER,
                            onClick = null
                        )
                        Text(
                            text = "Center",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Right alignment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAlignment == Gravity.END,
                                onClick = { selectedAlignment = Gravity.END },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAlignment == Gravity.END,
                            onClick = null
                        )
                        Text(
                            text = "Right",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAlignmentSelected(selectedAlignment)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DateTimeVisibilityDialog(
    show: Boolean,
    currentVisibility: Int,
    onDismiss: () -> Unit,
    onVisibilitySelected: (Int) -> Unit
) {
    if (show) {
        var selectedVisibility by remember { mutableIntStateOf(currentVisibility) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Date & Time Display") },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    // Show both date and time
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedVisibility == Constants.DateTime.ON,
                                onClick = { selectedVisibility = Constants.DateTime.ON },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedVisibility == Constants.DateTime.ON,
                            onClick = null
                        )
                        Text(
                            text = "Show Date & Time",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Show date only
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedVisibility == Constants.DateTime.DATE_ONLY,
                                onClick = { selectedVisibility = Constants.DateTime.DATE_ONLY },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedVisibility == Constants.DateTime.DATE_ONLY,
                            onClick = null
                        )
                        Text(
                            text = "Show Date Only",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Hide both
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedVisibility == Constants.DateTime.OFF,
                                onClick = { selectedVisibility = Constants.DateTime.OFF },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedVisibility == Constants.DateTime.OFF,
                            onClick = null
                        )
                        Text(
                            text = "Hide Date & Time",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onVisibilitySelected(selectedVisibility)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SwipeDownActionDialog(
    show: Boolean,
    currentAction: Int,
    onDismiss: () -> Unit,
    onActionSelected: (Int) -> Unit
) {
    if (show) {
        var selectedAction by remember { mutableIntStateOf(currentAction) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Swipe Down Action") },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    // Notifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAction == Constants.SwipeDownAction.NOTIFICATIONS,
                                onClick = { selectedAction = Constants.SwipeDownAction.NOTIFICATIONS },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAction == Constants.SwipeDownAction.NOTIFICATIONS,
                            onClick = null
                        )
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Search
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAction == Constants.SwipeDownAction.SEARCH,
                                onClick = { selectedAction = Constants.SwipeDownAction.SEARCH },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAction == Constants.SwipeDownAction.SEARCH,
                            onClick = null
                        )
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onActionSelected(selectedAction)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TextSizeDialog(
    show: Boolean,
    currentSize: Float,
    onDismiss: () -> Unit,
    onSizeSelected: (Float) -> Unit
) {
    if (show) {
        var selectedSize by remember { mutableFloatStateOf(currentSize) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Text Size") },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(vertical = 8.dp)
                ) {
                    // Size options
                    listOf(
                        Pair(Constants.TextSize.ONE, "1 (Smallest)"),
                        Pair(Constants.TextSize.TWO, "2"),
                        Pair(Constants.TextSize.THREE, "3"),
                        Pair(Constants.TextSize.FOUR, "4 (Default)"),
                        Pair(Constants.TextSize.FIVE, "5"),
                        Pair(Constants.TextSize.SIX, "6"),
                        Pair(Constants.TextSize.SEVEN, "7 (Largest)")
                    ).forEach { (size, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedSize == size,
                                    onClick = { selectedSize = size },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSize == size,
                                onClick = null
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSizeSelected(selectedSize)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}