package com.example.fitsforyou.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String, // Firebase UID
    val fullName: String,
    val email: String
)
