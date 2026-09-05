package com.example.fitsforyou.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitsforyou.R
import com.example.fitsforyou.model.OutfitWithClothing

class OutfitAdapter(
    private val onDeleteClick: (OutfitWithClothing) -> Unit,
    private val onEditClick: (OutfitWithClothing) -> Unit
) : ListAdapter<OutfitWithClothing, OutfitAdapter.OutfitViewHolder>(OutfitDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutfitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_outfit, parent, false)
        return OutfitViewHolder(view, onDeleteClick, onEditClick)
    }

    override fun onBindViewHolder(holder: OutfitViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OutfitViewHolder(
        itemView: View,
        private val onDeleteClick: (OutfitWithClothing) -> Unit,
        private val onEditClick: (OutfitWithClothing) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTxt: TextView = itemView.findViewById(R.id.outfitName)
        private val countTxt: TextView = itemView.findViewById(R.id.outfitItemCount)
        private val itemsContainer: LinearLayout = itemView.findViewById(R.id.itemsPreviewContainer)
        private val editBtn: ImageButton = itemView.findViewById(R.id.editOutfitBtn)
        private val deleteBtn: ImageButton = itemView.findViewById(R.id.deleteOutfitBtn)

        fun bind(outfitWithClothing: OutfitWithClothing) {
            val outfit = outfitWithClothing.outfit
            val clothes = outfitWithClothing.clothingItems

            nameTxt.text = outfit.name
            countTxt.text = "${clothes.size} pieces collection"
            
            itemsContainer.removeAllViews()
            clothes.take(3).forEach { item ->
                val box = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.layout_outfit_item_box, itemsContainer, false)
                
                val previewImg = box.findViewById<ImageView>(R.id.itemPreviewImage)
                previewImg.load(item.imageUri) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                }
                
                itemsContainer.addView(box)
            }

            deleteBtn.setOnClickListener { onDeleteClick(outfitWithClothing) }
            editBtn.setOnClickListener { onEditClick(outfitWithClothing) }
            itemView.setOnClickListener { onEditClick(outfitWithClothing) }
        }
    }

    class OutfitDiffCallback : DiffUtil.ItemCallback<OutfitWithClothing>() {
        override fun areItemsTheSame(oldItem: OutfitWithClothing, newItem: OutfitWithClothing): Boolean {
            return oldItem.outfit.id == newItem.outfit.id
        }

        override fun areContentsTheSame(oldItem: OutfitWithClothing, newItem: OutfitWithClothing): Boolean {
            return oldItem == newItem
        }
    }
}
