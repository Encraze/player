package com.spotifyplayer.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.spotifyplayer.app.R
import android.widget.ImageView
import coil.load

data class QueueDisplayItem(
    val positionLabel: String,
    val title: String,
    val subtitle: String,
    val playCount: Long,
    val isCurrent: Boolean,
    val rawPosition: Int,
    val albumImageUrl: String?
)

class QueueAdapter : RecyclerView.Adapter<QueueAdapter.VH>() {

    private val items = mutableListOf<QueueDisplayItem>()
    private var onItemClick: ((QueueDisplayItem, Int) -> Unit)? = null

    fun submit(list: List<QueueDisplayItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (QueueDisplayItem, Int) -> Unit) {
        onItemClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_queue, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val positionText: TextView = itemView.findViewById(R.id.positionText)
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val subtitleText: TextView = itemView.findViewById(R.id.subtitleText)
        private val playCountText: TextView = itemView.findViewById(R.id.playCountText)
        private val coverArt: ImageView = itemView.findViewById(R.id.albumArt)

        fun bind(item: QueueDisplayItem) {
            positionText.text = item.positionLabel
            titleText.text = item.title
            subtitleText.text = item.subtitle
            playCountText.text = if (item.playCount <= 0) "New" else "${item.playCount} plays"
            val context = itemView.context
            val backgroundColor = ContextCompat.getColor(
                context,
                if (item.isCurrent) R.color.queue_item_current_bg else R.color.queue_item_bg
            )
            itemView.setBackgroundColor(backgroundColor)
            positionText.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.isCurrent) R.color.spotify_text_primary else R.color.spotify_text_secondary
                )
            )
            coverArt.alpha = if (item.isCurrent) 1.0f else 0.7f
            coverArt.load(item.albumImageUrl) {
                placeholder(R.drawable.album_placeholder)
                error(R.drawable.album_placeholder)
                crossfade(true)
            }
            itemView.setOnClickListener {
                onItemClick?.invoke(item, bindingAdapterPosition)
            }
        }
    }
}

