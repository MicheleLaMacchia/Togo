package it.togo.app.presentation.activelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.presentation.additem.AddItemConfirmed
import it.togo.app.presentation.additem.AddItemUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel per la schermata Lista Attiva (MVI/UDF).
 *
 * - Espone [uiState] come StateFlow immutabile
 * - Gestisce [UiEvent] via [onEvent]
 * - Combina Flow da ShoppingListRepository + CatalogRepository per raggruppamento tassonomico L1→L2→L3
 * - Cache tassonomia in MutableStateFlow con default non-null
 * - Cold load: Room Flow emette subito, niente spinner
 */
class ActiveListViewModel(
    private val shoppingRepository: ShoppingListRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    // ---- State ----
    private val _uiState = MutableStateFlow(ActiveListUiState())
    val uiState = _uiState
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), ActiveListUiState())

    // ---- Cache tassonomia per raggruppamento ordinato (non-null default) ----
    private val taxonomyCache = MutableStateFlow(TaxonomyCache(emptyList(), emptyList(), emptyList()))

    init {
        loadTaxonomy()
        observeActiveItems()
        observeCheckedItems()
    }

    /** Carica tassonomia una volta per ordinamento categorie */
    private fun loadTaxonomy() {
        viewModelScope.launch {
            catalogRepository.getTaxonomy()
                .onEach { levels ->
                    val l1 = levels.filterIsInstance<TaxonomyLevel.Level1>().sortedBy { it.sortOrder }
                    val l2 = levels.filterIsInstance<TaxonomyLevel.Level2>().sortedBy { it.sortOrder }
                    val l3 = levels.filterIsInstance<TaxonomyLevel.Level3>().sortedBy { it.sortOrder }
                    taxonomyCache.value = TaxonomyCache(l1, l2, l3)
                }
                .launchIn(viewModelScope)
        }
    }

    /** Osserva voci attive (non checkate) e aggiorna UI state */
    private fun observeActiveItems() {
        viewModelScope.launch {
            combine(
                shoppingRepository.getActiveItems(),
                taxonomyCache.filterNotNull()
            ) { items, taxonomy ->
                buildUiState(items, taxonomy)
            }
            .onEach { newState ->
                _uiState.update { it.copy(
                    activeGroups = newState.activeGroups,
                    error = newState.error
                )}
            }
            .launchIn(viewModelScope)
        }
    }

    /** Osserva voci checkate per sezione "Presi" */
    private fun observeCheckedItems() {
        viewModelScope.launch {
            shoppingRepository.getCheckedItems()
                .onEach { checkedItems ->
                    _uiState.update { it.copy(checkedItems = checkedItems) }
                }
                .launchIn(viewModelScope)
        }
    }

    /** Costruisce ActiveListUiState raggruppando per tassonomia L1→L2→L3 */
    private fun buildUiState(
        items: List<ShoppingItem>,
        taxonomy: TaxonomyCache
    ): ActiveListUiState {
        val (l1List, l2List, l3List) = taxonomy

        // Mappe per lookup veloce: productId -> Level3 -> Level2 -> Level1
        val productToL3 = mutableMapOf<Long, TaxonomyLevel.Level3>()
        val l3ToL2 = mutableMapOf<Long, TaxonomyLevel.Level2>()
        val l2ToL1 = mutableMapOf<Long, TaxonomyLevel.Level1>()

        for (l3 in taxonomy.l3List) {
            productToL3[l3.id] = l3
            l3ToL2[l3.id] = taxonomy.l2List.find { it.id == l3.level2Id } ?: continue
            l2ToL1[l3.level2Id] = taxonomy.l1List.find { it.id == l3.level2Id } ?: continue
        }

        // Raggruppa items per Level3
        val itemsByL3 = mutableMapOf<Long, MutableList<ShoppingItem>>()
        for (item in items) {
            val l3 = productToL3[item.productId] ?: continue
            itemsByL3.getOrPut(l3.id) { mutableListOf() }.add(item)
        }

        // Ordina items dentro ogni Level3 per nome
        itemsByL3.values.forEach { it.sortBy { it.name } }

        // Build gruppi L1 → L2 → L3
        val groups = mutableListOf<CategoryGroup>()
        for (l1 in taxonomy.l1List) {
            val l2s = taxonomy.l2List.filter { it.level1Id == l1.id }
            val l2Groups = mutableListOf<Level2Group>()
            for (l2 in l2s) {
                val l3s = taxonomy.l3List.filter { it.level2Id == l2.id }
                val l3Groups = mutableListOf<Level3Group>()
                for (l3 in l3s) {
                    val l3Items = itemsByL3[l3.id] ?: emptyList()
                    if (l3Items.isNotEmpty()) {
                        l3Groups.add(Level3Group(l3, l3Items))
                    }
                }
                if (l3Groups.isNotEmpty()) {
                    l2Groups.add(Level2Group(l2, l3Groups))
                }
            }
            if (l2Groups.isNotEmpty()) {
                groups.add(CategoryGroup(l1, l2Groups))
            }
        }

        return ActiveListUiState(
            activeGroups = groups,
            checkedItems = _uiState.value.checkedItems,
            error = null
        )
    }

    /** Handle UI events */
    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.CheckOff -> handleCheckOff(event.itemId, event.isChecked)
            is UiEvent.EditItem -> handleEditItem(event.item)
            is UiEvent.DeleteItem -> handleDeleteItem(event.itemId)
            is UiEvent.NavigateToVoice -> { /* TODO: Story 4.x */ }
            is UiEvent.NavigateToHistory -> { /* TODO: Story 5.x */ }
            is UiEvent.ShareList -> { /* TODO: Story 5.x */ }
            is UiEvent.Checkout -> handleCheckout()
            is UiEvent.AddItemManual -> { /* Handled by screen opening bottom sheet */ }
        }
    }

    /** Handle confirmed item from AddItemBottomSheet */
    fun onAddItemConfirmed(confirmed: AddItemConfirmed) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingRepository.insert(confirmed.item)
            }
        }
    }

    private fun handleCheckOff(itemId: String, isChecked: Boolean) {
        // Trova l'item nello stato corrente (UI state) invece di ri-subscrivere al flow
        val item = _uiState.value.activeGroups
            .flatMap { it.level2Groups.flatMap { it.level3Groups.flatMap { it.items } } }
            .find { it.id == itemId }
            ?: _uiState.value.checkedItems.find { it.id == itemId }
            ?: return

        val updated = item.copyWith(isChecked = isChecked)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingRepository.update(updated)
            }
        }
    }

    private fun handleEditItem(item: ShoppingItem) {
        // TODO: Story 2.2 - apri bottom sheet modifica
    }

    private fun handleDeleteItem(itemId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingRepository.delete(itemId)
            }
        }
    }

    private fun handleCheckout() {
        viewModelScope.launch {
            // Leggi checked items direttamente dal repository per source of truth
            val checkedIds = shoppingRepository.getCheckedItems()
                .first()
                .map { it.id }
            if (checkedIds.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    shoppingRepository.checkout(checkedIds)
                }
            }
        }
    }

    /** Cache tassonomia per evitare race condition */
    private data class TaxonomyCache(
        val l1List: List<TaxonomyLevel.Level1>,
        val l2List: List<TaxonomyLevel.Level2>,
        val l3List: List<TaxonomyLevel.Level3>
    )
}