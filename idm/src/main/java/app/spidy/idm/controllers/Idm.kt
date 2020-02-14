package app.spidy.idm.controllers

import android.content.*
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.webkit.URLUtil
import androidx.room.Room
import app.spidy.idm.data.Snapshot
import app.spidy.idm.databases.IdmDatabase
import app.spidy.idm.interfaces.IdmListener
import app.spidy.idm.services.IdmService
import app.spidy.kotlinutils.onUiThread
import java.io.FileOutputStream
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread

class Idm(private val context: Context) {
    companion object {
        var onUpdate: ((snapshot: Snapshot) -> Unit)? = null
    }

    private var idmService: IdmService? = null
    private val tmpSnaps = ArrayList<Snapshot>()
    private var db: IdmDatabase = Room.databaseBuilder(context, IdmDatabase::class.java, "IdmDatabase")
        .fallbackToDestructiveMigration().build()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            idmService = null
        }
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val idmBinder = service as? IdmService.IdmBinder
            idmService = idmBinder?.service
            idmService?.onExit = {
                unbind()
            }

            for (snp in tmpSnaps) idmService?.queue = snp
            tmpSnaps.clear()
            idmService?.download()
        }
    }


    fun getSnapshots(callback: (List<Snapshot>) -> Unit) {
        thread {
            val snaps = db.idmDao().getSnapshots()

            onUiThread {
                callback(snaps)
            }
        }
    }

    fun download(snapshot: Snapshot) {
        if (snapshot.status == Snapshot.STATUS_NEW) {
            snapshot.destUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "Download/Fetcher"
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            }

            snapshot.fileName = URLUtil.guessFileName(snapshot.url, null, null)
            if (snapshot.isStream) {
                snapshot.fileName = snapshot.fileName.replace(".m3u8", ".mpg")
            }
            snapshot.status = Snapshot.STATUS_QUEUED
        }

        if (idmService == null) {
            val intent = Intent(context, IdmService::class.java)
            context.startService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        if (idmService == null) {
            tmpSnaps.add(snapshot)
        } else {
            idmService?.queue = snapshot
        }
    }

    fun pause(snapshot: Snapshot) {
        idmService?.pause(snapshot)
    }

    fun delete(snapshot: Snapshot, callback: () -> Unit) {
        thread {
            db.idmDao().removeSnapshot(snapshot)
            onUiThread { callback() }
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
}
