package com.droidtank.autoscroll.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recorded gesture path segment - one continuous touch stroke.
 */
data class GestureStroke(
    val points: List<GesturePoint>,  // sequential touch points
    val startTimeOffset: Long,       // ms from gesture start when this stroke begins
    val durationMs: Long             // how long this stroke takes
)

/**
 * A single touch point in the gesture.
 */
data class GesturePoint(
    val x: Float,          // percentage of screen width (0.0 - 1.0)
    val y: Float,          // percentage of screen height (0.0 - 1.0)
    val timeOffsetMs: Long // ms from stroke start
)

/**
 * A recorded gesture that can be replayed.
 * Uses relative coordinates (percentages) so it works on different screen sizes.
 */
@Entity(tableName = "gesture_recordings")
data class GestureRecording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val packageName: String? = null,     // null = global gesture
    val strokesJson: String,             // JSON serialized list of GestureStroke
    val totalDurationMs: Long = 0L,      // total gesture duration
    val isLoopingGesture: Boolean = false, // if true, replay in a loop
    val iconHint: String? = null,        // optional emoji hint for the UI
    val createdAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
)

/**
 * Compact view of a gesture for the list UI.
 */
data class GestureSummary(
    val id: Long,
    val name: String,
    val iconHint: String?,
    val strokeCount: Int,
    val totalDurationMs: Long,
    val usageCount: Int
)
