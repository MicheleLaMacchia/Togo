package it.togo.app.presentation.additem

import io.mockk.every
import io.mockk.mockk
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.StandardUnit
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelQuantityTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AddItemViewModel {
        val catalogRepository = mockk<CatalogRepository>()
        val shoppingRepository = mockk<ShoppingListRepository>()
        every { catalogRepository.search(any()) } returns flowOf(emptyList())
        return AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = shoppingRepository,
            createProductUseCase = CreateCanonicalProductUseCase(catalogRepository),
            createLevel3UseCase = CreateUserLevel3UseCase(catalogRepository),
            ioDispatcher = testDispatcher,
        )
    }

    private val product = CanonicalProduct(
        id = 1L,
        name = "Latte",
        level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.LITER),
    )

    @Test
    fun `la quantità può essere svuotata senza bloccare Avanti`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
        advanceUntilIdle()
        assertEquals("1", viewModel.uiState.value.quantityText)

        // Cancellare tutto il numero deve lasciare il campo davvero vuoto.
        viewModel.onEvent(AddItemUiEvent.QuantityChanged(""))
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.quantityText)
        assertEquals(0.0, viewModel.uiState.value.quantity, 0.0)
        assertTrue(viewModel.uiState.value.isValidForConfirm)

        // Quantità e unità sono opzionali: "Avanti" porta allo step 3.
        viewModel.onEvent(AddItemUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun `quantità zero mostra un errore e non procede`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
        viewModel.onEvent(AddItemUiEvent.QuantityChanged("0"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isValidForConfirm)

        viewModel.onEvent(AddItemUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentStep)
        assertEquals(
            "La quantità deve essere maggiore di zero",
            viewModel.uiState.value.errors["quantity"],
        )
    }

    @Test
    fun `il pulsante azzera svuota quantità e unità`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
        advanceUntilIdle()
        assertEquals(StandardUnit.LITER, viewModel.uiState.value.selectedUnit)

        viewModel.onEvent(AddItemUiEvent.ClearQuantityAndUnit)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.quantityText)
        assertEquals(null, viewModel.uiState.value.selectedUnit)
        assertTrue(viewModel.uiState.value.isValidForConfirm)

        viewModel.onEvent(AddItemUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun `quantità con virgola viene interpretata correttamente`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
        viewModel.onEvent(AddItemUiEvent.QuantityChanged("2,5"))
        advanceUntilIdle()

        assertEquals(2.5, viewModel.uiState.value.quantity, 0.0)
        assertTrue(viewModel.uiState.value.canProceedToStep3)

        viewModel.onEvent(AddItemUiEvent.NextStep)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.uiState.value.errors.isEmpty())
    }

    @Test
    fun `testo non numerico mostra un errore dedicato`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
        viewModel.onEvent(AddItemUiEvent.QuantityChanged("abc"))
        viewModel.onEvent(AddItemUiEvent.NextStep)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.currentStep)
        assertEquals("Inserisci una quantità valida", viewModel.uiState.value.errors["quantity"])
    }
}
