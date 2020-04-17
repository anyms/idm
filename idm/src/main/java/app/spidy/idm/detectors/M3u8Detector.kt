package app.spidy.idm.detectors

import app.spidy.hiper.Hiper
import app.spidy.idm.data.Detect
import app.spidy.idm.interfaces.DetectListener
import app.spidy.idm.utils.StringUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class M3u8Detector(private val detectListener: DetectListener) {
    private val detectedUrls = Collections.synchronizedList(ArrayList<String>())
    private val hiper = Hiper.getAsyncInstance()

    fun run(url: String, title: String, headers: HashMap<String, Any>, cookies: HashMap<String, String>) {
        if (!detectedUrls.contains(url)) {
            detectedUrls.add(url)
            hiper.get(url, headers = headers, cookies = cookies).then { response ->
                if (response.text != null && validateResponse(response.text!!)) {
                    detectedUrls.add(url)
                    detectListener.onDetect(Detect(
                        data = hashMapOf("url" to url, "title" to title, "filename" to getFileName(title, url)),
                        cookies = cookies,
                        requestHeaders = headers,
                        responseHeaders = hashMapOf(),
                        type = Detect.TYPE_STREAM,
                        isResumable = false
                    ))
                }
            }.catch()
        }
    }

    fun isIn(url: String): Boolean {
        return detectedUrls.contains(url)
    }

    private fun getFileName(title: String, url: String): String {
        val name = url.split("?")[0].split("#")[0].split("/").last().split(".")[0]
        return "${StringUtil.slugify(title)}_${name}_${StringUtil.randomUUID()}.mpg"
    }

    private fun validateResponse(text: String): Boolean {
        var isValid = true
        val lines = text.split("\n")
        for (line in lines) {
            if (!line.startsWith("#")) {
                val plainUrl = line.split("?")[0]

                if (plainUrl.endsWith(".m3u8")) {
                    isValid = false
                    break
                } else if (plainUrl.endsWith(".ts")) {
                    isValid = true
                    break
                }
            }
        }

        return isValid
    }

    fun clear() {
        detectedUrls.clear()
    }
}