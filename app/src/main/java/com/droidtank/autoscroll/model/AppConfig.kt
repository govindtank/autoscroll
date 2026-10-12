package com.droidtank.autoscroll.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Scroll direction options.
 */
enum class ScrollDirection(val displayName: String) {
    UP("Swipe Up"),
    DOWN("Swipe Down"),
    ALTERNATING("Alternating"),
    SMART("Smart")
}

/**
 * Per-app auto-scroll configuration.
 * Each installed app can have its own speed, direction, and gesture preset.
 */
@Entity(tableName = "app_configs")
data class AppConfig(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = false,
    val scrollSpeed: Int = 20,          // swipes per minute (1-120)
    val scrollDirection: String = "UP",  // UP, DOWN, ALTERNATING, SMART
    val alternatingInterval: Int = 5,    // switch direction every N scrolls (for ALTERNATING)
    val gestureRecordingId: Long? = null, // optional custom gesture to use
    val sessionScrollCount: Int = 0,    // total scrolls in current session
    val lastUsedTimestamp: Long = 0L    // when this app was last scrolled in
)
