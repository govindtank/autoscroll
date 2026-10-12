package com.droidtank.autoscroll.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.droidtank.autoscroll.ScrollController
import com.droidtank.autoscroll.model.AppConfig
import com.droidtank.autoscroll.model.AutoScrollDatabase
import com.droidtank.autoscroll.model.ScrollDirection
import com.droidtank.autoscroll.model.GestureSummaryRaw
import com.droidtank.autoscroll.ui.components.*
import com.droidtank.autoscroll.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

/**
 * App selection screen — shows installed apps and per-app config.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    scrollController: ScrollController,
    onStartScrolling: (packageName: String, appName: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var activeConfigApp by remember { mutableStateOf<AppInfo?>(null) }

    val db = remember { AutoScrollDatabase.getDatabase(context) }
    val dbConfigs by db.appConfigDao().getAllConfigs().collectAsState(initial = emptyList())
    val appConfigs = remember(dbConfigs) { dbConfigs.associateBy { it.packageName } }
    val gesturesList by db.gestureRecordingDao().getGestureSummaries().collectAsState(initial = emptyList())

    // Load installed apps
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Load Installed Apps
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedList = pm.queryIntentActivities(intent, 0)
            val fetchedApps = resolvedList.map {
                AppInfo(
                    packageName = it.activityInfo.packageName,
                    appName = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm)
                )
            }.distinctBy { it.packageName }.sortedBy { it.appName }

            appsList = fetchedApps
        }
        isLoading = false
    }

    val filteredApps = remember(searchQuery, appsList) {
        if (searchQuery.isBlank()) {
            appsList
        } else {
            appsList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val config = appConfigs[app.packageName]
                    val isEnabled = config?.isEnabled == true
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnabled) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // App Icon
                            val imageBitmap = remember(app.icon) {
                                app.icon?.toBitmap()?.asImageBitmap()
                            }
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap,
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Android, contentDescription = null)
                                }
                            }

                            // App Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                if (isEnabled && config != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DirectionBadge(direction = config.scrollDirection)
                                        SpeedIndicator(speed = config.scrollSpeed)
                                    }
                                }
                            }

                            // Actions
                            IconButton(onClick = { activeConfigApp = app }) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = "Configure",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (isEnabled) {
                                IconButton(
                                    onClick = { onStartScrolling(app.packageName, app.appName) }
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayCircle,
                                        contentDescription = "Start Scrolling",
                                        tint = StatusGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Config Dialog
    if (activeConfigApp != null) {
        val app = activeConfigApp!!
        val currentConfig = appConfigs[app.packageName] ?: AppConfig(
            packageName = app.packageName,
            appName = app.appName
        )

        var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
        var scrollSpeed by remember { mutableStateOf(currentConfig.scrollSpeed) }
        var scrollDirection by remember { mutableStateOf(currentConfig.scrollDirection) }
        var alternatingInterval by remember { mutableStateOf(currentConfig.alternatingInterval) }
        var gestureId by remember { mutableStateOf(currentConfig.gestureRecordingId) }
        var showGestureDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { activeConfigApp = null },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Configure ${app.appName}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Enable Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable for this app", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it }
                        )
                    }

                    if (isEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Scroll Speed
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Scroll Speed", style = MaterialTheme.typography.bodyMedium)
                                Text("$scrollSpeed swipes/min", fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = scrollSpeed.toFloat(),
                                onValueChange = { scrollSpeed = it.toInt() },
                                valueRange = 10f..100f,
                                steps = 9
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Direction Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Scroll Direction", style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dirs = listOf(
                                    ScrollDirection.UP,
                                    ScrollDirection.DOWN,
                                    ScrollDirection.ALTERNATING,
                                    ScrollDirection.SMART
                                )
                                dirs.forEach { dir ->
                                    val isSelected = scrollDirection == dir.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { scrollDirection = dir.name },
                                        label = { Text(dir.displayName) }
                                    )
                                }
                            }
                        }

                        if (scrollDirection == ScrollDirection.ALTERNATING.name) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Interval (swipes)", style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (alternatingInterval > 1) alternatingInterval-- },
                                        enabled = alternatingInterval > 1
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null)
                                    }
                                    Text("$alternatingInterval", fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { alternatingInterval++ }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Custom Gestures Selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Gesture Override", style = MaterialTheme.typography.bodyMedium)
                            Box {
                                OutlinedButton(
                                    onClick = { showGestureDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    val selectedGestureName = if (gestureId == null) {
                                        "Default Swipe"
                                    } else {
                                        gesturesList.firstOrNull { it.id == gestureId }?.name ?: "Default Swipe"
                                    }
                                    Text(selectedGestureName)
                                }

                                DropdownMenu(
                                    expanded = showGestureDropdown,
                                    onDismissRequest = { showGestureDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Default Swipe") },
                                        onClick = {
                                            gestureId = null
                                            showGestureDropdown = false
                                        }
                                    )
                                    gesturesList.forEach { gesture ->
                                        DropdownMenuItem(
                                            text = { Text(gesture.name) },
                                            onClick = {
                                                gestureId = gesture.id
                                                showGestureDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val configToSave = AppConfig(
                                packageName = app.packageName,
                                appName = app.appName,
                                isEnabled = isEnabled,
                                scrollSpeed = scrollSpeed,
                                scrollDirection = scrollDirection,
                                alternatingInterval = alternatingInterval,
                                gestureRecordingId = gestureId
                            )
                            scrollController.saveAppConfig(configToSave)
                        }
                        activeConfigApp = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Config")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeConfigApp = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
