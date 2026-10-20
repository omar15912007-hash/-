package com.nabdaldhikr.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nabdaldhikr.app.MainActivity
import com.nabdaldhikr.app.R

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderType = inputData.getString("reminder_type") ?: "morning"
        val text = inputData.getString("text")
            ?: if (reminderType == "morning") "لا تنسَ أذكار الصباح 🌅" else "لا تنسَ أذكار المساء 🌙"
        showNotification(reminderType, text)
        return Result.success()
    }

    private fun showNotification(type: String, text: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "azkar_reminders"
        val channel = NotificationChannel(
            channelId,
            "تذكيرات الأذكار",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تذكير بقراءة أذكار الصباح والمساء"
        }
        notificationManager.createNotificationChannel(channel)

        val isMorning = type == "morning"
        val notificationId = if (isMorning) 1001 else 1002
        val title = if (isMorning) "أذكار الصباح 🌅" else "أذكار المساء 🌙"

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, builder.build())
        }
    }
}