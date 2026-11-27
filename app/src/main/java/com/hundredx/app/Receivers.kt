package com.hundredx.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires every N minutes for an open task. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Scheduler.EXTRA_TASK_ID, -1)
        val task = TaskStore.get(context, id) ?: run {
            Scheduler.cancel(context, id)
            return
        }
        if (task.done) {
            Scheduler.cancel(context, id)
            return
        }
        Notifier.show(context, task)
        Scheduler.schedule(context, task)
    }
}

/** "Mark done" button on the notification. */
class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Scheduler.EXTRA_TASK_ID, -1)
        val task = TaskStore.get(context, id) ?: return
        TaskStore.update(context, task.copy(done = true, completedAt = System.currentTimeMillis()))
        Scheduler.cancel(context, id)
        Notifier.cancel(context, id)
    }
}

/** Alarms are wiped on reboot and app updates; put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.rescheduleAll(context)
    }
}
