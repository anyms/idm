package app.spidy.idm

import android.net.Uri
import android.webkit.MimeTypeMap
import app.spidy.fetcher.Fetcher
import app.spidy.idm.interfaces.IDMListener
import app.spidy.idm.models.Snapshot
import app.spidy.idm.utils.FileHandler
import app.spidy.idm.utils.UserAgent
import java.io.File
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

class IDM(val downloadLocation: File) {
    companion object {
        const val ERROR_UNABLE_TO_FETCH_HEADERS = 1
        const val ERROR_UNKNOWN_FILE_META_DATA = 2
        const val ERROR_UNABLE_TO_CHECK_RANGE = 3
    }

    val fetcher = Fetcher()
    val userAgent = UserAgent()
    var idmListener: IDMListener? = null
    private val fileHandler = FileHandler()

    fun getInstance(url: String): Downloader {
        return Downloader(this, url)
    }

    fun getInstance(snapshot: Snapshot): Downloader {
        return Downloader(this, snapshot)
    }


    fun guessFileName(rawUrl: String, mimeType: String?, contentDisposition: String?): String {
        val url = fetcher.urlDecode(rawUrl)
        if (contentDisposition != null) {
            val match = "filename=\"(.*?)\"".toRegex().find(contentDisposition)
            if (match != null) {
                return match.value.replace("filename=\"", "").replace("\"", "")
            }
        }

        val formattedUrl = url.split("?")[0]
        var fileName = fileHandler.getFileNameFromURL(formattedUrl)
        var ext = MimeTypeMap.getFileExtensionFromUrl(formattedUrl)
        if (ext == null || ext.toString() == "") ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (ext == null || ext.toString() == "") ext = fileHandler.getExtensionFromMimeType(mimeType)
        val arr = fileName.split(".").toMutableList()

        if (ext != null) {
            if (arr.size > 1) {
                arr[arr.lastIndex] = ext
            } else {
                arr.add(ext)
            }
        }
        if (arr.size > 1 && arr[0].trim() == "") arr[0] = "download"

        fileName = arr.joinToString(".")

        while (true) {
            fileName = Uri.decode(fileName)
            if (fileName.endsWith(".ts")) fileName = fileName.replace(".ts", ".mpeg")
            val f = File("${downloadLocation.absolutePath}${File.separator}$fileName")
            fileName = if (f.exists()) {
                val tmpArr = fileName.split(".").toMutableList()
                tmpArr.removeAt(tmpArr.lastIndex)
                val fileCountArr = tmpArr.joinToString(".").split("-")
                val num: Int? = fileCountArr.last().toIntOrNull()
                if (num == null) {
                    if (ext != null) "${fileCountArr[0]}-1.$ext" else "${fileCountArr[0]}-1"
                } else {
                    if (ext != null) "${fileCountArr[0]}-${num + 1}.$ext" else "${fileCountArr[0]}-${num + 1}"
                }
            } else {
                break
            }
        }

        return fileName
    }


    /**
     * Format given bytes to KB, MB or GB
     */
    fun formatBytes(bytes: Long, isSpeed: Boolean = false): String {
        val unit = if (isSpeed) 1000.0 else 1024.0
        return when {
            bytes < unit * unit -> "${((bytes / unit).toFloat() * 100.0).roundToInt() / 100.0}KB"
            bytes < (unit.pow(2.0) * 1000) -> "${((bytes / unit.pow(2.0)).toFloat() * 100.0).roundToInt() / 100.0}MB"
            else -> "${((bytes / unit.pow(3.0)).toFloat() * 100.0).roundToInt() / 100.0}GB"
        }
    }


    /**
     * Convert seconds to string version of time
     */
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
}