package app.spidy.idm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshot")
data class Snapshot(
    @PrimaryKey
    val uId: String,
    val url: String,
    var status: String = STATUS_NEW,
    var fileName: String = "download",
    var isResumable: Boolean = false,
    var downloadedSize: Long = 0,
    var totalSize: Long = 0,
    var mimeType: String = "text/plain",
    var destUri: String = "Download/Fetcher",
    var isStream: Boolean = false,
    var streamUrls: List<String> = arrayListOf(),
    var progress: Int = 0,
    var downloadSpeed: String = "0Kb/s",
    var remainingTime: String = "0sec",
    var userAgent: String = "Fetcher/1.0"
) {
    companion object {
        const val STATUS_QUEUED = "queued"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_PAUSED = "paused"
        const val STATUS_FAILED = "failed"
        const val STATUS_DOWNLOADING = "downloading"
        const val STATUS_NEW = "new"
    }
}