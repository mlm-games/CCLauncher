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
import androidx.compose.runtime.key
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
import app.cclauncher.settings.AppPreference
import app.cclauncher.settings.AppSettings
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
import app.cclauncher.ui.dialogs.AccessibilityDisclosureDialog
import app.cclauncher.ui.dialogs.DropdownSettingDialog
import app.cclauncher.ui.dialogs.SettingsLockDialog
import app.cclauncher.ui.dialogs.SliderSettingDialog
import app.cclauncher.ui.util.updateStatusBarVisibility
import app.cclauncher.ui.viewmodels.SettingsViewModel
import app.cclauncher.settings.AppPicker
import app.cclauncher.settings.AppSettingsSchema
import app.cclauncher.settings.ColorPicker
import app.cclauncher.settings.FontPicker
import app.cclauncher.settings.IconPackPicker
import io.github.mlmgames.settings.core.types.Button
import io.github.mlmgames.settings.core.SettingField
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import kotlin.reflect.KClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    mainViewModel: MainViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.settingsState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val schema = remember { AppSettingsSchema }

    // Dialog states (now track a SettingField instead of KProperty/annotations)
    var showingDialog by remember { mutableStateOf<String?>(null) }
    var currentField by remember { mutableStateOf<SettingField<AppSettings, *>?>(null) }

    var showGridWarningDialog by remember { mutableStateOf(false) }
    var pendingGridChange by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val pickFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { viewModel.setCustomFont(it) }
        }
    )

    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.resetUnlockState() }
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
                    coroutineScope.launch {
                        if (viewModel.validatePin(pin)) {
                            viewModel.setShowLockDialog(false)
                        } else {
                            // Show error (handled in dialog)
                        }
                    }
                }
            }
        )
    }

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

    when (showingDialog) {
        "slider" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                val currentValue = when (val v = field.get(uiState)) {
                    is Int -> v.toFloat()
                    is Float -> v
                    is Double -> v.toFloat()
                    is Long -> v.toFloat()
                    else -> 0f
                }

                SliderSettingDialog(
                    title = meta.title,
                    currentValue = currentValue,
                    min = meta.min,
                    max = meta.max,
                    step = meta.step,
                    onDismiss = { showingDialog = null },
                    onValueSelected = { newValue ->
                        coroutineScope.launch {
                            val propertyName = field.name
                            val intValue = newValue.toInt()

                            if ((propertyName == "homeScreenRows" || propertyName == "homeScreenColumns") &&
                                viewModel.willGridChangeAffectItems(propertyName, intValue)
                            ) {
                                pendingGridChange = propertyName to intValue
                                showGridWarningDialog = true
                                showingDialog = null
                                return@launch
                            }

                            when (field.get(uiState)) {
                                is Int -> {
                                    if (propertyName == "homeScreenRows" || propertyName == "homeScreenColumns") {
                                        viewModel.updateGridSize(propertyName, intValue)
                                    } else {
                                        viewModel.updateSetting(propertyName, intValue)
                                    }
                                }
                                is Float -> viewModel.updateSetting(propertyName, newValue)
                                is Double -> viewModel.updateSetting(propertyName, newValue.toDouble())
                                is Long -> viewModel.updateSetting(propertyName, newValue.toLong())
                                else -> { /* ignore */ }
                            }

                            showingDialog = null
                        }
                    }
                )
            }
        }

        "dropdown" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                val selectedIndex = (field.get(uiState) as? Int) ?: 0

                DropdownSettingDialog(
                    title = meta.title,
                    options = meta.options,
                    selectedIndex = selectedIndex,
                    onDismiss = { showingDialog = null },
                    onOptionSelected = { index ->
                        coroutineScope.launch {
                            viewModel.updateSetting(field.name, index)
                            showingDialog = null
                        }
                    }
                )
            }
        }

        "button" -> {
            val field = currentField
            if (field != null) {
                when (field.name) {
                    "plainWallpaper" -> {
                        setPlainWallpaperByTheme(context, appTheme = uiState.appTheme)
                        showingDialog = null
                    }
                    else -> showingDialog = null
                }
            }
        }

        "font_picker" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                FontPickerDialog(
                    title = meta.title,
                    onDismiss = { showingDialog = null },
                    onSelectClicked = { pickFontLauncher.launch("font/*") },
                    viewModel = viewModel,
                    onResetClicked = {
                        viewModel.clearCustomFont()
                        showingDialog = null
                    }
                )
            }
        }

        "color_picker" -> {
            val field = currentField
            val meta = field?.meta
            if (field != null && meta != null) {
                val currentColor = (field.get(uiState) as? Int) ?: 0

                ColorPickerDialog(
                    title = meta.title,
                    currentColor = currentColor,
                    onDismiss = { showingDialog = null },
                    onColorSelected = { color ->
                        coroutineScope.launch {
                            viewModel.updateSetting(field.name, color)
                            showingDialog = null
                        }
                    }
                )
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
            val grouped = schema.groupedByCategory()
            val categories = schema.orderedCategories()

            for (category: KClass<*> in categories) {
                val categoryFields = grouped[category].orEmpty()
                if (categoryFields.isEmpty()) continue

                item(key = "cat_${category.qualifiedName ?: category.simpleName}") {
                    val title = (category.simpleName ?: "Settings")
                        .lowercase()
                        .capitalize(Locale.getDefault())

                    SettingsSection(title = title) {
                        categoryFields.forEach { field ->
                            key(field.name) {
                                val meta = field.meta
                                val isEnabled = if (meta != null) schema.isEnabled(uiState, field) else false

                                if (meta != null) {
                                    when (meta.type) {
                                        Toggle::class -> {
                                            val value = (field.get(uiState) as? Boolean) ?: false
                                            SettingsToggle(
                                                title = meta.title,
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                isChecked = value,
                                                enabled = isEnabled,
                                                onCheckedChange = { checked ->
                                                    coroutineScope.launch {
                                                        viewModel.updateSetting(field.name, checked)

                                                        when (field.name) {
                                                            "statusBar" -> {
                                                                try {
                                                                    (context as? Activity)?.let { activity ->
                                                                        updateStatusBarVisibility(
                                                                            activity,
                                                                            checked
                                                                        )
                                                                    }
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }
                                                            }

                                                            "doubleTapToLock" -> {
                                                                if (checked) {
                                                                    showAccessibilityDisclosure =
                                                                        true
                                                                } else {
                                                                    viewModel.updateSetting(
                                                                        "doubleTapToLock",
                                                                        false
                                                                    )
                                                                }
                                                            }

                                                            // Keep here if you later re-introduce a setting that controls orientation
                                                            "forceLandscapeMode" -> {
                                                                (context as? Activity)?.let { activity ->
                                                                    activity.requestedOrientation =
                                                                        if (checked) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                                                        else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        Slider::class -> {
                                            val subtitle = when (val v = field.get(uiState)) {
                                                is Int -> v.toString()
                                                is Float -> String.format(
                                                    Locale.getDefault(),
                                                    "%.1f",
                                                    v
                                                )

                                                is Double -> String.format(
                                                    Locale.getDefault(),
                                                    "%.1f",
                                                    v
                                                )

                                                is Long -> v.toString()
                                                else -> ""
                                            }

                                            SettingsItem(
                                                title = meta.title,
                                                subtitle = subtitle,
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                enabled = isEnabled,
                                                onClick = {
                                                    currentField = field
                                                    showingDialog = "slider"
                                                }
                                            )
                                        }


                                        Dropdown::class -> {
                                            val idx = (field.get(uiState) as? Int) ?: 0
                                            val options = meta.options
                                            val displayText = options.getOrNull(idx) ?: "Unknown"

                                            SettingsItem(
                                                title = meta.title,
                                                subtitle = displayText,
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                enabled = isEnabled,
                                                onClick = {
                                                    currentField = field
                                                    showingDialog = "dropdown"
                                                }
                                            )
                                        }

                                        Button::class -> {
                                            SettingsAction(
                                                title = meta.title,
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                enabled = isEnabled,
                                                onClick = {
                                                    currentField = field
                                                    showingDialog = "button"
                                                }
                                            )
                                        }

                                        AppPicker::class -> {
                                            val pref = (field.get(uiState) as? AppPreference)
                                                ?: AppPreference(label = "Not set")
                                            SettingsItem(
                                                title = meta.title,
                                                subtitle = pref.label.ifBlank { "Not set" },
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                enabled = isEnabled,
                                                onClick = {
                                                    val selectionType = when (field.name) {
                                                        "swipeLeftApp" -> AppSelectionType.SWIPE_LEFT_APP
                                                        "swipeRightApp" -> AppSelectionType.SWIPE_RIGHT_APP
                                                        "swipeUpApp" -> AppSelectionType.SWIPE_UP_APP
                                                        "swipeDownApp" -> AppSelectionType.SWIPE_DOWN_APP
                                                        else -> null
                                                    }

                                                    selectionType?.let {
                                                        coroutineScope.launch {
                                                            viewModel.emitEvent(
                                                                UiEvent.NavigateToAppSelection(
                                                                    it
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }

                                        IconPackPicker::class -> {
                                            val iconCache = remember { IconCache(context) }
                                            var availableIconPacks by remember {
                                                mutableStateOf<List<IconPackManager.IconPackInfo>>(
                                                    emptyList()
                                                )
                                            }
                                            var showIconPackDialog by remember {
                                                mutableStateOf(
                                                    false
                                                )
                                            }

                                            LaunchedEffect(Unit) {
                                                availableIconPacks =
                                                    iconCache.getAvailableIconPacks()
                                            }

                                            val selectedPackName =
                                                (field.get(uiState) as? String) ?: "default"
                                            val selectedPackDisplayName = availableIconPacks.find {
                                                it.packageName == selectedPackName
                                            }?.name ?: "Default Icons"

                                            SettingsItem(
                                                title = meta.title,
                                                subtitle = selectedPackDisplayName,
                                                description = meta.description.takeIf { it.isNotEmpty() },
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
                                                            viewModel.updateSetting(
                                                                field.name,
                                                                selectedPack
                                                            )
                                                            iconCache.clearCache()
                                                            showIconPackDialog = false
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        FontPicker::class -> {
                                            val fontPath = (field.get(uiState) as? String).orEmpty()
                                            val displayText = if (fontPath.isEmpty()) {
                                                "System default"
                                            } else {
                                                fontPath.split("/").last()
                                            }

                                            SettingsItem(
                                                title = meta.title,
                                                subtitle = displayText,
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                enabled = isEnabled,
                                                onClick = {
                                                    currentField = field
                                                    showingDialog = "font_picker"
                                                }
                                            )
                                        }

                                        ColorPicker::class -> {
                                            val colorValue = (field.get(uiState) as? Int) ?: 0
                                            val displayText =
                                                if (colorValue == 0) "Theme Default" else "Custom Color"

                                            SettingsItem(
                                                title = meta.title,
                                                subtitle = displayText,
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                enabled = isEnabled,
                                                onClick = {
                                                    currentField = field
                                                    showingDialog = "color_picker"
                                                }
                                            )
                                        }

                                        else -> {
                                            // Unknown / custom type not handled
                                            SettingsItem(
                                                title = meta.title,
                                                subtitle = "Unsupported setting type",
                                                description = meta.description.takeIf { it.isNotEmpty() },
                                                enabled = false,
                                                onClick = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Widgets section (unchanged)
            item(key = "widgets") {
                SettingsSection(title = "Widgets") {
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

            // Private Space section (unchanged)
            item(key = "private_space_$refreshTrigger") {
                SettingsSection(title = "Private Space") {
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

            // System section (unchanged)
            item(key = "system") {
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

                    SettingsToggle(
                        title = "Lock Settings",
                        description = "Prevent changes to settings without a PIN",
                        isChecked = uiState.lockSettings,
                        onCheckedChange = { locked ->
                            if (locked) {
                                viewModel.setShowLockDialog(true, true)
                            } else {
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

    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onDismiss = {
                showAccessibilityDisclosure = false
                coroutineScope.launch { viewModel.updateSetting("doubleTapToLock", false) }
            },
            onAccept = {
                showAccessibilityDisclosure = false
                coroutineScope.launch {
                    viewModel.updateSetting("doubleTapToLock", true)
                    // Preserving your current behavior (even though it looks inverted)
                    viewModel.updateSetting("accessibilityConsent", false)
                }
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    Toast.makeText(
                        context,
                        "Enable CCLauncher under Accessibility > Downloaded services.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {}
            }
        )
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