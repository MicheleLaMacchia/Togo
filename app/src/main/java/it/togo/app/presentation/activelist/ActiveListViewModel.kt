package it.togo.app.presentation.activelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.presentation.additem.AddItemConfirmed
import it.togo.app.presentation.additem.AddItemUiEvent
import it.togo.app.presentation.additem.AddItemViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

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

    // Stato per Snackbar undo - con auto-dismiss 5s
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    // Item eliminato per undo - observable per config changes
    private val _deletedItemForUndo = MutableStateFlow<ShoppingItem?>(null)
    val deletedItemForUndo = _deletedItemForUndo
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    // Contatore per evitare race condition su snackbar timer
    private val _snackbarCounter = AtomicInteger(0)

    init {
        loadTaxonomy()
        observeAllItems()
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

    /** Osserva TUTTE le voci (attive + checkate) per evitare race condition cold load */
    private fun observeAllItems() {
        viewModelScope.launch {
            combine(
                shoppingRepository.getActiveItems(),  // ora restituisce TUTTI gli item (checked + unchecked)
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
            l2ToL1[l2.id] = taxonomy.l1List.find { it.id == l2.level1Id } ?: continue
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
            is UiEvent.UndoDelete -> handleUndoDelete()
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

        if (isChecked) {
            // CHECK-OFF: 300ms delay + slide animation
            // 1. Imposta pending state per animazione
            _uiState.update { it.copy(pendingCheckOffItemId = itemId) }
            
            // 2. Dopo 300ms, sposta l'item da active a checked
            viewModelScope.launch {
                kotlinx.coroutines.delay(300)
                _uiState.update { state ->
                    val updated = item.copyWith(isChecked = true)
                    val newActiveGroups = state.activeGroups.map { group ->
                        group.copy(
                            level2Groups = group.level2Groups.map { l2Group ->
                                l2Group.copy(
                                    level3Groups = l2Group.level3Groups.map { l3Group ->
                                        l3Group.copy(
                                            items = l3Group.items.filter { it.id != itemId }
                                        )
                                    ).filter { it.items.isNotEmpty() }
                                ).filter { it.level2Groups.isNotEmpty() }
                            ).filter { it.level2Groups.isNotEmpty() }
                        }
                    .copy(
                        pendingCheckOffItemId = null,
                        checkingOffItemId = itemId
                    )
                }
                // Dopo animazione, aggiorna repo
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        shoppingRepository.update(item.copyWith(isChecked = true))
                    }
                    // Pulisci checkingOffItemId dopo un po'
                    kotlinx.coroutines.delay(300)
                    _uiState.update { it.copy(checkingOffItemId = null) }
                }
            }
        } else {
            // UNCHECK: instant restore to original category
            _uiState.update { state ->
                val updated = item.copyWith(isChecked = false)
                // Trova la categoria originale per reinserire l'item
                val newActiveGroups = state.activeGroups.map { group ->
                    group.copy(
                        level2Groups = group.level2Groups.map { l2Group ->
                            l2Group.copy(
                                level3Groups = l2Group.level3Groups.map { l3Group ->
                                    if (l3Group.items.any { it.id == itemId }) {
                                        l3Group.copy(items = (l3Group.items + updated).sortedBy { it.name })
                                    } else {
                                        l3Group
                                    }
                                }
                            ).filter { it.level3Groups.isNotEmpty() }
                        ).filter { it.level2Groups.isNotEmpty() }
                }
                state.copy(
                    checkedItems = state.checkedItems.filter { it.id != itemId },
                    activeGroups = newActiveGroups
                )
            }
            // Aggiorna repo
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    shoppingRepository.update(item.copyWith(isChecked = false))
                }
            }
        }
    }

    private fun handleEditItem(item: ShoppingItem) {
        // Apre bottom sheet in modalità edit - gestito da ActiveListScreen tramite UiState
        // Qui potremmo emettere un evento per aprire il bottom sheet
    }

    private fun handleDeleteItem(itemId: String) {
        // Trova l'item per possibile undo
        val item = _uiState.value.activeGroups
            .flatMap { it.level2Groups.flatMap { it.level3Groups.flatMap { it.items } } }
            .find { it.id == itemId }
            ?: _uiState.value.checkedItems.find { it.id == itemId }
            ?: return

        // Salva item per undo PRIMA del delete
        _deletedItemForUndo.value = item

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    shoppingRepository.delete(itemId)
                }
                // Snackbar DOPO delete riuscito
                showUndoSnackbar("Voce eliminata")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Eliminazione fallita: ${e.message}") }
            }
        }
    }

    private fun handleUndoDelete() {
        _deletedItemForUndo.value?.let { item ->
            // Aggiorna timestamp per audit trail
            val restored = item.copyWith(updatedAt = System.currentTimeMillis())
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    shoppingRepository.insert(restored)
                }
            }
            _deletedItemForUndo.value = null
        }
        _snackbarMessage.value = null
    }

    /** Mostra snackbar con auto-dismiss 5s */
    private fun showUndoSnackbar(message: String) {
        val currentCount = _snackbarCounter.incrementAndGet()
        _snackbarMessage.value = message
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            // Controlla che nessun altro snackbar abbia sovrascritto questo
            if (_snackbarCounter.get() == currentCount && _snackbarMessage.value != null) {
                _snackbarMessage.value = null
            }
        }
    }

    private fun handleCheckout() {
        viewModelScope.launch {
            // Usa lo stato UI già osservato per evitare cold flow hang
            val checkedIds = _uiState.value.checkedItems.map { it.id }
            if (checkedIds.isNotEmpty()) {
                try {
                    withContext(Dispatchers.IO) {
                        shoppingRepository.checkout(checkedIds)
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Checkout fallito: ${e.message}") }
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