package app.spidy.idmexample

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import app.spidy.idm.Downloader
import app.spidy.idm.IDM
import app.spidy.idm.interfaces.IDMListener
import app.spidy.idm.models.Snapshot

class MainActivity : AppCompatActivity() {
    private lateinit var downloadBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var resumeBtn: Button
    private lateinit var speedView: TextView
    private lateinit var remainingTimeView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var downloader: Downloader

    private var snapshot: Snapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadBtn = findViewById(R.id.downloadBtn)
        pauseBtn = findViewById(R.id.pauseBtn)
        resumeBtn = findViewById(R.id.resumeBtn)
        speedView = findViewById(R.id.speedView)
        remainingTimeView = findViewById(R.id.remainingTimeView)
        progressBar = findViewById(R.id.progressBar)

        PermissionHandler.requestStorage(this, "need storage permission") {}

        val url = "https://sohowww.nascom.nasa.gov/gallery/Movies/animation/Solarwind_snd.mov"
        val downloadLocation = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val idm = IDM(downloadLocation)
        idm.idmListener = idmListener

        downloadBtn.setOnClickListener {
            Toast.makeText(this, "DOWNLOADING", Toast.LENGTH_LONG).show()
            downloader = idm.getInstance(url)
            downloader.download(speed = 4096)
        }

        pauseBtn.setOnClickListener {
            Toast.makeText(this, "PAUSED", Toast.LENGTH_LONG).show()
            downloader.pause()
        }

        resumeBtn.setOnClickListener {
            Toast.makeText(this, "RESUMED", Toast.LENGTH_LONG).show()
            downloader = idm.getInstance(snapshot!!)
            downloader.download(speed = 4096)
        }
    }

    private val idmListener = object : IDMListener {
        @SuppressLint("SetTextI18n")
        override fun onPrepare() {
            remainingTimeView.text = "Preparing..."
        }

        @SuppressLint("SetTextI18n")
        override fun onStart(snapshot: Snapshot) {
            this@MainActivity.snapshot = snapshot
            remainingTimeView.text = "0sec"
        }

        override fun onProgress(snapshot: Snapshot) {
            progressBar.progress = snapshot.progress
            remainingTimeView.text = snapshot.remainingTime
        }

        override fun onFail(snapshot: Snapshot) {
            this@MainActivity.snapshot = snapshot
            Toast.makeText(this@MainActivity, "FAILED.", Toast.LENGTH_LONG).show()
        }

        override fun onFail(code: Int) {
            Toast.makeText(this@MainActivity, "FAILED $code.", Toast.LENGTH_LONG).show()
            super.onFail(code)
        }

        override fun onPause(snapshot: Snapshot) {
            this@MainActivity.snapshot = snapshot
            Toast.makeText(this@MainActivity, "PAUSED.", Toast.LENGTH_LONG).show()
        }

        override fun onFinish(snapshot: Snapshot) {
            this@MainActivity.snapshot = snapshot
            Toast.makeText(this@MainActivity, "FINISHED.", Toast.LENGTH_LONG).show()
        }

        override fun onSpeed(snapshot: Snapshot) {
            speedView.text = snapshot.speed
        }

        override fun onFetchStream(count: Int, total: Int) {
            super.onFetchStream(count, total)
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
}
