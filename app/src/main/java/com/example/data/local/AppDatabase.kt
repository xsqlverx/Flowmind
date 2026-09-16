package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ReminderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ReminderItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return getDatabase(context, CoroutineScope(Dispatchers.IO))
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flowmind_reminders.db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialRealData(database.reminderDao())
                    }
                }
            }

            private suspend fun populateInitialRealData(dao: ReminderDao) {
                val now = System.currentTimeMillis()
                val oneHour = 3600 * 1000L
                val oneDay = 24 * oneHour

                val initialReminders = listOf(
                    // Active Queue Items (Home & Upcoming)
                    ReminderItem(
                        title = "Review pitch deck",
                        notes = "Review pitch deck with design leads and prepare keynote slides.",
                        category = "Design",
                        tag = "#design",
                        priority = "NORMAL",
                        dueDateMillis = now + (2 * oneHour),
                        dueTimeString = "5:00 PM Today",
                        isCompleted = false
                    ),
                    ReminderItem(
                        title = "Pickup dry cleaning",
                        notes = "Pick up suits before store closes at 7pm.",
                        category = "Personal",
                        tag = "#personal",
                        priority = "NORMAL",
                        dueDateMillis = now + (3 * oneHour),
                        dueTimeString = "6:30 PM Today",
                        isCompleted = false
                    ),
                    ReminderItem(
                        title = "Submit Q4 budget review with Sarah",
                        notes = "Finalize forecast spreadsheet and send deck 30 min before sync.",
                        category = "Finance",
                        tag = "#finance",
                        priority = "HIGH",
                        dueDateMillis = now + oneDay + (5 * oneHour),
                        dueTimeString = "Tomorrow · 3:30 PM",
                        participant = "Sarah",
                        isCompleted = false
                    ),
                    ReminderItem(
                        title = "Cardio workout & stretch session",
                        notes = "35 min interval run, followed by hip flexibility drills.",
                        category = "Health",
                        tag = "#health",
                        priority = "NORMAL",
                        dueDateMillis = now + oneDay - (4 * oneHour),
                        dueTimeString = "Tomorrow · 7:00 AM",
                        recurrence = "WEEKDAYS",
                        isCompleted = false
                    ),
                    ReminderItem(
                        title = "Dentist appointment with Dr. Aris",
                        notes = "Routine cleaning + update orthodontic scan files.",
                        category = "Health",
                        tag = "#personal",
                        priority = "NORMAL",
                        dueDateMillis = now + (3 * oneDay),
                        dueTimeString = "Mon, Oct 28 · 10:00 AM",
                        participant = "Dr. Aris",
                        location = "Downtown Dental",
                        isCompleted = false
                    ),
                    ReminderItem(
                        title = "Renew server SSL certificate",
                        notes = "Primary production cluster wildcard cert on Cloudflare.",
                        category = "Dev",
                        tag = "#dev",
                        priority = "NORMAL",
                        dueDateMillis = now + (6 * oneDay),
                        dueTimeString = "Oct 31 · 11:59 PM",
                        isCompleted = false
                    ),
                    ReminderItem(
                        title = "Call insurance agent regarding claim",
                        notes = "Policy renewal quote validation and auto insurance addendum.",
                        category = "Finance",
                        tag = "#finance",
                        priority = "NORMAL",
                        dueDateMillis = now + (4 * oneDay),
                        dueTimeString = "Tue, Oct 29 · 2:00 PM",
                        isCompleted = false
                    ),
                    ReminderItem(
                        title = "System backup validation & audit",
                        notes = "Verify encrypted Room SQLite snapshot integrity on device storage.",
                        category = "Dev",
                        tag = "#dev",
                        priority = "NORMAL",
                        dueDateMillis = now + (5 * oneDay),
                        dueTimeString = "Wed, Oct 30 · 4:15 PM",
                        isCompleted = false
                    ),

                    // Completed Journal Items (Completed Screen)
                    ReminderItem(
                        title = "Send invoice to Apex Studio",
                        notes = "Sent invoice #4920 via Stripe invoicing portal.",
                        category = "Finance",
                        tag = "#finance",
                        priority = "NORMAL",
                        dueDateMillis = now - (3 * oneHour),
                        dueTimeString = "2:15 PM",
                        isCompleted = true,
                        completedAtMillis = now - (3 * oneHour)
                    ),
                    ReminderItem(
                        title = "Team standup sprint alignment",
                        notes = "Reviewed Q4 engineering goals and unblocked migration tickets.",
                        category = "Work",
                        tag = "#work",
                        priority = "NORMAL",
                        dueDateMillis = now - (6 * oneHour),
                        dueTimeString = "10:30 AM",
                        isCompleted = true,
                        completedAtMillis = now - (6 * oneHour)
                    ),
                    ReminderItem(
                        title = "Order air purifier replacement filters",
                        notes = "Ordered HEPA 13 replacement set from supplier.",
                        category = "Personal",
                        tag = "#personal",
                        priority = "NORMAL",
                        dueDateMillis = now - oneDay,
                        dueTimeString = "8:40 PM",
                        isCompleted = true,
                        completedAtMillis = now - oneDay
                    ),
                    ReminderItem(
                        title = "Deploy API rate limiter middleware",
                        notes = "Merged and verified PR #184 token bucket rate limiting on staging.",
                        category = "Dev",
                        tag = "#engineering",
                        priority = "HIGH",
                        dueDateMillis = now - oneDay - (3 * oneHour),
                        dueTimeString = "5:12 PM",
                        isCompleted = true,
                        completedAtMillis = now - oneDay - (3 * oneHour)
                    )
                )

                dao.insertReminders(initialReminders)
            }
        }
    }
}
