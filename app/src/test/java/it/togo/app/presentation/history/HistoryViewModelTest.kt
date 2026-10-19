package it.togo.app.presentation.history

import io.mockk.*
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

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(
        items: List<HistoricalItem> = emptyList(),
        products: Map<Long, CanonicalProduct> = emptyMap()
    ): Triple<HistoryViewModel, HistoryRepository, ShoppingListRepository> {
        val historyRepo = mockk<HistoryRepository>()
        val catalogRepo = mockk<CatalogRepository>()
        val shoppingRepo = mockk<ShoppingListRepository>()

        every { historyRepo.getAll() } returns flowOf(items)
        every { shoppingRepo.getActiveItems() } returns flowOf(emptyList())
        coEvery { shoppingRepo.getByProductId(any()) } returns null
        coEvery { shoppingRepo.insert(any()) } returns Unit
        coEvery { shoppingRepo.update(any()) } returns Unit
        coEvery { shoppingRepo.delete(any()) } returns Unit
        coEvery { catalogRepo.getById(any()) } returns null
        products.forEach { (id, product) ->
            coEvery { catalogRepo.getById(id) } returns product
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
        Thread.sleep(200) // ViewModel usa Dispatchers.IO, attendi collect

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
        Thread.sleep(200)

        vm.onSearchQueryChange("latte")
        Thread.sleep(200)

        assertEquals("latte", vm.uiState.value.searchQuery)
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
        Thread.sleep(200)

        vm.onSearchQueryChange("")
        Thread.sleep(200)

        assertEquals(1, vm.uiState.value.filteredItems.size)
    }

    @Test
    fun `reAddToActiveList chiama insert sul repository`() = runTest(testDispatcher) {
        val items = listOf(
            HistoricalItem(id = "1", productId = 1L, lastQuantity = 2.0, lastUnit = StandardUnit.LITER, purchasedAt = 1000L)
        )
        val (vm, _, shoppingRepo) = createViewModel(items)
        Thread.sleep(200)

        vm.reAddToActiveList(items[0])
        Thread.sleep(200)

        coVerify { shoppingRepo.insert(any()) }
    }

    @Test
    fun `reAdd duplicato mostra snackbar`() = runTest(testDispatcher) {
        val items = listOf(
            HistoricalItem(id = "1", productId = 1L, lastQuantity = 2.0, lastUnit = StandardUnit.LITER, purchasedAt = 1000L)
        )
        val activeItems = listOf(
            ShoppingItem(id = "active-1", productId = 1L, quantity = 2.0, unit = StandardUnit.LITER, isChecked = false)
        )
        val historyRepo = mockk<HistoryRepository>()
        val catalogRepo = mockk<CatalogRepository>()
        val shoppingRepo = mockk<ShoppingListRepository>()

        every { historyRepo.getAll() } returns flowOf(items)
        every { shoppingRepo.getActiveItems() } returns flowOf(activeItems)
        coEvery { shoppingRepo.getByProductId(any()) } returns activeItems.firstOrNull()
        coEvery { shoppingRepo.insert(any()) } returns Unit
        coEvery { catalogRepo.getById(any()) } returns null

        val vm = HistoryViewModel(historyRepo, catalogRepo, shoppingRepo)
        Thread.sleep(200)

        vm.reAddToActiveList(items[0])
        Thread.sleep(200)

        assertEquals("Già nella lista", vm.uiState.value.snackbarMessage)
        coVerify(exactly = 0) { shoppingRepo.insert(any()) }
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
        Thread.sleep(200)
        vm.clearSnackbar()
    }
}