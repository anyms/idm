package app.spidy.idm.data

data class Snapshot(
    val uId: String,
    val url: String,
    val fileName: String,
    var headers: HashMap<String, String> = hashMapOf(),
    var isResumable: Boolean = false,
    var downloadedSize: Long = 0,
    var totalSize: Long = 0,
    var destUri: String? = null,
    var isStream: Boolean = false,
    val streamUrls: ArrayList<String> = arrayListOf()
)