package app.spidy.idmexample

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.idmexample.R
import app.spidy.idm.controllers.Idm
import app.spidy.idm.controllers.formatBytes
import app.spidy.idm.data.Snapshot
import app.spidy.kotlinutils.debug
import java.net.URL

class SnapAdapter(
    private val context: Context,
    private val snaps: ArrayList<Snapshot>,
    private val idm: Idm,
    private val updateView: () -> Unit
) : RecyclerView.Adapter<SnapAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_snap, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return snaps.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        debug(snaps[position])

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val attrs = intArrayOf(R.attr.selectableItemBackground)
            val typedArray = holder.itemView.context.obtainStyledAttributes(attrs)
            val selectableItemBackground = typedArray.getResourceId(0, 0)
            typedArray.recycle()

            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
            holder.itemView.foreground = holder.itemView.context.getDrawable(selectableItemBackground)
        }

        holder.progressBar.progress = snaps[position].progress
        holder.fileTitleView.text = snaps[position].fileName
        holder.fileStatusView.text = "${formatBytes(snaps[position].downloadedSize)} • ${formatBytes(snaps[position].totalSize)}"


        when (snaps[position].status) {
            Snapshot.STATUS_DOWNLOADING,
            Snapshot.STATUS_QUEUED -> {
                if (snaps[position].isResumable) {
                    holder.quickControlImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_pause
                        )
                    )
                } else {
                    holder.quickControlImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.ic_stop
                        )
                    )
                }

                if (snaps[position].status == Snapshot.STATUS_DOWNLOADING) {
                    holder.remainingTimeView.text = snaps[position].remainingTime
                    holder.progressBar.visibility = View.VISIBLE
                } else {
                    holder.remainingTimeView.text = "Queued"
                    holder.progressBar.visibility = View.GONE
                }

                holder.quickControlImage.setOnClickListener {
                    idm.pause(snaps[position])
                    updateView()
                }
            }
            Snapshot.STATUS_PAUSED -> {
                holder.progressBar.visibility = View.GONE
                if (snaps[position].isResumable) {
                    holder.remainingTimeView.text = "Paused"
                } else {
                    holder.remainingTimeView.text = "Stopped"
                }
                holder.quickControlImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play_dark))
                holder.quickControlImage.setOnClickListener {
                    idm.download(snaps[position])
                    updateView()
                }
            }
            Snapshot.STATUS_COMPLETED -> {
                holder.progressBar.visibility = View.GONE
                holder.remainingTimeView.text = "Finished"
                holder.quickControlImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_delete))

                holder.quickControlImage.setOnClickListener {
                    idm.delete(snaps[position]) {
                        updateView()
                    }
                }
            }
            Snapshot.STATUS_FAILED -> {
                holder.progressBar.visibility = View.GONE
                holder.remainingTimeView.text = "Failed"
                holder.quickControlImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_delete))

                holder.quickControlImage.setOnClickListener {
                    idm.delete(snaps[position]) {
                        updateView()
                    }
                }
            }
        }

        holder.rootView.setOnClickListener {
            Toast.makeText(context, "hello", Toast.LENGTH_SHORT).show()
        }
    }


    inner class ViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val rootView: ConstraintLayout = v.findViewById(R.id.root_view)
        val progressBar: ProgressBar = v.findViewById(R.id.progressBar)
        val fileTitleView: TextView = v.findViewById(R.id.file_title_view)
        val fileStatusView: TextView = v.findViewById(R.id.file_status_view)
        val remainingTimeView: TextView = v.findViewById(R.id.remaining_time_view)
        val quickControlImage: ImageView = v.findViewById(R.id.quick_control_image)
    }
}