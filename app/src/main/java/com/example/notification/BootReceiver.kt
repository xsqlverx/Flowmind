package com.example.notification

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RECOVERY_ACTIONS) return
        val result = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000) { ReminderNotificationScheduler.rescheduleAllActiveReminders(appContext) }
            } catch (error: Exception) {
                Log.e("FlowMindRecovery", "Couldn't restore reminder alarms", error)
            } finally { result.finish() }
        }
    }
    companion object {
        val RECOVERY_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED, "android.intent.action.QUICKBOOT_POWERON")
    }
}
