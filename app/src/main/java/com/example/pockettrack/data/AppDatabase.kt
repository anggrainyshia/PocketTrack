package com.example.pockettrack.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Transaction::class, Category::class, Budget::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "pockettrack_v2")
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.categoryDao()?.let { dao ->
                                    if (dao.count() == 0) seedCategories(dao)
                                }
                            }
                        }
                    })
                    .build().also { INSTANCE = it }
            }

        private suspend fun seedCategories(dao: CategoryDao) {
            val defaults = listOf(
                Category(name = "Food & Drink",   icon = "🍔", color = "#FF6B6B", type = "Expense"),
                Category(name = "Transport",       icon = "🚗", color = "#4ECDC4", type = "Expense"),
                Category(name = "Shopping",        icon = "🛍️", color = "#A855F7", type = "Expense"),
                Category(name = "Bills",           icon = "💡", color = "#F59E0B", type = "Expense"),
                Category(name = "Health",          icon = "💊", color = "#EF4444", type = "Expense"),
                Category(name = "Entertainment",   icon = "🎮", color = "#3B82F6", type = "Expense"),
                Category(name = "Education",       icon = "📚", color = "#6366F1", type = "Expense"),
                Category(name = "Other",           icon = "📦", color = "#9CA3AF", type = "Expense"),
                Category(name = "Salary",          icon = "💼", color = "#10B981", type = "Income"),
                Category(name = "Freelance",       icon = "💻", color = "#059669", type = "Income"),
                Category(name = "Investment",      icon = "📈", color = "#0EA5E9", type = "Income"),
                Category(name = "Gift",            icon = "🎁", color = "#F472B6", type = "Income"),
            )
            defaults.forEach { dao.insert(it) }
        }
    }
}