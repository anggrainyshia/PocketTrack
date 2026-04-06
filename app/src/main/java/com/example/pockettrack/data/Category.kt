package com.example.pockettrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,           // emoji icon e.g. "🍔"
    val color: String,          // hex color e.g. "#FF5733"
    val parentId: Int = 0,      // 0 = top-level, >0 = sub-category
    val type: String = "Expense" // "Income" or "Expense"
)