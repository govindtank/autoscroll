package com.droidtank.autoscroll.gesture

import android.accessibilityservice.AccessibilityService
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.droidtank.autoscroll.AutoScrollAccessibilityService
import com.droidtank.autoscroll.model.GesturePoint
import com.droidtank.autoscroll.model.GestureRecording
import com.droidtank.autoscroll.model.GestureStroke
import org.json.JSONArray
import org.json.JSONObject

/**
 * Records touch gestures for replay.
 *
 * Works by monitoring AccessibilityEvents for touch interactions on the screen.
 * When recording is active, every touch down/move/up is captured and stored as
 * relative coordinates (percentage of screen width/height) so gestures are
 * screen-size independent.
 *
 * Usage:
 *   val recorder = GestureRecorder(accessibilityService)
 *   recorder.startRecording("My Gesture")
 *   // ... user performs gesture ...
 *   val recording = recorder.stopRecording()
 */
class GestureRecorder(private val service: AutoScrollAccessibilityService) {

    companion object {
        private const val TAG = "GestureRecorder"
        private const val MAX_STROKES = 10
        private const val MAX_POINTS_PER_STROKE = 500
        private const val STROKE_TIMEOUT_MS = 500L // ms without touch = new stroke
    }

    private var isRecording = false
    private var currentStrokes = mutableListOf<GestureStroke>()
    private var currentPoints = mutableListOf<GesturePoint>()
    private var strokeStartTime: Long = 0L
    private var gestureStartTime: Long = 0L
    private var lastTouchTime: Long = 0L
    private var gestureName: String = ""
    private var screenWidthPx: Int = 0
    private var screenHeightPx: Int = 0

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var strokeTimeoutRunnable: Runnable? = null

    /** Whether a recording session is currently active. */
    fun isRecording(): Boolean = isRecording

    /**
     * Start recording a new gesture.
     * @param name display name for the gesture
     */
    fun startRecording(name: String) {
        if (isRecording) return
        isRecording = true
        gestureName = name
        currentStrokes.clear()
        currentPoints.clear()
        gestureStartTime = System.currentTimeMillis()
        strokeStartTime = 0L
        lastTouchTime = 0L

        // Capture screen dimensions
        try {
            val metrics = service.resources.displayMetrics
            screenWidthPx = metrics.widthPixels
            screenHeightPx = metrics.heightPixels
        } catch (e: Exception) {
            screenWidthPx = 1080
            screenHeightPx = 2400
        }

        Log.d(TAG, "Gesture recording started: $name")
    }

    /**
     * Stop recording and return the captured gesture.
     * @return the recorded GestureRecording, or null if no gesture was captured
     */
    fun stopRecording(): GestureRecording? {
        if (!isRecording) return null
        isRecording = false
        timeoutHandler.removeCallbacksAndMessages(null)

        // Finalize the current stroke if there are points
        if (currentPoints.size >= 2) {
            finalizeCurrentStroke()
        }

        if (currentStrokes.isEmpty()) {
            Log.w(TAG, "No strokes recorded")
            return null
        }

        val totalDuration = System.currentTimeMillis() - gestureStartTime
        val json = strokesToJson(currentStrokes)

        Log.d(TAG, "Recording stopped: ${currentStrokes.size} strokes, ${totalDuration}ms")

        return GestureRecording(
            name = gestureName,
            strokesJson = json,
            totalDurationMs = totalDuration,
            isLoopingGesture = false
        )
    }

    /**
     * Cancel the current recording without saving.
     */
    fun cancelRecording() {
        if (!isRecording) return
        isRecording = false
        timeoutHandler.removeCallbacksAndMessages(null)
        currentStrokes.clear()
        currentPoints.clear()
        Log.d(TAG, "Recording cancelled")
    }

    /**
     * Called by the AccessibilityService when a touch event is detected.
     * This should be invoked from the service's onAccessibilityEvent handler.
     */
    fun onTouchEvent(event: AccessibilityEvent) {
        if (!isRecording) return

        val sourceBounds = event.source?.let { src ->
            try {
                val bounds = android.graphics.Rect()
                src.getBoundsInScreen(bounds)
                bounds
            } catch (e: Exception) {
                null
            }
        }

        if (sourceBounds == null) return

        val now = System.currentTimeMillis()
        val relativeX = sourceBounds.centerX().toFloat() / screenWidthPx.coerceAtLeast(1)
        val relativeY = sourceBounds.centerY().toFloat() / screenHeightPx.coerceAtLeast(1)

        val eventType = event.eventType

        when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Treat clicks as a quick tap stroke
                if (currentPoints.size > 0) {
                    finalizeCurrentStroke()
                }
                val pt = GesturePoint(relativeX, relativeY, 0)
                val tapStroke = GestureStroke(
                    points = listOf(
                        GesturePoint(relativeX, relativeY, 0),
                        GesturePoint(relativeX, relativeY, 100)
                    ),
                    startTimeOffset = now - gestureStartTime,
                    durationMs = 100
                )
                currentStrokes.add(tapStroke)
                strokeStartTime = now
                lastTouchTime = now
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                // Touch / scroll detection — start of interaction or scroll event
                if (currentPoints.isEmpty()) {
                    strokeStartTime = now
                }
                currentPoints.add(GesturePoint(relativeX, relativeY, now - strokeStartTime))
                lastTouchTime = now

                // Reset stroke timeout
                strokeTimeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
                strokeTimeoutRunnable = Runnable {
                    if (currentPoints.size >= 2) {
                        finalizeCurrentStroke()
                    }
                }
                timeoutHandler.postDelayed(strokeTimeoutRunnable!!, STROKE_TIMEOUT_MS)
            }

            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                // Touch up — finalize the stroke
                if (currentPoints.size >= 2) {
                    finalizeCurrentStroke()
                }
            }
        }
    }

    /**
     * Record a manual path (for programmatic gesture creation).
     */
    fun recordManualPath(pathPoints: List<Pair<Float, Float>>, name: String) {
        gestureName = name
        currentStrokes.clear()
        currentPoints.clear()

        if (pathPoints.size < 2) return

        val points = pathPoints.mapIndexed { index, (x, y) ->
            GesturePoint(x / screenWidthPx.coerceAtLeast(1), y / screenHeightPx.coerceAtLeast(1), (index * 16).toLong())
        }

        val stroke = GestureStroke(
            points = points,
            startTimeOffset = 0L,
            durationMs = (points.size * 16L).coerceAtLeast(100L)
        )
        currentStrokes.add(stroke)
    }

    // ── Private Helpers ──

    private fun finalizeCurrentStroke() {
        if (currentPoints.size < 2) {
            currentPoints.clear()
            return
        }
        if (currentStrokes.size >= MAX_STROKES) {
            currentPoints.clear()
            return
        }
        val startOffset = currentStrokes.sumOf { it.startTimeOffset + it.durationMs }
        val duration = currentPoints.last().timeOffsetMs - currentPoints.first().timeOffsetMs
        currentStrokes.add(
            GestureStroke(
                points = currentPoints.toList(),
                startTimeOffset = startOffset,
                durationMs = duration.coerceAtLeast(50L)
            )
        )
        currentPoints.clear()
    }

    private fun strokesToJson(strokes: List<GestureStroke>): String {
        val root = JSONArray()
        for (stroke in strokes) {
            val strokeObj = JSONObject()
            strokeObj.put("startTimeOffset", stroke.startTimeOffset)
            strokeObj.put("durationMs", stroke.durationMs)
            val pointsArray = JSONArray()
            for (pt in stroke.points) {
                val ptObj = JSONObject()
                ptObj.put("x", pt.x.toDouble())
                ptObj.put("y", pt.y.toDouble())
                ptObj.put("timeOffsetMs", pt.timeOffsetMs)
                pointsArray.put(ptObj)
            }
            strokeObj.put("points", pointsArray)
            root.put(strokeObj)
        }
        return root.toString()
    }
}
