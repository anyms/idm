package app.spidy.idmexample

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.hiper.controllers.Hiper
import app.spidy.idm.controllers.Idm
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.IdmListener
import app.spidy.idm.services.IdmService
import app.spidy.kotlinutils.DEBUG_MODE
import app.spidy.kotlinutils.debug
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    init {
        DEBUG_MODE = true
    }

    private lateinit var downloadBtn: Button
    private lateinit var urlField: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var fileNameView: TextView
    private lateinit var fileNameField: EditText
    private lateinit var pauseBtn: Button
    private lateinit var resumeBtn: Button

    private lateinit var snapshot: Snapshot
    private lateinit var adapter: SnapAdapter
    private lateinit var idm: Idm

    private val snaps = ArrayList<Snapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionHandler.requestStorage(this, "need storage permission") {}

        idm = Idm(this)

        downloadBtn = findViewById(R.id.download_button)
        urlField = findViewById(R.id.url_field)
        progressBar = findViewById(R.id.progress_bar)
        fileNameView = findViewById(R.id.filename_text_view)
        fileNameField = findViewById(R.id.filename_field)
        pauseBtn = findViewById(R.id.pause_button)
        resumeBtn = findViewById(R.id.resume_button)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        adapter = SnapAdapter(this, snaps, idm) {
            updateView()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter


        val snapshot = Snapshot(
            uId = UUID.randomUUID().toString(),
            url = "https://sohowww.nascom.nasa.gov/gallery/Movies/animation/Solarwind.mpg"
        )

        downloadBtn.setOnClickListener {
            idm.download(snapshot)
        }

        Idm.onProgress = { snp, progress ->
            progressBar.progress = progress
            urlField.setText(snp.url)
            fileNameField.setText(snp.fileName)

            findSnapIndex(snp.uId) { index ->
                snaps[index].progress = progress
                snaps[index].downloadSpeed = snp.downloadSpeed
                snaps[index].remainingTime = snp.remainingTime
                snaps[index].downloadedSize = snp.downloadedSize

                adapter.notifyItemChanged(index)
            }
        }
    }


    private fun findSnapIndex(uId: String, callback: (index: Int) -> Unit) {
        var index = -1
        for (i in 0 until snaps.size) {
            if (snaps[i].uId == uId) {
                index = i
                break
            }
        }
        if (index != -1) {
            callback(index)
        }
    }


    private fun updateView() {
        idm.getSnapshots {
            snaps.clear()
            it.forEach { snap ->
                snaps.add(snap)
            }
            adapter.notifyDataSetChanged()
        }
    }


    override fun onResume() {
        updateView()
        super.onResume()
    }


    override fun onDestroy() {
        super.onDestroy()
        idm.unbind()
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
