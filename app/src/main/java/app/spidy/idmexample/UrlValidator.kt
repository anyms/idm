package app.spidy.idmexample

import android.util.Log

class UrlValidator {
    private val exts = listOf(
        "html",
        "css",
        "js",
        "csv",
        "json",

        "jpg",
        "png",
        "jpeg",
        "ico",
        "gif",
        "svg",

        "woff",
        "woff2",
        "ttf"
    )

    private val adUrls = listOf(
        "adsystem.com",
        "advertising.com",
        "doubleclick.net",
        "fonts.googleapis.com",
        "googlesyndication.com",
        "connectad.io",
        "adnxs.com",
        "adform.com",
        "adsrvr.org"
    )

    fun validate(url: String): Boolean {
        val ext = url.split("?")[0].split("#")[0].split(".").last()
        return !exts.contains(ext) && !inAdUrls(url)
    }

    private fun inAdUrls(url: String): Boolean {
        var isIn = false
        for (u in adUrls) {
            if (url.contains(u)) {
                isIn = true
                break
            }
        }
        return isIn
    }
}