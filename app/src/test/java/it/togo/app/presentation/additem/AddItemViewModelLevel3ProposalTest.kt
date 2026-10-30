package it.togo.app.presentation.additem

import io.mockk.mockk
import it.togo.app.domain.model.CanonicalProduct
import kotlinx.coroutines.CompletableDeferred
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ListItemRepository
import it.togo.app.presentation.FixedClock
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import it.togo.app.domain.usecase.CreateUserLevel3UseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
 * Test del pannello "Proponi nuovo Livello 3" nel [AddItemViewModel]
 * (Story 7.2).
 *
 * `uiState` è uno `stateIn(WhileSubscribed)`: ogni test tiene un collector
 * attivo in `backgroundScope`, altrimenti `.value` resterebbe al valore iniziale.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelLevel3ProposalTest {

    private val testDispatcher = StandardTestDispatcher()

    private val level1 = TaxonomyLevel.Level1(id = 1L, name = "Alimentari", sortOrder = 1)
    private val level2Bevande = TaxonomyLevel.Level2(
        id = 21L,
        name = "Bevande",
        sortOrder = 1,
        level1Id = 1L,
    )
    private val level2Cereali = TaxonomyLevel.Level2(
        id = 22L,
        name = "Cereali",
        sortOrder = 2,
        level1Id = 1L,
    )
    private val level3Bevande = TaxonomyLevel.Level3(
        id = 12L,
        name = "Bevande vegetali",
        sortOrder = 1,
        level2Id = 21L,
        isUserDefined = false,
    )
    private val level3Cereali = TaxonomyLevel.Level3(
        id = 13L,
        name = "Cereali da colazione",
        sortOrder = 1,
        level2Id = 22L,
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

    private class FakeCatalogRepository(initialTaxonomy: List<TaxonomyLevel>) : CatalogRepository {

        val taxonomy = MutableStateFlow(initialTaxonomy)
        val products = mutableListOf<CanonicalProduct>()
        var insertLevel3Calls = 0
            private set
        var insertProductCalls = 0
            private set
        var insertLevel3Error: Exception? = null
        var productInsertGate: CompletableDeferred<Unit>? = null
        private var nextLevel3Id = 900L
        private var nextProductId = 500L

        override fun search(query: String): Flow<List<CanonicalProduct>> = flowOf(emptyList())

        override suspend fun getById(id: Long): CanonicalProduct? =
            products.firstOrNull { it.id == id }

        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = taxonomy

        override suspend fun getLevel3ByLevel2(level2Id: Long): List<TaxonomyLevel.Level3> =
            taxonomy.value
                .filterIsInstance<TaxonomyLevel.Level3>()
                .filter { it.level2Id == level2Id }

        override suspend fun insertLevel3(name: String, level2Id: Long): TaxonomyLevel.Level3 {
            insertLevel3Calls++
            insertLevel3Error?.let { throw it }
            val maxOrder = taxonomy.value
                .filterIsInstance<TaxonomyLevel.Level3>()
                .filter { it.level2Id == level2Id }
                .maxOfOrNull { it.sortOrder } ?: 0
            val created = TaxonomyLevel.Level3(
                id = nextLevel3Id++,
                name = name.trim(),
                sortOrder = maxOrder + 1,
                level2Id = level2Id,
                isUserDefined = true,
            )
            taxonomy.value = taxonomy.value + created
            return created
        }

        override suspend fun getByLevel3(level3Id: Long): List<CanonicalProduct> =
            products.filter { it.level3Id == level3Id }

        override suspend fun insertProduct(name: String, level3Id: Long): CanonicalProduct {
            productInsertGate?.await()
            insertProductCalls++
            val product = CanonicalProduct(
                id = nextProductId++,
                name = name.trim(),
                level3Id = level3Id,
                isUserDefined = true,
            )
            products += product
            return product
        }
    }

    private fun TestScope.createViewModel(repository: FakeCatalogRepository): AddItemViewModel {
        val viewModel = AddItemViewModel(
            catalogRepository = repository,
            shoppingRepository = mockk<ListItemRepository>(relaxed = true),
            clock = FixedClock(),
            createProductUseCase = CreateCanonicalProductUseCase(repository),
            createLevel3UseCase = CreateUserLevel3UseCase(repository),
            ioDispatcher = testDispatcher,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        return viewModel
    }

    private fun newRepository() = FakeCatalogRepository(
        listOf(level1, level2Bevande, level2Cereali, level3Bevande, level3Cereali)
    )

    private fun AddItemViewModel.openFormWithLevel2(level2Id: Long = level2Bevande.id) {
        onEvent(AddItemUiEvent.ShowNewProductForm)
        onEvent(AddItemUiEvent.NewProductLevel1Selected(level1.id))
        onEvent(AddItemUiEvent.NewProductLevel2Selected(level2Id))
    }

    @Test
    fun `proposta non visibile senza Livello 2 selezionato`() = runTest(testDispatcher) {
        val viewModel = createViewModel(newRepository())

        viewModel.onEvent(AddItemUiEvent.ShowNewProductForm)
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLevel3ProposalVisible)
    }

    @Test
    fun `proposta visibile con Livello 2 selezionato`() = runTest(testDispatcher) {
        val viewModel = createViewModel(newRepository())

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLevel3ProposalVisible)
    }

    @Test
    fun `annulla proposta chiude il pannello e azzera i campi`() = runTest(testDispatcher) {
        val viewModel = createViewModel(newRepository())

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
        viewModel.onEvent(AddItemUiEvent.HideLevel3Proposal)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLevel3ProposalVisible)
        assertEquals("", state.newLevel3Name)
        assertNull(state.level3ProposalError)
        assertNull(state.duplicateLevel3Candidate)
    }

    @Test
    fun `nome vuoto mostra errore e non crea`() = runTest(testDispatcher) {
        val repository = newRepository()
        val viewModel = createViewModel(repository)

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Inserisci il nome del nuovo Livello 3", state.level3ProposalError)
        assertEquals(0, repository.insertLevel3Calls)
        assertTrue(state.isLevel3ProposalVisible)
    }

    @Test
    fun `creazione seleziona il nuovo Livello 3 e aggiorna la tassonomia`() = runTest(testDispatcher) {
        val repository = newRepository()
        val viewModel = createViewModel(repository)

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
        viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val created = repository.taxonomy.value
            .filterIsInstance<TaxonomyLevel.Level3>()
            .first { it.name == "Bevande bio" }

        assertEquals(1, repository.insertLevel3Calls)
        assertEquals(created.id, state.newProductLevel3Id)
        assertTrue(created.isUserDefined)
        assertEquals(level2Bevande.id, created.level2Id)
        assertTrue(state.taxonomy.contains(created))
        assertFalse(state.isLevel3ProposalVisible)
        assertEquals("", state.newLevel3Name)
        assertNull(state.level3ProposalError)
        assertFalse(state.isCreatingLevel3)
    }

    @Test
    fun `duplicato valorizza il candidato e non crea`() = runTest(testDispatcher) {
        val repository = newRepository()
        val viewModel = createViewModel(repository)

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("  BEVANDE VEGETALI  "))
        viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.duplicateLevel3Candidate)
        assertEquals(level3Bevande.id, state.duplicateLevel3Candidate!!.id)
        assertEquals(0, repository.insertLevel3Calls)
        assertTrue(state.isLevel3ProposalVisible)
    }

    @Test
    fun `conferma duplicato crea il Livello 3 e lo seleziona`() = runTest(testDispatcher) {
        val repository = newRepository()
        val viewModel = createViewModel(repository)

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande vegetali"))
        viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.duplicateLevel3Candidate)

        viewModel.onEvent(AddItemUiEvent.ConfirmDuplicateLevel3)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.duplicateLevel3Candidate)
        assertEquals(1, repository.insertLevel3Calls)
        assertNotNull(state.newProductLevel3Id)
        assertTrue(state.newProductLevel3Id != level3Bevande.id)
        assertFalse(state.isLevel3ProposalVisible)
        assertEquals(2, repository.taxonomy.value.count {
            it is TaxonomyLevel.Level3 && it.name == "Bevande vegetali"
        })
    }

    @Test
    fun `cambio Livello 2 chiude il pannello e azzera il Livello 3`() = runTest(testDispatcher) {
        val viewModel = createViewModel(newRepository())

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
        viewModel.onEvent(AddItemUiEvent.NewProductLevel2Selected(level2Cereali.id))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLevel3ProposalVisible)
        assertEquals("", state.newLevel3Name)
        assertNull(state.newProductLevel3Id)
        assertNull(state.level3ProposalError)
    }

    @Test
    fun `selezione di un Livello 3 esistente chiude il pannello`() = runTest(testDispatcher) {
        val viewModel = createViewModel(newRepository())

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
        viewModel.onEvent(AddItemUiEvent.NewProductLevel3Selected(level3Bevande.id))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLevel3ProposalVisible)
        assertEquals("", state.newLevel3Name)
        assertEquals(level3Bevande.id, state.newProductLevel3Id)
    }

    @Test
    fun `dopo la creazione del Livello 3 il prodotto usa il nuovo level3Id`() = runTest(testDispatcher) {
        val repository = newRepository()
        val viewModel = createViewModel(repository)

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
        viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
        advanceUntilIdle()

        val createdId = viewModel.uiState.value.newProductLevel3Id
        assertNotNull(createdId)

        viewModel.onEvent(AddItemUiEvent.NewProductNameChanged("Latte bio"))
        viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
        advanceUntilIdle()

        assertEquals(1, repository.insertProductCalls)
        assertEquals(createdId, repository.products.single().level3Id)
        assertEquals(2, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `doppio tap su Crea Livello 3 inserisce un solo livello`() = runTest(testDispatcher) {
        val repository = newRepository()
        val viewModel = createViewModel(repository)

        viewModel.openFormWithLevel2()
        advanceUntilIdle()
        viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
        viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
        viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
        viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
        advanceUntilIdle()

        assertEquals(1, repository.insertLevel3Calls)
        assertFalse(viewModel.uiState.value.isCreatingLevel3)
    }

    @Test
    fun `seconda proposta con lo stesso nome rileva il L3 user-defined appena creato`() =
        runTest(testDispatcher) {
            val repository = newRepository()
            val viewModel = createViewModel(repository)

            viewModel.openFormWithLevel2()
            advanceUntilIdle()
            viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
            viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
            viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
            advanceUntilIdle()
            assertEquals(1, repository.insertLevel3Calls)
            val createdId = viewModel.uiState.value.newProductLevel3Id
            assertNotNull(createdId)

            viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
            viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("  BEVANDE BIO  "))
            viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.duplicateLevel3Candidate)
            assertEquals(createdId, state.duplicateLevel3Candidate!!.id)
            assertEquals(1, repository.insertLevel3Calls)
        }

    @Test
    fun `errore di scrittura mostra il messaggio e sblocca il pannello`() =
        runTest(testDispatcher) {
            val repository = newRepository()
            repository.insertLevel3Error = IllegalStateException("DB non disponibile")
            val viewModel = createViewModel(repository)

            viewModel.openFormWithLevel2()
            advanceUntilIdle()
            viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
            viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))
            viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Errore durante la creazione del Livello 3", state.level3ProposalError)
            assertFalse(state.isCreatingLevel3)
            assertTrue(state.isLevel3ProposalVisible)
            assertEquals(1, repository.insertLevel3Calls)
        }

    @Test
    fun `Crea Livello 3 non parte mentre il prodotto è in salvataggio`() =
        runTest(testDispatcher) {
            val repository = newRepository()
            val gate = CompletableDeferred<Unit>()
            repository.productInsertGate = gate
            val viewModel = createViewModel(repository)

            viewModel.openFormWithLevel2()
            advanceUntilIdle()
            viewModel.onEvent(AddItemUiEvent.NewProductLevel3Selected(level3Bevande.id))
            viewModel.onEvent(AddItemUiEvent.NewProductNameChanged("Latte bio"))
            viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal)
            viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged("Bevande bio"))

            viewModel.onEvent(AddItemUiEvent.CreateNewProduct)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isCreatingProduct)

            viewModel.onEvent(AddItemUiEvent.CreateNewLevel3)
            advanceUntilIdle()
            assertEquals(0, repository.insertLevel3Calls)

            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, repository.insertProductCalls)
        }
}
