package app.cclauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.cclauncher.MainViewModel

@Composable
fun PrivateSpaceToggle(viewModel: MainViewModel) {
    val privateSpaceState by viewModel.privateSpaceState.collectAsState()

    if (privateSpaceState == MainViewModel.PrivateSpaceState.Unsupported) {
        return // Don't show anything if not supported
    }

    val icon = when (privateSpaceState) {
        MainViewModel.PrivateSpaceState.Locked -> Icons.Default.Lock
        MainViewModel.PrivateSpaceState.Unlocked -> Icons.Default.LockOpen
        else -> Icons.Default.Lock
    }

    val tint = when (privateSpaceState) {
        MainViewModel.PrivateSpaceState.Locked -> MaterialTheme.colorScheme.primary
        MainViewModel.PrivateSpaceState.Unlocked -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { viewModel.togglePrivateSpace() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Toggle Private Space",
            tint = tint
        )
    }
}

@Composable
fun PrivateSpaceIndicator(isInPrivateSpace: Boolean) {
    if (!isInPrivateSpace) return

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Private Space App",
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(10.dp)
        )
    }
}