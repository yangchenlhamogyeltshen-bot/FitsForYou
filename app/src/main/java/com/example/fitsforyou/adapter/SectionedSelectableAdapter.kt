package com.example.fitsforyou.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitsforyou.R
import com.example.fitsforyou.model.Clothing

class SectionedSelectableAdapter(
    private val onSelectionChanged: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<Any>()
    private val selectedIds = mutableSetOf<Int>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    fun submitData(newList: List<Any>) {
        items = newList
        notifyDataSetChanged()
    }

    fun toggleSelection(id: Int) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        notifyDataSetChanged()
    }

    fun setSelection(ids: List<Int>) {
        selectedIds.clear()
        selectedIds.addAll(ids)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = if (items[position] is String) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_section_header, parent, false))
        } else {
            ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_selectable_clothing_small, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            holder.bind(items[position] as String)
        } else if (holder is ItemViewHolder) {
            val clothing = items[position] as Clothing
            holder.bind(clothing, selectedIds.contains(clothing.id))
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.headerTitle)
        fun bind(text: String) { title.text = text }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image = view.findViewById<ImageView>(R.id.clothingImage)
        private val name = view.findViewById<TextView>(R.id.clothingName)
        private val overlay = view.findViewById<View>(R.id.selectedOverlay)
        private val check = view.findViewById<ImageView>(R.id.checkIcon)

        fun bind(clothing: Clothing, isSelected: Boolean) {
            name.text = clothing.name
            image.load(clothing.imageUri) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
            }
            overlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            check.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            itemView.setOnClickListener {
                onSelectionChanged(clothing.id, !isSelected)
            }
        }
    }
}
