package com.example.lab1.ai

import com.example.lab1.model.Category
import com.example.lab1.model.DiscountCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmResponseParserTest {

    private val parser = LlmResponseParser()
    private val categories = listOf(
        Category(1, "Супермаркеты", "shopping_cart"),
        Category(2, "Одежда", "checkroom"),
        Category(3, "Кафе и рестораны", "restaurant")
    )
    private val cards = listOf(
        DiscountCard(1, "Пятёрочка", "4600123456789", 1, 0xFF2E7D32),
        DiscountCard(2, "Zara", "2000000000015", 2, 0xFF1565C0),
        DiscountCard(3, "Вкусно и точка", "1234567890123", 3, 0xFFEF6C00)
    )

    @Test
    fun parsesJsonInsideMarkdownFenceAndFillsNames() {
        val raw = """
            ```json
            {"summary":"Нужно переместить одну карту","recommendations":[
              {"cardId":2,"suggestedCategoryId":3,"reason":"Магазин одежды стоит в категории кафе"}
            ]}
            ```
        """.trimIndent()

        val result = parser.parse(raw, cards, categories)

        assertTrue(result.structured)
        assertEquals(1, result.recommendations.size)
        val recommendation = result.recommendations.single()
        assertEquals("Zara", recommendation.shopName)
        assertEquals("Одежда", recommendation.currentCategoryName)
        assertEquals("Кафе и рестораны", recommendation.suggestedCategoryName)
        assertEquals("Магазин одежды стоит в категории кафе", recommendation.reason)
    }

    @Test
    fun resolvesCategoryByNameWhenIdIsWrong() {
        val raw = """
            {"summary":"Проверка","recommendations":[
              {"cardId":2,"suggestedCategoryId":1,
               "suggestedCategoryName":"Кафе и рестораны",
               "reason":"Магазин одежды стоит в категории кафе"}
            ]}
        """.trimIndent()

        val result = parser.parse(raw, cards, categories)

        assertEquals(1, result.recommendations.size)
        assertEquals(
            "Кафе и рестораны",
            result.recommendations.single().suggestedCategoryName
        )
    }

    @Test
    fun dropsUnknownCardsAndSameCategorySuggestions() {
        val raw = """
            {"summary":"Проверка","recommendations":[
              {"cardId":99,"suggestedCategoryId":2,"reason":"Нет такой карты"},
              {"cardId":1,"suggestedCategoryId":1,"reason":"Категория не меняется"},
              {"cardId":3,"suggestedCategoryId":1,"reason":"Кафе должно быть в супермаркетах"}
            ]}
        """.trimIndent()

        val result = parser.parse(raw, cards, categories)

        assertEquals(1, result.recommendations.size)
        assertEquals(3, result.recommendations.single().cardId)
    }

    @Test
    fun returnsUnstructuredResultWhenJsonIsBroken() {
        val result = parser.parse("Не могу разобрать коллекцию", cards, categories)

        assertFalse(result.structured)
        assertTrue(result.recommendations.isEmpty())
        assertEquals("Не могу разобрать коллекцию", result.summary)
    }
}
