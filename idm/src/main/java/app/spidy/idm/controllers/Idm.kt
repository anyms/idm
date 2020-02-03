package app.spidy.idm.controllers

import android.content.*
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.IdmListener
import app.spidy.idm.services.IdmService
import java.io.FileOutputStream
import java.lang.IllegalArgumentException

class Idm(private val context: Context) {
    private var idmService: IdmService? = null
    private val snapshots = ArrayList<Snapshot>()

    private var activityIdmListener: IdmListener? = null
    private val idmListener = object : IdmListener {
        override fun onFinish(snapshot: Snapshot) {
            activityIdmListener?.onFinish(snapshot)
        }
        override fun onDone() {
            kill()
            activityIdmListener?.onDone()
        }
        override fun onStart(snapshot: Snapshot) {
            activityIdmListener?.onStart(snapshot)
        }
        override fun onProgress(snapshot: Snapshot, progress: Int) {
            activityIdmListener?.onProgress(snapshot, progress)
        }
        override fun onFail(snapshot: Snapshot) {
            activityIdmListener?.onFail(snapshot)
        }
        override fun onInterrupt(snapshot: Snapshot) {
            activityIdmListener?.onInterrupt(snapshot)
        }
    }


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            idmService = null
        }
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val idmBinder = service as? IdmService.IdmBinder
            idmService = idmBinder?.service
            idmService?.idmListener = idmListener

            for (snp in snapshots) idmService?.addQueue(snp)
            snapshots.clear()
            idmService?.download()
        }
    }

    fun run(idmListener: IdmListener) {
        if (idmService == null) {
            activityIdmListener = idmListener
            val intent = Intent(context, IdmService::class.java)
            context.startService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun addQueue(snapshot: Snapshot) {
        if (idmService == null) {
            snapshots.add(snapshot)
        } else {
            idmService?.addQueue(snapshot)
        }
    }

    fun kill() {
        unbind()
        context.stopService(Intent(context, IdmService::class.java))
    }

    fun unbind() {
        try {
            context.unbindService(serviceConnection)
            idmService = null
        } catch (e: IllegalArgumentException) {}
    }


    fun pause() {
        idmService?.pause()
    }






    fun downloadQ() {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "CuteKitten001")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/PerracoLabs")
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val file: ParcelFileDescriptor? = if (uri == null) null else resolver.openFileDescriptor(uri, "rw")
        val out = FileOutputStream(file?.fileDescriptor!!)
        val channel = out.channel
        channel.position(0L)
//        channel.write(ByteBuffer.wrap())
    }
}
