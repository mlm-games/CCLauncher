package app.cclauncher.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cclauncher.data.HomeItem
import app.cclauncher.ui.components.BaseDialog
import kotlin.math.roundToInt

/**
 * Unified resize dialog for both apps and widgets
 */
@Composable
fun ResizeDialog(
    item: HomeItem?,
    currentRows: Int,
    currentColumns: Int,
    onDismiss: () -> Unit,
    onResize: (HomeItem, Int, Int) -> Unit
) {
    if (item == null) return

    var colSpan by remember { mutableIntStateOf(item.columnSpan) }
    var rowSpan by remember { mutableIntStateOf(item.rowSpan) }

    LaunchedEffect(item) {
        colSpan = item.columnSpan
        rowSpan = item.rowSpan
    }

    // Calculate max possible span based on current position and grid size
    val maxColSpan = (currentColumns - item.column).coerceAtLeast(1)
    val maxRowSpan = (currentRows - item.row).coerceAtLeast(1)

    val title = when (item) {
        is HomeItem.App -> "Resize App"
        is HomeItem.Widget -> "Resize Widget"
    }

    BaseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmButton = {
            TextButton(
                onClick = {
                    if (colSpan != item.columnSpan || rowSpan != item.rowSpan) {
                        onResize(item, rowSpan, colSpan)
                    }
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
    ) {
        ResizeSliders(
            colSpan = colSpan,
            rowSpan = rowSpan,
            maxColSpan = maxColSpan,
            maxRowSpan = maxRowSpan,
            onColSpanChange = { colSpan = it },
            onRowSpanChange = { rowSpan = it }
        )
    }
}

@Composable
private fun ResizeSliders(
    colSpan: Int,
    rowSpan: Int,
    maxColSpan: Int,
    maxRowSpan: Int,
    onColSpanChange: (Int) -> Unit,
    onRowSpanChange: (Int) -> Unit
) {
    Column {
        Text("Width (columns): $colSpan")

        if (maxColSpan > 1) {
            Slider(
                value = colSpan.toFloat(),
                onValueChange = { newValue ->
                    onColSpanChange(newValue.roundToInt().coerceIn(1, maxColSpan))
                },
                valueRange = 1f..maxColSpan.toFloat(),
                steps = (maxColSpan - 2).coerceAtLeast(0),
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        } else {
            Text(
                text = "Cannot expand width further",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text("Height (rows): $rowSpan")

        if (maxRowSpan > 1) {
            Slider(
                value = rowSpan.toFloat(),
                onValueChange = { newValue ->
                    onRowSpanChange(newValue.roundToInt().coerceIn(1, maxRowSpan))
                },
                valueRange = 1f..maxRowSpan.toFloat(),
                steps = (maxRowSpan - 2).coerceAtLeast(0),
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                text = "Cannot expand height further",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}