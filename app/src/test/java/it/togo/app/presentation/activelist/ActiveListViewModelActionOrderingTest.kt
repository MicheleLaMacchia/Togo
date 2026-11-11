package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica che il ViewModel applichi l'ordinamento "dalla più recente" alle
 * azioni spuntate della sezione Fatte (Story 9.1).
 *
 * I test precedenti coprivano il solo helper `mostRecentFirst`; questo test
 * copre invece le due chiamate nel ViewModel:
 * - `buildUiState` (lettura da Room via getActions)
 * - l'aggiornamento ottimistico dopo `onEvent(UiEvent.CheckOff(id, true))`
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActiveListViewModelActionOrderingTest {

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
    fun `ui state lists checked actions newest-first from repository`() =
        runTest(testDispatcher) {
            val oldest = checked("a", createdAt = 1L, updatedAt = 10L)
            val middle = checked("b", createdAt = 2L, updatedAt = 20L)
            val newest = checked("c", createdAt = 3L, updatedAt = 30L)
            // Room restituisce ORDER BY created_at ASC: il nuovo arriva ultimo.
            val repo = OrderedActionsRepository(listOf(oldest, middle, newest))
            val viewModel = ActiveListViewModel(repo, FakeCatalogRepository())

            // Aspetta che combine abbia emesso (checkedActions popolato dal repo).
            val state = viewModel.uiState.first { it.checkedActions.size == 3 }

            assertEquals(
                "le azioni spuntate devono uscire dalla più recente",
                listOf("c", "b", "a"),
                state.checkedActions.map { it.id },
            )
        }

    @Test
    fun `just-checked action lands at the top of checked actions`() =
        runTest(testDispatcher) {
            val already = checked("old", createdAt = 1L, updatedAt = 10L)
            val notYet = checked("new", createdAt = 2L, updatedAt = 20L, isChecked = false)
            // "new" è già nota al ViewModel (attiva), ma non ancora spuntata.
            val repo = OrderedActionsRepository(listOf(already, notYet))
            val viewModel = ActiveListViewModel(repo, FakeCatalogRepository())

            // Aspetta lo stato iniziale (solo "old" spuntato).
            viewModel.uiState.first { it.checkedActions.size == 1 }

            viewModel.onEvent(UiEvent.CheckOff("new", true))
            // I 300 ms dell'animazione di check-off devono passare prima che
            // l'azione venga spostata in checkedActions. Si attende l'emissione
            // dello StateFlow: `uiState` usa WhileSubscribed, quindi senza
            // collector attivo non riceve gli aggiornamenti di `_uiState` e
            // `uiState.value` resterebbe fermo allo stato precedente.
            val state = viewModel.uiState.first { current ->
                current.checkedActions.any { it.id == "new" }
            }

            assertEquals(
                "l'azione appena spuntata deve stare per prima in Fatte",
                listOf("new", "old"),
                state.checkedActions.map { it.id },
            )
        }

    private fun checked(
        id: String,
        createdAt: Long,
        updatedAt: Long,
        isChecked: Boolean = true,
    ) = ActionItem(
        id = id,
        text = "Azione $id",
        scope = ActionScope.CASA,
        urgency = ActionUrgency.A_TEMPO_PERSO,
        isChecked = isChecked,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private class OrderedActionsRepository(
        private val actions: List<ActionItem>,
    ) : ShoppingListRepository {
        // getAllItems restituisce solo azioni (nessun prodotto), come farebbe
        // Room dopo la migrazione Epic 8.
        override fun getAllItems(): Flow<List<ShoppingItem>> = flowOf(emptyList())
        override fun getActions(): Flow<List<ActionItem>> = flowOf(actions)
        override fun getActiveItems(): Flow<List<ShoppingItem>> = flowOf(emptyList())
        override fun getCheckedItems(): Flow<List<ShoppingItem>> = flowOf(emptyList())

        override suspend fun insert(item: ShoppingItem) = Unit
        override suspend fun update(item: ShoppingItem) = Unit
        override suspend fun delete(itemId: String) = Unit
        override suspend fun getByProductId(productId: Long): ShoppingItem? = null
        override suspend fun checkout(checkedItemIds: List<String>): Boolean = true

        override suspend fun addAction(action: ActionItem) = Unit
        override suspend fun updateAction(action: ActionItem) = Unit
        override suspend fun deleteAction(actionId: String) = Unit
        override suspend fun checkOffAction(actionId: String, checked: Boolean) = Unit
        override suspend fun checkDuplicateAction(
            normalizedText: String,
            scope: ActionScope,
            urgency: ActionUrgency,
            excludeActionId: String?,
        ): Boolean = false
    }

    private class FakeCatalogRepository : CatalogRepository {
        override fun search(query: String): Flow<List<CanonicalProduct>> =
            flowOf<List<CanonicalProduct>>(emptyList())
        override suspend fun getById(id: Long): CanonicalProduct? = null
        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> =
            flowOf<List<TaxonomyLevel>>(emptyList())
    }
}
