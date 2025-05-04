package app.cclauncher.ui.dialogs

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
    currentRows: Int,
    currentColumns: Int,
    onDismiss: () -> Unit,
    onResize: (widget: HomeItem.Widget, newRowSpan: Int, newColSpan: Int) -> Unit
) {
    if (widgetItem == null) return


    var colSpan by remember { mutableIntStateOf(widgetItem.columnSpan) }
    var rowSpan by remember { mutableIntStateOf(widgetItem.rowSpan) }

    // Calculate max possible span based on current position and grid size
    val maxColSpan = currentColumns - widgetItem.column + 1
    val maxRowSpan = currentRows - widgetItem.row + 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resize Widget") },
        text = {
            Column {
                Text("Width (columns): $colSpan")
                Slider(
                    value = colSpan.toFloat(),
                    onValueChange = { newValue ->
                        colSpan = newValue.roundToInt().coerceIn(1, maxColSpan)
                    },
                    valueRange = 1f..maxColSpan.toFloat(),
                    steps = (maxColSpan - 1 - 1).coerceAtLeast(0), // Steps between 1 and max
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text("Height (rows): $rowSpan")
                Slider(
                    value = rowSpan.toFloat(),
                    onValueChange = { newValue ->
                        rowSpan = newValue.roundToInt().coerceIn(1, maxRowSpan)
                    },
                    valueRange = 1f..maxRowSpan.toFloat(),
                    steps = (maxRowSpan - 1 - 1).coerceAtLeast(0),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
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