package app.spidy.idm.detectors

import android.webkit.MimeTypeMap
import app.spidy.idm.data.Detect
import app.spidy.idm.interfaces.DetectListener
import app.spidy.idm.utils.StringUtil

class FacebookVideoDetector(private val detectListener: DetectListener) {
    private val videoIds = ArrayList<String>()
    private val detects = ArrayList<Detect>()
    private val detectedUrls = ArrayList<String>()

    fun run(url: String, title: String, videoId: String, headers: HashMap<String, Any>, cookies: HashMap<String, String>) {
        if (!videoIds.contains(videoId)) {
            videoIds.add(videoId)
            detects.add(Detect(
                data = hashMapOf("id" to videoId, "title" to title, "filename" to
                        getFileName(title, url)),
                cookies = cookies,
                requestHeaders = headers,
                responseHeaders = hashMapOf(),
                type = Detect.TYPE_FACEBOOK,
                isResumable = false
            ))
            detectListener.onDetect(detects)
        }
    }

    fun isIn(url: String): Boolean {
        return detectedUrls.contains(url)
    }

    private fun getFileName(title: String, url: String): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        val ext = MimeTypeMap.getFileExtensionFromUrl(url)
        return "${StringUtil.slugify(title)}_$name.$ext"
    }
}