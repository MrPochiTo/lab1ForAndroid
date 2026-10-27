package com.example.lab1.data

import android.content.Context
import com.example.lab1.model.Category
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class CategoryRepository(private val context: Context) : CategoryProvider {

    override val categories: List<Category> = loadCategories()

    private fun loadCategories(): List<Category> {
        try {
            val inputStream = context.assets.open("categories.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<Category>>() {}.type
            val result: List<Category> = Gson().fromJson(json, listType)
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    override fun getById(id: Int): Category? {
        for (category in categories) {
            if (category.id == id) {
                return category
            }
        }
        return null
    }
}
