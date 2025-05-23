package app.cclauncher.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.settings.AppPreference
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.data.settings.Setting
import app.cclauncher.data.settings.SettingCategory
import app.cclauncher.data.settings.SettingType
import app.cclauncher.data.settings.SettingsManager
import app.cclauncher.helper.isAccessServiceEnabled
import app.cclauncher.helper.isClauncherDefault
import app.cclauncher.helper.setPlainWallpaperByTheme
import app.cclauncher.ui.AppSelectionType
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.util.updateStatusBarVisibility
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.reflect.KProperty1
import app.cclauncher.ui.dialogs.SettingsLockDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit = {}
) {
//    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager() }

    // Dialog states
    var showingDialog by remember { mutableStateOf<String?>(null) }
    var currentProperty by remember { mutableStateOf<KProperty1<AppSettings, *>?>(null) }
    var currentAnnotation by remember { mutableStateOf<Setting?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUnlockState()
        }
    }

    val effectiveLockState by viewModel.effectiveLockState.collectAsState()
    val showLockDialog by viewModel.showLockDialog.collectAsState()
    val isSettingPin by viewModel.isSettingPin.collectAsState()

    BackHandler(onBack = {
        viewModel.resetUnlockState()
        onNavigateBack()
    })

    if (showLockDialog) {
        SettingsLockDialog(
            isSettingPin = isSettingPin,
            onDismiss = { viewModel.setShowLockDialog(false) },
            onConfirm = { pin ->
                if (isSettingPin) {
                    viewModel.setPin(pin)
                    viewModel.toggleLockSettings(true)
                    viewModel.setShowLockDialog(false)
                } else {
                    if (viewModel.validatePin(pin)) {
                        viewModel.setShowLockDialog(false)
                    } else {
                        // Show error (handled in dialog)
                    }
                }
            }
        )
    }

    // Handle orientation based on settings
    LaunchedEffect(uiState.forceLandscapeMode) {
        (context as? Activity)?.let { activity ->
            if (uiState.forceLandscapeMode) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Display the appropriate dialog based on setting type
    when (showingDialog) {
        "slider" -> {
            currentProperty?.let { prop ->
                currentAnnotation?.let { annotation ->
                    SliderSettingDialog(
                        title = annotation.title,
                        currentValue = when (prop.returnType.classifier) {
                            Int::class -> (prop.get(uiState) as Int).toFloat()
                            Float::class -> prop.get(uiState) as Float
                            else -> 0f
                        },
                        min = annotation.min,
                        max = annotation.max,
                        step = annotation.step,
                        onDismiss = { showingDialog = null },
                        onValueSelected = { newValue ->
                            coroutineScope.launch {
                                when (prop.returnType.classifier) {
                                    Int::class -> viewModel.updateSetting(prop.name, newValue.toInt())
                                    Float::class -> viewModel.updateSetting(prop.name, newValue)
                                }
                            }
                        }
                    )
                }
            }
        }
        "dropdown" -> {
            currentProperty?.let { prop ->
                currentAnnotation?.let { annotation ->
                    DropdownSettingDialog(
                        title = annotation.title,
                        options = annotation.options.toList(),
                        selectedIndex = when (prop.returnType.classifier) {
                            Int::class -> prop.get(uiState) as Int
                            else -> 0
                        },
                        onDismiss = { showingDialog = null },
                        onOptionSelected = { index ->
                            coroutineScope.launch {
                                viewModel.updateSetting(prop.name, index)
                            }
                        }
                    )
                }
            }
        }
        "app_picker" -> {
            currentProperty?.let { prop ->
                coroutineScope.launch {
                    // Determine which app selection type to use
                    val selectionType = when (prop.name) {
                        "swipeLeftApp" -> AppSelectionType.SWIPE_LEFT_APP
                        "swipeRightApp" -> AppSelectionType.SWIPE_RIGHT_APP
                        "clockApp" -> AppSelectionType.CLOCK_APP
                        "calendarApp" -> AppSelectionType.CALENDAR_APP
                        else -> null
                    }

                    selectionType?.let {
                        viewModel.emitEvent(UiEvent.NavigateToAppSelection(it))
                        showingDialog = null
                    }
                }
            }
        }
        "button" -> {
            currentProperty?.let { prop ->
                when (prop.name) {
                    "plainWallpaper" -> {
                        setPlainWallpaperByTheme(context, appTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        showingDialog = null
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (viewModel.isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        if (effectiveLockState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.8f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Settings Locked",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Settings are locked",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Enter your PIN to access settings",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.setShowLockDialog(true, false) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unlock Settings")
                        }
                    }
                }
            }
            return@Scaffold
        }

            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group settings by category
            val settingsByCategory = settingsManager.getSettingsByCategory()

            // Display each category
            for (category in SettingCategory.entries) {
                val categorySettings = settingsByCategory[category] ?: continue

                item {
                    SettingsSection(title = category.name.lowercase().capitalize(Locale.getDefault())) {
                        categorySettings.forEach { (property, annotation) ->
                            // Check if this setting is enabled
                            val isEnabled = settingsManager.isSettingEnabled(uiState, property, annotation)

                            when (annotation.type) {
                                SettingType.TOGGLE -> {
                                    if (property.returnType.classifier == Boolean::class) {
                                        val value = property.get(uiState) as Boolean
                                        SettingsToggle(
                                            title = annotation.title,
                                            description = annotation.description.takeIf { it.isNotEmpty() },
                                            isChecked = value,
                                            enabled = isEnabled,
                                            onCheckedChange = {
                                                coroutineScope.launch {
                                                    viewModel.updateSetting(property.name, it)

                                                    // Special handling for specific settings
                                                    when (property.name) {
                                                        "statusBar" -> {
                                                            try {
                                                                (context as? Activity)?.let { activity ->
                                                                    updateStatusBarVisibility(activity, it)
                                                                }
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                            }
                                                        }
                                                        "doubleTapToLock" -> {
                                                            if (it && !isAccessServiceEnabled(context)) {
                                                                Toast.makeText(context, "Enable accessibility permission for this functionality.", Toast.LENGTH_SHORT).show()
                                                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                                                context.startActivity(intent)
                                                            }
                                                        }
                                                        "forceLandscapeMode" -> {
                                                            (context as? Activity)?.let { activity ->
                                                                if (it) {
                                                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                                                } else {
                                                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                                SettingType.SLIDER -> {
                                    SettingsItem(
                                        title = annotation.title,
                                        subtitle = when (property.returnType.classifier) {
                                            Int::class -> "${property.get(uiState) as Int}"
                                            Float::class -> String.format(Locale.getDefault(), "%.1f", property.get(uiState) as Float)
                                            else -> ""
                                        },
                                        description = annotation.description.takeIf { it.isNotEmpty() },
                                        enabled = isEnabled,
                                        onClick = {
                                            currentProperty = property
                                            currentAnnotation = annotation
                                            showingDialog = "slider"
                                        }
                                    )
                                }
                                SettingType.DROPDOWN -> {
                                    val options = annotation.options

                                    if (options.isNotEmpty() && property.returnType.classifier == Int::class) {
                                        val value = property.get(uiState) as Int
                                        val displayText = if (value >= 0 && value < options.size) {
                                            options[value]
                                        } else {
                                            "Unknown"
                                        }

                                        SettingsItem(
                                            title = annotation.title,
                                            subtitle = displayText,
                                            description = annotation.description.takeIf { it.isNotEmpty() },
                                            enabled = isEnabled,
                                            onClick = {
                                                currentProperty = property
                                                currentAnnotation = annotation
                                                showingDialog = "dropdown"
                                            }
                                        )
                                    }
                                }
                                SettingType.BUTTON -> {
                                    SettingsAction(
                                        title = annotation.title,
                                        description = annotation.description.takeIf { it.isNotEmpty() },
                                        enabled = isEnabled,
                                        onClick = {
                                            currentProperty = property
                                            showingDialog = "button"
                                        }
                                    )
                                }
                                SettingType.APP_PICKER -> {
                                    val appPreference = property.get(uiState)
                                    val appName = when (appPreference) {
                                        is AppPreference -> appPreference.label
                                        else -> "Not set"
                                    }
                                    SettingsItem(
                                        title = annotation.title,
                                        subtitle = appName,
                                        description = annotation.description.takeIf { it.isNotEmpty() },
                                        enabled = isEnabled,
                                        onClick = {
                                            val selectionType = when (property.name) {
                                                "swipeLeftApp" -> {
                                                    AppSelectionType.SWIPE_LEFT_APP
                                                }
                                                "swipeRightApp" -> {
                                                    AppSelectionType.SWIPE_RIGHT_APP
                                                }
                                                "clockApp" -> {
                                                    AppSelectionType.CLOCK_APP
                                                }
                                                "calendarApp" -> {
                                                    AppSelectionType.CALENDAR_APP
                                                }
                                                else -> {
                                                    null
                                                }
                                            }

                                            selectionType?.let {
                                                coroutineScope.launch {
                                                    viewModel.emitEvent(UiEvent.NavigateToAppSelection(it))
                                                }
                                            }
                                        }
                                    )
                                }
                                else -> { /* Why is color picker even needed? */ }
                            }
                        }
                    }
                }
            }

            // System section (mostly handled separately)
            item {
                SettingsSection(title = "Widgets") {
//                    SettingsItem(
//                        title = "Manage Widgets",
//                        subtitle = "Add, remove, and configure widgets",
//                        onClick = {
//                            coroutineScope.launch {
//                                viewModel.emitEvent(UiEvent.NavigateToWidgetManager)
//                            }
//                        }
//                    )

                    SettingsAction(
                        title = "Add Widget",
                        description = "Add a widget to your home screen",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.emitEvent(UiEvent.NavigateToWidgetPicker)
                            }
                        }
                    )
                }
            }

//                    SettingsToggle(
//                        title = "Transparent Widget Background",
//                        isChecked = uiState.transparentWidgetBackground,
//                        onCheckedChange = {
//                            coroutineScope.launch {
//                                viewModel.prefsDataStore.setTransparentWidgetBackground(it)
//                                viewModel.updateSettingsState()
//                            }
//                        }
//                    )

            item {
                SettingsSection(title = "System") {
                    SettingsItem(
                        title = "Set as Default Launcher",
                        subtitle = if (isClauncherDefault(context)) "CCLauncher is default" else "CCLauncher is not default",
                        onClick = {
//                             (context as? MainActivity)?.requestDefaultLauncher(context)
                            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                            context.startActivity(intent)
                        },
                        transparency = if (isClauncherDefault(context)) 0.7f else 1.0f
                    )

                    SettingsToggle(
                        title = "Lock Settings",
                        description = "Prevent changes to settings without a PIN",
                        isChecked = uiState.lockSettings,
                        onCheckedChange = { locked ->
                            if (locked) {
                                // When enabling lock, show dialog to set PIN
                                viewModel.setShowLockDialog(true, true)
                            } else {
                                // When disabling, just turn it off (no PIN required to disable)
                                viewModel.toggleLockSettings(false)
                            }
                        }
                    )

                    SettingsItem(
                        title = "Hidden Apps",
                        onClick = onNavigateToHiddenApps
                    )

                    SettingsItem(
                        title = "App Info",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )

                    SettingsItem(
                        title = "About CCLauncher",
                        subtitle = "Version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.emitEvent(UiEvent.ShowDialog(Constants.Dialog.ABOUT))
                            }
                        }
                    )
                }
            }
        }
    }
}

// Helper functions
fun String.capitalize(locale: Locale): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
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

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    transparency: Float = 1.0f
) {
    Surface(
        modifier = Modifier
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

@Composable
fun SettingsToggle(
    title: String,
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
        modifier = Modifier
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
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
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
            Text("Set")
        }
    }
}

@Composable
fun SliderSettingDialog(
    title: String,
    currentValue: Float,
    min: Float,
    max: Float,
    step: Float,
    onDismiss: () -> Unit,
    onValueSelected: (Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(String.format(Locale.getDefault(), "%.1f", sliderValue))
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        // Round to nearest step
                        val steps = ((it - min) / step).toInt()
                        sliderValue = min + (steps * step)
                    },
                    valueRange = min..max,
                    steps = ((max - min) / step).toInt() - 1
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueSelected(sliderValue)
                onDismiss()
            }) {
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
fun DropdownSettingDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onDismiss: () -> Unit,
    onOptionSelected: (Int) -> Unit
) {
    var selected by remember { mutableIntStateOf(selectedIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.indices.toList().size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = index }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { selected = index }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(options[index])
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onOptionSelected(selected)
                onDismiss()
            }) {
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

