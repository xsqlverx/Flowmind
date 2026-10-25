package com.example.notification

import android.content.Context

/** Delivery state survives process death without changing the user's reminder schema. */
class NotificationState(context: Context) {
    private val prefs = context.getSharedPreferences("flowmind_notification_state", Context.MODE_PRIVATE)

    fun wasDelivered(id: Long, dueMillis: Long) = prefs.contains("delivered:$id") && prefs.getLong("delivered:$id", 0) == dueMillis
    fun markDelivered(id: Long, dueMillis: Long) = prefs.edit().putLong("delivered:$id", dueMillis).commit()
    fun isSnoozed(id: Long, dueMillis: Long) = prefs.contains("snoozed:$id") && prefs.getLong("snoozed:$id", 0) == dueMillis
    fun markSnoozed(id: Long, dueMillis: Long) = prefs.edit().putLong("snoozed:$id", dueMillis).commit()
    fun clear(id: Long) = prefs.edit().remove("delivered:$id").remove("snoozed:$id").commit()
}
