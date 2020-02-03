package app.spidy.idm.controllers

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

fun formatBytes(bytes: Long, isSpeed: Boolean = false): String {
    val unit = if (isSpeed) 1000.0 else 1024.0
    return when {
        bytes < unit * unit -> "${((bytes / unit).toFloat() * 100.0).roundToInt() / 100.0}KB"
        bytes < (unit.pow(2.0) * 1000) -> "${((bytes / unit.pow(2.0)).toFloat() * 100.0).roundToInt() / 100.0}MB"
        else -> "${((bytes / unit.pow(3.0)).toFloat() * 100.0).roundToInt() / 100.0}GB"
    }
}


fun secsToTime(secs: Long): String {
    val hours = floor(secs / 3600.0)
    val minutes = floor((secs % 3600.0) / 60.0)
    val seconds = secs % 60
    var time = ""

    if (hours > 0) {
        time += "${hours.toInt()}h "
    }

    if (minutes > 0) {
        time += "${minutes.toInt()}min "
    }

    if (seconds > 0) {
        time += "${seconds.toInt()}sec"
    }
    return time
}