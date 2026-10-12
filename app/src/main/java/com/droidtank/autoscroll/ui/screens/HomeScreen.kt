package com.droidtank.autoscroll.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.droidtank.autoscroll.AutoScrollAccessibilityService
import com.droidtank.autoscroll.AutoScrollActivity
import com.droidtank.autoscroll.AutoScrollService
import com.droidtank.autoscroll.ScrollController
import com.droidtank.autoscroll.ui.components.*
import com.droidtank.autoscroll.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Home / Dashboard screen — main landing tab.
 */
@Composable
fun HomeScreen(
    scrollController: ScrollController,
    isAccessibilityEnabled: Boolean,
    onRequestEnableAccessibility: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToGestures: () -> Unit
) {
    val state by scrollController.state.collectAsState()
    val enabledAppsCount by scrollController.enabledAppsCount.collectAsState()
    val totalScrollsAllTime by scrollController.totalScrollsAllTime.collectAsState()
    val currentAppName by scrollController.currentAppName.collectAsState()
    val context = LocalContext.current

    // Auto-updating elapsed time
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state) {
        while (state is ScrollController.State.Scrolling) {
            elapsed = scrollController.getElapsedSeconds()
            delay(1000)
        }
    }

    val isScrolling = state is ScrollController.State.Scrolling

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Hero Status Card ──
        HeroStatusCard(
            state = state,
            isAccessibilityEnabled = isAccessibilityEnabled,
            onRequestEnableAccessibility = onRequestEnableAccessibility,
            isScrolling = isScrolling,
            elapsed = elapsed,
            context = context
        )

        // ── Control Buttons ──
        ControlButtonsRow(
            state = state,
            isAccessibilityEnabled = isAccessibilityEnabled,
            onRequestEnableAccessibility = onRequestEnableAccessibility,
            scrollController = scrollController,
            context = context
        )

        // ── Stats Grid ──
        if (isScrolling) {
            StatsRow(
                state = state as ScrollController.State.Scrolling,
                elapsed = elapsed,
                scrollController = scrollController
            )
        }

        // ── Quick Info Cards ──
        if (enabledAppsCount > 0 && !isScrolling) {
            GlassCard(
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Configured Apps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$enabledAppsCount apps enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = onNavigateToApps) {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Manage")
                    }
                }
            }
        }

        // ── Gesture Quick Action ──
        if (isScrolling && currentAppName != null) {
            GlassCard(
                gradientColors = listOf(
                    TertiaryContainerLight.copy(alpha = 0.2f),
                    StatusYellow.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.TouchApp,
                        contentDescription = null,
                        tint = StatusYellow,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Recording in $currentAppName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap to customize gestures for this app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(onClick = onNavigateToGestures) {
                        Text("Gestures")
                    }
                }
            }
        }

        // ── All-Time Stats ──
        if (totalScrollsAllTime > 0 && !isScrolling) {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Total Scrolls",
                        value = formatNumber(totalScrollsAllTime),
                        icon = Icons.Outlined.SwipeVertical
                    )
                    StatItem(
                        label = "Sessions",
                        value = "${scrollController.getSessionCount()}",
                        icon = Icons.Outlined.History
                    )
                    StatItem(
                        label = "Apps",
                        value = "$enabledAppsCount",
                        icon = Icons.Outlined.Apps
                    )
                }
            }
        }

        // Branding
        CreatorCredit()

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Hero status card with animated background.
 */
@Composable
private fun HeroStatusCard(
    state: ScrollController.State,
    isAccessibilityEnabled: Boolean,
    onRequestEnableAccessibility: () -> Unit,
    isScrolling: Boolean,
    elapsed: Long,
    context: Context
) {
    val statusText = when {
        !isAccessibilityEnabled -> "Accessibility Required"
        isScrolling -> "Scrolling Active"
        state is ScrollController.State.Paused -> "Paused"
        else -> "Engine Ready"
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            !isAccessibilityEnabled -> StatusRed
            isScrolling -> StatusGreen
            state is ScrollController.State.Paused -> StatusYellow
            else -> StatusGreen
        },
        animationSpec = tween(400),
        label = "statusColor"
    )

    val bgColors = if (isScrolling) {
        listOf(StatusGreen.copy(alpha = 0.12f), MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
    } else {
        listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.surface)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isScrolling) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(colors = bgColors),
                    RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated icon
                StatusDot(
                    isActive = isScrolling,
                    color = statusColor,
                    modifier = Modifier.size(14.dp)
                )

                Text(
                    text = "AutoScroll Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )

                if (isScrolling) {
                    val scrollingState = state as ScrollController.State.Scrolling
                    val avgSpeed = if (elapsed > 0) {
                        (scrollingState.count * 60) / elapsed
                    } else 0

                    Text(
                        text = "${scrollingState.count} scrolls · ${formatElapsed(elapsed)} · $avgSpeed/min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isAccessibilityEnabled) {
                    Button(
                        onClick = onRequestEnableAccessibility,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusRed
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enable Accessibility", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Play/Pause/Stop control buttons.
 */
@Composable
private fun ControlButtonsRow(
    state: ScrollController.State,
    isAccessibilityEnabled: Boolean,
    onRequestEnableAccessibility: () -> Unit,
    scrollController: ScrollController,
    context: Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state !is ScrollController.State.Idle) {
            // Stop button
            FilledTonalButton(
                onClick = {
                    scrollController.stopScrolling()
                    context.stopService(Intent(context, AutoScrollService::class.java))
                },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = StatusRed.copy(alpha = 0.15f),
                    contentColor = StatusRed
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(24.dp))
        }

        // Play/Pause button
        FilledTonalButton(
            onClick = {
                if (!isAccessibilityEnabled) {
                    onRequestEnableAccessibility()
                    return@FilledTonalButton
                }

                when (state) {
                    is ScrollController.State.Idle,
                    is ScrollController.State.Paused -> {
                        val serviceIntent = Intent(context, AutoScrollService::class.java)
                        context.startForegroundService(serviceIntent)
                        scrollController.startScrolling()
                    }
                    is ScrollController.State.Scrolling -> {
                        scrollController.pauseScrolling()
                    }
                }
            },
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (state is ScrollController.State.Scrolling)
                    StatusYellow.copy(alpha = 0.15f)
                else
                    StatusGreen.copy(alpha = 0.15f),
                contentColor = if (state is ScrollController.State.Scrolling)
                    StatusYellow
                else
                    StatusGreen
            )
        ) {
            Icon(
                imageVector = when (state) {
                    is ScrollController.State.Scrolling -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = when (state) {
                    is ScrollController.State.Scrolling -> "Pause"
                    else -> "Start"
                },
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

/**
 * Stats row showing scroll metrics.
 */
@Composable
private fun StatsRow(
    state: ScrollController.State.Scrolling,
    elapsed: Long,
    scrollController: ScrollController
) {
    val avgSpeed = if (elapsed > 0) (state.count * 60) / elapsed else 0

    GlassCard(
        gradientColors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Scrolls",
                value = "${state.count}",
                icon = Icons.Outlined.SwipeVertical,
                tint = StatusGreen
            )
            StatItem(
                label = "Elapsed",
                value = formatElapsed(elapsed),
                icon = Icons.Outlined.Timer,
                tint = MaterialTheme.colorScheme.primary
            )
            StatItem(
                label = "Avg/min",
                value = "$avgSpeed",
                icon = Icons.Outlined.Speed,
                tint = when {
                    avgSpeed < 10 -> StatusRed
                    avgSpeed < 30 -> StatusYellow
                    else -> StatusGreen
                }
            )
        }
    }
}

// ── Utility ──

private fun formatElapsed(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

private fun formatNumber(n: Int): String {
    return when {
        n >= 10000 -> "${n / 1000}k"
        n >= 1000 -> "${n / 1000}.${(n % 1000) / 100}k"
        else -> "$n"
    }
}
