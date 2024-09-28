import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rajuse.camex_sensor.R
import java.io.File


class VideoListAdapter(private var videoList: List<File> = emptyList(), var onVideoItemClick: () -> Unit) : RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {

    fun submitList(videos: List<File>) {
        val diffCallback = VideoDiffCallback(videoList, videos)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        videoList = videos
        diffResult.dispatchUpdatesTo(this)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val videoFile = videoList[position]
            holder.bind(videoFile)
    }

    override fun getItemCount(): Int = videoList.size

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoNameTextView: TextView = itemView.findViewById(R.id.textView)

        fun bind(videoFile: File) {
            videoNameTextView.text = videoFile.name

            itemView.setOnClickListener {
                onVideoItemClick.invoke()

                playVideo(itemView.context, videoFile)
            }
        }

        private fun playVideo(context: Context, videoFile: File) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(videoFile), "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }
    }

    class VideoDiffCallback(
    private val oldList: List<File>,
    private val newList: List<File>
    ) : DiffUtil.Callback() {

        // Number of items in the old list
        override fun getOldListSize(): Int = oldList.size

        // Number of items in the new list
        override fun getNewListSize(): Int = newList.size

        // Check whether two items represent the same video (by file path)
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].absolutePath == newList[newItemPosition].absolutePath
        }

        // Check whether the contents of two items are the same (by file content)
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
