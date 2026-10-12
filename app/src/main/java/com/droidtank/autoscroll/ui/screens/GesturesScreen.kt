package com.droidtank.autoscroll.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidtank.autoscroll.AutoScrollAccessibilityService
import com.droidtank.autoscroll.model.AutoScrollDatabase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.droidtank.autoscroll.model.GesturePoint
import com.droidtank.autoscroll.model.GestureRecording
import com.droidtank.autoscroll.model.GestureSummaryRaw
import com.droidtank.autoscroll.ui.components.*
import com.droidtank.autoscroll.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gesture recording and management screen.
 */
@Composable
fun GesturesScreen(
    isAccessibilityEnabled: Boolean = true,
    onRequestEnableAccessibility: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var gestures by remember { mutableStateOf<List<GestureSummaryRaw>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }

    // Load gestures
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            AutoScrollDatabase.getDatabase(context).gestureRecordingDao()
                .getGestureSummaries().collect { list ->
                    gestures = list
                }
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "Gesture Cloning",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Record custom gestures and replay them on any app",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Info card about gesture recording
        GlassCard(
            gradientColors = listOf(
                TertiaryContainerLight.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap \"Record\" to capture a touch gesture. " +
                                "The gesture will be saved and can be replayed instead of a standard swipe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Record new gesture button
        Button(
            onClick = { showRecordDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = StatusRed)
            Spacer(Modifier.width(8.dp))
            Text("Record New Gesture", fontWeight = FontWeight.Bold)
        }

        // Gesture list
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (gestures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.Gesture,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No saved gestures yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Record your first gesture to create a custom scroll pattern",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            SectionHeader(
                title = "Saved Gestures (${gestures.size})"
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(gestures, key = { it.id }) { gesture ->
                    GestureListItem(
                        summary = gesture,
                        onReplay = {
                            if (!isAccessibilityEnabled) {
                                onRequestEnableAccessibility()
                            } else {
                                replayGesture(context, gesture.id)
                            }
                        },
                        onDelete = {
                            showDeleteConfirm = gesture.id
                        }
                    )
                }
            }
        }
    }

    // Record dialog
    if (showRecordDialog) {
        RecordGestureDialog(
            context = context,
            onDismiss = { showRecordDialog = false },
            onSaved = { showRecordDialog = false }
        )
    }

    // Delete confirmation
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Delete Gesture?", fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone. The gesture will be removed from all app configurations.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            AutoScrollDatabase.getDatabase(context).gestureRecordingDao()
                                .deleteGesture(showDeleteConfirm!!)
                        }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Single gesture list item.
 */
@Composable
private fun GestureListItem(
    summary: GestureSummaryRaw,
    onReplay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
            // Gesture icon
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = TertiaryContainerLight.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = summary.iconHint ?: "✋",
                        fontSize = MaterialTheme.typography.titleLarge.fontSize
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(summary.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Used ${summary.usageCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            IconButton(onClick = onReplay) {
                Icon(
                    Icons.Outlined.PlayCircle,
                    contentDescription = "Replay",
                    tint = StatusGreen
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog for recording a new gesture.
 */
@Composable
private fun RecordGestureDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var gestureName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("✋") }
    var recordStep by remember { mutableIntStateOf(0) } // 0=name, 1=record, 2=done
    var recordedMs by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    val pathPoints = remember { mutableStateListOf<GesturePoint>() }
    val pointsToDraw = remember { mutableStateListOf<Offset>() }
    val strokeStart = remember { mutableLongStateOf(0L) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val emojiOptions = listOf("✋", "👆", "👇", "👉", "👈", "🔄", "↕️", "➡️", "⬅️", "⭐", "❤️", "✅")

    AlertDialog(
        onDismissRequest = {
            if (recordStep != 1) onDismiss()
        },
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = when (recordStep) {
                    0 -> "New Gesture"
                    1 -> "Draw Gesture"
                    2 -> "Done!"
                    else -> "Gesture"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (recordStep) {
                    0 -> {
                        // Name input
                        OutlinedTextField(
                            value = gestureName,
                            onValueChange = { gestureName = it },
                            label = { Text("Gesture Name") },
                            placeholder = { Text("e.g., Fast swipe down") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Icon selector
                        Text("Choose an icon", style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            emojiOptions.take(6).forEach { emoji ->
                                FilterChip(
                                    selected = selectedIcon == emoji,
                                    onClick = { selectedIcon = emoji },
                                    label = { Text(emoji, fontSize = MaterialTheme.typography.titleMedium.fontSize) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            emojiOptions.drop(6).forEach { emoji ->
                                FilterChip(
                                    selected = selectedIcon == emoji,
                                    onClick = { selectedIcon = emoji },
                                    label = { Text(emoji, fontSize = MaterialTheme.typography.titleMedium.fontSize) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Canvas drawing board UI
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Use your finger to draw the swipe gesture path below:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .onSizeChanged { canvasSize = it }
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                pathPoints.clear()
                                                pointsToDraw.clear()
                                                strokeStart.longValue = System.currentTimeMillis()
                                                val w = canvasSize.width.coerceAtLeast(1)
                                                val h = canvasSize.height.coerceAtLeast(1)
                                                pathPoints.add(
                                                    GesturePoint(
                                                        x = offset.x / w,
                                                        y = offset.y / h,
                                                        timeOffsetMs = 0L
                                                    )
                                                )
                                                pointsToDraw.add(offset)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val offset = change.position
                                                val elapsed = System.currentTimeMillis() - strokeStart.longValue
                                                val w = canvasSize.width.coerceAtLeast(1)
                                                val h = canvasSize.height.coerceAtLeast(1)
                                                pathPoints.add(
                                                    GesturePoint(
                                                        x = offset.x / w,
                                                        y = offset.y / h,
                                                        timeOffsetMs = elapsed
                                                    )
                                                )
                                                pointsToDraw.add(offset)
                                            },
                                            onDragEnd = {
                                                recordedMs = System.currentTimeMillis() - strokeStart.longValue
                                            }
                                        )
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (pointsToDraw.size > 1) {
                                        val path = Path().apply {
                                            val first = pointsToDraw.first()
                                            moveTo(first.x, first.y)
                                            for (i in 1 until pointsToDraw.size) {
                                                val pt = pointsToDraw[i]
                                                lineTo(pt.x, pt.y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = StatusGreen,
                                            style = Stroke(
                                                width = 8f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }

                                if (pointsToDraw.isEmpty()) {
                                    Text(
                                        text = "Touch and Drag Here",
                                        color = Color.White.copy(alpha = 0.3f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            if (pointsToDraw.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        pathPoints.clear()
                                        pointsToDraw.clear()
                                        recordedMs = 0L
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear and Redraw")
                                }
                            }
                        }
                    }

                    2 -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = StatusGreen.copy(alpha = 0.1f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusGreen,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Gesture Saved!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen
                                    )
                                    Text(
                                        text = formatDuration(recordedMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (recordStep) {
                0 -> {
                    Button(
                        onClick = {
                            if (gestureName.isBlank()) {
                                Toast.makeText(context, "Please enter a gesture name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            recordStep = 1
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = gestureName.isNotBlank()
                    ) {
                        Text("Next")
                    }
                }
                1 -> {
                    Button(
                        onClick = {
                            if (pathPoints.size < 2) {
                                Toast.makeText(context, "Please draw a gesture first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch(Dispatchers.IO) {
                                val db = AutoScrollDatabase.getDatabase(context)
                                val root = org.json.JSONArray()
                                val strokeObj = org.json.JSONObject()
                                val duration = recordedMs.coerceAtLeast(100L)
                                strokeObj.put("startTimeOffset", 0L)
                                strokeObj.put("durationMs", duration)

                                val pointsArray = org.json.JSONArray()
                                for (pt in pathPoints) {
                                    val ptObj = org.json.JSONObject()
                                    ptObj.put("x", pt.x.toDouble())
                                    ptObj.put("y", pt.y.toDouble())
                                    ptObj.put("timeOffsetMs", pt.timeOffsetMs)
                                    pointsArray.put(ptObj)
                                }
                                strokeObj.put("points", pointsArray)
                                root.put(strokeObj)

                                db.gestureRecordingDao().upsertGesture(
                                    GestureRecording(
                                        name = gestureName,
                                        iconHint = selectedIcon,
                                        strokesJson = root.toString(),
                                        totalDurationMs = duration
                                    )
                                )
                            }
                            recordStep = 2
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = pathPoints.size >= 2
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save Gesture")
                    }
                }
                2 -> {
                    Button(
                        onClick = onSaved,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        },
        dismissButton = {
            if (recordStep != 2) {
                TextButton(
                    onClick = {
                        if (recordStep == 1) {
                            recordStep = 0
                            pathPoints.clear()
                            pointsToDraw.clear()
                            recordedMs = 0L
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Text(if (recordStep == 1) "Back" else "Cancel")
                }
            }
        }
    )
}

// ── Helpers ──

private fun formatDuration(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${ms / 1000}.${(ms % 1000) / 100}s"
        else -> "${ms / 60000}m ${(ms % 60000) / 1000}s"
    }
}

/**
 * Replay a saved gesture.
 */
private fun replayGesture(context: android.content.Context, gestureId: Long) {
    val service = AutoScrollAccessibilityService.instance
    if (service == null) {
        Toast.makeText(context, "Accessibility Service not enabled", Toast.LENGTH_SHORT).show()
        return
    }

    // Load and replay gesture asynchronously
    kotlinx.coroutines.MainScope().launch {
        val gesture = withContext(Dispatchers.IO) {
            AutoScrollDatabase.getDatabase(context).gestureRecordingDao().getGesture(gestureId)
        }
        if (gesture != null) {
            service.performCustomGesture(gesture)
            withContext(Dispatchers.IO) {
                AutoScrollDatabase.getDatabase(context).gestureRecordingDao().incrementUsage(gestureId)
            }
            Toast.makeText(context, "Replayed: ${gesture.name}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Gesture not found", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Simulate a recording for demo purposes.
 * In production, this would connect to a real touch event listener.
 */
private fun simulateRecording(
    context: android.content.Context,
    onComplete: (Long) -> Unit
) {
    Toast.makeText(context, "Gesture recording started... Perform your touch gesture!", Toast.LENGTH_LONG).show()
    // Simulate a 2-second recording
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        Toast.makeText(context, "Recording finished!", Toast.LENGTH_SHORT).show()
        onComplete(1800L)
    }, 2000)
}
