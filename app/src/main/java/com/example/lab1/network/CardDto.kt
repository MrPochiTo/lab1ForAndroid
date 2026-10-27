package com.example.lab1.network

import com.google.gson.annotations.SerializedName

data class CardDto(
    @SerializedName("id")           val id: Int,
    @SerializedName("shopName")     val shopName: String,
    @SerializedName("barcodeNumber") val barcodeNumber: String,
    @SerializedName("categoryId")   val categoryId: Int,
    @SerializedName("colorHex")     val colorHex: Long
)
