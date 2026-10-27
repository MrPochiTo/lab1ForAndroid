package com.example.lab1.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lab1.model.DiscountCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY id")
    fun getAll(): Flow<List<DiscountCard>>

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: DiscountCard): Long

    @Update
    suspend fun update(card: DiscountCard): Int

    @Delete
    suspend fun delete(card: DiscountCard): Int

    @Query("DELETE FROM cards WHERE isUserAdded = 0")
    suspend fun deleteApiCards(): Int
}