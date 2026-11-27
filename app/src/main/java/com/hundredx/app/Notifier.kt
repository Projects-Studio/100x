package com.hundredx.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifier {
    const val CHANNEL_ID = "hundredx_reminders"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Repeating nudges until a task is marked done"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun show(ctx: Context, task: Task) {
        ensureChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val now = System.currentTimeMillis()
        val status = if (now >= task.deadline) {
            "Overdue by ${Format.duration(now - task.deadline)}"
        } else {
            "Due in ${Format.duration(task.deadline - now)}"
        }
        val quote = Quotes.random()

        val openApp = PendingIntent.getActivity(
            ctx, task.id,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markDone = PendingIntent.getBroadcast(
            ctx, 1_000_000 + task.id,
            Intent(ctx, DoneReceiver::class.java).putExtra(Scheduler.EXTRA_TASK_ID, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setColor(0xFFFFD23F.toInt())
            .setContentTitle(task.title)
            .setContentText("$status — $quote")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$status\n\n“$quote”"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(0, "Mark done", markDone)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(task.id, notification)
        } catch (e: SecurityException) {
            // Notification permission was revoked; nothing to show.
        }
    }

    fun cancel(ctx: Context, id: Int) {
        NotificationManagerCompat.from(ctx).cancel(id)
    }
}
