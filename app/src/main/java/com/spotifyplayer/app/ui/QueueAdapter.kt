package com.spotifyplayer.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spotifyplayer.app.R

data class QueueDisplayItem(
    val positionLabel: String,
    val title: String,
    val subtitle: String,
    val playCount: Long,
    val isCurrent: Boolean,
    val rawPosition: Int
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

        fun bind(item: QueueDisplayItem) {
            positionText.text = item.positionLabel
            titleText.text = item.title
            subtitleText.text = item.subtitle
            playCountText.text = "plays: ${item.playCount}"
            val bgRes = if (item.isCurrent) R.color.queue_item_current_bg else R.color.queue_item_bg
            itemView.setBackgroundResource(bgRes)
            itemView.setOnClickListener {
                onItemClick?.invoke(item, bindingAdapterPosition)
            }
        }
    }
}

