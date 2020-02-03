package app.spidy.idmexample

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import app.spidy.hiper.controllers.Hiper
import app.spidy.idm.controllers.Idm
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.IdmListener
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val links = arrayListOf(
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment1_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93",
        "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment2_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment3_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment4_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment5_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment6_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment7_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment8_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment9_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment10_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment11_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment12_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment13_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment14_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment15_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment16_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment17_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment18_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment19_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment20_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment21_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment22_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment23_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment24_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment25_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment26_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93","https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/segment27_0_a.ts?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=/i/songs/41/2742841/28077463/28077463_64.mp4/*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93"
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

    private fun debug(s: Any?) {
        Log.d("hello", s.toString())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadBtn = findViewById(R.id.download_button)
        urlField = findViewById(R.id.url_field)
        progressBar = findViewById(R.id.progress_bar)
        fileNameView = findViewById(R.id.filename_text_view)
        fileNameField = findViewById(R.id.filename_field)
        pauseBtn = findViewById(R.id.pause_button)
        resumeBtn = findViewById(R.id.resume_button)


        val snapshot = Snapshot(
            uId = UUID.randomUUID().toString(),
            url = "https://vodhls-vh.akamaihd.net/i/songs/41/2742841/28077463/28077463_64.mp4/index_0_a.m3u8?set-akamai-hls-revision=5&hdntl=exp=1580828940~acl=%2fi%2fsongs%2f41%2f2742841%2f28077463%2f28077463_64.mp4%2f*~data=hdntl~hmac=fc9fbc3a69b1a41d863b512ea4d546c616868f3c945d34998ded9780429bca93",
            fileName = "songs.mp3",
            isStream = true,
            streamUrls = links
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
        }

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
}
