package app.spidy.idmexample

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import app.spidy.hiper.controllers.Hiper
import app.spidy.idm.controllers.Idm
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.IdmListener
import app.spidy.kotlinutils.DEBUG_MODE
import app.spidy.kotlinutils.debug
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    init {
        DEBUG_MODE = true
    }

    private val links = arrayListOf(
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment1_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment2_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment3_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment4_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment5_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment6_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment7_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment8_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment9_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment10_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment11_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment12_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment13_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment14_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment15_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment16_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment17_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment18_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment19_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment20_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment21_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment22_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment23_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment24_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment25_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment26_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment27_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment28_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/segment29_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=/i/songs/41/2742841/28236538/28236538_64.mp4/*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b"
    )



    private lateinit var downloadBtn: Button
    private lateinit var urlField: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var fileNameView: TextView
    private lateinit var fileNameField: EditText
    private lateinit var pauseBtn: Button
    private lateinit var resumeBtn: Button

    private lateinit var snapshot: Snapshot


    private val idm = Idm(this)
    private val hiper = Hiper()
    private val hiperLegacy = hiper.Legacy()
    private val headers: HashMap<String, Any?> = hashMapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36"
    )

    private val idmListener = object : IdmListener {
        override fun onFinish(snapshot: Snapshot) {

        }
        override fun onDone() {
            fileNameView.text = "Done!"
        }
        override fun onStart(snapshot: Snapshot) {
            fileNameView.text = snapshot.fileName
            debug("onStart")
        }
        override fun onProgress(snapshot: Snapshot, progress: Int) {
            progressBar.progress = progress
        }
        override fun onFail(snapshot: Snapshot) {
            debug("onFail")
        }
        override fun onInterrupt(snapshot: Snapshot) {
            this@MainActivity.snapshot = snapshot
            debug("onInterrupt")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionHandler.requestStorage(this, "need storage permission") {}

        downloadBtn = findViewById(R.id.download_button)
        urlField = findViewById(R.id.url_field)
        progressBar = findViewById(R.id.progress_bar)
        fileNameView = findViewById(R.id.filename_text_view)
        fileNameField = findViewById(R.id.filename_field)
        pauseBtn = findViewById(R.id.pause_button)
        resumeBtn = findViewById(R.id.resume_button)


        val snapshot = Snapshot(
            uId = UUID.randomUUID().toString(),
            url = "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28236538/28236538_64.mp4/index_0_a.m3u8?set-akamai-hls-revision=5&hdntl=exp=1581172329~acl=%2fi%2fsongs%2f41%2f2742841%2f28236538%2f28236538_64.mp4%2f*~data=hdntl~hmac=461e84d546bb88001e8e76aa7572c15e090b62a0556529d91a6f200bd931e92b",
            fileName = "songs.mpg",
            isStream = true,
            streamUrls = links,
            destUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        )


        thread {
            for (link in links) {
                try {
                    val resp = hiperLegacy.head(link, headers = headers)
                    if (!resp.isSuccessful) break
                    snapshot.totalSize += resp.headers.get("content-length")!!.toLong()
                } catch (e: Exception) {
                    break
                }
            }

            debug(snapshot.totalSize.toString())
        }

//        val snapshot = Snapshot(
//            uId = UUID.randomUUID().toString(),
//            url = "https://www.cinemaworldtheaters.com/trailers/bombshell.mp4",
//            fileName = "bombshell.mp4",
//            destUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
//        )

        downloadBtn.setOnClickListener {
            idm.run(idmListener)
            idm.addQueue(snapshot)
        }

        pauseBtn.setOnClickListener {
            idm.pause()
        }

        resumeBtn.setOnClickListener {
            idm.run(idmListener)
            idm.addQueue(snapshot)
        }
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
