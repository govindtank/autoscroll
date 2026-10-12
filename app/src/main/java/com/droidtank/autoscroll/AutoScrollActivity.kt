package com.droidtank.autoscroll

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.droidtank.autoscroll.ui.components.AccessibilityDisclosureDialog
import com.droidtank.autoscroll.ui.screens.*
import com.droidtank.autoscroll.ui.theme.AutoScrollTheme
import com.droidtank.autoscroll.util.AccessibilityHelper

/**
 * Navigation destinations.
 */
private enum class Screen(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    APPS("Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    GESTURES("Gestures", Icons.Filled.Gesture, Icons.Outlined.Gesture),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class AutoScrollActivity : ComponentActivity() {

    private lateinit var scrollController: ScrollController

    companion object {
        var activeInstance: AutoScrollActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeInstance = this
        scrollController = ScrollController.getInstance(this)
        AccessibilityHelper.updateServiceState(this)

        setContent {
            AutoScrollTheme {
                AutoScrollApp(scrollController = scrollController)
            }
        }

        requestRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        AccessibilityHelper.updateServiceState(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    fun updateScrollCount(count: Int) {
        // UI updates reactively via StateFlow
    }

    private fun requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(
                            this,
                            "Notification permission denied",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * Root composable for the app — bottom navigation scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoScrollApp(scrollController: ScrollController) {
    val context = LocalContext.current
    val isAccessibilityEnabled by AccessibilityHelper.isServiceEnabled.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showDisclosureDialog by remember { mutableStateOf(false) }

    // Check on startup: display disclosure if not accepted and service is not active
    LaunchedEffect(Unit) {
        AccessibilityHelper.updateServiceState(context)
        if (!AccessibilityHelper.hasAcceptedDisclosure(context) && !AccessibilityHelper.isServiceEnabled(context)) {
            showDisclosureDialog = true
        }
    }

    if (showDisclosureDialog) {
        AccessibilityDisclosureDialog(
            onDismiss = { showDisclosureDialog = false },
            onAccept = { showDisclosureDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            Screen.HOME -> "AutoScroll Pro"
                            Screen.APPS -> "App Selection"
                            Screen.GESTURES -> "Gestures"
                            Screen.SETTINGS -> "Settings"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showDisclosureDialog = true }) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Accessibility Disclosure",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == screen)
                                    screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label
                            )
                        },
                        label = { Text(screen.label) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
                },
                label = "screenTransition"
            ) { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        scrollController = scrollController,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onRequestEnableAccessibility = { showDisclosureDialog = true },
                        onNavigateToApps = { currentScreen = Screen.APPS },
                        onNavigateToGestures = { currentScreen = Screen.GESTURES }
                    )
                    Screen.APPS -> {
                        val ctx = LocalContext.current
                        AppsScreen(
                            scrollController = scrollController,
                            onStartScrolling = { packageName, appName ->
                                startScrollingInApp(
                                    scrollController = scrollController,
                                    context = ctx,
                                    packageName = packageName,
                                    appName = appName,
                                    isAccessibilityEnabled = isAccessibilityEnabled,
                                    onRequestEnableAccessibility = { showDisclosureDialog = true }
                                )
                            }
                        )
                    }
                    Screen.GESTURES -> GesturesScreen(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onRequestEnableAccessibility = { showDisclosureDialog = true }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        scrollController = scrollController,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        onRequestEnableAccessibility = { showDisclosureDialog = true }
                    )
                }
            }
        }
    }
}

/**
 * Start scrolling specifically for a chosen app.
 */
private fun startScrollingInApp(
    scrollController: ScrollController,
    context: Context,
    packageName: String,
    appName: String,
    isAccessibilityEnabled: Boolean,
    onRequestEnableAccessibility: () -> Unit
) {
    if (!isAccessibilityEnabled) {
        onRequestEnableAccessibility()
        return
    }

    // Launch the target app
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch $appName", Toast.LENGTH_SHORT).show()
    }

    // Start scrolling foreground service
    val serviceIntent = Intent(context, AutoScrollService::class.java).apply {
        putExtra("app_name", appName)
    }
    context.startForegroundService(serviceIntent)
    scrollController.startScrolling(packageName, appName)
}
