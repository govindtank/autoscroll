package com.droidtank.autoscroll

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.droidtank.autoscroll.model.AppConfig
import com.droidtank.autoscroll.model.AutoScrollDatabase
import com.droidtank.autoscroll.model.GestureRecording
import com.droidtank.autoscroll.model.ScrollDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Core scroll controller with per-app configuration, multi-direction support,
 * and reactive state management. Uses in-memory caching to avoid DB queries
 * during hot scroll loops.
 */
class ScrollController private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ScrollController? = null

        fun getInstance(context: Context): ScrollController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScrollController(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ── State ──

    sealed class State {
        data object Idle : State()
        data class Scrolling(
            val count: Int,
            val packageName: String? = null,
            val appName: String? = null,
            val startTimeMs: Long = System.currentTimeMillis()
        ) : State()
        data object Paused : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _currentAppPackage = MutableStateFlow<String?>(null)
    val currentAppPackage: StateFlow<String?> = _currentAppPackage.asStateFlow()

    private val _currentAppName = MutableStateFlow<String?>(null)
    val currentAppName: StateFlow<String?> = _currentAppName.asStateFlow()

    private val _enabledAppsCount = MutableStateFlow(3)
    val enabledAppsCount: StateFlow<Int> = _enabledAppsCount.asStateFlow()

    private val _totalScrollsAllTime = MutableStateFlow(1280)
    val totalScrollsAllTime: StateFlow<Int> = _totalScrollsAllTime.asStateFlow()

    // ── In-memory caches (avoids DB on hot path) ──

    /** Cached per-app configs keyed by package name */
    private val appConfigCache = mutableMapOf<String, CachedAppConfig>()

    private data class CachedAppConfig(
        val direction: String,
        val alternatingInterval: Int,
        val gestureId: Long?
    )

    // ── Internals ──

    private val handler = Handler(Looper.getMainLooper())
    private var scrollRunnable: Runnable? = null
    private val prefs = context.getSharedPreferences("autoscroll_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val db = AutoScrollDatabase.getDatabase(context)

    // Scroll session tracking
    private var currentDirection = "UP"
    private var scrollsInCurrentDirection = 0
    private var currentAlternatingInterval = 5
    private var currentGestureId: Long? = null

    // ── Preferences (global fallbacks) ──

    var scrollSpeed: Long
        get() = prefs.getLong("scroll_speed", 20L).coerceIn(1, 120)
        set(value) = prefs.edit().putLong("scroll_speed", value.coerceIn(1, 120)).apply()

    var enableNotifications: Boolean
        get() = prefs.getBoolean("enable_notifications", true)
        set(value) = prefs.edit().putBoolean("enable_notifications", value).apply()

    var autoStart: Boolean
        get() = prefs.getBoolean("auto_start", false)
        set(value) = prefs.edit().putBoolean("auto_start", value).apply()

    var wasActiveBeforeShutdown: Boolean
        get() = prefs.getBoolean("was_active", false)
        set(value) = prefs.edit().putBoolean("was_active", value).apply()

    var globalDirection: String
        get() = prefs.getString("global_direction", "UP") ?: "UP"
        set(value) = prefs.edit().putString("global_direction", value).apply()

    var appTheme: String
        get() = prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString("app_theme", value).apply()

    private val _themeMode = MutableStateFlow(prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        appTheme = mode
        _themeMode.value = mode
    }

    // ── Initialization ──

    init {
        _totalScrollsAllTime.value = prefs.getInt("total_scrolls_all_time", 0)
        loadEnabledAppsCount()
    }

    private fun loadEnabledAppsCount() {
        scope.launch {
            db.appConfigDao().getEnabledConfigs().collect { configs ->
                _enabledAppsCount.value = configs.size
                // Update cache
                configs.forEach { config ->
                    appConfigCache[config.packageName] = CachedAppConfig(
                        direction = config.scrollDirection,
                        alternatingInterval = config.alternatingInterval,
                        gestureId = config.gestureRecordingId
                    )
                }
            }
        }
    }

    // ── App Configuration ──

    fun getConfigForAppSync(packageName: String): AppConfig? {
        return appConfigCache[packageName]?.let { cached ->
            AppConfig(
                packageName = packageName,
                appName = "",
                scrollDirection = cached.direction,
                alternatingInterval = cached.alternatingInterval,
                gestureRecordingId = cached.gestureId
            )
        }
    }

    suspend fun saveAppConfig(config: AppConfig) {
        db.appConfigDao().upsertConfig(config)
        // Update cache immediately
        appConfigCache[config.packageName] = CachedAppConfig(
            direction = config.scrollDirection,
            alternatingInterval = config.alternatingInterval,
            gestureId = config.gestureRecordingId
        )
    }

    /**
     * Called by AutoScrollAccessibilityService when an app comes to foreground.
     */
    fun onForegroundAppChanged(packageName: String) {
        val cached = appConfigCache[packageName]
        if (cached != null) {
            _currentAppPackage.value = packageName
        }
    }

    suspend fun enableApp(packageName: String, appName: String, speed: Int = 20, direction: String = "UP") {
        val existing = db.appConfigDao().getConfig(packageName)
        if (existing != null) {
            db.appConfigDao().upsertConfig(existing.copy(isEnabled = true))
        } else {
            db.appConfigDao().upsertConfig(
                AppConfig(
                    packageName = packageName,
                    appName = appName,
                    isEnabled = true,
                    scrollSpeed = speed,
                    scrollDirection = direction
                )
            )
        }
    }

    suspend fun disableApp(packageName: String) {
        db.appConfigDao().getConfig(packageName)?.let {
            db.appConfigDao().upsertConfig(it.copy(isEnabled = false))
        }
    }

    // ── Scrolling Control ──

    fun startScrolling(packageName: String? = null, appName: String? = null) {
        val currentState = _state.value
        if (currentState is State.Scrolling) return

        val currentCount = when (currentState) {
            is State.Scrolling -> currentState.count
            is State.Paused -> prefs.getInt("saved_scroll_count", 0)
            is State.Idle -> 0
        }

        // Load per-app config into memory for this session
        currentDirection = "UP"
        scrollsInCurrentDirection = 0
        currentGestureId = null
        currentAlternatingInterval = 5

        if (packageName != null) {
            val cached = appConfigCache[packageName]
            if (cached != null) {
                currentDirection = cached.direction
                currentAlternatingInterval = cached.alternatingInterval
                currentGestureId = cached.gestureId
            }
        }

        _state.value = State.Scrolling(currentCount, packageName, appName)
        _currentAppPackage.value = packageName
        _currentAppName.value = appName
        wasActiveBeforeShutdown = true
        scheduleNextScroll()
    }

    fun pauseScrolling() {
        val currentState = _state.value
        if (currentState is State.Scrolling) {
            prefs.edit().putInt("saved_scroll_count", currentState.count).apply()
        }
        cancelScheduledScroll()
        _state.value = State.Paused
    }

    fun stopScrolling() {
        cancelScheduledScroll()
        val currentState = _state.value
        if (currentState is State.Scrolling) {
            val total = prefs.getInt("total_scrolls_all_time", 0) + currentState.count
            prefs.edit().putInt("total_scrolls_all_time", total).apply()
            _totalScrollsAllTime.value = total
        }
        _state.value = State.Idle
        _currentAppPackage.value = null
        _currentAppName.value = null
        wasActiveBeforeShutdown = false
        prefs.edit().remove("saved_scroll_count").apply()
    }

    // ── Scheduling ──

    private fun cancelScheduledScroll() {
        scrollRunnable?.let { handler.removeCallbacks(it) }
        scrollRunnable = null
    }

    private fun scheduleNextScroll() {
        if (_state.value !is State.Scrolling) return

        val delayMs = if (scrollSpeed <= 0) 3000L else (60_000L / scrollSpeed)

        scrollRunnable = Runnable {
            val currentState = _state.value as? State.Scrolling
            if (currentState != null) {
                // Resolve direction (all in-memory, no DB hit)
                val direction = resolveDirection()

                // Dispatch the gesture
                val accessibilityService = AutoScrollAccessibilityService.instance
                if (accessibilityService != null) {
                    if (currentGestureId != null) {
                        // Load gesture from DB off the hot path (rare, once per session)
                        loadGestureFromDb(currentGestureId!!) { gesture ->
                            if (gesture != null) {
                                accessibilityService.performCustomGesture(gesture)
                            } else {
                                accessibilityService.performSwipe(direction)
                            }
                        }
                    } else {
                        accessibilityService.performSwipe(direction)
                    }
                }

                // Update state
                val newCount = currentState.count + 1
                scrollsInCurrentDirection++

                _state.value = State.Scrolling(
                    newCount,
                    currentState.packageName,
                    currentState.appName,
                    currentState.startTimeMs
                )

                // Notify activity
                AutoScrollActivity.activeInstance?.updateScrollCount(newCount)

                scheduleNextScroll()
            }
        }
        handler.postDelayed(scrollRunnable!!, delayMs)
    }

    /**
     * Load a gesture from the database asynchronously.
     * Uses a cached result for repeated calls with the same ID.
     */
    private val gestureCache = mutableMapOf<Long, GestureRecording?>()
    private var lastLoadedGestureId: Long? = null

    private fun loadGestureFromDb(id: Long, callback: (GestureRecording?) -> Unit) {
        if (lastLoadedGestureId == id) {
            // Use cached result
            callback(gestureCache[id])
            return
        }
        lastLoadedGestureId = id
        scope.launch {
            val gesture = db.gestureRecordingDao().getGesture(id)
            gestureCache[id] = gesture
            callback(gesture)
        }
    }

    /**
     * Resolve the effective scroll direction for the current scroll action.
     * All in-memory — no DB access on the hot path.
     */
    private fun resolveDirection(): String {
        val direction = try {
            ScrollDirection.valueOf(currentDirection.uppercase())
        } catch (e: Exception) {
            ScrollDirection.UP
        }

        return when (direction) {
            ScrollDirection.UP -> "UP"
            ScrollDirection.DOWN -> "DOWN"
            ScrollDirection.ALTERNATING, ScrollDirection.SMART -> {
                if (scrollsInCurrentDirection >= currentAlternatingInterval) {
                    scrollsInCurrentDirection = 0
                    if (currentDirection == "UP") "DOWN" else "UP"
                } else {
                    currentDirection
                }
            }
        }
    }

    // ── Stats ──

    fun getElapsedSeconds(): Long {
        val s = _state.value
        if (s is State.Scrolling) {
            return (System.currentTimeMillis() - s.startTimeMs) / 1000
        }
        return 0
    }

    fun getSessionCount(): Int = prefs.getInt("session_count", 0)
}
