package app.spidy.idm

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.spidy.hiper.Hiper
import app.spidy.hiper.controllers.Caller
import app.spidy.idm.data.Detect
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.CopyListener
import app.spidy.idm.interfaces.DetectListener
import app.spidy.idm.interfaces.IdmListener
import app.spidy.idm.utils.Formatter.formatBytes
import app.spidy.idm.utils.Formatter.secsToTime
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.URI
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Idm(private val context: Context) {
//    val detector = Detector(detectListener)
    var idmListener: IdmListener? = null
    var maxSpeed = 4096

    private val fileIO = FileIO(context)
    private val hiper = Hiper.getAsyncInstance()
    private val hiperSync = Hiper.getSyncInstance()
    private val queues = HashMap<String, Caller>()

    fun download(detect: Detect) {
        when (detect.type) {
            Detect.TYPE_VIDEO -> downloadVideo(detect)
            Detect.TYPE_FACEBOOK -> downloadFacebookVideo(detect)
            Detect.TYPE_AUDIO -> downloadAudio(detect)
            Detect.TYPE_STREAM -> downloadM3u8(detect)
            Detect.TYPE_FILE -> downloadFile(detect)
        }
    }

    fun pause(uId: String) {
        queues[uId]?.cancel()
        queues.remove(uId)
    }
    fun resume(snapshot: Snapshot) {
        when (snapshot.type) {
            Detect.TYPE_VIDEO -> downloadVideo(null, snapshot)
            Detect.TYPE_FACEBOOK -> downloadFacebookVideo(null, snapshot)
            Detect.TYPE_AUDIO -> downloadAudio(null, snapshot)
            Detect.TYPE_STREAM -> downloadM3u8(null, snapshot)
            Detect.TYPE_FILE -> downloadFile(null, snapshot)
        }
    }

    private fun downloadFile(detect: Detect?, snp: Snapshot? = null) {
        if (snp == null) {
            val requestHeaders = detect!!.requestHeaders
            val uId = UUID.randomUUID().toString()
            idmListener?.onInit(uId, "Fetching headers")
            val heads = HashMap(requestHeaders)
            heads["range"] = "bytes=0-"
            hiper.head(detect.data["url"]!!, headers = heads, cookies = detect.cookies).then { headResponse ->
                val snapshot = Snapshot(
                    uId = uId,
                    fileName = detect.data["filename"].toString(),
                    downloadedSize = 0,
                    contentSize = headResponse.headers.get("content-length")?.toLong() ?: 0,
                    requestHeaders = requestHeaders,
                    responseHeaders = headResponse.headers.toHashMap(),
                    cookies = detect.cookies,
                    isResumable = headResponse.statusCode == 206,
                    type = Detect.TYPE_FILE,
                    data = detect.data,
                    speed = "0Kb/s",
                    remainingTime = "0sec",
                    state = Snapshot.STATE_PROGRESS
                )
                Handler(Looper.getMainLooper()).post {
                    download(snapshot)
                }
            }.catch {e ->
                idmListener?.onError(e, uId)
            }
        } else {
            download(snp)
        }
    }

    private fun downloadVideo(detect: Detect?, snp: Snapshot? = null) {
        val snapshot = snp
            ?: Snapshot(
                uId = UUID.randomUUID().toString(),
                fileName = detect!!.data["filename"].toString(),
                downloadedSize = 0,
                contentSize = detect.responseHeaders["content-length"]?.toLong() ?: 0,
                requestHeaders = detect.requestHeaders,
                responseHeaders = detect.responseHeaders,
                cookies = detect.cookies,
                isResumable = detect.isResumable,
                type = Detect.TYPE_VIDEO,
                data = detect.data,
                speed = "0Kb/s",
                remainingTime = "0sec",
                state = Snapshot.STATE_PROGRESS
            )

        download(snapshot)
    }

    private fun downloadAudio(detect: Detect?, snp: Snapshot? = null) {
        val snapshot = snp
            ?: Snapshot(
                uId = UUID.randomUUID().toString(),
                fileName = detect!!.data["filename"].toString(),
                downloadedSize = 0,
                contentSize = detect.responseHeaders["content-length"]?.toLong() ?: 0,
                requestHeaders = detect.requestHeaders,
                responseHeaders = detect.responseHeaders,
                cookies = detect.cookies,
                isResumable = detect.isResumable,
                type = Detect.TYPE_AUDIO,
                data = detect.data,
                speed = "0Kb/s",
                remainingTime = "0sec",
                state = Snapshot.STATE_PROGRESS
            )

        download(snapshot)
    }

    private fun downloadFacebookVideo(detect: Detect?, snp: Snapshot? = null) {
        if (snp == null) {
            val uId = UUID.randomUUID().toString()
            idmListener?.onInit(uId, "Extracting facebook information")
            val videoId = detect!!.data["id"]
            val requestHeaders = HashMap(detect.requestHeaders)
            val cookies = detect.cookies
            requestHeaders["user-agent"] =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.92 Safari/537.36"
            hiper.get(
                "https://facebook.com/watch/?v=${videoId}",
                headers = requestHeaders,
                cookies = cookies
            ).then { initResponse ->
                val regex = "hd_src:\"(.+?)\"".toRegex()
                val hdSrc = regex.find(initResponse.text.toString())?.groups?.get(1)?.value

                if (hdSrc != null) {
                    idmListener?.onInit(uId, "Fetching headers")
                    Handler(Looper.getMainLooper()).post {
                        requestHeaders["range"] = "bytes=0-"
                        hiper.head(hdSrc, headers = requestHeaders, cookies = detect.cookies)
                            .then { headResponse ->
                                detect.data["url"] = hdSrc
                                val snapshot = Snapshot(
                                    uId = uId,
                                    fileName = detect.data["filename"].toString(),
                                    downloadedSize = 0,
                                    contentSize = headResponse.headers.get("content-length")?.toLong()
                                        ?: 0,
                                    requestHeaders = detect.requestHeaders,
                                    responseHeaders = headResponse.headers.toHashMap(),
                                    cookies = detect.cookies,
                                    isResumable = headResponse.statusCode == 206,
                                    type = Detect.TYPE_FACEBOOK,
                                    data = detect.data,
                                    speed = "0Kb/s",
                                    remainingTime = "0sec",
                                    state = Snapshot.STATE_PROGRESS
                                )
                                download(snapshot)
                            }.catch { e ->
                            idmListener?.onError(e, uId)
                        }
                    }
                } else {
                    idmListener?.onError(IOException("Unable to extract facebook information"), uId)
                }
            }.catch { e ->
                idmListener?.onError(e, uId)
            }
        } else {
            download(snp)
        }
    }

    private fun downloadM3u8(detect: Detect?, snp: Snapshot? = null) {
        val snapshot = snp ?: Snapshot(
            uId = UUID.randomUUID().toString(),
            fileName = detect!!.data["filename"]!!,
            downloadedSize = 0,
            contentSize = 0,
            requestHeaders = detect.requestHeaders,
            responseHeaders = detect.responseHeaders,
            cookies = detect.cookies,
            isResumable = false,
            type = Detect.TYPE_STREAM,
            data = detect.data,
            speed = "0Kb/s",
            remainingTime = "0sec",
            state = Snapshot.STATE_PROGRESS
        )
        idmListener?.onInit(snapshot.uId, "Fetching m3u8 configs")
        hiper.get(snapshot.data["url"]!!, headers = snapshot.requestHeaders, cookies = snapshot.cookies).then { initResponse ->
            val urls = parseStream(snapshot.data["url"]!!, initResponse.text!!)
            var currentTime = 0L
            for (i in urls.indices) {
                if (currentTime + 1000 < System.currentTimeMillis()) {
                    idmListener?.onInit(snapshot.uId, "(${((i+1) / urls.size.toFloat() * 100).toInt()}%) fetching headers")
                    currentTime = System.currentTimeMillis()
                }
                val res = hiperSync.head(urls[i], headers = snapshot.requestHeaders, cookies = snapshot.cookies)
                snapshot.contentSize += res.headers.get("content-length")!!.toLong()
            }
            Handler(Looper.getMainLooper()).post {
                calcSpeed(snapshot.downloadedSize, snapshot)
                idmListener?.onStart(snapshot)
                downloadChunks(snapshot, urls)
            }
        }.catch { e ->
            idmListener?.onError(e, snapshot.uId)
        }
    }

    private fun downloadChunks(snapshot: Snapshot, urls: List<String>, index: Int = 0) {
        val caller = hiper.get(urls[index], headers = snapshot.requestHeaders,
            cookies = snapshot.cookies, isStream = true).then { response ->
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
            val path = "$dir${File.separator}${snapshot.fileName}"
            val destination = RandomAccessFile(path, "rw")
            destination.seek(snapshot.downloadedSize)

            try {
                val bytes = ByteArray(maxSpeed)
                var bufferSize = response.stream!!.read(bytes)
                while (bufferSize != -1) {
                    destination.write(bytes, 0, bufferSize)
                    snapshot.downloadedSize += bufferSize
                    bufferSize = response.stream!!.read(bytes)
                }
            } catch (e: Exception) {
                snapshot.state = Snapshot.STATE_DONE
                Thread.sleep(1000)
                val message = e.message.toString().toLowerCase(Locale.ROOT)
                if (message.contains("cancel") || message.contains("closed")) {
                    idmListener?.onPause(snapshot)
                } else {
                    Log.d("hello", "Download Error: $e")
                    idmListener?.onFail(snapshot)
                }
            } finally {
                destination.close()
                response.stream?.close()
            }

            if (index < urls.size - 1) {
                downloadChunks(snapshot, urls, index+1)
            } else {
                snapshot.state = Snapshot.STATE_DONE
                Thread.sleep(1000)
                idmListener?.onComplete(snapshot)
                fileIO.copyToSdCard(File(path), Environment.DIRECTORY_DOWNLOADS,
                    snapshot.responseHeaders["content-type"] ?: "", object : CopyListener {
                        override fun onCopy(progress: Int) {
                            idmListener?.onCopy(progress)
                        }

                        override fun onCopied() {
                            idmListener?.onCopied(snapshot)
                        }

                        override fun onCopyError(e: Exception) {
                            idmListener?.onCopyError(e, snapshot)
                        }
                    })
            }
        }.catch { e ->
            idmListener?.onError(e, snapshot.uId)
        }
        queues[snapshot.uId] = caller
    }

    private fun download(snapshot: Snapshot) {
        if (snapshot.isResumable) {
            snapshot.requestHeaders["range"] = "bytes=${snapshot.downloadedSize}-"
        } else {
            snapshot.downloadedSize = 0
        }

        snapshot.state = Snapshot.STATE_PROGRESS
        idmListener?.onStart(snapshot)
        calcSpeed(0, snapshot)
        val caller = hiper.get(snapshot.data["url"]!!, headers = snapshot.requestHeaders,
            cookies = snapshot.cookies, isStream = true).then { response ->
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
            val path = "$dir${File.separator}${snapshot.fileName}"
            val destination = RandomAccessFile(path, "rw")
            destination.seek(snapshot.downloadedSize)
            var isException = false
            try {
                val bytes = ByteArray(maxSpeed)
                var bufferSize = response.stream!!.read(bytes)
                while (bufferSize != -1) {
                    destination.write(bytes, 0, bufferSize)
                    snapshot.downloadedSize += bufferSize
                    bufferSize = response.stream!!.read(bytes)
                }
                snapshot.state = Snapshot.STATE_DONE
                Thread.sleep(1000)
                idmListener?.onComplete(snapshot)
            } catch (e: Exception) {
                snapshot.state = Snapshot.STATE_DONE
                Thread.sleep(1000)
                val message = e.message.toString().toLowerCase(Locale.ROOT)
                if (message.contains("cancel") || message.contains("closed")) {
                    idmListener?.onPause(snapshot)
                } else {
                    Log.d("hello", "Download Error: $e")
                    idmListener?.onFail(snapshot)
                }
                isException = true
            } finally {
                destination.close()
                response.stream?.close()
                if (!isException) {
                    fileIO.copyToSdCard(File(path), Environment.DIRECTORY_DOWNLOADS,
                        snapshot.responseHeaders["content-type"] ?: "", object : CopyListener {
                            override fun onCopy(progress: Int) {
                                idmListener?.onCopy(progress)
                            }

                            override fun onCopied() {
                                idmListener?.onCopied(snapshot)
                            }

                            override fun onCopyError(e: Exception) {
                                idmListener?.onCopyError(e, snapshot)
                            }
                        })
                }
            }
        }.catch {
            snapshot.state = Snapshot.STATE_DONE
            Thread.sleep(1000)
            idmListener?.onError(it, snapshot.uId)
        }

        queues[snapshot.uId] = caller
    }

    private fun calcSpeed(prevDownloaded: Long, snapshot: Snapshot) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (prevDownloaded != 0L) {
                val downloadPerSec = snapshot.downloadedSize - prevDownloaded
                if (downloadPerSec != 0L) {
                    snapshot.speed = "${formatBytes(downloadPerSec, true)}/s"
                    snapshot.remainingTime = secsToTime((snapshot.contentSize - snapshot.downloadedSize) / downloadPerSec)
                }
            }
            idmListener?.onProgress(snapshot)
            Log.d("hello", "State: ${snapshot.state}")
            if (snapshot.state == Snapshot.STATE_PROGRESS) {
                calcSpeed(snapshot.downloadedSize, snapshot)
            }
        }, 1000)
    }

    private fun parseStream(url: String, text: String): List<String> {
        val lines = text.split("\n")
        val uri = URI.create(url)
        val urls = ArrayList<String>()
        for (line in lines) {
            if (!line.startsWith("#")) {
                if (line.startsWith("http://") || line.startsWith("https://")) {
                    urls.add(line)
                } else {
                    urls.add(uri.resolve(URI.create(line)).toString())
                }
            }
        }
        return urls
    }
}