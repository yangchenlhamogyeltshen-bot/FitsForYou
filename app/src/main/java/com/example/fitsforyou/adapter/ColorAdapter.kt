package com.example.fitsforyou.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fitsforyou.R
import com.google.android.material.imageview.ShapeableImageView

class ColorAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_color_picker, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position == selectedPosition)
    }

    override fun getItemCount() = colors.size

    fun setSelectedColor(color: String) {
        selectedPosition = colors.indexOf(color)
        notifyDataSetChanged()
    }

    inner class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val colorCircle = view.findViewById<ShapeableImageView>(R.id.colorCircle)
        private val selectedRing = view.findViewById<View>(R.id.selectedRing)

        fun bind(colorHex: String, isSelected: Boolean) {
            try {
                colorCircle.setBackgroundColor(Color.parseColor(colorHex))
            } catch (e: Exception) {
                colorCircle.setBackgroundColor(Color.GRAY)
            }
            
            selectedRing.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                selectedPosition = adapterPosition
                onColorSelected(colorHex)
                notifyDataSetChanged()
            }
        }
    }
}
