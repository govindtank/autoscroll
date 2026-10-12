package com.droidtank.autoscroll

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Auto-start on boot: re-enables auto-scroll if it was active before shutdown.
 * Launches the foreground service so scrolling can resume.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("autoscroll_prefs", Context.MODE_PRIVATE)
            val wasActive = prefs.getBoolean("was_active", false)
            val autoStart = prefs.getBoolean("auto_start", false)

            if (wasActive && autoStart) {
                val serviceIntent = Intent(context, AutoScrollService::class.java).apply {
                    putExtra("auto_start", true)
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
