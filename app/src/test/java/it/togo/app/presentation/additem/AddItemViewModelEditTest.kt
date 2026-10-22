package it.togo.app.presentation.additem

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
        val shoppingRepository = mockk<ShoppingListRepository>()
        val product = CanonicalProduct(
            id = 1L,
            name = "Latte",
            level3Id = 5L,
            compatibleUnits = listOf(StandardUnit.LITER),
        )
        val existing = ShoppingItem(
            id = "item-1",
            productId = 1L,
            quantity = 2.0,
            unit = StandardUnit.LITER,
        )
        coEvery { catalogRepository.getById(1L) } returns product
        every { catalogRepository.search(any()) } returns flowOf(emptyList())
        coEvery { shoppingRepository.update(any()) } returns Unit

        val viewModel = AddItemViewModel(
            catalogRepository = catalogRepository,
            shoppingRepository = shoppingRepository,
            createProductUseCase = CreateCanonicalProductUseCase(catalogRepository),
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
            shoppingRepository.update(match {
                it.id == "item-1" && it.quantity == 2.0 && it.unit == StandardUnit.LITER
            })
        }
    }
}
