package com.example.fitsforyou.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitsforyou.R
import com.example.fitsforyou.model.Clothing

class SelectedItemsAdapter(
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedItemsAdapter.SelectedViewHolder>() {

    private var items = listOf<Clothing>()

    fun submitList(newList: List<Clothing>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_clothing, parent, false)
        return SelectedViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class SelectedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image = view.findViewById<ImageView>(R.id.clothingImage)
        fun bind(clothing: Clothing) {
            image.load(clothing.imageUri) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
            }
            itemView.setOnClickListener { onRemoveClick(clothing.id) }
        }
    }
}
