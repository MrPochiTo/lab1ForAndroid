package com.example.lab1.ai

import com.example.lab1.data.AiAnalysisCache
import com.example.lab1.data.AiAnalysisDao
import com.example.lab1.data.CardDao
import com.example.lab1.data.CardRepository
import com.example.lab1.data.CategoryProvider
import com.example.lab1.model.Category
import com.example.lab1.model.DiscountCard
import com.example.lab1.network.CardApiService
import com.example.lab1.network.CardDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeCollectionUseCaseTest {

    private val categories = listOf(
        Category(1, "Супермаркеты", "shopping_cart"),
        Category(2, "Одежда", "checkroom"),
        Category(3, "Кафе и рестораны", "restaurant")
    )

    private val initialCards = listOf(
        DiscountCard(1, "Пятёрочка", "4600123456789", 1, 0xFF2E7D32),
        DiscountCard(2, "Zara", "2000000000015", 2, 0xFF1565C0),
        DiscountCard(3, "Вкусно и точка", "1234567890123", 3, 0xFFEF6C00)
    )

    private val modelResponse = """
        {"summary":"Одну карту нужно переместить","recommendations":[
          {"cardId":3,"suggestedCategoryId":1,"reason":"Кафе ошибочно отнесено к ресторанам"}
        ]}
    """.trimIndent()

    @Test
    fun executeParsesResponseAndCachesIt() = runBlocking {
        val cards = MutableStateFlow(initialCards)
        val cache = FakeAiAnalysisDao()
        val llm = FakeLlmRepository(modelResponse)
        val useCase = AnalyzeCollectionUseCase(
            cardRepository = CardRepository(FakeCardDao(cards), FakeCardApi()),
            categoryProvider = FakeCategoryProvider(categories),
            llmRepository = llm,
            cacheDao = cache
        )

        val first = useCase.execute(forceRefresh = true)

        assertEquals(1, llm.calls)
        assertFalse(first.fromCache)
        assertEquals(1, first.analysis.recommendations.size)
        assertEquals(3, first.analysis.recommendations.single().cardId)
        assertTrue(llm.lastPrompt.contains("Вкусно и точка"))
        assertTrue(cache.get() != null)

        val second = useCase.execute(forceRefresh = false)

        assertEquals(1, llm.calls)
        assertTrue(second.fromCache)
        assertEquals(first.analysis.summary, second.analysis.summary)
    }

    @Test
    fun changedCollectionInvalidatesCache() = runBlocking {
        val cards = MutableStateFlow(initialCards)
        val cache = FakeAiAnalysisDao()
        val llm = FakeLlmRepository(modelResponse)
        val useCase = AnalyzeCollectionUseCase(
            cardRepository = CardRepository(FakeCardDao(cards), FakeCardApi()),
            categoryProvider = FakeCategoryProvider(categories),
            llmRepository = llm,
            cacheDao = cache
        )

        useCase.execute(forceRefresh = true)
        cards.update { list -> list.map { if (it.id == 3) it.copy(categoryId = 2) else it } }
        val result = useCase.execute(forceRefresh = false)

        assertEquals(2, llm.calls)
        assertFalse(result.fromCache)
    }

    private class FakeCardDao(
        private val cards: MutableStateFlow<List<DiscountCard>>
    ) : CardDao {
        override fun getAll(): Flow<List<DiscountCard>> = cards
        override suspend fun getCount(): Int = cards.value.size

        override suspend fun insert(card: DiscountCard): Long {
            cards.update { it + card }
            return card.id.toLong()
        }

        override suspend fun update(card: DiscountCard): Int {
            cards.update { list -> list.map { if (it.id == card.id) card else it } }
            return 1
        }

        override suspend fun delete(card: DiscountCard): Int {
            cards.update { list -> list.filterNot { it.id == card.id } }
            return 1
        }

        override suspend fun deleteApiCards(): Int {
            cards.update { list -> list.filter { it.isUserAdded } }
            return 1
        }
    }

    private class FakeCardApi : CardApiService {
        override suspend fun getCards(): List<CardDto> = emptyList()
    }

    private class FakeCategoryProvider(
        override val categories: List<Category>
    ) : CategoryProvider {
        override fun getById(id: Int): Category? = categories.find { it.id == id }
    }

    private class FakeLlmRepository(
        private val response: String
    ) : LlmRepository {
        private val mutableState = MutableStateFlow<LlmState>(LlmState.ModelReady)
        override val state: StateFlow<LlmState> = mutableState.asStateFlow()

        var calls = 0
        var lastPrompt = ""

        override fun checkCompatibility(): DeviceCompatibility = DeviceCompatibility(true)
        override fun availableModelUri(): String? = "file:///test/model.gguf"
        override fun modelName(uri: String?): String? = "test.gguf"
        override fun selectModel(uri: String): ModelValidation = ModelValidation(true)
        override fun loadModel(uri: String?) = Unit
        override fun isModelLoaded(): Boolean = true
        override fun stop() = Unit
        override fun release() = Unit

        override suspend fun generate(prompt: String): String {
            calls++
            lastPrompt = prompt
            mutableState.value = LlmState.Success(response)
            return response
        }
    }

    private class FakeAiAnalysisDao : AiAnalysisDao {
        private var cache: AiAnalysisCache? = null

        override suspend fun get(): AiAnalysisCache? = cache

        override suspend fun save(cache: AiAnalysisCache): Long {
            this.cache = cache
            return 1
        }

        override suspend fun clear(): Int {
            cache = null
            return 1
        }
    }
}
