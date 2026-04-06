package com.example.pockettrack.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAll(): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = 0 ORDER BY name ASC")
    fun getTopLevel(): LiveData<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY name ASC")
    fun getSubCategories(parentId: Int): LiveData<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(c: Category)

    @Update
    suspend fun update(c: Category)

    @Delete
    suspend fun delete(c: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
