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

@Composable
fun TextSizeSliderDialog(
    show: Boolean,
    currentSize: Float,
    onDismiss: () -> Unit,
    onSizeSelected: (Float) -> Unit
) {
    if (!show) return

    var sliderPosition by remember { mutableStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Text Size") },
        text = {
            Column {
                Text(
                    "Preview Text",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * sliderPosition
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(String.format("%.1f", sliderPosition))

                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSizeSelected(sliderPosition)
                    onDismiss()
                }
            ) {
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
fun IconCornerRadiusDialog(
    show: Boolean,
    currentRadius: Int,
    onDismiss: () -> Unit,
    onRadiusSelected: (Int) -> Unit
) {
    if (!show) return

    var sliderPosition by remember { mutableStateOf(currentRadius.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Icon Corner Radius") },
        text = {
            Column {
                // Preview of icon with corner radius
                // This is a placeholder - in a real implementation you'd show an actual app icon
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(sliderPosition.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box {}
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("${sliderPosition.toInt()} dp")

                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 0f..50f,
                    steps = 50,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onRadiusSelected(sliderPosition.toInt())
                    onDismiss()
                }
            ) {
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
fun ItemSpacingDialog(
    show: Boolean,
    currentSpacing: Int,
    onDismiss: () -> Unit,
    onSpacingSelected: (Int) -> Unit
) {
    if (!show) return

    val options = listOf("None", "Small", "Medium", "Large")
    var selectedOption by remember { mutableStateOf(currentSpacing) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Item Spacing") },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedOption == index,
                            onClick = { selectedOption = index }
                        )
                        Text(
                            text = option,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Show preview spacing between items
                    if (selectedOption == index) {
                        Column {
                            repeat(3) { itemIndex ->
                                Text(
                                    "Item ${itemIndex + 1}",
                                    modifier = Modifier.padding(
                                        vertical = when(index) {
                                            0 -> 0.dp
                                            1 -> 4.dp
                                            2 -> 8.dp
                                            3 -> 16.dp
                                            else -> 0.dp
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSpacingSelected(selectedOption)
                    onDismiss()
                }
            ) {
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
fun OrientationIconsDialog(
    show: Boolean,
    showInLandscape: Boolean,
    showInPortrait: Boolean,
    onDismiss: () -> Unit,
    onSettingsChanged: (landscape: Boolean, portrait: Boolean) -> Unit
) {
    if (!show) return

    var landscapeEnabled by remember { mutableStateOf(showInLandscape) }
    var portraitEnabled by remember { mutableStateOf(showInPortrait) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Show App Icons") },
        text = {
            Column {
                Text(
                    "Configure when to show app icons based on device orientation:",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = landscapeEnabled,
                        onCheckedChange = { landscapeEnabled = it }
                    )
                    Text(
                        text = "Show icons in landscape mode",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = portraitEnabled,
                        onCheckedChange = { portraitEnabled = it }
                    )
                    Text(
                        text = "Show icons in portrait mode",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSettingsChanged(landscapeEnabled, portraitEnabled)
                    onDismiss()
                }
            ) {
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
fun SearchResultsAppearanceDialog(
    show: Boolean,
    useHomeFont: Boolean,
    fontSize: Float,
    onDismiss: () -> Unit,
    onSettingsChanged: (useHomeFont: Boolean, fontSize: Float) -> Unit
) {
    if (!show) return

    var useHomeFontSetting by remember { mutableStateOf(useHomeFont) }
    var fontSizeSetting by remember { mutableStateOf(fontSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Results Appearance") },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = useHomeFontSetting,
                        onCheckedChange = { useHomeFontSetting = it }
                    )
                    Text(
                        text = "Use home screen font size",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (!useHomeFontSetting) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        "Search results font size:",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        "Preview Text",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontSizeSetting
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(String.format("%.1f", fontSizeSetting))

                    Slider(
                        value = fontSizeSetting,
                        onValueChange = { fontSizeSetting = it },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = !useHomeFontSetting
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSettingsChanged(useHomeFontSetting, fontSizeSetting)
                    onDismiss()
                }
            ) {
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