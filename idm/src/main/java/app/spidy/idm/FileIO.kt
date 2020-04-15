package app.spidy.idm

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import app.spidy.idm.interfaces.CopyListener
import java.io.*
import java.lang.Exception


class FileIO(private val context: Context) {
    fun copyToSdCard(file: File, sdCardLocation: String, mimeType: String = "*/*", copyListener: CopyListener? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, file.name)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.RELATIVE_PATH, sdCardLocation)
            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
            val descriptor =
                context.contentResolver.openFileDescriptor(uri!!, "w") //"w" specify's write mode
            val fileDescriptor: FileDescriptor = descriptor!!.fileDescriptor
            val dataInputStream: InputStream = context.openFileInput(file.absolutePath)

            val output = FileOutputStream(fileDescriptor)
            val buf = ByteArray(4096)
            var bytesRead = dataInputStream.read(buf)
            var copiedBytes = bytesRead.toLong()
            var lastProgress = 0
            while (bytesRead > 0) {
                output.write(buf, 0, bytesRead)
                copiedBytes += bytesRead
                listenProgress(copiedBytes, file.length()) {
                    lastProgress = it
                    copyListener?.onCopy(it)
                }
                bytesRead = dataInputStream.read(buf)
            }
            if (lastProgress != 100) {
                copyListener?.onCopy(100)
            }
            dataInputStream.close()
            output.close()
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(sdCardLocation).absolutePath
            val destination = RandomAccessFile("$dir${File.separator}${file.name}", "rw")
            destination.seek(0)

            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(file)
                val bytes = ByteArray(4096)
                var bytesRead = fis.read(bytes)
                var copiedBytes = bytesRead.toLong()
                var lastProgress = 0
                while (bytesRead != -1) {
                    destination.write(bytes, 0, bytesRead)
                    copiedBytes += bytesRead
                    listenProgress(copiedBytes, file.length()) {
                        lastProgress = it
                        copyListener?.onCopy(it)
                    }
                    bytesRead = fis.read(bytes)
                }
                if (lastProgress != 100) {
                    copyListener?.onCopy(100)
                }
            } catch (e: Exception) {
                Log.d("hello", "Err: $e")
            } finally {
                destination.close()
                fis?.close()
            }
        }
    }

    private var lastCalled = 0L
    private fun listenProgress(current: Long, total: Long, callback: (progress: Int) -> Unit) {
        if (System.currentTimeMillis() > lastCalled + 1000) {
            callback((current / total.toFloat() * 100).toInt())
            lastCalled = System.currentTimeMillis()
        }
    }
}