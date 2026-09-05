package com.example.fitsforyou.model

import androidx.room.Entity

@Entity(primaryKeys = ["id", "clothingId"])
data class OutfitClothingCrossRef(
    val id: Long, // Outfit ID
    val clothingId: Int // Clothing ID
)
