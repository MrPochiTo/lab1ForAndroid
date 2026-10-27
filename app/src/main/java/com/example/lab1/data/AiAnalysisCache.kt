package com.example.lab1.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "ai_analysis_cache")
data class AiAnalysisCache(
    @PrimaryKey val id: Int = 1,
    val resultJson: String,
    val cardsHash: String,
    val createdAt: Long
)

@Dao
interface AiAnalysisDao {

    @Query("SELECT * FROM ai_analysis_cache WHERE id = 1")
    suspend fun get(): AiAnalysisCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cache: AiAnalysisCache): Long

    @Query("DELETE FROM ai_analysis_cache")
    suspend fun clear(): Int
}
