package app.cclauncher.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cclauncher.helper.iconpack.IconPackManager
import app.cclauncher.ui.viewmodels.SettingsViewModel

/**
 * A settings section with a title and card container
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * A clickable settings item with optional subtitle and description
 */
@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    transparency: Float = 1.0f,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(if (enabled) transparency else 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * A settings item with a toggle switch
 */
@Composable
fun SettingsToggle(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isChecked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    var toggleState by remember { mutableStateOf(isChecked) }

    LaunchedEffect(isChecked) {
        toggleState = isChecked
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (enabled) {
                    toggleState = !toggleState
                    onCheckedChange(toggleState)
                }
            }
            .padding(16.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Switch(
            checked = toggleState,
            onCheckedChange = { if (enabled) {
                toggleState = it
                onCheckedChange(it)
            }},
            enabled = enabled
        )
    }
}

@Composable
fun SettingsAction(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    buttonText: String = "Set",
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
            )

            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.7f else 0.5f)
                )
            }
        }

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.padding(start = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(buttonText)
        }
    }
}

@Composable
fun GridSizeWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Grid Size Change",
        message = "Changing the grid size may move some apps and widgets to inaccessible positions. Do you want to continue?",
        confirmText = "Continue",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun PageReduceWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Remove Pages",
        message = "Some pages being removed contain apps or widgets. They will be moved to remaining pages if space is available, otherwise they will be removed. Do you want to continue?",
        confirmText = "Continue",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun IconPackSelectionDialog(
    iconPacks: List<IconPackManager.IconPackInfo>,
    selectedPack: String,
    onDismiss: () -> Unit,
    onPackSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf(selectedPack) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Icon Pack") },
        text = {
            LazyColumn {
                items(iconPacks.size) { index ->
                    val pack = iconPacks[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = pack.packageName }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == pack.packageName,
                            onClick = { selected = pack.packageName }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pack.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPackSelected(selected) }) {
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
fun FontPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelectClicked: () -> Unit,
    onResetClicked: () -> Unit,
    viewModel: SettingsViewModel
) {
    val fontInfo by viewModel.customFontInfo.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    "Current font: ${fontInfo?.first ?: "System default"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (fontInfo != null) {
                    Text(
                        "Size: ${Formatter.formatFileSize(LocalContext.current, fontInfo!!.second)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ABCDEFGabcdefg123",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = onSelectClicked) {
                        Text("Select Font")
                    }

                    if (fontInfo != null) {
                        Button(
                            onClick = onResetClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reset to Default")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ColorPickerDialog(
    title: String,
    currentColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    // Color opts.
    val colorOptions = remember {
        listOf(
            Color.White to "White",
            Color.Black to "Black",
            Color(0xFFF5F5F5) to "Light Gray",
            Color(0xFF9E9E9E) to "Gray",
            Color(0xFF424242) to "Dark Gray",
            Color(0xFFFF5252) to "Red",
            Color(0xFFE91E63) to "Pink",
            Color(0xFF9C27B0) to "Purple",
            Color(0xFF673AB7) to "Deep Purple",
            Color(0xFF3F51B5) to "Indigo",
            Color(0xFF2196F3) to "Blue",
            Color(0xFF03A9F4) to "Light Blue",
            Color(0xFF00BCD4) to "Cyan",
            Color(0xFF009688) to "Teal",
            Color(0xFF4CAF50) to "Green",
            Color(0xFF8BC34A) to "Light Green",
            Color(0xFFCDDC39) to "Lime",
            Color(0xFFFFEB3B) to "Yellow",
            Color(0xFFFFC107) to "Amber",
            Color(0xFFFF9800) to "Orange",
            Color(0xFFFF5722) to "Deep Orange",
            Color(0xFF795548) to "Brown",
        )
    }

    var selectedColor by remember {
        mutableIntStateOf(
            if (currentColor == 0) Color.White.toArgb()
            else currentColor
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Preview of selected color
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(selectedColor)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sample Text",
                            color = Color(selectedColor),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Color grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(colorOptions) { (color, name) ->
                        ColorOption(
                            color = color,
                            isSelected = selectedColor == color.toArgb(),
                            onClick = { selectedColor = color.toArgb() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedColor)
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
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                },
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Extension function to calculate luminance
fun Color.luminance(): Float {
    val red = red * 0.299f
    val green = green * 0.587f
    val blue = blue * 0.114f
    return red + green + blue
}