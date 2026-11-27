package com.hundredx.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Task(
    val id: Int,
    val title: String,
    val deadline: Long,
    val intervalMinutes: Int,
    /** true = nag from the moment the task is created; false = only once the deadline passes */
    val nagBeforeDeadline: Boolean,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

/** Everything lives on the phone in private app storage. No server, no account. */
object TaskStore {
    private const val PREFS = "hundredx_tasks"
    private const val KEY_TASKS = "tasks"
    private const val KEY_NEXT_ID = "next_id"

    fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun all(ctx: Context): List<Task> {
        val raw = prefs(ctx).getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Task(
                    id = o.getInt("id"),
                    title = o.getString("title"),
                    deadline = o.getLong("deadline"),
                    intervalMinutes = o.optInt("interval", 10),
                    nagBeforeDeadline = o.optBoolean("nagBefore", true),
                    done = o.optBoolean("done", false),
                    createdAt = o.optLong("createdAt", 0L),
                    completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun saveAll(ctx: Context, tasks: List<Task>) {
        val arr = JSONArray()
        tasks.forEach { t ->
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("deadline", t.deadline)
            o.put("interval", t.intervalMinutes)
            o.put("nagBefore", t.nagBeforeDeadline)
            o.put("done", t.done)
            o.put("createdAt", t.createdAt)
            if (t.completedAt != null) o.put("completedAt", t.completedAt)
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY_TASKS, arr.toString()).commit()
    }

    @Synchronized
    fun get(ctx: Context, id: Int): Task? = all(ctx).firstOrNull { it.id == id }

    @Synchronized
    fun add(ctx: Context, title: String, deadline: Long, intervalMinutes: Int, nagBefore: Boolean): Task {
        val p = prefs(ctx)
        val id = p.getInt(KEY_NEXT_ID, 1)
        p.edit().putInt(KEY_NEXT_ID, id + 1).commit()
        val task = Task(id, title, deadline, intervalMinutes, nagBefore)
        saveAll(ctx, all(ctx) + task)
        return task
    }

    @Synchronized
    fun update(ctx: Context, task: Task) {
        saveAll(ctx, all(ctx).map { if (it.id == task.id) task else it })
    }

    @Synchronized
    fun delete(ctx: Context, id: Int) {
        saveAll(ctx, all(ctx).filterNot { it.id == id })
    }
}
