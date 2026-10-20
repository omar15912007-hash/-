package com.nabdaldhikr.app.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    const val MORNING_WORK = "morning_azkar_reminder"
    const val EVENING_WORK = "evening_azkar_reminder"

    // وقت أذكار الصباح: بعد طلوع الفجر (الساعة 5:00 صباحاً)
    private const val MORNING_HOUR = 5
    private const val MORNING_MINUTE = 0

    // وقت أذكار المساء: بعد صلاة العصر (الساعة 4:00 عصراً)
    private const val EVENING_HOUR = 16
    private const val EVENING_MINUTE = 0

    fun schedule(context: Context, reminderEnabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!reminderEnabled) {
            workManager.cancelUniqueWork(MORNING_WORK)
            workManager.cancelUniqueWork(EVENING_WORK)
            return
        }

        schedulePeriodic(workManager, MORNING_WORK, "morning", MORNING_HOUR, MORNING_MINUTE, "morning_reminder")
        schedulePeriodic(workManager, EVENING_WORK, "evening", EVENING_HOUR, EVENING_MINUTE, "evening_reminder")
    }

    private fun schedulePeriodic(
        workManager: WorkManager,
        workName: String,
        type: String,
        targetHour: Int,
        targetMinute: Int,
        tag: String
    ) {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var next = now.withHour(targetHour).withMinute(targetMinute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        val initialDelayMin = java.time.Duration.between(now, next).toMinutes()

        workManager.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMin, TimeUnit.MINUTES)
                .addTag(tag)
                .setInputData(buildNotificationInput(type))
                .build()
        )
    }

    private fun buildNotificationInput(type: String): Data {
        val text = if (type == "morning") {
            "لا تنسَ أذكار الصباح\n\nمن قالها أجير من الجن حتى يمسي"
        } else {
            "لا تنسَ أذكار المساء\n\nمن قالها أجير من الجن حتى يصبح"
        }
        return Data.Builder()
            .putString("reminder_type", type)
            .putString("text", text)
            .build()
    }
}