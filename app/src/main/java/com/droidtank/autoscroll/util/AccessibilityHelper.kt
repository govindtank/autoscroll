package com.droidtank.autoscroll.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.droidtank.autoscroll.AutoScrollAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AccessibilityHelper {
    private const val PREFS_NAME = "autoscroll_accessibility_prefs"
    private const val KEY_ACCEPTED_DISCLOSURE = "has_accepted_accessibility_disclosure"

    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    fun updateServiceState(context: Context) {
        _isServiceEnabled.value = isServiceEnabled(context)
    }

    fun isServiceEnabled(context: Context): Boolean {
        // Fast path: active service instance in memory
        if (AutoScrollAccessibilityService.instance != null) {
            return true
        }

        // System Settings verification
        val expectedComponentName = ComponentName(context, AutoScrollAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null) {
                if (enabledComponent == expectedComponentName) {
                    return true
                }
                if (enabledComponent.packageName == context.packageName &&
                    enabledComponent.className.contains("AutoScrollAccessibilityService")
                ) {
                    return true
                }
            }
        }
        return false
    }

    fun hasAcceptedDisclosure(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ACCEPTED_DISCLOSURE, false)
    }

    fun setAcceptedDisclosure(context: Context, accepted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ACCEPTED_DISCLOSURE, accepted).apply()
    }
}
