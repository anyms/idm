package app.spidy.idm.models

data class Snapshot(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    var progress: Int,
    var downloaded: Long,
    val url: String,
    var isResumable: Boolean,
    val destPath: String,
    var isPaused: Boolean,
    var isFinished: Boolean,
    var speed: String,
    var remainingTime: String,
    var isDownloading: Boolean,
    val headers: HashMap<String, String?>
)