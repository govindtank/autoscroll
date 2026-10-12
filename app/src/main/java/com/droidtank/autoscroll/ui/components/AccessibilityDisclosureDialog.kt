package com.droidtank.autoscroll.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.droidtank.autoscroll.ui.theme.StatusGreen
import com.droidtank.autoscroll.util.AccessibilityHelper

/**
 * Mandatory Google Play Prominent Disclosure Dialog for Android AccessibilityService API.
 * Meets all Google Play User Data & Accessibility policy requirements:
 * 1. Presented before requesting permission or directing to system settings.
 * 2. Requires explicit affirmative action (Two clear buttons: Accept & Continue vs Decline).
 * 3. Does not allow click-outside dismissal (dismissOnClickOutside = false).
 * 4. Back button navigation is strictly treated as decline, not consent.
 * 5. Fully details every feature using the API and provides strict privacy guarantees.
 */
@Composable
fun AccessibilityDisclosureDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.AccessibilityNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Accessibility Service Disclosure",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AutoScroll Pro uses the Android AccessibilityService API permission to provide hands-free automated scrolling and gesture controls.",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                Text(
                    text = "Why is this permission required?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "• Automated Touch Scrolling: Dispatches simulated swipe and scroll gestures on your screen according to your speed settings, enabling hands-free reading for ebooks, web articles, and video feeds.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "• Custom Gesture Playback: Replays your recorded touch sequences and macro gestures across any application.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "• Foreground App Detection: Detects when your designated configured apps (e.g. Chrome, Kindle, YouTube Shorts) open to automatically apply your personalized scroll profiles.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "• Volume Key Controls: Intercepts physical volume key presses while scrolling to quickly pause or resume hands-free scrolling without opening the app.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Strict Privacy & Data Safety Guarantee:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = StatusGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "• NO Personal Data Collected: AutoScroll Pro does NOT read, inspect, store, or monitor any personal data, typed text, passwords, messages, keystrokes, contacts, or financial details.\n• Zero Remote Transmission: Operates purely as a local touch dispatcher on your device. No data is stored externally, tracked, or sent over the internet to any server.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AccessibilityHelper.setAcceptedDisclosure(context, true)
                    onAccept()
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Accept & Continue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Decline", fontWeight = FontWeight.Medium)
            }
        }
    )
}
