package app.cclauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cclauncher.data.HomeItem
import kotlin.math.roundToInt

@Composable
fun ResizeWidgetDialog(
    widgetItem: HomeItem.Widget?,
    currentRows: Int,      // Total rows on the grid
    currentColumns: Int,   // Total columns on the grid
    onDismiss: () -> Unit,
    onResize: (widget: HomeItem.Widget, newRowSpan: Int, newColSpan: Int) -> Unit
) {
    if (widgetItem == null) return

    // Remember the potential new spans
    var colSpan by remember { mutableIntStateOf(widgetItem.columnSpan) }
    var rowSpan by remember { mutableIntStateOf(widgetItem.rowSpan) }

    // Calculate max possible span based on current position and grid size
    val maxColSpan = currentColumns - widgetItem.column + 1
    val maxRowSpan = currentRows - widgetItem.row + 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resize Widget") }, // Consider adding widget label here
        text = {
            Column {
                // --- Column Span Selector ---
                Text("Width (columns): $colSpan")
                Slider(
                    value = colSpan.toFloat(),
                    onValueChange = { newValue ->
                        colSpan = newValue.roundToInt().coerceIn(1, maxColSpan) // Ensure within bounds
                    },
                    valueRange = 1f..maxColSpan.toFloat(), // Range from 1 to max possible
                    steps = (maxColSpan - 1 - 1).coerceAtLeast(0), // Steps between 1 and max
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                Spacer(Modifier.height(16.dp))

                // --- Row Span Selector ---
                Text("Height (rows): $rowSpan")
                Slider(
                    value = rowSpan.toFloat(),
                    onValueChange = { newValue ->
                        rowSpan = newValue.roundToInt().coerceIn(1, maxRowSpan) // Ensure within bounds
                    },
                    valueRange = 1f..maxRowSpan.toFloat(), // Range from 1 to max possible
                    steps = (maxRowSpan - 1 - 1).coerceAtLeast(0), // Steps between 1 and max
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Apply the resize if the value changed and is valid
                if (colSpan != widgetItem.columnSpan || rowSpan != widgetItem.rowSpan) {
                    onResize(widgetItem, rowSpan, colSpan)
                }
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}