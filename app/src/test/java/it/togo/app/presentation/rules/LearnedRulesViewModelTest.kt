package it.togo.app.presentation.rules

import io.mockk.*
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.usecase.LearnedRuleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearnedRulesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(
        rules: List<LearnedRule> = emptyList(),
        products: Map<Long, CanonicalProduct> = emptyMap()
    ): Triple<LearnedRulesViewModel, LearnedRulesRepository, LearnedRuleManager> {
        val rulesRepo = mockk<LearnedRulesRepository>()
        val catalogRepo = mockk<CatalogRepository>()
        val manager = mockk<LearnedRuleManager>()

        every { rulesRepo.getAll() } returns flowOf(rules)
        coEvery { catalogRepo.getById(any()) } returns null
        coEvery { manager.deactivate(any()) } just Runs
        coEvery { manager.reactivate(any()) } just Runs
        coEvery { manager.delete(any()) } returns true
        coEvery { manager.update(any(), any(), any()) } returns sampleRule()
        products.forEach { (id, product) ->
            coEvery { catalogRepo.getById(id) } returns product
        }

        val vm = LearnedRulesViewModel(manager, catalogRepo, rulesRepo)
        return Triple(vm, rulesRepo, manager)
    }

    private fun sampleRule(id: String = "r1", expression: String = "latte", active: Boolean = true) = LearnedRule(
        id = id, userExpression = expression, productId = 1L, level3Id = 5L, isActive = active
    )

    private val product1 = CanonicalProduct(id = 1L, name = "Latte Intero", level3Id = 5L)
    private val product2 = CanonicalProduct(id = 2L, name = "Caffè Macinato", level3Id = 12L)

    @Test
    fun `stato iniziale vuoto`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `con regole mostra lista`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2", "caffè"))
        val (vm, _, _) = createViewModel(rules)
        Thread.sleep(200)

        assertFalse(vm.uiState.value.isEmpty)
        assertEquals(2, vm.uiState.value.filteredRules.size)
    }

    @Test
    fun `ricerca per espressione filtra`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1", "latte"), sampleRule("r2", "caffè"))
        val (vm, _, _) = createViewModel(rules)
        Thread.sleep(200)

        vm.onSearchQueryChange("latte")
        Thread.sleep(200)

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("latte", vm.uiState.value.filteredRules[0].userExpression)
    }

    @Test
    fun `ricerca per nome prodotto filtra`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1", "latte"), sampleRule("r2", "caffè"))
        val products = mapOf(1L to product1, 2L to product2)
        val (vm, _, _) = createViewModel(rules, products)
        Thread.sleep(200)

        vm.onSearchQueryChange("latte intero")
        Thread.sleep(200)

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("latte", vm.uiState.value.filteredRules[0].userExpression)
    }

    @Test
    fun `ricerca vuota mostra tutto`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2"))
        val (vm, _, _) = createViewModel(rules)
        Thread.sleep(200)

        vm.onSearchQueryChange("")
        Thread.sleep(200)

        assertEquals(2, vm.uiState.value.filteredRules.size)
    }

    @Test
    fun `regole orfane filtrate fuori`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2"))
        val (vm, _, _) = createViewModel(rules)
        Thread.sleep(200)

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("r1", vm.uiState.value.filteredRules[0].id)
    }

    @Test
    fun `disattiva chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.deactivateRule(rule)
        Thread.sleep(200)

        coVerify { manager.deactivate(rule.id) }
    }

    @Test
    fun `riattiva chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule(active = false)
        val (vm, _, manager) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.reactivateRule(rule)
        Thread.sleep(200)

        coVerify { manager.reactivate(rule.id) }
    }

    @Test
    fun `requestDelete apre dialog`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.requestDelete(rule)
        Thread.sleep(200)

        assertTrue(vm.uiState.value.isDeleteDialogOpen)
        assertEquals(rule, vm.uiState.value.ruleToDelete)
    }

    @Test
    fun `confirmDelete chiude dialog e chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.requestDelete(rule)
        Thread.sleep(200)
        vm.confirmDelete()
        Thread.sleep(200)

        coVerify { manager.delete(rule.id) }
        assertFalse(vm.uiState.value.isDeleteDialogOpen)
    }

    @Test
    fun `cancelDelete chiude dialog senza eliminare`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.requestDelete(rule)
        Thread.sleep(200)
        vm.cancelDelete()
        Thread.sleep(200)

        assertFalse(vm.uiState.value.isDeleteDialogOpen)
        coVerify(exactly = 0) { manager.delete(any()) }
    }

    @Test
    fun `startEditCategory apre editing`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.startEditCategory(rule)
        Thread.sleep(200)

        assertNotNull(vm.uiState.value.editingRule)
        assertEquals(rule, vm.uiState.value.editingRule)
    }

    @Test
    fun `updateCategory chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.startEditCategory(rule)
        Thread.sleep(200)
        vm.updateCategory(12L)
        Thread.sleep(200)

        coVerify { manager.update(rule.id, level3Id = 12L) }
        assertNull(vm.uiState.value.editingRule)
    }

    @Test
    fun `cancelEdit chiude editing`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        Thread.sleep(200)

        vm.startEditCategory(rule)
        Thread.sleep(200)
        vm.cancelEdit()
        Thread.sleep(200)

        assertNull(vm.uiState.value.editingRule)
    }

    @Test(timeout = 100)
    fun `operazioni ViewModel completate sotto 100ms`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()
        vm.onSearchQueryChange("latte")
        Thread.sleep(200)
    }
}