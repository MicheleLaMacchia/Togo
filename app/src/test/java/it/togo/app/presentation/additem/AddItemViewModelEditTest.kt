package it.togo.app.presentation.additem

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ListItemRepository
import it.togo.app.presentation.FixedClock
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import it.togo.app.domain.usecase.CreateUserLevel3UseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelEditTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `edit mode prefills and updates the existing item`() = runTest(testDispatcher) {
        val catalogRepository = mockk<CatalogRepository>()
        val shoppingRepository = mockk<ListItemRepository>()
        val product = CanonicalProduct(
            id = 1L,
            name = "Latte",
            level3Id = 5L,
            compatibleUnits = listOf(StandardUnit.LITER),
        )
        val existing = ListItem.create(
            id = "item-1",
            productId = 1L,
            quantity = 2.0,
            unit = StandardUnit.LITER.name,
            createdAt = 100L,
            status = ListItemStatus.TO_BUY,
        )
        coEvery { catalogRepository.getById(1L) } returns product
        every { catalogRepository.search(any()) } returns flowOf(emptyList())
        coEvery { shoppingRepository.updateDetails(any()) } returns existing

        val viewModel = AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = shoppingRepository,
            clock = FixedClock(),
            createProductUseCase = CreateCanonicalProductUseCase(catalogRepository),
            createLevel3UseCase = CreateUserLevel3UseCase(catalogRepository),
            editItem = existing,
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        // Salta lo stato iniziale dello stateIn: il prefill arriva dalla
        // raccolta upstream avviata con WhileSubscribed.
        val prefilled = viewModel.uiState.drop(1).first()
        assertTrue("deve essere in modalità modifica", prefilled.isEditMode)
        assertEquals(2.0, prefilled.quantity, 0.0)
        assertEquals(StandardUnit.LITER, prefilled.selectedUnit)
        assertEquals(product, prefilled.selectedProduct)

        viewModel.onEvent(AddItemUiEvent.NextStep)
        viewModel.onEvent(AddItemUiEvent.NextStep)
        viewModel.onEvent(AddItemUiEvent.Confirm)
        advanceUntilIdle()

        coVerify {
            shoppingRepository.updateDetails(match {
                it.id == "item-1" &&
                    it.quantity == 2.0 &&
                    it.unit == StandardUnit.LITER.name &&
                    it.status == ListItemStatus.TO_BUY &&
                    it.createdAt == existing.createdAt &&
                    it.statusChangedAt == existing.statusChangedAt
            })
        }
    }

    @Test
    fun `insert uses default values and injected clock`() = runTest(testDispatcher) {
        val product = CanonicalProduct(
            id = 1L,
            name = "Latte",
            level3Id = 5L,
            compatibleUnits = listOf(StandardUnit.LITER),
        )
        val catalogRepository = mockk<CatalogRepository>(relaxed = true)
        val listItemRepository = mockk<ListItemRepository>(relaxed = true)
        every { catalogRepository.search(any()) } returns flowOf(emptyList())
        coEvery { listItemRepository.add(any()) } answers { firstArg() }

        val viewModel = AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = listItemRepository,
            clock = FixedClock(1_234L),
            createProductUseCase = CreateCanonicalProductUseCase(catalogRepository),
            createLevel3UseCase = CreateUserLevel3UseCase(catalogRepository),
            ioDispatcher = testDispatcher,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.ProductSelected(product))
        viewModel.onEvent(AddItemUiEvent.NextStep)
        viewModel.onEvent(AddItemUiEvent.NextStep)
        viewModel.onEvent(AddItemUiEvent.Confirm)
        advanceUntilIdle()

        coVerify {
            listItemRepository.add(match {
                it.quantity == 1.0 &&
                    it.unit == StandardUnit.PIECE.name &&
                    it.status == ListItemStatus.TO_BUY &&
                    it.createdAt == 1_234L &&
                    it.statusChangedAt == 1_234L
            })
        }
    }

    @Test
    fun `edit not found stays open and reports the failure`() = runTest(testDispatcher) {
        val product = CanonicalProduct(id = 1L, name = "Latte", level3Id = 5L)
        val existing = ListItem.create(
            id = "item-1",
            productId = 1L,
            quantity = 2.0,
            unit = StandardUnit.LITER.name,
            createdAt = 100L,
            status = ListItemStatus.TO_BUY,
        )
        val catalogRepository = mockk<CatalogRepository>(relaxed = true)
        val listItemRepository = mockk<ListItemRepository>()
        every { catalogRepository.search(any()) } returns flowOf(emptyList())
        coEvery { catalogRepository.getById(1L) } returns product
        coEvery { listItemRepository.updateDetails(any()) } returns null

        val viewModel = AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = listItemRepository,
            clock = FixedClock(),
            createProductUseCase = CreateCanonicalProductUseCase(catalogRepository),
            createLevel3UseCase = CreateUserLevel3UseCase(catalogRepository),
            editItem = existing,
            ioDispatcher = testDispatcher,
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(AddItemUiEvent.NextStep)
        viewModel.onEvent(AddItemUiEvent.NextStep)
        viewModel.onEvent(AddItemUiEvent.Confirm)
        advanceUntilIdle()

        assertEquals("La voce non e piu modificabile", viewModel.uiState.value.errors["save"])
        coVerify(exactly = 0) { listItemRepository.add(any()) }
    }
}
