package app.cclauncher.ui.composables

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cclauncher.data.AppModel
import app.cclauncher.settings.AppSettings
import app.cclauncher.helper.IconCache
import kotlinx.coroutines.launch

@Composable
fun HomeAppItem(
    modifier: Modifier = Modifier,
    app: AppModel,
    settings: AppSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    appWidth: Dp,
    appHeight: Dp
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val iconCache = remember { IconCache(context) }
    var loadedIcon by remember(app.getKey()) { mutableStateOf(app.appIcon) }
    val textColor = if (settings.useCustomTextColor && settings.textColor != 0) {
        Color(settings.textColor)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val showHomeIcons = if (settings.showHomeScreenIcons) {
        if (isLandscape) settings.showIconsInLandscape else settings.showIconsInPortrait
    } else { false }

    // Load icon asynchronously if needed and not already loaded
    LaunchedEffect(app.getKey(), showHomeIcons) {
        if (showHomeIcons && loadedIcon == null) {
            coroutineScope.launch {
                val icon = iconCache.getIcon(
                    packageName = app.appPackage,
                    className = app.activityClassName,
                    user = app.user
                )
                loadedIcon = icon
            }
        }
    }

    val showIcons = showHomeIcons
    val showName = if (showHomeIcons) settings.showAppNames else true //TODO: Add a separate setting later? When settings are arranged properly ig
    val fontScale = settings.textSizeScale
    val fontWeight = remember(settings.fontWeight) {
        when (settings.fontWeight) {
            0 -> FontWeight.Thin
            1 -> FontWeight.Light
            2 -> FontWeight.Normal
            3 -> FontWeight.Medium
            4 -> FontWeight.Bold
            5 -> FontWeight.Black
            else -> FontWeight.Normal
        }
    }
    val iconCornerRadius = settings.iconCornerRadius.dp

    val (iconSize, scaledFontSize) = if (settings.scaleHomeApps) {
        val computedIconSize = (minOf(appWidth, appHeight) * 0.6f)
        // Use a baseline cell height (e.g. 80.dp) to compute scale factor; adjust as needed.
        val baselineCellHeight = 80.dp
        val scaleFactor = appWidth / baselineCellHeight
        val computedFontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * scaleFactor).sp
        Pair(computedIconSize, computedFontSize)
    } else {
        Pair(48.dp, MaterialTheme.typography.bodyMedium.fontSize)
    }

    val textAlign = when (settings.appLabelAlignment) {
        1 -> TextAlign.Center
        2 -> TextAlign.Right
        else -> TextAlign.Left
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (settings.showHomeScreenIcons) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (showIcons && loadedIcon != null) {
            Surface(
                shape = RoundedCornerShape(iconCornerRadius),
                modifier = Modifier
                    .size(iconSize)
                    .aspectRatio(1f), // Ensure square aspect ratio
                color = Color.Transparent
            ) {
                Image(
                    bitmap = loadedIcon!!,
                    contentDescription = "${app.appLabel} icon",
                )
            }
            Spacer(modifier = Modifier.height(if (showName) 4.dp else 0.dp)) // Space between icon and text
        }

        if (showName) {
            Text(
                text = app.appLabel,
                fontSize = scaledFontSize,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                    fontWeight = fontWeight
                ),
                color = textColor,
                textAlign = textAlign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}