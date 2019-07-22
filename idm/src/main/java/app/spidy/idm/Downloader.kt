package app.spidy.idm

import app.spidy.fetcher.Caller
import app.spidy.fetcher.utils.onUiThread
import app.spidy.idm.models.Snapshot
import java.io.File
import java.io.RandomAccessFile
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class Downloader {
    private var idm: IDM
    private lateinit var url: String
    private lateinit var snapshot: Snapshot
    private lateinit var caller: Caller

    private var isDone = true
    private var isPaused = false


    constructor(idm: IDM, url: String) {
        this.idm = idm
        this.url = url
    }
    constructor(idm: IDM, snapshot: Snapshot) {
        this.idm = idm
        this.snapshot = snapshot
    }


    fun download(userAgent: String? = null, speed: Int = 4096) {
        onUiThread { idm.idmListener?.onPrepare() }
        val headers = hashMapOf<String, Any?>()
        headers["User-Agent"] = userAgent ?: idm.userAgent.random()

        if (::snapshot.isInitialized) {
            downloadGet(headers, speed)
        } else {
            idm.fetcher.head(url, headers = headers)
                .ifFailedOrException {
                    onUiThread { idm.idmListener?.onFail(IDM.ERROR_UNABLE_TO_FETCH_HEADERS) }
                }
                .ifSucceed {
                    checkResumeCapability(url, headers) { isResumable ->
                        try {
                            val fileName = idm.guessFileName(url, it.headers["content-type"].toString(), it.headers["content-disposition"])
                            this.snapshot = Snapshot(
                                UUID.randomUUID().toString(),
                                fileName,
                                it.headers["content-type"].toString(),
                                it.headers["content-length"]!!.toLong(),
                                0,
                                0L,
                                url,
                                isResumable,
                                "${idm.downloadLocation.absolutePath}${File.separator}$fileName",
                                isPaused = false,
                                isFinished = false,
                                speed = "0KB/s",
                                remainingTime = "0sec",
                                isDownloading = true,
                                headers = it.headers
                            )
                            downloadGet(headers, speed)
                        } catch (e: Exception) {
                            onUiThread { idm.idmListener?.onFail(IDM.ERROR_UNKNOWN_FILE_META_DATA) }
                        }
                    }
                }
        }
    }

    private fun initiateSpeed() {
        thread {
            var prevDownloaded = 0L
            while (!isDone) {
                if (prevDownloaded != 0L) {
                    val downloadPerSec = snapshot.downloaded - prevDownloaded
                    if (downloadPerSec != 0L) {
                        snapshot.speed = "${idm.formatBytes(downloadPerSec, true)}/s"
                        snapshot.remainingTime = idm.secsToTime((snapshot.fileSize - snapshot.downloaded) / downloadPerSec)
                        onUiThread { idm.idmListener?.onSpeed(snapshot) }
                    }
                } else {
                    onUiThread { idm.idmListener?.onSpeed(snapshot) }
                }

                prevDownloaded = snapshot.downloaded
                Thread.sleep(1000)
            }
        }
    }

    fun pause() {
        if (::caller.isInitialized) {
            isPaused = true
            caller.cancel()
        }
    }

    private fun downloadGet(headers: HashMap<String, Any?>, speed: Int) {
        isDone = false
        onUiThread { idm.idmListener?.onStart(snapshot) }
        initiateSpeed()
        val file = RandomAccessFile(snapshot.destPath, "rw")
        file.seek(snapshot.downloaded)

        caller = idm.fetcher.get(snapshot.url, headers = headers, isStream = true, byteSize = speed)
            .ifFailedOrException {
                if (!isPaused) {
                    file.close()
                    snapshot.isPaused = true
                    snapshot.isFinished = false
                    onUiThread { idm.idmListener?.onFail(snapshot) }
                    isDone = true
                }
            }
            .ifStream { buffer, byteSize ->
                snapshot.progress = (snapshot.downloaded / snapshot.fileSize.toFloat() * 100.0).toInt()
                onUiThread { idm.idmListener?.onProgress(snapshot) }
                if (byteSize == -1) {
                    file.close()
                    if (isPaused) {
                        snapshot.isPaused = true
                        snapshot.isFinished = false
                        onUiThread { idm.idmListener?.onPause(snapshot) }
                    } else {
                        snapshot.isPaused = false
                        snapshot.isFinished = true
                        onUiThread { idm.idmListener?.onFinish(snapshot) }
                    }
                    isDone = true
                } else {
                    file.write(buffer, 0, byteSize)
                    snapshot.downloaded += byteSize
                }
            }
            .ifSucceed {  }
    }

    private fun checkResumeCapability(url: String, headers: HashMap<String, Any?>, callback: (Boolean) -> Unit) {
        val heads = HashMap<String, Any?>()
        for ((key, value) in headers) {
            heads[key] = value
        }
        heads["Range"] = "bytes=0-0"
        idm.fetcher.get(url, headers = heads)
            .ifFailedOrException {
                onUiThread { idm.idmListener?.onFail(IDM.ERROR_UNABLE_TO_CHECK_RANGE) }
            }
            .ifSucceed {
                callback(it.statusCode == 206)
            }
    }
}