package app.spidy.idm.services


import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import app.spidy.hiper.controllers.Caller
import app.spidy.hiper.controllers.Hiper
import app.spidy.idm.App
import app.spidy.idm.R
import app.spidy.idm.controllers.Idm
import app.spidy.idm.controllers.formatBytes
import app.spidy.idm.controllers.guessFileName
import app.spidy.idm.controllers.secsToTime
import app.spidy.idm.data.Snapshot
import app.spidy.idm.databases.IdmDatabase
import app.spidy.idm.interfaces.IdmListener
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.onUiThread
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.thread

class IdmService: Service() {
    companion object {
        const val STICKY_NOTIFICATION_ID = 101
    }

    private lateinit var db: IdmDatabase
    private lateinit var snapshot: Snapshot
    private lateinit var notification: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat
    private val queues = ArrayList<Snapshot>()
    private var downloadSpeed: String = "0Kb/s"
    private var remainingTime: String = "0sec"
    private var progress: Int = 0
    private var isDone = false
    private val hiper = Hiper()
    private var prevDownloaded = 0L
    private var isCalcSpeedRunning = false
    private var caller: Caller? = null
    var isRunning = true
    var onExit: (() -> Unit)? = null

    var queue: Snapshot
        get() = queues[0]
        set(value) {
            thread {
                snapshot.status = Snapshot.STATUS_QUEUED
                db.idmDao().putSnapshot(value)
            }
            queues.add(0, value)
        }

    private var idmListener = object : IdmListener {
        override fun onDone() {
            kill()
            debug("Done")
        }
        override fun onFinish(snapshot: Snapshot) {
            snapshot.status = Snapshot.STATUS_COMPLETED
            thread {
                db.idmDao().updateSnapshot(snapshot)
                onUiThread { download() }
            }
            debug("Finish")
        }
        override fun onStart(snapshot: Snapshot) {
            snapshot.status = Snapshot.STATUS_DOWNLOADING
            thread {
                db.idmDao().updateSnapshot(snapshot)
            }
            debug("Start")
        }
        override fun onProgress(snapshot: Snapshot, progress: Int) {
            Idm.onProgress?.invoke(snapshot, progress)
        }
        override fun onInterrupt(snapshot: Snapshot, e: Exception?) {
            snapshot.status = Snapshot.STATUS_PAUSED
            thread {
                db.idmDao().updateSnapshot(snapshot)
                onUiThread { download() }
            }
            debug("Interrupt")
        }
        override fun onFail(snapshot: Snapshot) {
            snapshot.status = Snapshot.STATUS_FAILED
            debug(snapshot)
            thread {
                db.idmDao().updateSnapshot(snapshot)
                onUiThread { download() }
            }
            debug("Fail")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return IdmBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        db = Room.databaseBuilder(this, IdmDatabase::class.java, "IdmDatabase")
            .fallbackToDestructiveMigration().build()
        notificationManager = NotificationManagerCompat.from(this@IdmService)
        notification = NotificationCompat.Builder(this@IdmService, App.CHANNEL_ID)
        notification.setProgress(100, 0, true)
        notification.setSmallIcon(android.R.drawable.stat_sys_download)
        notification.setOnlyAlertOnce(true)
        notification.color = ContextCompat.getColor(this@IdmService, R.color.colorAccent)
        isRunning = true

        startForeground(STICKY_NOTIFICATION_ID, notification.build())
        return START_NOT_STICKY
    }

    private fun kill() {
        ignore { onExit?.invoke() }
        stopSelf()
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
                    snapshot.downloadSpeed = downloadSpeed
                    snapshot.remainingTime = remainingTime
                }
            }
            prevDownloaded = snapshot.downloadedSize

            if (!isDone) {
                calcSpeed()
            } else {
                isCalcSpeedRunning = false
                notificationManager.cancel(STICKY_NOTIFICATION_ID)
            }
        }, 1000)
    }

    fun download() {
        if (queues.isEmpty()) {
            isDone = true
            idmListener.onDone()
            debug("Done.")
        } else {
            snapshot = queues.removeAt(0)
            prepare(snapshot) {
                if (!isCalcSpeedRunning) calcSpeed()
                idmListener.onStart(snapshot)
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
                onUiThread { callback(snp) }
            }
            snp.totalSize == 0L -> {
                hiper.head(snp.url, headers = hashMapOf("User-Agent" to snapshot.userAgent))
                    .ifException {
                        debug("Error: ${it?.message}")
                        onUiThread {
                            idmListener.onFail(snp)
                        }
                    }
                    .ifFailed {
                        debug("Request failed on headers")
                        onUiThread {
                            idmListener.onFail(snp)
                        }
                    }
                    .finally { headerResponse ->
                        snp.totalSize = headerResponse.headers.get("content-length")!!.toLong()
                        snp.mimeType = headerResponse.headers.get("content-type")!!.toString()
                        val tmpHeaders: HashMap<String, Any?> = hashMapOf()
                        tmpHeaders["range"] = "bytes=0-0"
                        tmpHeaders["user-agent"] = snapshot.userAgent
                        if (snp.fileName == null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                snp.fileName = URLUtil.guessFileName(snp.url, headerResponse.headers.get("content-disposition"), snp.mimeType)
                            } else {
                                snp.fileName = guessFileName(
                                    snp.destUri!!,
                                    snp.url,
                                    snp.mimeType,
                                    headerResponse.headers.get("content-disposition")
                                )
                            }
                        }
                        hiper.get(snp.url, headers = tmpHeaders)
                            .ifException {
                                debug("Error: ${it?.message}")
                                onUiThread { callback(snp) }
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
        headers["user-agent"] = snapshot.userAgent
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
            put(MediaStore.MediaColumns.RELATIVE_PATH, snapshot.destUri)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        val outputStream: OutputStream? = if (uri == null) null else resolver.openOutputStream(uri)

        if (snapshot.isStream) {
            downloadQStream(outputStream, 0)
        } else {
            caller = hiper.get(snapshot.url, headers = headers, isStream = true)
                .ifException { e ->
                    outputStream?.flush()
                    outputStream?.close()
                    onUiThread {
                        idmListener.onInterrupt(snapshot, e)
                    }
                }
                .ifFailed {
                    outputStream?.flush()
                    outputStream?.close()
                    onUiThread {
                        idmListener.onFail(snapshot)
                    }
                }
                .ifStream { buffer, byteSize ->
                    progress =
                        (snapshot.downloadedSize / snapshot.totalSize.toFloat() * 100.0).toInt()
                    if (byteSize == -1) {
                        outputStream?.flush()
                        outputStream?.close()
                        onUiThread {
                            idmListener.onFinish(snapshot)
                        }
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
                hiper.get(snapshot.streamUrls[count], headers = hashMapOf("User-Agent" to snapshot.userAgent), isStream = true)
                    .ifFailed {
                        onUiThread {
                            idmListener.onFail(snapshot)
                        }
                    }
                    .ifException { e ->
                        onUiThread {
                            idmListener.onInterrupt(snapshot, e)
                        }
                    }
                    .ifStream { buffer, byteSize ->
                        progress =
                            (snapshot.downloadedSize / snapshot.totalSize.toFloat() * 100.0).toInt()
                        if (byteSize == -1) {
                            debug("Recursive called")
                            onUiThread {
                                downloadQStream(outputStream, count+1)
                            }
                        } else {
                            outputStream?.write(buffer!!, 0, byteSize)
                            snapshot.downloadedSize += byteSize
                        }
                    }
                    .finally { }
        } else {
            outputStream?.flush()
            outputStream?.close()
            idmListener.onFinish(snapshot)
        }
    }

    private fun downloadLegacyStream(file: RandomAccessFile, count: Int) {
        if (snapshot.streamUrls.size > count) {
            debug("URL: ${snapshot.streamUrls[count]}")
            caller =
                hiper.get(snapshot.streamUrls[count], headers = hashMapOf("User-Agent" to snapshot.userAgent), isStream = true)
                    .ifFailed {
                        onUiThread {
                            idmListener.onFail(snapshot)
                        }
                    }
                    .ifException { e ->
                        onUiThread {
                            idmListener.onInterrupt(snapshot, e)
                        }
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
            idmListener.onFinish(snapshot)
        }
    }

    private fun downloadLegacy() {
        val headers = HashMap<String, Any?>()
        headers["user-agent"] = snapshot.userAgent

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
                .ifException { e ->
                    file.close()
                    onUiThread {
                        idmListener.onInterrupt(snapshot, e)
                    }
                }
                .ifFailed {
                    file.close()
                    onUiThread {
                        idmListener.onFail(snapshot)
                    }
                }
                .ifStream { buffer, byteSize ->
                    progress =
                        (snapshot.downloadedSize / snapshot.totalSize.toFloat() * 100.0).toInt()
                    if (byteSize == -1) {
                        file.close()
                        onUiThread {
                            idmListener.onFinish(snapshot)
                        }
                    } else {
                        file.write(buffer!!, 0, byteSize)
                        snapshot.downloadedSize += byteSize
                    }
                }
                .finally {}
        }
    }


    private fun findSnapIndex(uId: String, callback: (index: Int) -> Unit) {
        var index = -1
        for (i in 0 until queues.size) {
            if (queues[i].uId == uId) {
                index = i
                break
            }
        }
        callback(index)
    }


    fun pause(snapshot: Snapshot) {
        findSnapIndex(snapshot.uId) {
            if (it == -1) {
                caller?.cancel()
            } else {
                val snp = queues[it]
                snp.status = Snapshot.STATUS_PAUSED
                thread {
                    db.idmDao().updateSnapshot(snp)
                }
                queues.removeAt(it)
            }
        }
    }


    private fun updateInfo(title: String?, downloadedSize: Long, totalSize: Long) {
        notification.setContentTitle(title)
        notification.setContentText("${formatBytes(downloadedSize)}/${formatBytes(totalSize)}")
        notification.setContentInfo(downloadSpeed)
        notification.setProgress(100, progress, false)
        updateNotification(notification)
        idmListener.onProgress(snapshot, progress)
    }

    private fun updateNotification(notification: NotificationCompat.Builder) {
        notificationManager.notify(STICKY_NOTIFICATION_ID, notification.build())
    }


    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }



    inner class IdmBinder: Binder() {
        val service: IdmService
            get() = this@IdmService
    }
}