package it.togo.app.presentation.history

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ListItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun item(
        id: String,
        productId: Long,
        status: ListItemStatus,
        changedAt: Long,
        brand: String? = null,
    ) = ListItem.create(
        id = id,
        productId = productId,
        quantity = 2.0,
        unit = "LITER",
        brand = brand,
        createdAt = changedAt,
        statusChangedAt = changedAt,
        purchaseCount = if (status == ListItemStatus.PURCHASED) 1 else 0,
        status = status,
    )

    @Test
    fun `history includes purchased and deleted rows and searches attributes`() = runTest(testDispatcher) {
        val purchased = item("bought", 1, ListItemStatus.PURCHASED, 20, brand = "Coop")
        val deleted = item("deleted", 2, ListItemStatus.DELETED, 10)
        val active = item("active", 3, ListItemStatus.TO_BUY, 30)
        val repository = mockk<ListItemRepository>(relaxed = true)
        val catalog = mockk<CatalogRepository>()
        every { repository.observeAll() } returns flowOf(listOf(purchased, deleted, active))
        coEvery { catalog.getById(1) } returns CanonicalProduct(id = 1, name = "Latte", level3Id = 1)
        coEvery { catalog.getById(2) } returns CanonicalProduct(id = 2, name = "Pane", level3Id = 1)
        coEvery { catalog.getById(3) } returns null

        val viewModel = HistoryViewModel(repository, catalog, testDispatcher)
        advanceUntilIdle()

        assertEquals(listOf("bought", "deleted"), viewModel.uiState.value.items.map { it.id })
        viewModel.onSearchQueryChange("latte")
        advanceUntilIdle()
        assertEquals(listOf("bought"), viewModel.uiState.value.filteredItems.map { it.id })

        viewModel.onSearchQueryChange("coop")
        advanceUntilIdle()
        assertEquals(listOf("bought"), viewModel.uiState.value.filteredItems.map { it.id })
    }

    @Test
    fun `readd and restore delegate to repository transitions`() = runTest(testDispatcher) {
        val purchased = item("bought", 1, ListItemStatus.PURCHASED, 20)
        val deleted = item("deleted", 2, ListItemStatus.DELETED, 10)
        val active = item("active", 1, ListItemStatus.TO_BUY, 30)
        val repository = mockk<ListItemRepository>(relaxed = true)
        val catalog = mockk<CatalogRepository>(relaxed = true)
        every { repository.observeAll() } returns flowOf(listOf(purchased, deleted))
        coEvery { repository.reAdd("bought") } returns active
        coEvery { repository.restore("deleted") } returns deleted

        val viewModel = HistoryViewModel(repository, catalog, testDispatcher)
        viewModel.reAddToActiveList(purchased)
        advanceUntilIdle()
        assertEquals("Aggiunto alla lista", viewModel.uiState.value.snackbarMessage)

        viewModel.restore(deleted)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.reAdd("bought") }
        coVerify(exactly = 1) { repository.restore("deleted") }
        assertEquals("Voce ripristinata", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `clearSnackbar removes the visible message`() = runTest(testDispatcher) {
        val repository = mockk<ListItemRepository>()
        every { repository.observeAll() } returns flowOf(emptyList())
        val viewModel = HistoryViewModel(repository, mockk(relaxed = true), testDispatcher)

        viewModel.showSnackbar("Messaggio")
        advanceUntilIdle()
        assertEquals("Messaggio", viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbar()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `repository errors remain visible after readd`() = runTest(testDispatcher) {
        val purchased = item("bought", 1, ListItemStatus.PURCHASED, 20)
        val repository = mockk<ListItemRepository>(relaxed = true)
        every { repository.observeAll() } returns flowOf(listOf(purchased))
        coEvery { repository.reAdd("bought") } throws IllegalStateException("database")
        coEvery { repository.restore("deleted") } throws IllegalStateException("database")
        val viewModel = HistoryViewModel(repository, mockk(relaxed = true), testDispatcher)

        viewModel.reAddToActiveList(purchased)
        advanceUntilIdle()

        assertEquals("Aggiunta non riuscita", viewModel.uiState.value.snackbarMessage)

        viewModel.restore(item("deleted", 2, ListItemStatus.DELETED, 10))
        advanceUntilIdle()
        assertEquals("Ripristino non riuscito", viewModel.uiState.value.snackbarMessage)
    }
}
