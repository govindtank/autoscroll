package com.droidtank.autoscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.droidtank.autoscroll.model.GesturePoint
import com.droidtank.autoscroll.model.GestureRecording
import com.droidtank.autoscroll.model.GestureStroke
import com.droidtank.autoscroll.util.AccessibilityHelper
import org.json.JSONArray

/**
 * Enhanced Accessibility Service with multi-direction scrolling, custom gesture replay,
 * smart container scrolling, and volume key toggles.
 */
class AutoScrollAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            if (pkg != applicationContext.packageName) {
                ScrollController.getInstance(this).onForegroundAppChanged(pkg)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AccessibilityHelper.updateServiceState(this)
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        AccessibilityHelper.updateServiceState(this)
        Log.d(TAG, "Accessibility service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        AccessibilityHelper.updateServiceState(this)
        Log.d(TAG, "Accessibility service destroyed")
    }

    /**
     * Intercept physical volume key presses to play/pause auto-scroll.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                val controller = ScrollController.getInstance(this)
                val state = controller.state.value

                if (state is ScrollController.State.Scrolling) {
                    controller.pauseScrolling()
                    Toast.makeText(this, "AutoScroll: Paused", Toast.LENGTH_SHORT).show()
                    return true
                } else if (state is ScrollController.State.Paused) {
                    val pkg = controller.currentAppPackage.value
                    val app = controller.currentAppName.value
                    controller.startScrolling(pkg, app)
                    Toast.makeText(this, "AutoScroll: Resumed", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    /**
     * Perform a scroll gesture in the given direction.
     */
    fun performSwipe(direction: String) {
        try {
            if (direction.uppercase() == "SMART") {
                val smartScrollSuccess = performSmartScroll()
                if (smartScrollSuccess) {
                    Log.d(TAG, "Smart scroll executed successfully")
                    return
                }
                Log.d(TAG, "Smart scroll failed or no container found. Falling back to swipe up.")
            }

            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            val startX = width / 2f
            val startY: Float
            val endY: Float

            when (direction.uppercase()) {
                "UP", "SMART" -> {
                    startY = height * 0.75f
                    endY = height * 0.25f
                }
                "DOWN" -> {
                    startY = height * 0.25f
                    endY = height * 0.75f
                }
                else -> {
                    startY = height * 0.75f
                    endY = height * 0.25f
                }
            }

            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(startX, endY)

            val gestureDescription = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 80L, 200L))
                .build()

            dispatchGesture(gestureDescription, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform swipe gesture: $direction", e)
        }
    }

    /**
     * Perform custom gesture replaying.
     */
    fun performCustomGesture(gesture: GestureRecording) {
        try {
            val strokes = parseStrokesFromJson(gesture.strokesJson)
            if (strokes.isEmpty()) {
                Log.w(TAG, "Custom gesture has no strokes: ${gesture.name}")
                return
            }

            val builder = GestureDescription.Builder()
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            for (stroke in strokes) {
                if (stroke.points.size < 2) continue

                val path = Path()
                val firstPoint = stroke.points.first()
                path.moveTo(
                    firstPoint.x * width,
                    firstPoint.y * height
                )

                for (i in 1 until stroke.points.size) {
                    val pt = stroke.points[i]
                    path.lineTo(
                        pt.x * width,
                        pt.y * height
                    )
                }

                builder.addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        stroke.startTimeOffset,
                        stroke.durationMs
                    )
                )
            }

            dispatchGesture(builder.build(), null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform custom gesture: ${gesture.name}", e)
        }
    }

    /**
     * Traverses the view hierarchy to find a scrollable node and scrolls it programmatically.
     */
    private fun performSmartScroll(): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollableNode = findScrollableNode(root)
        return if (scrollableNode != null) {
            val success = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollableNode.recycle()
            success
        } else {
            false
        }
    }

    /**
     * Depth-first search for a scrollable node in the Accessibility hierarchy.
     */
    private fun findScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val scrollable = findScrollableNode(child)
            child.recycle()
            if (scrollable != null) {
                return scrollable
            }
        }
        return null
    }

    /**
     * Get the currently focused app package name.
     */
    fun getForegroundPackageName(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windows.firstOrNull { window ->
                    window.root?.let { root ->
                        root.packageName?.toString()?.isNotEmpty() == true
                    } ?: false
                }?.root?.let { it.packageName?.toString() }
            } else {
                @Suppress("DEPRECATION")
                rootInActiveWindow?.packageName?.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseStrokesFromJson(json: String): List<GestureStroke> {
        try {
            val root = JSONArray(json)
            val strokes = mutableListOf<GestureStroke>()
            for (i in 0 until root.length()) {
                val strokeObj = root.getJSONObject(i)
                val startTime = strokeObj.getLong("startTimeOffset")
                val duration = strokeObj.getLong("durationMs")
                val pointsArray = strokeObj.getJSONArray("points")
                val points = mutableListOf<GesturePoint>()
                for (j in 0 until pointsArray.length()) {
                    val ptObj = pointsArray.getJSONObject(j)
                    points.add(
                        GesturePoint(
                            x = ptObj.getDouble("x").toFloat(),
                            y = ptObj.getDouble("y").toFloat(),
                            timeOffsetMs = ptObj.getLong("timeOffsetMs")
                        )
                    )
                }
                strokes.add(GestureStroke(points, startTime, duration))
            }
            return strokes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse gesture JSON", e)
            return emptyList()
        }
    }

    companion object {
        private const val TAG = "AutoScrollAccessibility"
        @Volatile
        var instance: AutoScrollAccessibilityService? = null
    }
}
