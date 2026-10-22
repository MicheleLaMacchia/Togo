package it.togo.app.presentation.additem

import io.mockk.mockk
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test del sotto-flusso "Nuovo prodotto" del [AddItemViewModel] (Story 7.1).
 *
 * `uiState` è uno `stateIn(WhileSubscribed)`: ogni test tiene un collector
 * attivo in `backgroundScope`, altrimenti `.value` resterebbe al valore iniziale.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelNewProductTest {

    private val testDispatcher = StandardTestDispatcher()

    private val level1 = TaxonomyLevel.Level1(id = 1L, name = "Alimentari", sortOrder = 1)
    private val level2 = TaxonomyLevel.Level2(id = 21L, name = "Bevande", sortOrder = 1, level1Id = 1L)
    private val level3 = TaxonomyLevel.Level3(
        id = 12L,
        name = "Bevande vegetali",
        sortOrder = 1,
        level2Id = 21L,
        isUserDefined = false,
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCatalogRepository(
        initialProducts: List<CanonicalProduct> = emptyList(),
        taxonomy: List<TaxonomyLevel> = emptyList(),
    ) : CatalogRepository {

        val products = MutableStateFlow(initialProducts)
        private val taxonomyFlow = MutableStateFlow(taxonomy)
        private var nextId = 500L

        override fun search(query: String): Flow<List<CanonicalProduct>> =
            products.map { list ->
                if (query.isBlank()) emptyList()
                else list.filter { it.name.contains(query, ignoreCase = true) }
            }

        override suspend fun getById(id: Long): CanonicalProduct? =
            products.value.firstOrNull { it.id == id }

        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = taxonomyFlow

        override suspend fun getByLevel3(level3Id: Long): List<CanonicalProduct> =
            products.value.filter { it.level3Id == level3Id }

        override suspend fun insertProduct(name: String, level3Id: Long): CanonicalProduct {
            val product = CanonicalProduct(
                id = nextId++,
                name = name.trim(),
                level3Id = level3Id,
                isUserDefined = true,
            )
            products.value = products.value + product
            return product
        }
    }

    private fun TestScope.createViewModel(
        repository: FakeCatalogRepository,
    ): AddItemViewModel {
        val viewModel = AddItemViewModel(
            catalogRepository = repository,
            shoppingRepository = mockk<ShoppingListRepository>(relaxed = true),
            createProductUseCase = CreateCanonicalProductUseCase(repository),
            ioDispatcher = testDispatcher,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        return viewModel
    }

    private fun AddItemViewModel.selectWholeTaxonomy() {
        onEvent(AddItemUiEvent.NewProductNameChanged("Latte di avena"))
        onEvent(AddItemUiEvent.NewProductLevel1Selected(level1.id))
        onEvent(AddItemUiEvent.NewProductLevel2Selected(level2.id))
        onEvent(AddItemUiEvent.NewProductLevel3Selected(level3.id))
    }

    @Test
    fun `apertura form prefillata con la query e tassonomia caricata`() = runTest(testDispatcher) {
        val repository = FakeCatalogRepository(taxonomy = listOf(level1, level2, level3))
        val viewModel = createViewModel(repository)

        viewModel.onEvent(AddItemUiEvent.SearchQueryChanged("  latte di avena  "))
        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isNewProductFormVisible)
        assertEquals("latte di avena", state.newProductName)
        assertEquals(3, state.taxonomy.size)
        assertNull(state.newProductLevel1Id)
        assertNull(state.newProductLevel2Id)
        assertNull(state.newProductLevel3Id)
        assertFalse(state.isCreatingProduct)
    }

    @Test
    fun `cambio L1 azzera L2 e L3`() = runTest(testDispatcher) {
        val repository = FakeCatalogRepository(taxonomy = listOf(level1, level2, level3))
        val viewModel = createViewModel(repository)

        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.NewProductLevel1Selected(level1.id))
        viewModel.onEvent(AddItemUiEvent.NewProductLevel2Selected(level2.id))
        viewModel.onEvent(AddItemUiEvent.NewProductLevel3Selected(level3.id))

        viewModel.onEvent(AddItemUiEvent.NewProductLevel1Selected(2L))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2L, state.newProductLevel1Id)
        assertNull(state.newProductLevel2Id)
        assertNull(state.newProductLevel3Id)
    }

    @Test
    fun `creazione senza L3 mostra errore e non inserisce`() = runTest(testDispatcher) {
        val repository = FakeCatalogRepository(taxonomy = listOf(level1, level2, level3))
        val viewModel = createViewModel(repository)

        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.NewProductNameChanged("Latte di avena"))
        viewModel.onEvent(AddItemUiEvent.NewProductLevel1Selected(level1.id))
        viewModel.onEvent(AddItemUiEvent.NewProductLevel2Selected(level2.id))
        viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Seleziona Livello 1, Livello 2 e Livello 3", state.newProductError)
        assertFalse(state.isCreatingProduct)
        assertTrue(repository.products.value.isEmpty())
    }

    @Test
    fun `duplicato valorizza il candidato senza creare il prodotto`() = runTest(testDispatcher) {
        val existing = CanonicalProduct(
            id = 99L,
            name = "Latte di avena",
            level3Id = level3.id,
        )
        val repository = FakeCatalogRepository(
            initialProducts = listOf(existing),
            taxonomy = listOf(level1, level2, level3),
        )
        val viewModel = createViewModel(repository)

        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()
        viewModel.selectWholeTaxonomy()
        viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(existing, state.duplicateCandidate)
        assertNull(state.selectedProduct)
        assertTrue(state.isNewProductFormVisible)
        assertFalse(state.isCreatingProduct)
        assertEquals(1, repository.products.value.size)
    }

    @Test
    fun `conferma duplicato crea il prodotto e passa allo step 2`() = runTest(testDispatcher) {
        val existing = CanonicalProduct(
            id = 99L,
            name = "Latte di avena",
            level3Id = level3.id,
        )
        val repository = FakeCatalogRepository(
            initialProducts = listOf(existing),
            taxonomy = listOf(level1, level2, level3),
        )
        val viewModel = createViewModel(repository)

        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()
        viewModel.selectWholeTaxonomy()
        viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.duplicateCandidate)

        viewModel.onEvent(AddItemUiEvent.ConfirmDuplicateProduct)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.duplicateCandidate)
        assertFalse(state.isNewProductFormVisible)
        assertEquals(2, state.currentStep)
        assertEquals(2, repository.products.value.size)
        assertNotNull(state.selectedProduct)
        assertTrue(state.selectedProduct!!.isUserDefined)
        assertEquals(StandardUnit.PIECE, state.selectedUnit)
    }

    @Test
    fun `creazione normale passa allo step 2 con unita PIECE e prodotto user-defined`() =
        runTest(testDispatcher) {
            val repository = FakeCatalogRepository(taxonomy = listOf(level1, level2, level3))
            val viewModel = createViewModel(repository)

            viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
            advanceUntilIdle()
            viewModel.selectWholeTaxonomy()
            viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isNewProductFormVisible)
            assertEquals(2, state.currentStep)
            assertEquals(StandardUnit.PIECE, state.selectedUnit)
            assertNotNull(state.selectedProduct)
            assertTrue(state.selectedProduct!!.isUserDefined)
            assertEquals("Latte di avena", state.selectedProduct!!.name)
            assertEquals(1, repository.products.value.size)
        }

    @Test
    fun `annulla avviso duplicato nasconde il dialog e lascia il form aperto`() =
        runTest(testDispatcher) {
            val existing = CanonicalProduct(
                id = 99L,
                name = "Latte di avena",
                level3Id = level3.id,
            )
            val repository = FakeCatalogRepository(
                initialProducts = listOf(existing),
                taxonomy = listOf(level1, level2, level3),
            )
            val viewModel = createViewModel(repository)

            viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
            advanceUntilIdle()
            viewModel.selectWholeTaxonomy()
            viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
            advanceUntilIdle()

            viewModel.onEvent(AddItemUiEvent.DismissDuplicateWarning)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.duplicateCandidate)
            assertTrue(state.isNewProductFormVisible)
            assertEquals(1, repository.products.value.size)
        }

    @Test
    fun `indietro con form aperto chiude il form senza cambiare step`() = runTest(testDispatcher) {
        val repository = FakeCatalogRepository(taxonomy = listOf(level1, level2, level3))
        val viewModel = createViewModel(repository)

        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.PreviousStep)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isNewProductFormVisible)
        assertEquals(1, state.currentStep)
    }

    @Test
    fun `doppio tap su crea prodotto non inserisce duplicati`() = runTest(testDispatcher) {
        val repository = FakeCatalogRepository(taxonomy = listOf(level1, level2, level3))
        val viewModel = createViewModel(repository)

        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()
        viewModel.selectWholeTaxonomy()
        // Due eventi prima che la coroutine di creazione venga eseguita.
        viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
        viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
        advanceUntilIdle()

        assertEquals(1, repository.products.value.size)
        assertEquals(2, viewModel.uiState.value.currentStep)
    }
}
