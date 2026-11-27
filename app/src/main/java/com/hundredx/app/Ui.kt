package com.hundredx.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

// Palette: deep indigo night, highlighter-yellow for action, hot pink for "you're late".
private val Ink = Color(0xFF161233)
private val Card = Color(0xFF221C47)
private val CardEdge = Color(0xFF332B66)
private val Volt = Color(0xFFFFD23F)
private val Late = Color(0xFFFF4F8B)
private val Mint = Color(0xFF3DDC97)
private val Haze = Color(0xFFA29FC4)

@Composable
fun HundredXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Volt,
            onPrimary = Ink,
            background = Ink,
            onBackground = Color.White,
            surface = Card,
            onSurface = Color.White,
            surfaceVariant = CardEdge,
            onSurfaceVariant = Haze,
            outline = CardEdge,
            error = Late,
        ),
        content = content
    )
}

@Composable
fun MainScreen(
    tasks: List<Task>,
    perms: PermState,
    onAdd: (String, Long, Int, Boolean) -> Unit,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onFixNotifications: () -> Unit,
    onFixExact: () -> Unit,
    onFixBattery: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }

    val pending = tasks.filter { !it.done }.sortedBy { it.deadline }
    val done = tasks.filter { it.done }.sortedByDescending { it.completedAt ?: 0L }
    val overdueCount = pending.count { it.deadline <= now }

    Scaffold(
        containerColor = Ink,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                containerColor = Volt,
                contentColor = Ink,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add task", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header(pending.size, overdueCount, done.size) }

            if (!perms.allOk) {
                item { SetupCard(perms, onFixNotifications, onFixExact, onFixBattery) }
            }

            if (pending.isEmpty()) {
                item { EmptyState() }
            }

            items(pending, key = { it.id }) { task ->
                TaskRow(task, now, onToggleDone, onDelete)
            }

            if (done.isNotEmpty()) {
                item {
                    TextButton(onClick = { showDone = !showDone }) {
                        Text(
                            if (showDone) "Hide finished (${done.size})" else "Show finished (${done.size})",
                            color = Haze
                        )
                    }
                }
                if (showDone) {
                    items(done, key = { "done-${it.id}" }) { task ->
                        TaskRow(task, now, onToggleDone, onDelete)
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onSave = { title, deadline, interval, nagBefore ->
                onAdd(title, deadline, interval, nagBefore)
                showAdd = false
            }
        )
    }
}

@Composable
private fun Header(pending: Int, overdue: Int, done: Int) {
    Column(Modifier.padding(top = 20.dp, bottom = 8.dp)) {
        Text(
            "100x",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            color = Volt,
            letterSpacing = (-3).sp,
            lineHeight = 64.sp
        )
        Spacer(Modifier.height(6.dp))
        val line = when {
            overdue > 0 -> "$overdue overdue. Clear those first."
            pending > 0 -> "$pending open, nothing late yet. Keep it that way."
            done > 0 -> "All clear. $done finished."
            else -> "Add a task and give it a deadline."
        }
        Text(line, color = if (overdue > 0) Late else Haze, fontSize = 16.sp)
    }
}

@Composable
private fun SetupCard(
    perms: PermState,
    onFixNotifications: () -> Unit,
    onFixExact: () -> Unit,
    onFixBattery: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Volt)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Reminders can’t reach you yet", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (!perms.notifications) {
            SetupLine("Notifications are off for 100x.", "Turn on", onFixNotifications)
        }
        if (!perms.exactAlarms) {
            SetupLine("Exact alarms aren’t allowed, so reminders may run late.", "Allow", onFixExact)
        }
        if (!perms.battery) {
            SetupLine("Battery saver may silence reminders when the screen is off.", "Unrestrict", onFixBattery)
        }
    }
}

@Composable
private fun SetupLine(text: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Volt),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) { Text(action, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nothing on the list.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap Add task. 100x will keep pinging you until it’s done.",
            color = Haze,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TaskRow(task: Task, now: Long, onToggle: (Task) -> Unit, onDelete: (Task) -> Unit) {
    val overdue = !task.done && now >= task.deadline
    val edge = when {
        task.done -> Card
        overdue -> Late
        else -> CardEdge
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .border(if (overdue) 2.dp else 1.dp, edge, RoundedCornerShape(18.dp))
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (task.done) Mint else Color.Transparent)
                .border(2.dp, if (task.done) Mint else Volt, CircleShape)
                .clickable { onToggle(task) },
            contentAlignment = Alignment.Center
        ) {
            if (task.done) {
                Icon(Icons.Filled.Check, contentDescription = "Reopen", tint = Ink, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                color = if (task.done) Haze else Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            val (status, color) = when {
                task.done -> "Finished ${Format.dateTime(task.completedAt ?: now)}" to Mint
                overdue -> "Overdue by ${Format.duration(now - task.deadline)}" to Late
                else -> "Due in ${Format.duration(task.deadline - now)} · ${Format.dateTime(task.deadline)}" to Volt
            }
            Text(status, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (!task.done) {
                Text(
                    if (task.nagBeforeDeadline) "Reminds every ${task.intervalMinutes} min until done"
                    else "Reminds every ${task.intervalMinutes} min once overdue",
                    color = Haze,
                    fontSize = 12.sp
                )
            }
        }
        IconButton(onClick = { confirmDelete = true }) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete task", tint = Haze)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Card,
            title = { Text("Delete this task?") },
            text = { Text("“${task.title}” and its reminders will be removed.", color = Haze) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(task) }) {
                    Text("Delete", color = Late, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep", color = Haze) }
            }
        )
    }
}

@Composable
private fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Volt else Color.Transparent)
            .border(1.dp, if (selected) Volt else CardEdge, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) Ink else Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

private fun defaultDeadline(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.HOUR_OF_DAY, 1)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun pickDate(context: Context, current: Long, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = current }
    DatePickerDialog(
        context,
        { _, y, m, d ->
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, d)
            onPicked(cal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun pickTime(context: Context, current: Long, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = current }
    TimePickerDialog(
        context,
        { _, h, min ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            onPicked(cal.timeInMillis)
        },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        DateFormat.is24HourFormat(context)
    ).show()
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, Long, Int, Boolean) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var deadline by remember { mutableLongStateOf(defaultDeadline()) }
    var interval by remember { mutableIntStateOf(10) }
    var custom by remember { mutableStateOf("") }
    var nagBefore by remember { mutableStateOf(true) }
    val presets = listOf(5, 10, 15, 30, 60)

    val customValue = custom.toIntOrNull()?.coerceIn(1, 1440)
    val effectiveInterval = customValue ?: interval

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Card,
        title = { Text("New task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What needs to get done?") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Deadline", color = Haze, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { pickDate(context, deadline) { deadline = it } },
                        modifier = Modifier.weight(1f)
                    ) { Text(Format.date(deadline), color = Color.White) }
                    OutlinedButton(
                        onClick = { pickTime(context, deadline) { deadline = it } },
                        modifier = Modifier.weight(1f)
                    ) { Text(Format.time(deadline), color = Color.White) }
                }

                Text("Remind me every", color = Haze, fontSize = 13.sp)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { m ->
                        Pill(
                            text = if (m == 60) "1 hr" else "$m min",
                            selected = customValue == null && interval == m,
                            onClick = { interval = m; custom = "" }
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { v -> custom = v.filter { it.isDigit() }.take(4) },
                    label = { Text("Or enter minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Start reminding", color = Haze, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Right away", nagBefore) { nagBefore = true }
                    Pill("After deadline", !nagBefore) { nagBefore = false }
                }
                Text(
                    if (nagBefore)
                        "You’ll get a reminder every $effectiveInterval min from now until you mark it done."
                    else
                        "Quiet until the deadline, then a reminder every $effectiveInterval min until you mark it done.",
                    color = Haze,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.trim(), deadline, effectiveInterval, nagBefore) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Volt, contentColor = Ink)
            ) { Text("Add task", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Haze) }
        }
    )
}
