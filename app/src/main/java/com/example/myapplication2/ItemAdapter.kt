package com.example.myapplication2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.myapplication2.databinding.ItemRowBinding
import java.text.SimpleDateFormat
import java.util.*

class ItemAdapter(
    private val items: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {


    inner class ItemViewHolder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {

        val binding = ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        // Get the specific item for the current row
        val currentItem = items[position]
        val context = holder.itemView.context

        holder.itemView.setOnClickListener {
            onItemClick(currentItem)
        }
        // -----------------------------------------------------------

        holder.binding.apply {

            ivItemImage.load(currentItem.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
            }

            tvItemTitle.text = currentItem.title

            // Format the timestamp from Firestore into a readable date string (e.g., "Jun 26, 2025")
            if (currentItem.datePosted != null) {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvItemDate.text = "Posted on ${sdf.format(currentItem.datePosted)}"
            } else {
                tvItemDate.text = "Date not available"
            }

            tvItemStatus.text = currentItem.status
            // Set the background color of the tag based on the item's status
            if (currentItem.status == "LOST") {
                tvItemStatus.background.setTint(ContextCompat.getColor(context, R.color.color_lost_red))
            } else {
                tvItemStatus.background.setTint(ContextCompat.getColor(context, R.color.color_found_green))
            }
        }
    }
}