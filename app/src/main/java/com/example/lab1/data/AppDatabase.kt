package com.example.lab1.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lab1.model.DiscountCard

// version=3: добавлена таблица кэша результатов AI-ревизии.
@Database(
    entities = [DiscountCard::class, AiAnalysisCache::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun aiAnalysisDao(): AiAnalysisDao
}