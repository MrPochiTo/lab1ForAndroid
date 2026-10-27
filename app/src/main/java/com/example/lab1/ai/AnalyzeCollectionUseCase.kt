package com.example.lab1.ai

import com.example.lab1.data.AiAnalysisCache
import com.example.lab1.data.AiAnalysisDao
import com.example.lab1.data.CardRepository
import com.example.lab1.data.CategoryProvider
import com.example.lab1.model.Category
import com.example.lab1.model.DiscountCard
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.first

class AnalyzeCollectionUseCase(
    private val cardRepository: CardRepository,
    private val categoryProvider: CategoryProvider,
    private val llmRepository: LlmRepository,
    private val cacheDao: AiAnalysisDao
) {

    private val gson = Gson()
    private val parser = LlmResponseParser()

    suspend fun getCached(): AnalysisResult? {
        val cards = cardRepository.cards.first()
        val categories = categoryProvider.categories
        if (cards.isEmpty() || categories.isEmpty()) return null

        val cache = cacheDao.get() ?: return null
        if (cache.cardsHash != fingerprint(cards, categories)) return null

        val analysis = runCatching {
            gson.fromJson(cache.resultJson, CollectionAnalysis::class.java)
        }.getOrNull() ?: return null

        return AnalysisResult(analysis, fromCache = true, createdAt = cache.createdAt)
    }

    suspend fun execute(forceRefresh: Boolean = false): AnalysisResult {
        val cards = cardRepository.cards.first()
        require(cards.isNotEmpty()) { "В коллекции нет карт для анализа." }

        val categories = categoryProvider.categories
        require(categories.isNotEmpty()) { "Не удалось загрузить категории карт." }

        if (!forceRefresh) {
            getCached()?.let { return it }
        }

        val rawResponse = llmRepository.generate(LlmPromptBuilder.build(cards, categories))
        val analysis = parser.parse(rawResponse, cards, categories)
        val createdAt = System.currentTimeMillis()

        cacheDao.save(
            AiAnalysisCache(
                resultJson = gson.toJson(analysis),
                cardsHash = fingerprint(cards, categories),
                createdAt = createdAt
            )
        )

        return AnalysisResult(analysis, fromCache = false, createdAt = createdAt)
    }

    private fun fingerprint(cards: List<DiscountCard>, categories: List<Category>): String =
        buildString {
            cards.sortedBy { it.id }.forEach {
                append(it.id).append(':').append(it.shopName).append(':').append(it.categoryId).append('|')
            }
            categories.sortedBy { it.id }.forEach {
                append(it.id).append(':').append(it.name).append('|')
            }
        }.hashCode().toString()
}

object LlmPromptBuilder {

    fun build(cards: List<DiscountCard>, categories: List<Category>): String {
        require(cards.isNotEmpty()) { "Коллекция карт пуста." }
        require(categories.isNotEmpty()) { "Справочник категорий пуст." }

        val categoryById = categories.associateBy { it.id }
        val categoriesText = categories.joinToString("\n") {
            val hint = when (it.name.lowercase()) {
                "супермаркеты" -> "продуктовые магазины и сети супермаркетов"
                "одежда" -> "одежда, обувь и fashion-магазины"
                "кафе и рестораны" -> "кафе, рестораны, фастфуд и кофейни"
                "аптеки" -> "аптеки, лекарства и товары для здоровья"
                else -> "категория карт"
            }
            "- id=${it.id}, название=${it.name} ($hint)"
        }
        val cardsText = cards.joinToString("\n") {
            val category = categoryById[it.categoryId]?.name ?: "неизвестная"
            "- id=${it.id}, магазин=${it.shopName}, штрихкод=${it.barcodeNumber}, " +
                "categoryId=${it.categoryId}, категория=$category"
        }

        return """
            <|im_start|>system
            Проверь коллекцию дисконтных карт. Найди карты, которые лежат не в своей категории, предложи правильную категорию и объясни причину. Не считай карты и не пересказывай список. Используй только данные ниже. Сначала кратко определи, какая карта лежит не в своей категории. Затем верни JSON: {"summary":"краткий итог","recommendations":[{"cardId":1,"suggestedCategoryId":2,"suggestedCategoryName":"название категории","reason":"причина"}]}. Если нашёл хотя бы одну ошибку, recommendations не должен быть пустым: укажи cardId из списка карт, suggestedCategoryId и suggestedCategoryName из списка категорий. suggestedCategoryName должен точно совпадать с названием категории из списка. Если ошибок нет, recommendations должен быть пустым.
            <|im_end|>
            <|im_start|>user
            Категории:
            $categoriesText

            Карты:
            $cardsText
            <|im_end|>
            <|im_start|>assistant
        """.trimIndent().trim()
    }
}

class LlmResponseParser(private val gson: Gson = Gson()) {

    fun parse(
        rawResponse: String,
        cards: List<DiscountCard>,
        categories: List<Category>
    ): CollectionAnalysis {
        val raw = rawResponse.trim()
        if (raw.isEmpty()) {
            return CollectionAnalysis("Модель вернула пустой ответ.", structured = false)
        }

        val json = extractJson(raw)
        val dto = json?.let {
            try {
                gson.fromJson(it, AnalysisDto::class.java)
            } catch (_: JsonSyntaxException) {
                null
            }
        }

        if (dto == null) {
            return CollectionAnalysis(raw, structured = false)
        }

        val cardsById = cards.associateBy { it.id }
        val categoriesById = categories.associateBy { it.id }
        val usedCardIds = mutableSetOf<Int>()

        val recommendations = dto.recommendations.orEmpty().mapNotNull { item ->
            val cardId = item.cardId ?: return@mapNotNull null
            val card = cardsById[cardId] ?: return@mapNotNull null
            if (!usedCardIds.add(cardId)) return@mapNotNull null

            val suggestedCategory = item.suggestedCategoryName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { name ->
                    categoriesById.values.firstOrNull { it.name.equals(name, ignoreCase = true) }
                }
                ?: item.suggestedCategoryId?.let(categoriesById::get)
                ?: return@mapNotNull null
            if (suggestedCategory.id == card.categoryId) return@mapNotNull null

            AiRecommendation(
                cardId = card.id,
                shopName = card.shopName,
                currentCategoryId = card.categoryId,
                currentCategoryName = categoriesById[card.categoryId]?.name ?: "Без категории",
                suggestedCategoryId = suggestedCategory.id,
                suggestedCategoryName = suggestedCategory.name,
                reason = item.reason?.trim().orEmpty().ifBlank {
                    "Карта больше соответствует категории ${suggestedCategory.name}."
                }
            )
        }

        val summary = dto.summary
            ?.trim()
            ?.takeUnless { it.isBlank() || it.equals("краткий итог", ignoreCase = true) }
            ?: if (recommendations.isEmpty()) {
                "Модель не обнаружила карт, требующих перемещения."
            } else {
                "Модель предлагает переместить карт: ${recommendations.size}."
            }

        return CollectionAnalysis(summary, recommendations, structured = true)
    }

    private fun extractJson(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else null
    }

    private data class AnalysisDto(
        val summary: String? = null,
        val recommendations: List<RecommendationDto>? = null
    )

    private data class RecommendationDto(
        val cardId: Int? = null,
        val suggestedCategoryId: Int? = null,
        val suggestedCategoryName: String? = null,
        val reason: String? = null
    )
}
