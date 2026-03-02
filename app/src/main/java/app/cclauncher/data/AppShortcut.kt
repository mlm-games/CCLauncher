package app.cclauncher.data

import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap

data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val user: UserHandle,
    val icon: ImageBitmap? = null,
)
