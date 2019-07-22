package app.spidy.idm.utils

import java.net.MalformedURLException
import java.net.URL

class FileHandler {
    private val extensions = hashMapOf(
        "audio/aac" to "acc",
        "application/x-abiword" to "abw",
        "application/x-freearc" to "arc",
        "video/x-msvideo" to "avi",
        "application/vnd.amazon.ebook" to "azw",
        "application/octet-stream" to "bin",
        "image/bmp" to "bmp",
        "application/x-bzip2" to "bz2",
        "application/x-bzip" to "bz",
        "application/x-csh" to "csh",
        "text/html" to "html",
        "text/css" to "css",
        "text/csv" to "csv",
        "application/msword" to "doc",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
        "application/vnd.ms-fontobject" to "eot",
        "application/epub+zip" to "epub",
        "image/gif" to "gif",
        "image/vnd.microsoft.icon" to "ico",
        "text/calendar" to "ics",
        "application/java-archive" to "jar",
        "image/jpeg" to "jpeg",
        "application/ld+json" to "jsonld",
        "audio/x-midi" to "midi",
        "audio/mpeg" to "mp3",
        "video/mpeg" to "mpeg",
        "application/vnd.apple.installer+xml" to "mpkg",
        "application/vnd.oasis.opendocument.presentation" to "odp",
        "application/vnd.oasis.opendocument.spreadsheet" to "ods",
        "application/vnd.oasis.opendocument.text" to "odt",
        "audio/ogg" to "oga",
        "video/ogg" to "ogv",
        "application/ogg" to "ogx",
        "font/otf" to "otf",
        "image/png" to "png",
        "application/pdf" to "pdf",
        "application/vnd.ms-powerpoint" to "ppt",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" to "pptx",
        "application/x-rar-compressed" to "rar",
        "application/rtf" to "rtf",
        "application/x-sh" to "sh",
        "image/svg+xml" to "svg",
        "application/x-shockwave-flash" to "swf",
        "application/x-tar" to "tar",
        "image/tiff" to "tiff",
        "font/ttf" to "ttf",
        "text/plain" to "txt",
        "application/vnd.visio" to "vsd",
        "text/javascript" to "js",
        "application/json" to "json",
        "audio/wav" to "wav",
        "audio/webm" to "weba",
        "video/webm" to "webm",
        "image/webp" to "webp",
        "font/woff2" to "woff2",
        "font/woff" to "woff",
        "application/xhtml+xml" to "xhtml",
        "application/vnd.ms-excel" to "xls",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx",
        "application/xml" to "xml",
        "text/xml" to "xml",
        "application/vnd.mozilla.xul+xml" to "xul",
        "application/zip" to "zip",
        "video/3gpp" to "3gp",
        "audio/3gpp" to "3gp",
        "video/3gpp2" to "3g2",
        "audio/3gpp2" to "3g2",
        "application/x-7z-compressed" to "7z"
    )


    fun getFileNameFromURL(url: String):String {
        try {
            val resource = URL(url)
            val host = resource.host
            if (host.isNotEmpty() && url.endsWith(host))
            {
                return "download"
            }
        } catch (e: MalformedURLException) {
            return ""
        }
        val startIndex = url.lastIndexOf('/') + 1
        val length = url.length
        var lastQMPos = url.lastIndexOf('?')
        if (lastQMPos == -1)
        {
            lastQMPos = length
        }
        var lastHashPos = url.lastIndexOf('#')
        if (lastHashPos == -1)
        {
            lastHashPos = length
        }
        val endIndex = Math.min(lastQMPos, lastHashPos)
        return url.substring(startIndex, endIndex)
    }

    fun getExtensionFromMimeType(mimeType: String?): String? {
        if (mimeType == null) return null

        var ext: String? = null
        for ((key, value) in extensions) {
            if (mimeType.startsWith(key)) {
                ext = value
                break
            }
        }
        return ext
    }
}