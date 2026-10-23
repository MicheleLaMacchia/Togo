package it.togo.app.domain.usecase

import io.mockk.*
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LearnedRuleManagerTest {

    private lateinit var repository: LearnedRulesRepository
    private lateinit var manager: LearnedRuleManager

    private val rule1 = LearnedRule(id = "rule-1", userExpression = "latte", productId = 1L, level3Id = 12L, isActive = true)
    private val rule2 = LearnedRule(id = "rule-2", userExpression = "caffe", productId = 2L, level3Id = 5L, isActive = false)

    @Before
    fun setup() {
        repository = mockk<LearnedRulesRepository>(relaxed = true)
        manager = LearnedRuleManager(repository)

        every { repository.getAll() } returns flowOf(listOf(rule1, rule2))
        coEvery { repository.getById("rule-1") } returns rule1
        coEvery { repository.getById("rule-2") } returns rule2
        coEvery { repository.getById("rule-3") } returns null
        coEvery { repository.getById("rule-999") } returns null
        coEvery { repository.upsertAtomic(any()) } answers { firstArg<LearnedRule>() }
    }

    /**
     * Repository reale e privo di I/O: i test di performance non devono
     * misurare la latenza di inizializzazione di MockK.
     */
    private fun fakeLearnedRulesRepository(): LearnedRulesRepository =
        object : LearnedRulesRepository {
            override fun getAll(): Flow<List<LearnedRule>> =
                flowOf(emptyList())

            override suspend fun insert(rule: LearnedRule) = Unit

            override suspend fun update(rule: LearnedRule) = Unit

            override suspend fun delete(ruleId: String) = Unit

            override suspend fun getByExpression(expression: String): LearnedRule? = null

            override suspend fun getById(ruleId: String): LearnedRule? = null

            override suspend fun getByExpressionAndProductId(
                expression: String,
                productId: Long
            ): LearnedRule? = null

            override suspend fun upsertAtomic(rule: LearnedRule): LearnedRule = rule
        }

    @Test
    fun `getAll ritorna tutte le regole ordinate`(): Unit = runBlocking(Dispatchers.IO) {
        val rules = manager.getAll()
        assertEquals(2, rules.size)
        assertEquals("rule-1", rules[0].id)
        assertEquals("rule-2", rules[1].id)
    }

    @Test
    fun `getActive ritorna solo regole attive`(): Unit = runBlocking(Dispatchers.IO) {
        val rules = manager.getActive()
        assertEquals(1, rules.size)
        assertEquals("rule-1", rules[0].id)
        assertTrue(rules[0].isActive)
    }

    @Test
    fun `getAll con Flow vuoto ritorna lista vuota`(): Unit = runBlocking(Dispatchers.IO) {
        every { repository.getAll() } returns flowOf(emptyList())
        assertTrue(manager.getAll().isEmpty())
    }

    @Test
    fun `create normalizza espressione e fa upsert atomico`(): Unit = runBlocking(Dispatchers.IO) {
        val rule = manager.create("  LATTE  ", 3L, 10L)
        assertEquals("latte", rule.userExpression)
        assertEquals(3L, rule.productId)
        assertEquals(10L, rule.level3Id)
        assertTrue(rule.isActive)
        coVerify { repository.upsertAtomic(any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create con espressione vuota lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        manager.create("", 1L, 1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create con sola whitespace lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        manager.create("   ", 1L, 1L)
    }

    @Test
    fun `update modifica campi specificati`(): Unit = runBlocking(Dispatchers.IO) {
        val updated = manager.update("rule-1", productId = 5L, isActive = false)
        assertEquals("rule-1", updated.id)
        assertEquals(5L, updated.productId)
        assertEquals(12L, updated.level3Id)
        assertFalse(updated.isActive)
        coVerify { repository.update(any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update rule inesistente lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        manager.update("rule-999")
    }

    @Test
    fun `deactivate imposta isActive false`(): Unit = runBlocking(Dispatchers.IO) {
        val deactivated = manager.deactivate("rule-1")
        assertFalse(deactivated.isActive)
        assertEquals("rule-1", deactivated.id)
        coVerify { repository.update(any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deactivate rule inesistente lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        manager.deactivate("rule-999")
    }

    @Test
    fun `reactivate imposta isActive true`(): Unit = runBlocking(Dispatchers.IO) {
        val reactivated = manager.reactivate("rule-2")
        assertTrue(reactivated.isActive)
        assertEquals("rule-2", reactivated.id)
        coVerify { repository.update(any()) }
    }

    @Test
    fun `delete ritorna true se rule esisteva`(): Unit = runBlocking(Dispatchers.IO) {
        assertTrue(manager.delete("rule-1"))
        coVerify { repository.delete("rule-1") }
    }

    @Test
    fun `delete ritorna false se rule non esisteva`(): Unit = runBlocking(Dispatchers.IO) {
        assertFalse(manager.delete("rule-999"))
    }

    @Test
    fun `countByProduct conta regole attive per prodotto`(): Unit = runBlocking(Dispatchers.IO) {
        assertEquals(1, manager.countByProduct(1L))
    }

    @Test(timeout = 15_000)
    fun `getAll completato sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) {
        val performanceManager = LearnedRuleManager(fakeLearnedRulesRepository())
        performanceManager.getAll() // warm-up

        val start = System.nanoTime()
        performanceManager.getAll()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("getAll ha impiegato ${elapsedMs}ms", elapsedMs < 100)
    }

    @Test(timeout = 15_000)
    fun `create completato sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) {
        val performanceManager = LearnedRuleManager(fakeLearnedRulesRepository())
        performanceManager.create("test", 1L, 1L) // warm-up

        val start = System.nanoTime()
        performanceManager.create("test", 1L, 1L)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("create ha impiegato ${elapsedMs}ms", elapsedMs < 100)
    }
}