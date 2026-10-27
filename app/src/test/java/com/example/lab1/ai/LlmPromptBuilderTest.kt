package com.example.lab1.ai

import com.example.lab1.model.Category
import com.example.lab1.model.DiscountCard
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmPromptBuilderTest {

    private val categories = listOf(
        Category(1, "Супермаркеты", "shopping_cart"),
        Category(2, "Одежда", "checkroom"),
        Category(3, "Кафе и рестораны", "restaurant")
    )

    private val cards = listOf(
        DiscountCard(1, "Пятёрочка", "4600123456789", 1, 0xFF2E7D32),
        DiscountCard(2, "Zara", "2000000000015", 3, 0xFF1565C0)
    )

    @Test
    fun promptContainsAllCardsCategoriesAndChatMl() {
        val prompt = LlmPromptBuilder.build(cards, categories)

        assertTrue(prompt.startsWith("<|im_start|>system"))
        assertTrue(prompt.contains("<|im_start|>user"))
        assertTrue(prompt.endsWith("<|im_start|>assistant"))
        assertTrue(prompt.contains("Пятёрочка"))
        assertTrue(prompt.contains("Zara"))
        assertTrue(prompt.contains("Супермаркеты"))
        assertTrue(prompt.contains("Одежда"))
        assertTrue(prompt.contains("categoryId=3"))
        assertTrue(prompt.contains("categoryId=1"))
        assertTrue(prompt.contains("\"recommendations\""))
    }
}
