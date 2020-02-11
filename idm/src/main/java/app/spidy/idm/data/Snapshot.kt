package app.spidy.idm.data

data class Snapshot(
    val uId: String,
    val url: String,
    var fileName: String? = null,
    var isResumable: Boolean = false,
    var downloadedSize: Long = 0,
    var totalSize: Long = 0,
    var mimeType: String = "text/plain",
    var destUri: String? = null,
    var isStream: Boolean = false,
    val streamUrls: ArrayList<String> = arrayListOf(),
    var downloadSpeed: String = "0Kb/s",
    var remainingTime: String = "0sec"
)