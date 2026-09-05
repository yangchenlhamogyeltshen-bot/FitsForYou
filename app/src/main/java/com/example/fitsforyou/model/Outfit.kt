package com.example.fitsforyou.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfits")
data class Outfit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val timesWorn: Int = 0,
    val lastWorn: Long? = null
)
