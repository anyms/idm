package app.spidy.idmexample

import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import app.spidy.hiper.Hiper
import app.spidy.idm.Detector
import app.spidy.idm.Idm
import app.spidy.idm.data.Detect
import app.spidy.idm.interfaces.DetectListener
import app.spidy.kookaburra.controllers.Browser

class BrowserListener(private val idm: Idm): Browser.Listener {
    private val cookieManager = CookieManager.getInstance()
    private var cookies = HashMap<String, String>()
    private var pageUrl: String? = null
    private val urlValidator = UrlValidator()

    var tmp = false
    private val detectListener = object : DetectListener {
        override fun onDetect(detect: Detect) {
            if (!tmp) {
                Log.d("hello", "DETECTED: $detect")
                idm.download(detect)
                tmp = true
            }
        }
    }
    private val detector = Detector(detectListener)

    override fun shouldInterceptRequest(view: WebView, activity: FragmentActivity?, url: String, request: WebResourceRequest?) {
        if (urlValidator.validate(url)) {
            detector.detect(url, request?.requestHeaders, cookies, pageUrl, view, activity)
        }
    }

    override fun onNewUrl(view: WebView, url: String) {
        pageUrl = url
        cookies = HashMap()
        val cooks = cookieManager.getCookie(url)?.split(";")

        if (cooks != null) {
            for (cook in cooks) {
                val nodes = cook.trim().split("=")
                cookies[nodes[0].trim()] = nodes[1].trim()
            }
        }
    }

    override fun onNewDownload(
        view: WebView,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String,
        contentLength: Long
    ) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
        val detect = Detect(
            data = hashMapOf("url" to url, "filename" to fileName, "title" to view.title),
            cookies = cookies,
            requestHeaders = hashMapOf("user-agent" to userAgent),
            responseHeaders = hashMapOf(),
            type = Detect.TYPE_FILE,
            isResumable = false
        )
        idm.download(detect)
    }
}