package app.spidy.idm.detectors

import android.util.Log
import android.webkit.MimeTypeMap
import app.spidy.hiper.data.HiperResponse
import app.spidy.idm.data.Detect
import app.spidy.idm.interfaces.DetectListener
import app.spidy.idm.utils.StringUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class VideoDetector(private val detectListener: DetectListener) {
    private val detectedUrls = Collections.synchronizedList(ArrayList<String>())

    fun run(url: String, title: String, response: HiperResponse, headers: HashMap<String, Any>, cookies: HashMap<String, String>) {
        if (!detectedUrls.contains(url)) {
            detectedUrls.add(url)
            detectListener.onDetect(Detect(
                data = hashMapOf("url" to url, "title" to title, "filename" to
                        getFileName(title, url, response.headers.get("content-type"))),
                cookies = cookies,
                requestHeaders = headers,
                responseHeaders = response.headers.toHashMap(),
                type = Detect.TYPE_VIDEO,
                isResumable = response.statusCode == 206
            ))
        }
    }

    fun isIn(url: String): Boolean {
        return detectedUrls.contains(url)
    }

    private fun getFileName(title: String, url: String, mimetype: String?): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        val singleton = MimeTypeMap.getSingleton()
        val ext = singleton.getExtensionFromMimeType(mimetype)
        if (ext != null) {
            return "${StringUtil.slugify(title)}_${name}_${StringUtil.randomUUID()}.$ext"
        }
        return "${StringUtil.slugify(title)}_$name"
    }

    fun clear() {
        detectedUrls.clear()
    }
}