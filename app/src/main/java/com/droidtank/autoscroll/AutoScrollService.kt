package com.droidtank.autoscroll

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps auto-scrolling alive in the background.
 * Provides media-style controls (Play, Pause, Stop) directly in the notification panel
 * and syncs reactively with the ScrollController singleton.
 */
class AutoScrollService : Service() {

    companion object {
        const val CHANNEL_ID = "autoscroll_channel"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "AutoScrollService"

        // Notification actions
        const val ACTION_PAUSE = "com.droidtank.autoscroll.ACTION_PAUSE"
        const val ACTION_RESUME = "com.droidtank.autoscroll.ACTION_RESUME"
        const val ACTION_STOP = "com.droidtank.autoscroll.ACTION_STOP"

        var isRunning = false
            private set
    }

    private lateinit var scrollController: ScrollController
    private var stateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true

        if (!::scrollController.isInitialized) {
            scrollController = ScrollController.getInstance(this)
        }

        // Handle action buttons from notification
        val action = intent?.action
        if (action != null) {
            when (action) {
                ACTION_PAUSE -> {
                    scrollController.pauseScrolling()
                }
                ACTION_RESUME -> {
                    val pkg = scrollController.currentAppPackage.value
                    val app = scrollController.currentAppName.value
                    scrollController.startScrolling(pkg, app)
                }
                ACTION_STOP -> {
                    scrollController.stopScrolling()
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        val autoStart = intent?.getBooleanExtra("auto_start", false) ?: false
        val appName = intent?.getStringExtra("app_name") ?: scrollController.currentAppName.value

        // Setup reactive observation of scroll state to update notification controls
        observeScrollControllerState()

        val state = scrollController.state.value
        val count = when (state) {
            is ScrollController.State.Scrolling -> state.count
            else -> 0
        }

        val notification = buildNotification(state, count, appName)
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (autoStart && state is ScrollController.State.Idle) {
            scrollController.startScrolling()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stateJob?.cancel()
    }

    private fun observeScrollControllerState() {
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            scrollController.state.collect { state ->
                val count = when (state) {
                    is ScrollController.State.Scrolling -> state.count
                    else -> 0
                }
                val appName = scrollController.currentAppName.value
                val notification = buildNotification(state, count, appName)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)

                if (state is ScrollController.State.Idle) {
                    stopSelf()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AutoScroll Pro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification for auto-scroll service"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(state: ScrollController.State, count: Int, appName: String? = null): Notification {
        val title = if (appName != null) "AutoScroll · $appName" else "AutoScroll Pro"
        val displayText = when (state) {
            is ScrollController.State.Scrolling -> "Scrolling active — $count scrolls"
            is ScrollController.State.Paused -> "Paused"
            else -> "Ready"
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AutoScrollActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(displayText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(state !is ScrollController.State.Idle)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Play/Pause toggle action
        if (state is ScrollController.State.Scrolling) {
            val pauseIntent = Intent(this, AutoScrollService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pendingPause = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pendingPause)
        } else if (state is ScrollController.State.Paused) {
            val resumeIntent = Intent(this, AutoScrollService::class.java).apply {
                action = ACTION_RESUME
            }
            val pendingResume = PendingIntent.getService(
                this,
                2,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", pendingResume)
        }

        // Stop Action
        if (state !is ScrollController.State.Idle) {
            val stopIntent = Intent(this, AutoScrollService::class.java).apply {
                action = ACTION_STOP
            }
            val pendingStop = PendingIntent.getService(
                this,
                3,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
        }

        return builder.build()
    }
}
