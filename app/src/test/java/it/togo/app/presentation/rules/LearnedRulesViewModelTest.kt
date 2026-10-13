package it.togo.app.presentation.rules

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.usecase.LearnedRuleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Test JVM per LearnedRulesViewModel.
 * Verifica MVI: lista, ricerca, CRUD completo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LearnedRulesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        rules: List<LearnedRule> = emptyList(),
        products: Map<Long, CanonicalProduct> = emptyMap()
    ): Triple<LearnedRulesViewModel, LearnedRulesRepository, LearnedRuleManager> {
        val rulesRepo = mock(LearnedRulesRepository::class.java)
        val catalogRepo = mock(CatalogRepository::class.java)
        val manager = mock(LearnedRuleManager::class.java)

        `when`(rulesRepo.getAll()).thenReturn(flowOf(rules))

        products.forEach { (id, product) ->
            `when`(catalogRepo.getById(id)).thenReturn(product)
        }

        val vm = LearnedRulesViewModel(manager, catalogRepo, rulesRepo)
        return Triple(vm, rulesRepo, manager)
    }

    private fun sampleRule(id: String = "r1", expression: String = "latte", active: Boolean = true) = LearnedRule(
        id = id,
        userExpression = expression,
        productId = 1L,
        level3Id = 5L,
        isActive = active
    )

    private val product1 = CanonicalProduct(id = 1L, name = "Latte Intero", level3Id = 5L)
    private val product2 = CanonicalProduct(id = 2L, name = "Caffè Macinato", level3Id = 12L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `stato iniziale vuoto`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()

        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `con regole mostra lista`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2", "caffè"))
        val (vm, _, _) = createViewModel(rules)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isEmpty)
        assertEquals(2, vm.uiState.value.filteredRules.size)
    }

    @Test
    fun `ricerca per espressione filtra`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1", "latte"), sampleRule("r2", "caffè"))
        val (vm, _, _) = createViewModel(rules)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onSearchQueryChange("latte")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("latte", vm.uiState.value.filteredRules[0].userExpression)
    }

    @Test
    fun `ricerca per nome prodotto filtra`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1", "latte"), sampleRule("r2", "caffè"))
        val products = mapOf(1L to product1, 2L to product2)
        val (vm, _, _) = createViewModel(rules, products)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onSearchQueryChange("latte intero")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("latte", vm.uiState.value.filteredRules[0].userExpression)
    }

    @Test
    fun `ricerca vuota mostra tutto`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2"))
        val (vm, _, _) = createViewModel(rules)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onSearchQueryChange("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.uiState.value.filteredRules.size)
    }

    @Test
    fun `regole orfane filtrate fuori`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2"))
        val (vm, _, _) = createViewModel(rules)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("r1", vm.uiState.value.filteredRules[0].id)
    }

    @Test
    fun `disattiva chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.deactivateRule(rule)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(manager).deactivate(rule.id)
    }

    @Test
    fun `riattiva chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule(active = false)
        val (vm, _, manager) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.reactivateRule(rule)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(manager).reactivate(rule.id)
    }

    @Test
    fun `requestDelete apre dialog`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.requestDelete(rule)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.isDeleteDialogOpen)
        assertEquals(rule, vm.uiState.value.ruleToDelete)
    }

    @Test
    fun `confirmDelete chiude dialog e chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.requestDelete(rule)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(manager).delete(rule.id)
        assertFalse(vm.uiState.value.isDeleteDialogOpen)
    }

    @Test
    fun `cancelDelete chiude dialog senza eliminare`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.requestDelete(rule)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.cancelDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isDeleteDialogOpen)
        verify(manager, never()).delete(any())
    }

    @Test
    fun `startEditCategory apre editing`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startEditCategory(rule)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(vm.uiState.value.editingRule)
        assertEquals(rule, vm.uiState.value.editingRule)
    }

    @Test
    fun `updateCategory chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startEditCategory(rule)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.updateCategory(12L)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(manager).update(rule.id, level3Id = 12L)
        assertNull(vm.uiState.value.editingRule)
    }

    @Test
    fun `cancelEdit chiude editing`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        testDispatcher.scheduler.advanceUntilIdle()

        vm.startEditCategory(rule)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.cancelEdit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.uiState.value.editingRule)
    }

    @Test(timeout = 100)
    fun `operazioni ViewModel completate sotto 100ms`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()

        vm.onSearchQueryChange("latte")
        testDispatcher.scheduler.advanceUntilIdle()
    }
}