package com.example.fitsforyou.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitsforyou.R
import com.example.fitsforyou.model.Clothing

class SelectableClothingAdapter(private val onSelectionChanged: (List<Int>) -> Unit) :
    ListAdapter<Clothing, SelectableClothingAdapter.ViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selectable_clothing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectedIds.contains(item.id)) {
            if (selectedIds.contains(item.id)) {
                selectedIds.remove(item.id)
            } else {
                selectedIds.add(item.id)
            }
            onSelectionChanged(selectedIds.toList())
            notifyItemChanged(position)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.clothingImage)
        private val name: TextView = itemView.findViewById(R.id.clothingName)
        private val overlay: View = itemView.findViewById(R.id.selectedOverlay)
        private val check: View = itemView.findViewById(R.id.checkIcon)

        fun bind(item: Clothing, isSelected: Boolean, onClick: () -> Unit) {
            name.text = item.name
            if (item.imageUri != null) {
                try {
                    image.setImageURI(Uri.parse(item.imageUri))
                } catch (e: Exception) {
                    image.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } else {
                image.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            overlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            check.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick() }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Clothing>() {
        override fun areItemsTheSame(oldItem: Clothing, newItem: Clothing) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Clothing, newItem: Clothing) = oldItem == newItem
    }
}
