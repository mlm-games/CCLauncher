package app.cclauncher.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cclauncher.MainViewModel
import app.cclauncher.data.Constants
import app.cclauncher.data.settings.AppPreference
import app.cclauncher.data.settings.AppSettings
import app.cclauncher.data.settings.Setting
import app.cclauncher.data.settings.SettingCategory
import app.cclauncher.data.settings.SettingType
import app.cclauncher.data.settings.SettingsManager
import app.cclauncher.helper.IconCache
import app.cclauncher.helper.PermissionManager
import app.cclauncher.helper.iconpack.IconPackManager
import app.cclauncher.helper.setPlainWallpaperByTheme
import app.cclauncher.ui.AppSelectionType
import app.cclauncher.ui.BackHandler
import app.cclauncher.ui.UiEvent
import app.cclauncher.ui.components.ColorPickerDialog
import app.cclauncher.ui.components.FontPickerDialog
import app.cclauncher.ui.components.GridSizeWarningDialog
import app.cclauncher.ui.components.IconPackSelectionDialog
import app.cclauncher.ui.components.SettingsAction
import app.cclauncher.ui.components.SettingsItem
import app.cclauncher.ui.components.SettingsSection
import app.cclauncher.ui.components.SettingsToggle
import app.cclauncher.ui.dialogs.DropdownSettingDialog
import app.cclauncher.ui.dialogs.SettingsLockDialog
import app.cclauncher.ui.dialogs.SliderSettingDialog
import app.cclauncher.ui.util.updateStatusBarVisibility
import app.cclauncher.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.reflect.KProperty1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
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

    var showGridWarningDialog by remember { mutableStateOf(false) }
    var pendingGridChange by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val pickFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                viewModel.setCustomFont(it)
            }
        }
    )


    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetUnlockState()
        }
    }

    val effectiveLockState by viewModel.effectiveLockState.collectAsState()
    val showLockDialog by viewModel.showLockDialog.collectAsState()
    val isSettingPin by viewModel.isSettingPin.collectAsState()

    val refreshTrigger by mainViewModel.refreshTrigger.collectAsState()

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
//    LaunchedEffect(uiState.forceLandscapeMode) {
//        (context as? Activity)?.let { activity ->
//            if (uiState.forceLandscapeMode) {
//                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//            } else {
//                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
//            }
//        }
//    }

    if (showGridWarningDialog && pendingGridChange != null) {
        GridSizeWarningDialog(
            onConfirm = {
                coroutineScope.launch {
                    val (propertyName, newValue) = pendingGridChange!!
                    viewModel.updateGridSize(propertyName, newValue)
                    showGridWarningDialog = false
                    pendingGridChange = null
                }
            },
            onDismiss = {
                showGridWarningDialog = false
                pendingGridChange = null
            }
        )
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
                                val propertyName = prop.name
                                val intValue = newValue.toInt()

                                // Check if this is a grid size change that affects items
                                if ((propertyName == "homeScreenRows" || propertyName == "homeScreenColumns") &&
                                    viewModel.willGridChangeAffectItems(propertyName, intValue)) {

                                    // Show warning dialog
                                    pendingGridChange = propertyName to intValue
                                    showGridWarningDialog = true
                                    showingDialog = null
                                } else {
                                    // Safe to change directly
                                    when (prop.returnType.classifier) {
                                        Int::class -> {
                                            if (propertyName == "homeScreenRows" || propertyName == "homeScreenColumns") {
                                                viewModel.updateGridSize(propertyName, intValue)
                                            } else {
                                                viewModel.updateSetting(propertyName, intValue)
                                            }
                                        }
                                        Float::class -> viewModel.updateSetting(propertyName, newValue)
                                    }
                                    showingDialog = null
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
                        "swipeUpApp" -> AppSelectionType.SWIPE_UP_APP
                        "swipeDownApp" -> AppSelectionType.SWIPE_DOWN_APP
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
                        setPlainWallpaperByTheme(context, appTheme = uiState.appTheme)
                        showingDialog = null
                    }
                }
            }
        }
        "font_picker" -> {
            currentProperty?.let { prop ->
                currentAnnotation?.let { annotation ->
                    FontPickerDialog(
                        title = annotation.title,
                        onDismiss = { showingDialog = null },
                        onSelectClicked = {
                            pickFontLauncher.launch("font/*")
                        },
                        viewModel = viewModel,
                        onResetClicked = {
                            viewModel.clearCustomFont()
                            showingDialog = null
                        }
                    )
                }
            }
        }
        "color_picker" -> {
            currentProperty?.let { prop ->
                currentAnnotation?.let { annotation ->
                    ColorPickerDialog(
                        title = annotation.title,
                        currentColor = prop.get(uiState) as Int,
                        onDismiss = { showingDialog = null },
                        onColorSelected = { color ->
                            coroutineScope.launch {
                                viewModel.updateSetting(prop.name, color)
                            }
                        }
                    )
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
                                                "swipeUpApp" -> {
                                                    AppSelectionType.SWIPE_UP_APP
                                                }
                                                "swipeDownApp" -> {
                                                    AppSelectionType.SWIPE_DOWN_APP
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
                                SettingType.ICON_PACK_PICKER -> {
                                    val iconCache = remember { IconCache(context) }
                                    var availableIconPacks by remember { mutableStateOf<List<IconPackManager.IconPackInfo>>(emptyList()) }
                                    var showIconPackDialog by remember { mutableStateOf(false) }

                                    LaunchedEffect(Unit) {
                                        availableIconPacks = iconCache.getAvailableIconPacks()
                                    }

                                    val selectedPackName = property.get(uiState) as String
                                    val selectedPackDisplayName = availableIconPacks.find {
                                        it.packageName == selectedPackName
                                    }?.name ?: "Default Icons"

                                    SettingsItem(
                                        title = annotation.title,
                                        subtitle = selectedPackDisplayName,
                                        description = annotation.description.takeIf { it.isNotEmpty() },
                                        enabled = isEnabled,
                                        onClick = { showIconPackDialog = true }
                                    )

                                    if (showIconPackDialog) {
                                        IconPackSelectionDialog(
                                            iconPacks = availableIconPacks,
                                            selectedPack = selectedPackName,
                                            onDismiss = { showIconPackDialog = false },
                                            onPackSelected = { selectedPack ->
                                                coroutineScope.launch {
                                                    viewModel.updateSetting(property.name, selectedPack)
                                                    iconCache.clearCache()
                                                    showIconPackDialog = false
                                                }
                                            }
                                        )
                                    }
                                }
                                SettingType.FONT_PICKER -> {
                                    val fontPath = property.get(uiState) as String
                                    val displayText = if (fontPath.isEmpty()) "System default" else fontPath.split("/").last()

                                    SettingsItem(
                                        title = annotation.title,
                                        subtitle = displayText,
                                        description = annotation.description.takeIf { it.isNotEmpty() },
                                        enabled = isEnabled,
                                        onClick = {
                                            currentProperty = property
                                            currentAnnotation = annotation
                                            showingDialog = "font_picker"
                                        }
                                    )
                                }
                                SettingType.COLOR_PICKER -> {
                                    val colorValue = property.get(uiState) as Int
                                    val displayText = if (colorValue == 0) "Theme Default" else "Custom Color"

                                    SettingsItem(
                                        title = annotation.title,
                                        subtitle = displayText,
                                        description = annotation.description.takeIf { it.isNotEmpty() },
                                        enabled = isEnabled,
                                        onClick = {
                                            currentProperty = property
                                            currentAnnotation = annotation
                                            showingDialog = "color_picker"
                                        }
                                    )
                                }
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

                item(key = "private_space_$refreshTrigger") {
                    SettingsSection(title = "Private Space") {
                        // Only show if Private Space is supported
                        if (mainViewModel.isPrivateSpaceSupported) {
                            val privateSpaceState by mainViewModel.privateSpaceState.collectAsState()

                            val subtitle = when (privateSpaceState) {
                                MainViewModel.PrivateSpaceState.NotSetUp -> "Private Space is not set up (or is not the default launcher)"
                                MainViewModel.PrivateSpaceState.Locked -> "Private Space is locked"
                                MainViewModel.PrivateSpaceState.Unlocked -> "Private Space is unlocked"
                                else -> ""
                            }

                            SettingsItem(
                                title = "Toggle Private Space",
                                subtitle = subtitle,
                                onClick = { mainViewModel.togglePrivateSpace() }
                            )

                            Text(
                                text = "Private Space allows you to hide apps from your main profile. " +
                                        "Apps in Private Space are only accessible when it's unlocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            SettingsItem(
                                title = "Private Space",
                                subtitle = "Requires Android 15 or higher",
                                enabled = false,
                                onClick = {},
                                transparency = 0.7f
                            )
                        }
                    }
                }

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

fun isAccessServiceEnabled(context: Context): Boolean {
    val permissionManager = PermissionManager(context)
    return permissionManager.hasAccessibilityPermission()
}

fun isClauncherDefault(context: Context): Boolean {
    val permissionManager = PermissionManager(context)
    return permissionManager.isDefaultLauncher()
}