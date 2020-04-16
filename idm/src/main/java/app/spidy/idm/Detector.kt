package app.spidy.idm

import android.net.Uri
import android.os.Handler
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import app.spidy.hiper.Hiper
import app.spidy.hiper.data.HiperResponse
import app.spidy.idm.data.Detect
import app.spidy.idm.detectors.*
import app.spidy.idm.interfaces.DetectListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class Detector(private val detectListener: DetectListener) {
    private val hiper = Hiper.getAsyncInstance()

    private val googleVideoDetector = GoogleVideoDetector(detectListener)
    private val facebookVideoDetector = FacebookVideoDetector(detectListener)
    private val videoDetector = VideoDetector(detectListener)
    private val audioDetector = AudioDetector(detectListener)
    private val m3u8Detector = M3u8Detector(detectListener)

    fun reset() {
        googleVideoDetector.clear()
        facebookVideoDetector.clear()
        videoDetector.clear()
        audioDetector.clear()
        m3u8Detector.clear()
    }

    fun detect(url: String, requestHeaders: Map<String, String>?,
               cookies: Map<String, String>, pageUrl: String?, view: WebView, activity: FragmentActivity?) {
        val headers = (requestHeaders as? HashMap<String, Any>) ?: hashMapOf()
        val cooks = cookies as HashMap<String, String>
        val plainUrl = url.split("://")[1].split("?")[0].split("/")[0]
        val uri = Uri.parse(url)

        if (videoDetector.isIn(url) || facebookVideoDetector.isIn(url) ||
            googleVideoDetector.isIn(uri.getQueryParameter("id")) || audioDetector.isIn(url) ||
            m3u8Detector.isIn(url)) {
            return
        }

        if (plainUrl.contains("fbcdn.net") && (plainUrl.contains("video.") || plainUrl.contains("/v/"))) {
            if (uri.getQueryParameter("bytestart") == "0") {
                activity?.runOnUiThread {
                    view.evaluateJavascript("""
                        (function() {
                            var video = document.querySelector("video");
                            var parent = video.parentNode;
                            var videoId = JSON.parse(parent.getAttribute("data-store"))["videoID"];
                            var mainParent = parent.parentNode;
                            var title = "";
                            for (var i = 0; i < 4; i++) {
                                if (mainParent.getAttribute("class") == "story_body_container") {
                                    title = mainParent.querySelector("strong").innerText;
                                    break;
                                }
                                mainParent = mainParent.parentNode;
                            }
                            return videoId + "," + title;
                        })();
                    """.trimIndent()) {
                        val ret = it.replace("\"", "")
                        if (ret != "null") {
                            val nodes = ret.split(",")
                            val videoId = nodes[0]
                            val title = if (nodes.last() == "null") view.title else nodes.last()
                            if (videoId != "null") {
                                verifyFacebook(url, title, videoId, headers, cooks)
                            }
                        }
                    }
                }
            }
        } else {
            headers["range"] = "bytes=0-"
            hiper.head(url, headers = headers, cookies = cooks).then { response ->
                if (response.isSuccessful) {
                    activity?.runOnUiThread {
                        val title = view.title
                        verify(url, title, response, headers, cooks)
                    }
                }
            }.catch { e ->
                Log.d("hello", "Err: $e")
            }
        }
    }

    private fun verifyFacebook(
        url: String,
        title: String,
        videoID: String,
        headers: HashMap<String, Any>,
        cooks: HashMap<String, String>
    ) {
        facebookVideoDetector.run(url, title, videoID, headers, cooks)
    }

    private fun verify(
        url: String,
        title: String,
        response: HiperResponse,
        headers: HashMap<String, Any>,
        cooks: HashMap<String, String>
    ) {
        val contentLength = response.headers.get("content-length")?.toLong()
        val contentType = response.headers.get("content-type")
        if (
            contentType != null && contentLength != null && contentLength > 0 &&
            contentType.toLowerCase(Locale.ROOT) != "video/mp2t" &&
            (contentType.startsWith("video/") || contentType.startsWith("audio/") ||
                    contentType.toLowerCase(Locale.ROOT) == "application/x-mpegurl" ||
                    contentType.toLowerCase(Locale.ROOT) == "vnd.apple.mpegurl")
        ) {
            val plainUrl = url.split("://")[1].split("?")[0].split("/")[0]

            if (contentType.toLowerCase(Locale.ROOT) == "application/x-mpegurl" ||
                contentType.toLowerCase(Locale.ROOT) == "vnd.apple.mpegurl") {

                m3u8Detector.run(url, title, headers, cooks)

            } else if (contentType.startsWith("video/")) {
                if (plainUrl.contains("googlevideo.com")) {
                    googleVideoDetector.run(url, title, response, headers, cooks)
                } else {
                    videoDetector.run(url, title, response, headers, cooks)
                }
            } else if (contentType.startsWith("audio/")) {
                if (plainUrl.contains("googlevideo.com")) {
                    googleVideoDetector.run(url, title, response, headers, cooks, isAudio = true)
                } else {
                    audioDetector.run(url, title, response, headers, cooks)
                }
            }
        }
    }
}