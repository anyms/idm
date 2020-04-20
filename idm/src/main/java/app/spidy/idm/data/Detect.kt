package app.spidy.idm.data

data class Detect(
    val data: HashMap<String, String>,
    val cookies: HashMap<String, String>,
    val requestHeaders: HashMap<String, Any>,
    var responseHeaders: HashMap<String, String>,
    val type: String,
    val isResumable: Boolean
) {
    companion object {
        const val TYPE_FACEBOOK = "app.spidy.idm.data.TYPE_FACEBOOK"
        const val TYPE_GOOGLE = "app.spidy.idm.data.TYPE_GOOGLE"
        const val TYPE_VIDEO = "app.spidy.idm.data.TYPE_VIDEO"
        const val TYPE_AUDIO = "app.spidy.idm.data.TYPE_AUDIO"
        const val TYPE_STREAM = "app.spidy.idm.data.TYPE_STREAM"
        const val TYPE_FILE = "app.spidy.idm.data.TYPE_FILE"
    }
}