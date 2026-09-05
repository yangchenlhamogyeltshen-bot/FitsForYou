package com.example.fitsforyou.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class OutfitWithClothing(
    @Embedded val outfit: Outfit,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            OutfitClothingCrossRef::class,
            parentColumn = "id",
            entityColumn = "clothingId"
        )
    )
    val clothingItems: List<Clothing>
)
