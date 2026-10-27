package com.example.lab1.data

import com.example.lab1.model.Category

interface CategoryProvider {
    val categories: List<Category>
    fun getById(id: Int): Category?
}
