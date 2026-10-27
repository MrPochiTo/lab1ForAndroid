package com.example.lab1.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lab1.ai.AnalyzeCollectionUseCase
import com.example.lab1.ai.LlmRepository
import com.example.lab1.ai.LlmRepositoryImpl
import com.example.lab1.data.AppDatabase
import com.example.lab1.data.CardRepository
import com.example.lab1.data.CategoryProvider
import com.example.lab1.data.CategoryRepository
import com.example.lab1.network.RetrofitClient
import com.example.lab1.ui.MainViewModel
import com.example.lab1.ui.ai.AiReviewViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ai_analysis_cache` (
                `id` INTEGER NOT NULL,
                `resultJson` TEXT NOT NULL,
                `cardsHash` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "cards.db")
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().cardDao() }
    single { get<AppDatabase>().aiAnalysisDao() }

    single { RetrofitClient.cardApiService }
    single { CardRepository(get(), get()) }
    single { CategoryRepository(androidContext()) }
    single<CategoryProvider> { get<CategoryRepository>() }

    single<LlmRepository> { LlmRepositoryImpl(androidContext()) }
    single { AnalyzeCollectionUseCase(get(), get(), get(), get()) }

    viewModel { MainViewModel(get(), get()) }
    viewModel { AiReviewViewModel(get(), get()) }
}
