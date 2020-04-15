package app.spidy.idm.data

data class Snapshot(
    var uId: String,
    val fileName: String,
    var downloadedSize: Long,
    var contentSize: Long,
    val requestHeaders: HashMap<String, Any>,
    val responseHeaders: HashMap<String, String>,
    val cookies: HashMap<String, String>,
    var isResumable: Boolean,
    val type: String,
    val data: HashMap<String, String>,
    var speed: String,
    var remainingTime: String,
    var state: String
) {
    companion object {
        const val STATE_PROGRESS = "app.spidy.idm.data.STATE_PROGRESS"
        const val STATE_DONE = "app.spidy.idm.data.STATE_DONE"
    }
}