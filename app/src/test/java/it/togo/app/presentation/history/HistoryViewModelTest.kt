package it.togo.app.presentation.history

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.HistoricalItem
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.HistoryRepository
import it.togo.app.domain.repository.ShoppingListRepository
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
import org.mockito.Mockito.*

/**
 * Test JVM per HistoryViewModel.
 * Verifica MVI: ricerca, riaggiunta, stato vuoto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

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
        items: List<HistoricalItem> = emptyList(),
        products: Map<Long, CanonicalProduct> = emptyMap()
    ): Triple<HistoryViewModel, HistoryRepository, ShoppingListRepository> {
        val historyRepo = mock(HistoryRepository::class.java)
        val catalogRepo = mock(CatalogRepository::class.java)
        val shoppingRepo = mock(ShoppingListRepository::class.java)

        `when`(historyRepo.getAll()).thenReturn(flowOf(items))
        `when`(shoppingRepo.getActiveItems()).thenReturn(flowOf(emptyList()))

        // Setup catalog mock per ricerca by name
        products.forEach { (id, product) ->
            `when`(catalogRepo.getById(id)).thenReturn(product)
        }

        val vm = HistoryViewModel(historyRepo, catalogRepo, shoppingRepo)
        return Triple(vm, historyRepo, shoppingRepo)
    }

    @Test
    fun `stato iniziale vuoto`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()

        assertTrue(vm.uiState.value.isEmpty)
        assertEquals("", vm.uiState.value.searchQuery)
    }

    @Test
    fun `con items mostra lista`() = runTest(testDispatcher) {
        val items = listOf(
            HistoricalItem(id = "1", productId = 1L, lastQuantity = 2.0, lastUnit = StandardUnit.LITER, purchasedAt = 1000L),
            HistoricalItem(id = "2", productId = 2L, lastQuantity = 1.0, lastUnit = StandardUnit.KILOGRAM, purchasedAt = 2000L)
        )
        val (vm, _, _) = createViewModel(items)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isEmpty)
        assertEquals(2, vm.uiState.value.filteredItems.size)
    }

    @Test
    fun `ricerca per nome filtra items`() = runTest(testDispatcher) {
        val items = listOf(
            HistoricalItem(id = "1", productId = 1L, lastQuantity = 2.0, lastUnit = StandardUnit.LITER, purchasedAt = 1000L),
            HistoricalItem(id = "2", productId = 2L, lastQuantity = 1.0, lastUnit = StandardUnit.KILOGRAM, purchasedAt = 2000L)
        )
        val products = mapOf(
            1L to CanonicalProduct(id = 1L, name = "Latte", level3Id = 1L),
            2L to CanonicalProduct(id = 2L, name = "Caffè", level3Id = 2L)
        )
        val (vm, _, _) = createViewModel(items, products)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onSearchQueryChange("latte")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("latte", vm.uiState.value.searchQuery)
        // Verifica che filteredItems contenga solo il prodotto con nome che matcha
        val filtered = vm.uiState.value.filteredItems
        assertTrue(filtered.all { item ->
            val product = products[item.productId]
            product?.name?.lowercase()?.contains("latte") == true
        })
    }

    @Test
    fun `ricerca vuota mostra tutto`() = runTest(testDispatcher) {
        val items = listOf(
            HistoricalItem(id = "1", productId = 1L, lastQuantity = 2.0, lastUnit = StandardUnit.LITER, purchasedAt = 1000L)
        )
        val (vm, _, _) = createViewModel(items)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onSearchQueryChange("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, vm.uiState.value.filteredItems.size)
    }

    @Test
    fun `reAddToActiveList chiama insert sul repository`() = runTest(testDispatcher) {
        val items = listOf(
            HistoricalItem(id = "1", productId = 1L, lastQuantity = 2.0, lastUnit = StandardUnit.LITER, purchasedAt = 1000L)
        )
        val (_, _, shoppingRepo) = createViewModel(items)
        testDispatcher.scheduler.advanceUntilIdle()

        // Recupera il ViewModel per chiamare reAddToActiveList
        val (vm, _, _) = createViewModel(items)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.reAddToActiveList(items[0])
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifica che insert sia stato chiamato sul mock del repository
        verify(shoppingRepo).insert(any<ShoppingItem>())
    }

    @Test
    fun `reAdd duplicato mostra snackbar`() = runTest(testDispatcher) {
        val items = listOf(
            HistoricalItem(id = "1", productId = 1L, lastQuantity = 2.0, lastUnit = StandardUnit.LITER, purchasedAt = 1000L)
        )
        val activeItems = listOf(
            ShoppingItem(id = "active-1", productId = 1L, quantity = 2.0, unit = StandardUnit.LITER, isChecked = false)
        )
        val historyRepo = mock(HistoryRepository::class.java)
        val catalogRepo = mock(CatalogRepository::class.java)
        val shoppingRepo = mock(ShoppingListRepository::class.java)

        `when`(historyRepo.getAll()).thenReturn(flowOf(items))
        `when`(shoppingRepo.getActiveItems()).thenReturn(flowOf(activeItems))

        val vm = HistoryViewModel(historyRepo, catalogRepo, shoppingRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.reAddToActiveList(items[0])
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifica snackbar "Già nella lista"
        assertEquals("Già nella lista", vm.uiState.value.snackbarMessage)
        // Verifica che insert NON sia stato chiamato
        verify(shoppingRepo, never()).insert(any())
    }

    @Test
    fun `clearSnackbar pulisce messaggio`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()

        vm.showSnackbar("Test")
        vm.clearSnackbar()

        assertNull(vm.uiState.value.snackbarMessage)
    }

    @Test(timeout = 100)
    fun `operazioni ViewModel completate sotto 100ms`() = runTest(testDispatcher) {
        val (vm, _, _) = createViewModel()

        vm.onSearchQueryChange("latte")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.clearSnackbar()
    }
}