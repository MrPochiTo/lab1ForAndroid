package com.example.lab1.network

import retrofit2.http.GET

interface CardApiService {
    @GET("cards")
    suspend fun getCards(): List<CardDto>
}
