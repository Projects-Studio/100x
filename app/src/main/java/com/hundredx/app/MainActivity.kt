package com.hundredx.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class PermState(
    val notifications: Boolean = true,
    val exactAlarms: Boolean = true,
    val battery: Boolean = true,
) {
    val allOk: Boolean get() = notifications && exactAlarms && battery
}

class MainActivity : ComponentActivity() {

    private val tasks = mutableStateOf<List<Task>>(emptyList())
    private val perms = mutableStateOf(PermState())

    private val storeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload() }

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPerms() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannel(this)
        Scheduler.rescheduleAll(this)
        reload()
        refreshPerms()

        if (Build.VERSION.SDK_INT >= 33 && !perms.value.notifications) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            HundredXTheme {
                MainScreen(
                    tasks = tasks.value,
                    perms = perms.value,
                    onAdd = ::addTask,
                    onToggleDone = ::toggleDone,
                    onDelete = ::deleteTask,
                    onFixNotifications = ::fixNotifications,
                    onFixExact = ::fixExactAlarms,
                    onFixBattery = ::fixBattery,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        TaskStore.prefs(this).registerOnSharedPreferenceChangeListener(storeListener)
        reload()
        refreshPerms()
    }

    override fun onStop() {
        TaskStore.prefs(this).unregisterOnSharedPreferenceChangeListener(storeListener)
        super.onStop()
    }

    private fun reload() {
        tasks.value = TaskStore.all(this)
    }

    private fun addTask(title: String, deadline: Long, interval: Int, nagBefore: Boolean) {
        val task = TaskStore.add(this, title, deadline, interval, nagBefore)
        Scheduler.schedule(this, task)
        reload()
    }

    private fun toggleDone(task: Task) {
        if (task.done) {
            val reopened = task.copy(done = false, completedAt = null)
            TaskStore.update(this, reopened)
            Scheduler.schedule(this, reopened)
        } else {
            TaskStore.update(this, task.copy(done = true, completedAt = System.currentTimeMillis()))
            Scheduler.cancel(this, task.id)
            Notifier.cancel(this, task.id)
        }
        reload()
    }

    private fun deleteTask(task: Task) {
        TaskStore.delete(this, task.id)
        Scheduler.cancel(this, task.id)
        Notifier.cancel(this, task.id)
        reload()
    }

    private fun refreshPerms() {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val notifications = granted && NotificationManagerCompat.from(this).areNotificationsEnabled()
        val exact = Build.VERSION.SDK_INT < 31 ||
            (getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: true)
        val battery = getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(packageName) ?: true
        perms.value = PermState(notifications, exact, battery)
    }

    private fun fixNotifications() {
        openSettings(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    private fun fixExactAlarms() {
        if (Build.VERSION.SDK_INT >= 31) {
            openSettings(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
        }
    }

    private fun fixBattery() {
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        if (!openSettings(direct)) {
            openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openSettings(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}
