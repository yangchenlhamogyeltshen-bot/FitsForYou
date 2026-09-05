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
import coil.load
import com.example.fitsforyou.R
import com.example.fitsforyou.model.Clothing

class ClothingAdapter(
    private val onItemClick: (Clothing) -> Unit,
    private val onItemLongClick: (Clothing) -> Unit
) : ListAdapter<Clothing, ClothingAdapter.ClothingViewHolder>(ClothingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClothingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clothing, parent, false)
        return ClothingViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ClothingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ClothingViewHolder(
        itemView: View,
        private val onItemClick: (Clothing) -> Unit,
        private val onItemLongClick: (Clothing) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.itemImageView)
        private val nameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.itemCategoryTextView)
        private val capsuleBadge: View = itemView.findViewById(R.id.capsuleBadge)

        fun bind(clothing: Clothing) {
            nameTextView.text = clothing.name
            categoryTextView.text = clothing.category
            capsuleBadge.visibility = if (clothing.isCapsule) View.VISIBLE else View.GONE

            imageView.load(clothing.imageUri) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }

            itemView.setOnClickListener { onItemClick(clothing) }
            itemView.setOnLongClickListener {
                onItemLongClick(clothing)
                true
            }
        }
    }

    class ClothingDiffCallback : DiffUtil.ItemCallback<Clothing>() {
        override fun areItemsTheSame(oldItem: Clothing, newItem: Clothing): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Clothing, newItem: Clothing): Boolean {
            return oldItem == newItem
        }
    }
}
