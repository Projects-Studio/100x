package com.hundredx.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    fun duration(ms: Long): String {
        val totalMin = ms / 60_000L
        val d = totalMin / (60 * 24)
        val h = (totalMin / 60) % 24
        val m = totalMin % 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m"
            else -> "under a minute"
        }
    }

    fun date(ms: Long): String = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(ms))
    fun time(ms: Long): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    fun dateTime(ms: Long): String = "${date(ms)}, ${time(ms)}"
}
