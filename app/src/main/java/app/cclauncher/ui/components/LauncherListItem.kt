package app.cclauncher.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.cclauncher.ui.theme.AnimationConfig

/**
 * Reusable list item component for the launcher
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherListItem(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageBitmap? = null,
    showIcon: Boolean = true,
    showLabel: Boolean = true,
    iconSize: Dp = 40.dp,
    iconCornerRadius: Dp = 0.dp,
    fontScale: Float = 1.0f,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color? = null,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 12.dp,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = AnimationConfig.standardIntTween)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick ?: {}
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon && icon != null) {
            Surface(
                shape = RoundedCornerShape(iconCornerRadius),
                modifier = Modifier.padding(end = 16.dp),
                color = Color.Transparent
            ) {
                Image(
                    bitmap = icon,
                    contentDescription = label,
                    modifier = Modifier.size(iconSize)
                )
            }
        } else if (showIcon) {
            // Spacer for alignment when icon is expected but not available
            Spacer(modifier = Modifier.size(iconSize).padding(end = 16.dp))
        }

        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale,
                    fontWeight = fontWeight
                ),
                color = textColor ?: MaterialTheme.colorScheme.onSurface, // Use custom color
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Trailing content (badges, indicators, etc.)
        trailing?.invoke()
    }
}

/**
 * Variant specifically for app items
 */
@Composable
fun AppListItem(
    appLabel: String,
    modifier: Modifier = Modifier,
    appIcon: ImageBitmap? = null,
    showIcon: Boolean = true,
    showLabel: Boolean = true,
    iconSize: Dp = 40.dp,
    iconCornerRadius: Dp = 0.dp,
    fontScale: Float = 1.0f,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    LauncherListItem(
        label = appLabel,
        icon = appIcon,
        showIcon = showIcon,
        showLabel = showLabel,
        iconSize = iconSize,
        iconCornerRadius = iconCornerRadius,
        fontScale = fontScale,
        fontWeight = fontWeight,
        textColor = textColor,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        trailing = trailing
    )
}