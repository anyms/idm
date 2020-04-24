package app.spidy.idmexample

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import app.spidy.idm.Idm
import app.spidy.idm.data.Detect
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.DetectListener
import app.spidy.idm.interfaces.IdmListener
import app.spidy.idm.media.Muxer
import app.spidy.kookaburra.fragments.BrowserFragment
import java.lang.Exception
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var idm: Idm
    private lateinit var browserFragment: BrowserFragment

    var tmp = false
    private val idmListener = object : IdmListener {
        override fun onCopied(snapshot: Snapshot) {

        }

        override fun onCopyError(e: Exception, snapshot: Snapshot) {
            Log.d("hello2", "CopyError: $e")
        }

        override fun onInit(uId: String, message: String) {

        }

        override fun onCopy(progress: Int) {

        }
        override fun onError(e: Exception, uId: String) {
            Log.d("hello2", "Err: $e")
        }
        override fun onStart(snapshot: Snapshot) {
            Log.d("hello2", "${snapshot.uId}: $snapshot")
        }
        override fun onComplete(snapshot: Snapshot) {
            Log.d("hello2", "${snapshot.uId}: COMPLETED!!!")
        }

        override fun onFail(snapshot: Snapshot) {

        }

        override fun onPause(snapshot: Snapshot) {
            Log.d("hello2", "${snapshot.uId}: PAUSED!!!")
        }
        override fun onProgress(snapshot: Snapshot) {
            val progress = (snapshot.downloadedSize / snapshot.contentSize.toFloat() * 100).toInt()
            Log.d("hello2", "$progress%")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionHandler.requestStorage(this, "need storage permission") {
            idm = Idm(this)
            idm.idmListener = idmListener

            browserFragment = BrowserFragment()
            browserFragment.browserListener = BrowserListener(idm)
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentBrowser, browserFragment)
                .commit()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PermissionHandler.STORAGE_PERMISSION_CODE ||
            requestCode == PermissionHandler.LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PermissionHandler.execute()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onBackPressed() {
        if (!browserFragment.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
