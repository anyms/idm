package app.spidy.idm.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File
import java.net.URLDecoder
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

object Formatter {
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

//    fun updateFileName(context: Context, fileName: String, incrementer: Int = 1): String {
//        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
//        val file = File("$dir${File.separator}$fileName")
//
//        if (file.exists()) {
//            return if (fileName.contains("(") && fileName.contains(")")) {
//                updateFileName(context, fileName.replace("\\([0-9]\\)".toRegex(), "($incrementer)"), incrementer+1)
//            } else if (fileName.contains(".")) {
//                val nodes = fileName.split(".").toMutableList()
//                val ext = nodes.removeAt(nodes.lastIndex)
//                val name = nodes.joinToString(".")
//                updateFileName(context, "$name ($incrementer).$ext", incrementer+1)
//            } else {
//                updateFileName(context, "$fileName ($incrementer)", incrementer+1)
//            }
//        }
//
//        return fileName
//    }
}