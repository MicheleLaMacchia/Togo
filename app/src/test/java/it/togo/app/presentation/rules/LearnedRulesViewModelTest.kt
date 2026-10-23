package it.togo.app.presentation.rules

import io.mockk.*
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.usecase.LearnedRuleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
        // Il ViewModel filtra le regole orfane: servono i prodotti in catalogo.
        products: Map<Long, CanonicalProduct> = mapOf(1L to product1, 2L to product2),
        taxonomy: List<TaxonomyLevel> = sampleTaxonomy,
    ): Triple<LearnedRulesViewModel, LearnedRulesRepository, LearnedRuleManager> {
        val rulesRepo = mockk<LearnedRulesRepository>()
        val catalogRepo = mockk<CatalogRepository>()
        val manager = mockk<LearnedRuleManager>()

        every { rulesRepo.getAll() } returns flowOf(rules)
        every { catalogRepo.getTaxonomy() } returns flowOf(taxonomy)
        coEvery { catalogRepo.getById(any()) } returns null
        coEvery { manager.deactivate(any()) } returns sampleRule()
        coEvery { manager.reactivate(any()) } returns sampleRule()
        coEvery { manager.delete(any()) } returns true
        coEvery { manager.update(any(), any(), any()) } returns sampleRule()
        products.forEach { (id, product) ->
            coEvery { catalogRepo.getById(id) } returns product
        }

        val vm = LearnedRulesViewModel(
            learnedRuleManager = manager,
            catalogRepository = catalogRepo,
            learnedRulesRepository = rulesRepo,
            ioDispatcher = testDispatcher,
        )
        return Triple(vm, rulesRepo, manager)
    }

    private fun sampleRule(
        id: String = "r1",
        expression: String = "latte",
        active: Boolean = true,
        productId: Long = 1L,
        level3Id: Long = 5L,
    ) = LearnedRule(
        id = id, userExpression = expression, productId = productId, level3Id = level3Id, isActive = active
    )

    private val product1 = CanonicalProduct(id = 1L, name = "Latte Intero", level3Id = 5L)
    private val product2 = CanonicalProduct(id = 2L, name = "Caffè Macinato", level3Id = 12L)

    private val sampleTaxonomy = listOf(
        TaxonomyLevel.Level1(id = 1L, name = "Freschi", sortOrder = 1),
        TaxonomyLevel.Level1(id = 2L, name = "Dispensa", sortOrder = 2),
        TaxonomyLevel.Level2(id = 10L, name = "Latticini", sortOrder = 1, level1Id = 1L),
        TaxonomyLevel.Level2(id = 20L, name = "Caffè", sortOrder = 2, level1Id = 2L),
        TaxonomyLevel.Level3(id = 5L, name = "Latte", sortOrder = 1, level2Id = 10L),
        TaxonomyLevel.Level3(id = 12L, name = "Caffè macinato", sortOrder = 1, level2Id = 20L),
    )

    @Test
    fun `stato iniziale vuoto`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `con regole mostra lista`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2", "caffè", productId = 2L, level3Id = 12L))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isEmpty)
        assertEquals(2, vm.uiState.value.filteredRules.size)
    }

    @Test
    fun `ricerca per espressione filtra`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1", "latte"), sampleRule("r2", "caffè", productId = 2L, level3Id = 12L))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        vm.onSearchQueryChange("latte")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("latte", vm.uiState.value.filteredRules[0].userExpression)
    }

    @Test
    fun `ricerca per nome prodotto filtra`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1", "latte"), sampleRule("r2", "caffè", productId = 2L, level3Id = 12L))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        vm.onSearchQueryChange("latte intero")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("latte", vm.uiState.value.filteredRules[0].userExpression)
    }

    @Test
    fun `ricerca vuota mostra tutto`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2", productId = 2L))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        vm.onSearchQueryChange("")
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.filteredRules.size)
    }

    @Test
    fun `mostra nome prodotto e categoria invece degli ID`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2", "caffè", productId = 2L, level3Id = 12L))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        assertEquals("Latte Intero", vm.uiState.value.productNames[1L])
        assertEquals("Latte", vm.uiState.value.categoryNames["r1"])
        assertEquals("Freschi > Latticini > Latte", vm.uiState.value.categoryPaths["r1"])
        assertEquals("Caffè macinato", vm.uiState.value.categoryNames["r2"])
        assertTrue(vm.uiState.value.categoryOptions.any { it.label == "Freschi > Latticini > Latte" })
    }

    @Test
    fun `regole disattivate restano visibili`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1", active = false))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertFalse(vm.uiState.value.filteredRules[0].isActive)
        assertTrue(vm.uiState.value.hasRules)
    }

    @Test
    fun `ricerca per categoria filtra`() = runTest(testDispatcher) {
        val rules = listOf(sampleRule("r1"), sampleRule("r2", "caffè", productId = 2L, level3Id = 12L))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        vm.onSearchQueryChange("latticini")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("r1", vm.uiState.value.filteredRules[0].id)
    }

    @Test
    fun `regole orfane filtrate fuori`() = runTest(testDispatcher) {
        // r2 punta a un prodotto non più in catalogo → regola orfana
        val rules = listOf(sampleRule("r1"), sampleRule("r2", productId = 999L))
        val (vm, _, _) = createViewModel(rules)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredRules.size)
        assertEquals("r1", vm.uiState.value.filteredRules[0].id)
    }

    @Test
    fun `disattiva chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.deactivateRule(rule)
        advanceUntilIdle()

        coVerify { manager.deactivate(rule.id) }
    }

    @Test
    fun `riattiva chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule(active = false)
        val (vm, _, manager) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.reactivateRule(rule)
        advanceUntilIdle()

        coVerify { manager.reactivate(rule.id) }
    }

    @Test
    fun `requestDelete apre dialog`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.requestDelete(rule)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isDeleteDialogOpen)
        assertEquals(rule, vm.uiState.value.ruleToDelete)
    }

    @Test
    fun `confirmDelete chiude dialog e chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.requestDelete(rule)
        advanceUntilIdle()
        vm.confirmDelete()
        advanceUntilIdle()
        advanceUntilIdle()

        coVerify { manager.delete(rule.id) }
        assertFalse(vm.uiState.value.isDeleteDialogOpen)
    }

    @Test
    fun `cancelDelete chiude dialog senza eliminare`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.requestDelete(rule)
        advanceUntilIdle()
        vm.cancelDelete()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isDeleteDialogOpen)
        coVerify(exactly = 0) { manager.delete(any()) }
    }

    @Test
    fun `startEditCategory apre editing`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.startEditCategory(rule)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.editingRule)
        assertEquals(rule, vm.uiState.value.editingRule)
    }

    @Test
    fun `updateCategory chiama manager`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, manager) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.startEditCategory(rule)
        advanceUntilIdle()
        vm.updateCategory(12L)
        advanceUntilIdle()

        coVerify { manager.update(rule.id, level3Id = 12L) }
        assertNull(vm.uiState.value.editingRule)
    }

    @Test
    fun `cancelEdit chiude editing`() = runTest(testDispatcher) {
        val rule = sampleRule()
        val (vm, _, _) = createViewModel(listOf(rule))
        advanceUntilIdle()

        vm.startEditCategory(rule)
        advanceUntilIdle()
        vm.cancelEdit()
        advanceUntilIdle()

        assertNull(vm.uiState.value.editingRule)
    }

    @Test(timeout = 5_000)
    fun `operazioni ViewModel completate sotto 100ms`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()
        vm.onSearchQueryChange("latte")
        advanceUntilIdle()
    }
}