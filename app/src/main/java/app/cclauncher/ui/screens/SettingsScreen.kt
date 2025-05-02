package app.cclauncher.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.repository.SettingsRepository
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.data.settings.Setting
import app.cclauncher.data.settings.SettingCategory
import app.cclauncher.data.settings.SettingType
import app.cclauncher.helper.isAccessServiceEnabled
import app.cclauncher.helper.isClauncherDefault
import app.cclauncher.helper.setPlainWallpaperByTheme
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.dialogs.*
import app.cclauncher.ui.util.updateStatusBarVisibility
import app.cclauncher.ui.AppSelectionType
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit = {}
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Dialog states
    var showingDialog by remember { mutableStateOf<String?>(null) }
    var currentProperty by remember { mutableStateOf<KProperty1<AppSettings, *>?>(null) }

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
    when {
        showingDialog == "slider" && currentProperty != null -> {
            val prop = currentProperty!!
            val annotation = prop.findAnnotation<Setting>()
            if (annotation != null) {
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

        showingDialog == "dropdown" && currentProperty != null -> {
            val prop = currentProperty!!
            val annotation = prop.findAnnotation<Setting>()
            if (annotation != null) {
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

        showingDialog == "theme" -> {
            ThemePickerDialog(
                show = true,
                currentTheme = uiState.appTheme,
                onDismiss = { showingDialog = null },
                onThemeSelected = { newTheme ->
                    coroutineScope.launch {
                        if (uiState.appTheme != newTheme) {
                            viewModel.updateSetting("appTheme", newTheme)
                            AppCompatDelegate.setDefaultNightMode(newTheme)
                            (context as? Activity)?.recreate()
                        }
                    }
                }
            )
        }

        showingDialog == "fontWeight" -> {
            FontWeightDialog(
                show = true,
                currentWeight = uiState.fontWeight,
                onDismiss = { showingDialog = null },
                onWeightSelected = { weight ->
                    coroutineScope.launch {
                        viewModel.updateSetting("fontWeight", weight)
                        (context as? Activity)?.recreate()
                    }
                }
            )
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group settings by category
            val settingsByCategory = AppSettings::class.memberProperties
                .filter { it.findAnnotation<Setting>() != null }
                .groupBy { it.findAnnotation<Setting>()!!.category }

            // Display each category
            for (category in SettingCategory.entries) {
                val categorySettings = settingsByCategory[category] ?: continue

                item {
                    SettingsSection(title = category.name.lowercase().capitalize(Locale.getDefault())) {
                        categorySettings.forEach { property ->
                            val annotation = property.findAnnotation<Setting>()!!
                            val dependsOn = annotation.dependsOn

                            // Check if this setting depends on another setting
                            val isEnabled = if (dependsOn.isNotEmpty()) {
                                val dependencyValue = AppSettings::class.memberProperties
                                    .find { it.name == dependsOn }
                                    ?.get(uiState)

                                when (dependencyValue) {
                                    is Boolean -> dependencyValue
                                    else -> true
                                }
                            } else {
                                true
                            }

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
                                            Float::class -> String.format("%.1f", property.get(uiState) as Float)
                                            else -> ""
                                        },
                                        description = annotation.description.takeIf { it.isNotEmpty() },
                                        enabled = isEnabled,
                                        onClick = {
                                            currentProperty = property
                                            showingDialog = "slider"
                                        }
                                    )
                                }

                                SettingType.DROPDOWN -> {
                                    val options = annotation.options

                                    // Special handling for theme
                                    if (property.name == "appTheme") {
                                        SettingsItem(
                                            title = annotation.title,
                                            subtitle = getThemeText(uiState.appTheme),
                                            description = annotation.description.takeIf { it.isNotEmpty() },
                                            enabled = isEnabled,
                                            onClick = {
                                                showingDialog = "theme"
                                            }
                                        )
                                    }
                                    // Special handling for font weight
                                    else if (property.name == "fontWeight") {
                                        SettingsItem(
                                            title = annotation.title,
                                            subtitle = getFontWeightText(uiState.fontWeight),
                                            description = annotation.description.takeIf { it.isNotEmpty() },
                                            enabled = isEnabled,
                                            onClick = {
                                                showingDialog = "fontWeight"
                                            }
                                        )
                                    }
                                    // Other dropdowns
                                    else if (options.isNotEmpty() && property.returnType.classifier == Int::class) {
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
                                            when (property.name) {
                                                "usePlainWallpaper" -> {
                                                    setPlainWallpaperByTheme(context, appTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                                                }
                                            }
                                        }
                                    )
                                }

                                else -> { /* Handle other types if needed */ }
                            }
                        }
                    }
                }
            }

            // System section (mostly handled separately)
            item {
                SettingsSection(title = "System") {
                    SettingsItem(
                        title = "Set as Default Launcher",
                        subtitle = if (isClauncherDefault(context)) "CCLauncher is default" else "CCLauncher is not default",
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                            context.startActivity(intent)
                        },
                        transparency = if (isClauncherDefault(context)) 0.7f else 1.0f
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
fun getThemeText(theme: Int): String = when(theme) {
    AppCompatDelegate.MODE_NIGHT_NO -> "Light"
    AppCompatDelegate.MODE_NIGHT_YES -> "Dark"
    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "System"
    else -> "Dark"
}

fun getFontWeightText(weight: Int): String = when(weight) {
    0 -> "Thin"
    1 -> "Light"
    2 -> "Normal"
    3 -> "Medium"
    4 -> "Bold"
    5 -> "Black"
    else -> "Normal"
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
                Text(String.format("%.1f", sliderValue))
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
                items(options.indices.toList()) { index ->
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

@Composable
fun FontWeightDialog(
    show: Boolean,
    currentWeight: Int,
    onDismiss: () -> Unit,
    onWeightSelected: (Int) -> Unit
) {
    if (!show) return

    val options = listOf("Thin", "Light", "Normal", "Medium", "Bold", "Black")
    var selected by remember { mutableIntStateOf(currentWeight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Font Weight") },
        text = {
            LazyColumn {
                items(options.indices.toList()) { index ->
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
                        Text(
                            text = options[index],
                            fontWeight = when(index) {
                                0 -> androidx.compose.ui.text.font.FontWeight.Thin
                                1 -> androidx.compose.ui.text.font.FontWeight.Light
                                2 -> androidx.compose.ui.text.font.FontWeight.Normal
                                3 -> androidx.compose.ui.text.font.FontWeight.Medium
                                4 -> androidx.compose.ui.text.font.FontWeight.Bold
                                5 -> androidx.compose.ui.text.font.FontWeight.Black
                                else -> androidx.compose.ui.text.font.FontWeight.Normal
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onWeightSelected(selected)
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