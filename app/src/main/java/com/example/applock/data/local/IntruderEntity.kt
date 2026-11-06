package com.example.applock.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intruders")
data class IntruderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attemptedPackage: String
)
