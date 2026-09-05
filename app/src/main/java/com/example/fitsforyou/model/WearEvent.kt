package com.example.fitsforyou.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wear_events")
data class WearEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val itemId: Int, // id of the clothing item
    val timestamp: Long = System.currentTimeMillis()
)
