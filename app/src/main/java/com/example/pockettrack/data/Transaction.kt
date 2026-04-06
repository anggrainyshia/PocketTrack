package com.example.pockettrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String,           // "Income" or "Expense"
    val categoryId: Int,
    val currency: String,       // "USD", "SGD", "IDR"
    val amountInUsd: Double,    // normalized for comparison
    val date: String,           // "yyyy-MM-dd"
    val note: String = "",
    val category: String = ""
)
