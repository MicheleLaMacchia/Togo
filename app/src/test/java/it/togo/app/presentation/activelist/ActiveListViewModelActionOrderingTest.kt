package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    @Test
    fun `unchecking a completed action restores it to active groups in scope urgency alphabetical order`() =
        runTest(testDispatcher) {
            val restored = checked(
                id = "restored",
                createdAt = 1L,
                updatedAt = 10L,
                text = "Zeta",
                scope = ActionScope.CASA,
                urgency = ActionUrgency.URGENTE,
            )
            val casaUrgentAlpha = checked(
                id = "casa-alpha",
                createdAt = 2L,
                updatedAt = 20L,
                isChecked = false,
                text = "Alfa",
                scope = ActionScope.CASA,
                urgency = ActionUrgency.URGENTE,
            )
            val casaRelaxed = checked(
                id = "casa-relaxed",
                createdAt = 3L,
                updatedAt = 30L,
                isChecked = false,
                text = "Commissione",
                scope = ActionScope.CASA,
                urgency = ActionUrgency.A_TEMPO_PERSO,
            )
            val praticheUrgent = checked(
                id = "pratiche-urgent",
                createdAt = 4L,
                updatedAt = 40L,
                isChecked = false,
                text = "Documento",
                scope = ActionScope.PRATICHE,
                urgency = ActionUrgency.URGENTE,
            )
            val repo = OrderedActionsRepository(
                listOf(restored, casaRelaxed, praticheUrgent, casaUrgentAlpha),
            )
            val viewModel = ActiveListViewModel(repo, FakeCatalogRepository())

            viewModel.uiState.first {
                it.checkedActions.any { action -> action.id == "restored" }
            }

            viewModel.onEvent(UiEvent.CheckOff("restored", false))

            // Aspetta anche la mutazione del repository: lo stato UI è ottimistico,
            // mentre questa emissione dimostra che la deselezione è stata persistita.
            repo.actions.first { actions ->
                actions.any { action -> action.id == "restored" && !action.isChecked }
            }

            val state = viewModel.uiState.first {
                it.checkedActions.none { action -> action.id == "restored" } &&
                    it.actions.any { action -> action.id == "restored" && !action.isChecked }
            }
            val restoredAction = state.actions.single { it.id == "restored" }

            assertEquals(ActionScope.CASA, restoredAction.scope)
            assertEquals(ActionUrgency.URGENTE, restoredAction.urgency)
            assertEquals(
                listOf(
                    ActionScope.CASA to "casa-alpha",
                    ActionScope.CASA to "restored",
                    ActionScope.CASA to "casa-relaxed",
                    ActionScope.PRATICHE to "pratiche-urgent",
                ),
                state.actionGroups.flatMap { group ->
                    group.actions.map { action -> group.scope to action.id }
                },
            )
        }

    @Test
    fun `conclude actions deletes checked actions and preserves active actions`() =
        runTest(testDispatcher) {
            val finished = checked("done", createdAt = 1L, updatedAt = 10L)
            val active = checked("active", createdAt = 2L, updatedAt = 20L, isChecked = false)
            val repo = OrderedActionsRepository(listOf(finished, active))
            val viewModel = ActiveListViewModel(repo, FakeCatalogRepository())

            viewModel.uiState.first { it.checkedActions.size == 1 && it.actions.size == 1 }
            viewModel.onEvent(UiEvent.DeleteAction("active"))
            viewModel.uiState.first { it.actionDeleteUndoMessage != null }

            viewModel.onEvent(UiEvent.ConcludeActions)

            val state = viewModel.uiState.first {
                it.checkedActions.isEmpty() && it.actionDeleteUndoMessage == null
            }
            assertEquals(
                "le azioni attive devono restare disponibili",
                listOf("active"),
                state.actions.map { it.id },
            )
            assertEquals(1, repo.deleteCheckedActionsCalls)
            assertEquals(0, repo.checkoutCalls)

            viewModel.onEvent(UiEvent.UndoDeleteAction)
            assertEquals(0, repo.addActionCalls)
        }

    @Test
    fun `shopping count includes active items without a catalog category`() =
        runTest(testDispatcher) {
            val active = ShoppingItem(
                id = "uncategorized-active",
                productId = 99L,
                quantity = 1.0,
                unit = StandardUnit.PIECE,
            )
            val repo = OrderedActionsRepository(actions = emptyList(), items = listOf(active))
            val viewModel = ActiveListViewModel(repo, FakeCatalogRepository())

            val initial = viewModel.uiState.first { it.activeCount == 1 }
            assertEquals(1, initial.activeCount)
            assertEquals(emptyList<CategoryGroup>(), initial.activeGroups)
        }

    @Test
    fun `shopping count tracks check and uncheck transitions`() = runTest(testDispatcher) {
        val active = ShoppingItem(
            id = "categorized-active",
            productId = 99L,
            quantity = 1.0,
            unit = StandardUnit.PIECE,
        )
        val taxonomy = listOf(
            TaxonomyLevel.Level1(id = 1L, name = "Alimentari", sortOrder = 1),
            TaxonomyLevel.Level2(id = 2L, name = "Pane", sortOrder = 1, level1Id = 1L),
            TaxonomyLevel.Level3(id = 3L, name = "Forno", sortOrder = 1, level2Id = 2L),
        )
        val repo = OrderedActionsRepository(actions = emptyList(), items = listOf(active))
        val catalog = FakeCatalogRepository(
            products = mapOf(99L to CanonicalProduct(id = 99L, name = "Pagnotta", level3Id = 3L)),
            taxonomy = taxonomy,
        )
        val viewModel = ActiveListViewModel(repo, catalog)

        viewModel.uiState.first { it.activeCount == 1 && it.activeGroups.isNotEmpty() }

        viewModel.onEvent(UiEvent.CheckOff(active.id, true))
        val checked = viewModel.uiState.first { it.activeCount == 0 }
        assertEquals(0, checked.activeCount)
        advanceUntilIdle()

        viewModel.onEvent(UiEvent.CheckOff(active.id, false))
        val restored = viewModel.uiState.first {
            it.activeCount == 1 && it.activeGroups.isNotEmpty()
        }
        assertEquals(1, restored.activeCount)
        advanceUntilIdle()
    }

    private fun checked(
        id: String,
        createdAt: Long,
        updatedAt: Long,
        isChecked: Boolean = true,
        text: String = "Azione $id",
        scope: ActionScope = ActionScope.CASA,
        urgency: ActionUrgency = ActionUrgency.A_TEMPO_PERSO,
    ) = ActionItem(
        id = id,
        text = text,
        scope = scope,
        urgency = urgency,
        isChecked = isChecked,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private class OrderedActionsRepository(
        actions: List<ActionItem>,
        items: List<ShoppingItem> = emptyList(),
    ) : ShoppingListRepository {
        val actions = MutableStateFlow(actions)
        private val shoppingItems = MutableStateFlow(items)
        var deleteCheckedActionsCalls = 0
        var checkoutCalls = 0
        var addActionCalls = 0
        // Per i test sulle azioni non ci sono prodotti; il parametro items
        // permette anche di verificare conteggi lista con categorie mancanti.
        override fun getAllItems(): Flow<List<ShoppingItem>> = shoppingItems
        override fun getActions(): Flow<List<ActionItem>> = actions
        override fun getActiveItems(): Flow<List<ShoppingItem>> = flowOf(emptyList())
        override fun getCheckedItems(): Flow<List<ShoppingItem>> = flowOf(emptyList())

        override suspend fun insert(item: ShoppingItem) = Unit
        override suspend fun update(item: ShoppingItem) {
            shoppingItems.value = shoppingItems.value.map { current ->
                if (current.id == item.id) item else current
            }
        }
        override suspend fun delete(itemId: String) = Unit
        override suspend fun getByProductId(productId: Long): ShoppingItem? = null
        override suspend fun checkout(checkedItemIds: List<String>): Boolean {
            checkoutCalls++
            return true
        }

        override suspend fun addAction(action: ActionItem) {
            addActionCalls++
        }
        override suspend fun updateAction(action: ActionItem) = Unit
        override suspend fun deleteAction(actionId: String) = Unit
        override suspend fun checkOffAction(actionId: String, checked: Boolean) {
            actions.value = actions.value.map { action ->
                if (action.id == actionId) {
                    action.copy(isChecked = checked, updatedAt = System.currentTimeMillis())
                } else {
                    action
                }
            }
        }
        override suspend fun deleteCheckedActions(): Int {
            deleteCheckedActionsCalls++
            val checked = actions.value.count { it.isChecked }
            actions.value = actions.value.filterNot { it.isChecked }
            return checked
        }
        override suspend fun checkDuplicateAction(
            normalizedText: String,
            scope: ActionScope,
            urgency: ActionUrgency,
            excludeActionId: String?,
        ): Boolean = false
    }

    private class FakeCatalogRepository(
        private val products: Map<Long, CanonicalProduct> = emptyMap(),
        private val taxonomy: List<TaxonomyLevel> = emptyList(),
    ) : CatalogRepository {
        override fun search(query: String): Flow<List<CanonicalProduct>> =
            flowOf(products.values.filter { it.name.contains(query, ignoreCase = true) })
        override suspend fun getById(id: Long): CanonicalProduct? = products[id]
        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> =
            flowOf(taxonomy)
    }
}
