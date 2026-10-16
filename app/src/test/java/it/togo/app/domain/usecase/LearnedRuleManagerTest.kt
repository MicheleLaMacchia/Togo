package it.togo.app.domain.usecase

import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Test JVM per LearnedRuleManager.
 * Verifica CRUD regole apprese.
 */
class LearnedRuleManagerTest {

    private lateinit var repository: LearnedRulesRepository
    private lateinit var manager: LearnedRuleManager

    private val rule1 = LearnedRule(
        id = "rule-1",
        userExpression = "latte",
        productId = 1L,
        level3Id = 12L,
        isActive = true
    )

    private val rule2 = LearnedRule(
        id = "rule-2",
        userExpression = "caffe",
        productId = 2L,
        level3Id = 5L,
        isActive = false
    )

    @Before
    fun setup() {
        repository = mock()
        manager = LearnedRuleManager(repository)

        `when`(repository.getAll()).thenReturn(flowOf(listOf(rule1, rule2)))
        runBlocking {
            `when`(repository.getById("rule-1")).thenReturn(rule1)
            `when`(repository.getById("rule-2")).thenReturn(rule2)
            `when`(repository.getById("rule-3")).thenReturn(null)
            `when`(repository.upsertAtomic(any())).thenAnswer { invocation ->
                invocation.arguments[0] as LearnedRule
            }
        }
    }

    @Test
    fun `getAll ritorna tutte le regole ordinate`() = runBlocking(Dispatchers.IO) {
        val rules = manager.getAll()

        assertEquals(2, rules.size)
        assertEquals("rule-1", rules[0].id)
        assertEquals("rule-2", rules[1].id)
    }

    @Test
    fun `getActive ritorna solo regole attive`() = runBlocking(Dispatchers.IO) {
        val rules = manager.getActive()

        assertEquals(1, rules.size)
        assertEquals("rule-1", rules[0].id)
        assertTrue(rules[0].isActive)
    }

    @Test
    fun `getAll con Flow vuoto ritorna lista vuota`() = runBlocking(Dispatchers.IO) {
        `when`(repository.getAll()).thenReturn(flowOf(emptyList()))

        val rules = manager.getAll()

        assertTrue(rules.isEmpty())
    }

    @Test
    fun `create normalizza espressione e fa upsert atomico`() = runBlocking(Dispatchers.IO) {
        val rule = manager.create("  LATTE  ", 3L, 10L)

        assertEquals("latte", rule.userExpression)
        assertEquals(3L, rule.productId)
        assertEquals(10L, rule.level3Id)
        assertTrue(rule.isActive)
        verify(repository).upsertAtomic(any())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create con espressione vuota lancia eccezione`() = runBlocking(Dispatchers.IO) {
        manager.create("", 1L, 1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create con sola whitespace lancia eccezione`() = runBlocking(Dispatchers.IO) {
        manager.create("   ", 1L, 1L)
    }

    @Test
    fun `update modifica campi specificati`() = runBlocking(Dispatchers.IO) {
        val updated = manager.update("rule-1", productId = 5L, isActive = false)

        assertEquals("rule-1", updated.id)
        assertEquals(5L, updated.productId)
        assertEquals(12L, updated.level3Id) // invariato
        assertFalse(updated.isActive)
        verify(repository).update(any())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `update rule inesistente lancia eccezione`() = runBlocking(Dispatchers.IO) {
        manager.update("rule-999")
    }

    @Test
    fun `deactivate imposta isActive = false`() = runBlocking(Dispatchers.IO) {
        val deactivated = manager.deactivate("rule-1")

        assertFalse(deactivated.isActive)
        assertEquals("rule-1", deactivated.id)
        verify(repository).update(any())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deactivate rule inesistente lancia eccezione`() = runBlocking(Dispatchers.IO) {
        manager.deactivate("rule-999")
    }

    @Test
    fun `reactivate imposta isActive = true`() = runBlocking(Dispatchers.IO) {
        val reactivated = manager.reactivate("rule-2") // era inactive

        assertTrue(reactivated.isActive)
        assertEquals("rule-2", reactivated.id)
        verify(repository).update(any())
    }

    @Test
    fun `delete ritorna true se rule esisteva`() = runBlocking(Dispatchers.IO) {
        val result = manager.delete("rule-1")

        assertTrue(result)
        verify(repository).delete("rule-1")
    }

    @Test
    fun `delete ritorna false se rule non esisteva`() = runBlocking(Dispatchers.IO) {
        val result = manager.delete("rule-999")

        assertFalse(result)
        verify(repository, never()).delete(any())
    }

    @Test
    fun `countByProduct conta regole attive per prodotto`() = runBlocking(Dispatchers.IO) {
        val count = manager.countByProduct(1L)

        assertEquals(1, count) // solo rule1 è active per productId=1
    }

    @Test(timeout = 100)
    fun `getAll completato sotto 100ms`() = runBlocking(Dispatchers.IO) {
        manager.getAll()
    }

    @Test(timeout = 100)
    fun `create completato sotto 100ms`() = runBlocking(Dispatchers.IO) {
        manager.create("test", 1L, 1L)
    }
}