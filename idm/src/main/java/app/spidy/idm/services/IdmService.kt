package app.spidy.idm.services


import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.*
import android.provider.MediaStore
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.spidy.hiper.controllers.Caller
import app.spidy.hiper.controllers.Hiper
import app.spidy.idm.App
import app.spidy.idm.R
import app.spidy.idm.controllers.formatBytes
import app.spidy.idm.controllers.guessFileName
import app.spidy.idm.controllers.secsToTime
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.IdmListener
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile

class IdmService: Service() {
    companion object {
        const val STICKY_NOTIFICATION_ID = 101
    }

    private lateinit var snapshot: Snapshot
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat
    private val queue = ArrayList<Snapshot>()
    private var downloadSpeed: String = "0Kb/s"
    private var remainingTime: String = "0sec"
    private var progress: Int = 0
    private var isDone = false
    private val hiper = Hiper()
    private var prevDownloaded = 0L
    private var isCalcSpeedRunning = false
    private var caller: Caller? = null

    var idmListener: IdmListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        return IdmBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = NotificationManagerCompat.from(this@IdmService)
        notification = NotificationCompat.Builder(this@IdmService, App.CHANNEL_ID)
        notification.setProgress(100, 0, true)
        notification.setSmallIcon(android.R.drawable.stat_sys_download)
        notification.setOnlyAlertOnce(true)
        notification.color = ContextCompat.getColor(this@IdmService, R.color.colorAccent)

        startForeground(STICKY_NOTIFICATION_ID, notification.build())
        return START_NOT_STICKY
    }

    fun addQueue(snapshot: Snapshot) {
        queue.add(snapshot)
    }

    private fun calcSpeed() {
        isCalcSpeedRunning = true
        Handler().postDelayed({
            updateInfo(
                snapshot.fileName,
                snapshot.downloadedSize,
                snapshot.totalSize
            )
            if (prevDownloaded != 0L) {
                val downloadPerSec = snapshot.downloadedSize - prevDownloaded
                if (downloadPerSec != 0L) {
                    downloadSpeed = "${formatBytes(downloadPerSec, true)}/s"
                    remainingTime = secsToTime((snapshot.totalSize - snapshot.downloadedSize) / downloadPerSec)
                }
            }
            prevDownloaded = snapshot.downloadedSize

            if (!isDone) {
                calcSpeed()
            } else {
                debug("kill it")
                isCalcSpeedRunning = false
                notificationManager.cancel(STICKY_NOTIFICATION_ID)
            }
        }, 1000)
    }

    fun download() {
        if (queue.isEmpty()) {
            isDone = true
            idmListener?.onDone()
            debug("Done.")
        } else {
            prepare(queue.removeAt(0)) {
                snapshot = it
                if (!isCalcSpeedRunning) calcSpeed()
                idmListener?.onStart(snapshot)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadQ()
                } else{
                    downloadLegacy()
                }
            }
        }
    }

    private fun prepare(snp: Snapshot, callback: (Snapshot) -> Unit) {
        when {
            snp.isStream -> {
                snp.isResumable = false
                // TODO: change the extension
                if (snp.fileName == null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        snp.fileName = URLUtil.guessFileName(snp.url, null, null)
                    } else {
                        snp.fileName = guessFileName(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                            snp.url,
                            null,
                            null
                        )
                    }
                }
                onUiThread { callback(snp) }
            }
            snp.totalSize == 0L -> {
                hiper.head(snp.url)
                    .ifException {
                        debug("Error: ${it?.message}")
                    }
                    .ifFailed {
                        debug("Request failed on headers")
                    }
                    .finally { headerResponse ->
                        snp.totalSize = headerResponse.headers.get("content-length")!!.toLong()
                        snp.mimeType = headerResponse.headers.get("content-type")!!.toString()
                        val tmpHeaders: HashMap<String, Any?> = hashMapOf()
                        tmpHeaders["range"] = "bytes=0-0"
                        if (snp.fileName == null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                snp.fileName = URLUtil.guessFileName(snp.url, headerResponse.headers.get("content-disposition"), snp.mimeType)
                            } else {
                                snp.fileName = guessFileName(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
                                    snp.url,
                                    snp.mimeType,
                                    headerResponse.headers.get("content-disposition")
                                )
                            }
                        }
                        hiper.get(snp.url, headers = tmpHeaders)
                            .ifException {
                                debug("Error: ${it?.message}")
                            }
                            .ifFailed {
                                debug("Request failed on resume check")
                                snp.isResumable = false
                                onUiThread { callback(snp) }
                            }
                            .finally { resumeResponse ->
                                snp.isResumable = resumeResponse.statusCode == 206
                                onUiThread { callback(snp) }
                            }
                    }
            }
            else -> {
                onUiThread { callback(snp) }
            }
        }
    }

    private fun downloadQ() {
        val headers = HashMap<String, Any?>()
        snapshot.downloadedSize = 0
        snapshot.isResumable = false

        if (snapshot.isStream) {
            snapshot.mimeType = "video/MP2T"
        }

        val fileName = snapshot.fileName?.split(".")?.dropLast(1)?.joinToString(".")

        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, snapshot.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Fetcher")
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        val outputStream: OutputStream? = if (uri == null) null else resolver.openOutputStream(uri)

        if (snapshot.isStream) {
            downloadQStream(outputStream, 0)
        } else {
            caller = hiper.get(snapshot.url, headers = headers, isStream = true)
                .ifException {
                    outputStream?.flush()
                    outputStream?.close()
                    idmListener?.onInterrupt(snapshot)
                    onUiThread { download() }
                }
                .ifFailed {
                    outputStream?.flush()
                    outputStream?.close()
                    idmListener?.onFail(snapshot)
                    onUiThread { download() }
                }
                .ifStream { buffer, byteSize ->
                    progress =
                        (snapshot.downloadedSize / snapshot.totalSize.toFloat() * 100.0).toInt()
                    if (byteSize == -1) {
                        outputStream?.flush()
                        outputStream?.close()
                        onUiThread { download() }
                    } else {
                        outputStream?.write(buffer!!, 0, byteSize)
                        snapshot.downloadedSize += byteSize
                    }
                }
                .finally {}
        }
    }

    private fun downloadQStream(outputStream: OutputStream?, count: Int) {
        if (snapshot.streamUrls.size > count) {
            debug("URL: ${snapshot.streamUrls[count]}")
            caller =
                hiper.get(snapshot.streamUrls[count], headers = hashMapOf(), isStream = true)
                    .ifFailed {
                        idmListener?.onFail(snapshot)
                        onUiThread { download() }
                    }
                    .ifException { e ->
                        idmListener?.onInterrupt(snapshot)
                        onUiThread { download() }
                    }
                    .ifStream { buffer, byteSize ->
                        progress =
                            (snapshot.downloadedSize / snapshot.totalSize.toFloat() * 100.0).toInt()
                        if (byteSize == -1) {
                            debug("Recursive called")
                            onUiThread { downloadQStream(outputStream, count+1) }
                        } else {
                            outputStream?.write(buffer!!, 0, byteSize)
                            snapshot.downloadedSize += byteSize
                        }
                    }
                    .finally { }
        } else {
            outputStream?.flush()
            outputStream?.close()
            download()
        }
    }

    private fun downloadLegacyStream(file: RandomAccessFile, count: Int) {
        if (snapshot.streamUrls.size > count) {
            debug("URL: ${snapshot.streamUrls[count]}")
            caller =
                hiper.get(snapshot.streamUrls[count], headers = hashMapOf(), isStream = true)
                    .ifFailed {
                        idmListener?.onFail(snapshot)
                        onUiThread { download() }
                    }
                    .ifException { e ->
                        idmListener?.onInterrupt(snapshot)
                        onUiThread { download() }
                    }
                    .ifStream { buffer, byteSize ->
                        progress =
                            (snapshot.downloadedSize / snapshot.totalSize.toFloat() * 100.0).toInt()
                        if (byteSize == -1) {
                            debug("Recursive called")
                            onUiThread { downloadLegacyStream(file, count+1) }
                        } else {
                            file.write(buffer!!, 0, byteSize)
                            snapshot.downloadedSize += byteSize
                        }
                    }
                    .finally { }
        } else {
            file.close()
            download()
        }
    }

    private fun downloadLegacy() {
        val headers = HashMap<String, Any?>()

        if (snapshot.isResumable) {
            headers["range"] = "bytes=${snapshot.downloadedSize}-${snapshot.totalSize}"
        } else {
            snapshot.downloadedSize = 0
        }

        val file = RandomAccessFile("${snapshot.destUri}${File.separator}${snapshot.fileName}", "rw")
        file.seek(snapshot.downloadedSize)

        if (snapshot.isStream) {
            downloadLegacyStream(file, 0)
        } else {
            caller = hiper.get(snapshot.url, headers = headers, isStream = true)
                .ifException {
                    file.close()
                    idmListener?.onInterrupt(snapshot)
                    onUiThread { download() }
                }
                .ifFailed {
                    file.close()
                    idmListener?.onFail(snapshot)
                    onUiThread { download() }
                }
                .ifStream { buffer, byteSize ->
                    progress =
                        (snapshot.downloadedSize / snapshot.totalSize.toFloat() * 100.0).toInt()
                    if (byteSize == -1) {
                        file.close()
                        onUiThread { download() }
                    } else {
                        file.write(buffer!!, 0, byteSize)
                        snapshot.downloadedSize += byteSize
                    }
                }
                .finally {}
        }
    }


    fun pause() {
        caller?.cancel()
    }


    private fun updateInfo(title: String?, downloadedSize: Long, totalSize: Long) {
        notification.setContentTitle(title)
        notification.setContentText("${formatBytes(downloadedSize)}/${formatBytes(totalSize)}")
        notification.setContentInfo(downloadSpeed)
        notification.setProgress(100, progress, false)
        updateNotification(notification)
        idmListener?.onProgress(snapshot, progress)
    }

    private fun updateNotification(notification: NotificationCompat.Builder) {
        notificationManager.notify(STICKY_NOTIFICATION_ID, notification.build())
    }



    inner class IdmBinder: Binder() {
        val service: IdmService
            get() = this@IdmService
    }
}