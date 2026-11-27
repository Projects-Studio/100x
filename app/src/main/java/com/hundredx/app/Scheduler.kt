package com.hundredx.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * One repeating alarm per open task. Each time it fires, ReminderReceiver shows a
 * notification and books the next one, until the task is marked done.
 */
object Scheduler {
    const val EXTRA_TASK_ID = "task_id"
    private const val ALARM_PREFS = "hundredx_alarms"

    private fun alarmPrefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE)

    private fun key(id: Int) = "next_$id"

    private fun pendingIntent(ctx: Context, id: Int, flags: Int): PendingIntent? {
        val intent = Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_TASK_ID, id)
        return PendingIntent.getBroadcast(ctx, id, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    /** When should the next reminder for this task go off? */
    fun nextFireTime(task: Task, now: Long): Long {
        val intervalMs = task.intervalMinutes.coerceAtLeast(1) * 60_000L
        val beforeDeadline = task.deadline - now > 5_000L
        return if (beforeDeadline) {
            // Always ping exactly at the deadline; before that, only if the task nags early.
            if (task.nagBeforeDeadline) minOf(now + intervalMs, task.deadline) else task.deadline
        } else {
            now + intervalMs
        }
    }

    /**
     * @param keepExisting reuse the already-booked time if there is one (used on app start / reboot
     * so opening the app doesn't push reminders back).
     */
    fun schedule(ctx: Context, task: Task, keepExisting: Boolean = false) {
        if (task.done) {
            cancel(ctx, task.id)
            return
        }
        val now = System.currentTimeMillis()
        val stored = alarmPrefs(ctx).getLong(key(task.id), 0L)
        val at = when {
            keepExisting && stored > now -> stored
            keepExisting && stored > 0L -> now + 30_000L // a reminder was missed (phone off) → fire soon
            else -> nextFireTime(task, now)
        }
        val pi = pendingIntent(ctx, task.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
        alarmPrefs(ctx).edit().putLong(key(task.id), at).commit()
    }

    fun cancel(ctx: Context, id: Int) {
        val pi = pendingIntent(ctx, id, PendingIntent.FLAG_NO_CREATE)
        if (pi != null) {
            ctx.getSystemService(AlarmManager::class.java)?.cancel(pi)
            pi.cancel()
        }
        alarmPrefs(ctx).edit().remove(key(id)).commit()
    }

    fun rescheduleAll(ctx: Context) {
        TaskStore.all(ctx).forEach { t ->
            if (t.done) cancel(ctx, t.id) else schedule(ctx, t, keepExisting = true)
        }
    }
}
