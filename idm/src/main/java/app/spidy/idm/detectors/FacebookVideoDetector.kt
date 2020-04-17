package app.spidy.idm.detectors

import android.webkit.MimeTypeMap
import app.spidy.idm.data.Detect
import app.spidy.idm.interfaces.DetectListener
import app.spidy.idm.utils.StringUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FacebookVideoDetector(private val detectListener: DetectListener) {
    private val videoIds = Collections.synchronizedList(ArrayList<String>())
    private val detectedUrls = Collections.synchronizedList(ArrayList<String>())

    fun run(url: String, title: String, videoId: String, headers: HashMap<String, Any>, cookies: HashMap<String, String>) {
        if (!videoIds.contains(videoId)) {
            videoIds.add(videoId)
            detectListener.onDetect(Detect(
                data = hashMapOf("id" to videoId, "title" to title, "filename" to
                        getFileName(title, url)),
                cookies = cookies,
                requestHeaders = headers,
                responseHeaders = hashMapOf(),
                type = Detect.TYPE_FACEBOOK,
                isResumable = false
            ))
        }
    }

    fun isIn(url: String): Boolean {
        return detectedUrls.contains(url)
    }

    private fun getFileName(title: String, url: String): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)
        return "${StringUtil.slugify(title)}_${name}_${StringUtil.randomUUID()}.$ext"
    }

    fun clear() {
        videoIds.clear()
        detectedUrls.clear()
    }
}