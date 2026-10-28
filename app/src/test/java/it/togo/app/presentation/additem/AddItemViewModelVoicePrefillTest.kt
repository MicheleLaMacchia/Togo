package it.togo.app.presentation.additem

import io.mockk.*
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ItemAttributes
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import it.togo.app.domain.usecase.CreateUserLevel3UseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Verifica la precompilazione del form di aggiunta a partire da un comando
 * vocale confermato.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelVoicePrefillTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val pane = CanonicalProduct(
        id = 1L,
        name = "Pane",
        level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.KILOGRAM, StandardUnit.PIECE),
    )

    private val paneIntegrale = CanonicalProduct(
        id = 2L,
        name = "Pane Integrale",
        level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.KILOGRAM),
    )

    private val paneSpecialeUno = CanonicalProduct(
        id = 3L,
        name = "Pane Speciale Uno",
        level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.KILOGRAM),
    )

    private val paneSpecialeDue = CanonicalProduct(
        id = 4L,
        name = "Pane Speciale Due",
        level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.KILOGRAM),
    )

    private fun createViewModel(
        command: ParsedCommand,
        lastAttributes: ItemAttributes? = null,
        search: (String) -> List<CanonicalProduct>,
    ): AddItemViewModel {
        val catalogRepository = mockk<CatalogRepository>()
        val shoppingRepository = mockk<ShoppingListRepository>()
        every { catalogRepository.search(any()) } answers {
            flowOf(search(firstArg<String>()))
        }
        if (lastAttributes != null) {
            coEvery { shoppingRepository.getLastAttributes(any()) } returns lastAttributes
        }
        return AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = shoppingRepository,
            createProductUseCase = CreateCanonicalProductUseCase(catalogRepository),
            createLevel3UseCase = CreateUserLevel3UseCase(catalogRepository),
            ioDispatcher = testDispatcher,
            initialVoiceCommand = command,
        )
    }

    private fun commandPaneIntegrale() = ParsedCommand(
        productName = "pane",
        quantity = 1.0,
        unit = StandardUnit.KILOGRAM,
        variant = "Integrale",
        categoryHint = "pane",
        confidence = 1.0,
    )

    @Test
    fun `match univoco seleziona prodotto quantita unita e variante`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(commandPaneIntegrale()) { query ->
                if (query == "pane integrale") listOf(pane) else emptyList()
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(pane, state.selectedProduct)
            assertEquals("1", state.quantityText)
            assertEquals(StandardUnit.KILOGRAM, state.selectedUnit)
            assertEquals("Integrale", state.variant)
            assertEquals(2, state.currentStep)
            assertFalse(state.isLoading)
        }

    @Test
    fun `comando vocale senza quantita lascia i campi vuoti`() =
        runTest(testDispatcher) {
            val command = ParsedCommand(
                productName = "pane",
                quantity = null,
                unit = null,
                variant = null,
                categoryHint = "pane",
                confidence = 1.0,
            )
            val viewModel = createViewModel(command) { query ->
                if (query == "pane") listOf(pane) else emptyList()
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(pane, state.selectedProduct)
            assertEquals("", state.quantityText)
            assertNull(state.selectedUnit)
            assertEquals(2, state.currentStep)
        }

    @Test
    fun `variante non duplicata se gia nel nome del prodotto`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(commandPaneIntegrale()) { query ->
                if (query == "pane integrale") listOf(paneIntegrale) else emptyList()
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(paneIntegrale, state.selectedProduct)
            assertEquals("", state.variant)
            assertEquals(2, state.currentStep)
        }

    @Test
    fun `comando vocale non precompila una variante storica diversa`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(
                command = commandPaneIntegrale(),
                lastAttributes = ItemAttributes(variant = "Bio"),
            ) { query ->
                if (query == "pane integrale") listOf(paneIntegrale) else emptyList()
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(paneIntegrale, state.selectedProduct)
            assertEquals("", state.variant)
            assertEquals(2, state.currentStep)
        }

    @Test
    fun `match esatto preferito anche se la ricerca LIKE ha piu risultati`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(commandPaneIntegrale()) { query ->
                if (query == "pane integrale") {
                    listOf(pane, paneIntegrale, paneSpecialeUno)
                } else {
                    emptyList()
                }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(paneIntegrale, state.selectedProduct)
            assertEquals("", state.variant)
            assertEquals(2, state.currentStep)
        }

    @Test
    fun `parola generica non forza il prodotto esatto tra piu alternative`() =
        runTest(testDispatcher) {
            val command = ParsedCommand(
                productName = "pane",
                quantity = 1.0,
                unit = StandardUnit.KILOGRAM,
                categoryHint = "pane",
                confidence = 1.0,
            )
            val viewModel = createViewModel(command) { query ->
                if (query == "pane") listOf(pane, paneIntegrale) else emptyList()
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect { }
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.selectedProduct)
            assertEquals(1, state.currentStep)
            assertEquals(2, state.searchResults.size)
            assertEquals("pane", state.searchQuery)
        }

    @Test
    fun `match ambiguo resta in ricerca e conserva variante`() =
        runTest(testDispatcher) {
            val command = commandPaneIntegrale().copy(variant = "Speciale")
            val viewModel = createViewModel(command) { query ->
                if (query == "pane speciale") {
                    listOf(paneSpecialeUno, paneSpecialeDue)
                } else {
                    emptyList()
                }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.selectedProduct)
            assertEquals(1, state.currentStep)
            assertEquals("pane speciale", state.searchQuery)
            assertEquals(2, state.searchResults.size)
            assertEquals("Speciale", state.variant)
            assertFalse(state.isLoading)
        }

    @Test
    fun `fallback sulla testa se la variante non esiste nel catalogo`() =
        runTest(testDispatcher) {
            val command = commandPaneIntegrale().copy(variant = "Senza glutine")
            val viewModel = createViewModel(command) { query ->
                when (query) {
                    "pane" -> listOf(pane)
                    else -> emptyList()
                }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(pane, state.selectedProduct)
            assertEquals("Senza glutine", state.variant)
            assertEquals("Pane", state.searchQuery)
            assertEquals(2, state.currentStep)
        }
}
