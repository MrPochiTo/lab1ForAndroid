package com.example.lab1.di

import com.example.lab1.data.CardRepository
import com.example.lab1.ui.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CardRepository() }
    viewModel { MainViewModel(get()) }
}