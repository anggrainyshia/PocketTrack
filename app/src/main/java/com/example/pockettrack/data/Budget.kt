package com.example.pockettrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val limitAmount: Double,
    val currency: String,
    val month: String           // "yyyy-MM"
)