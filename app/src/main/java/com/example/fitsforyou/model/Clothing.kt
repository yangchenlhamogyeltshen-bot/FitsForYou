package com.example.fitsforyou.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clothing")
data class Clothing(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val name: String,
    val category: String,
    val color: String,
    val season: String,
    val occasion: String = "Casual",
    val imageUri: String? = null,
    val isCapsule: Boolean = false,
    val timesWorn: Int = 0,
    val lastWorn: Long? = null,
    val addedOn: Long = System.currentTimeMillis()
)
