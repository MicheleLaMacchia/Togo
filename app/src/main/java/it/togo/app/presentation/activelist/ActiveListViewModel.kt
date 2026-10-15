package it.togo.app.presentation.activelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class ActiveListViewModel(
    private val shoppingRepository: ShoppingListRepository,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveListUiState())
    val uiState = _uiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ActiveListUiState())

    private val taxonomyCache = MutableStateFlow<TaxonomyCache?>(null)

    private val _snackbarCounter = AtomicInteger(0)
    private val _checkoutSnackbarCounter = AtomicInteger(0)

    private var _isCheckingOut = false
    private val _deletedItemForUndo = MutableStateFlow<ShoppingItem?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    init {
        loadTaxonomy()
        observeAllItems()
    }

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

    private fun observeAllItems() {
        viewModelScope.launch {
            combine(
                shoppingRepository.getActiveItems(),
                taxonomyCache.filterNotNull()
            ) { items, taxonomy ->
                buildUiState(items, taxonomy)
            }
                .onEach { newState ->
                    _uiState.value = newState
                }
                .launchIn(viewModelScope)
        }
    }

    private fun buildUiState(
        items: List<ShoppingItem>,
        taxonomy: TaxonomyCache
    ): ActiveListUiState {
        val (l1List, l2List, l3List) = taxonomy

        val l3ToL2 = mutableMapOf<Long, Long>()
        val l2ToL1 = mutableMapOf<Long, Long>()

        for (l3 in l3List) {
            l3ToL2[l3.id] = l3.level2Id
            l2ToL1[l3.level2Id] = l2List.find { it.id == l3.level2Id }?.level1Id ?: continue
        }

        val itemsByL3 = mutableMapOf<Long, MutableList<ShoppingItem>>()
        for (item in items) {
            val l3Id = l3List.find { it.id == item.productId }?.id ?: continue
            itemsByL3.getOrPut(l3Id) { mutableListOf() }.add(item)
        }

        itemsByL3.values.forEach { list -> list.sortBy { item -> item.productId } }

        val groups = mutableListOf<CategoryGroup>()
        for (l1 in l1List) {
            val l2s = l2List.filter { it.level1Id == l1.id }
            val l2Groups = mutableListOf<Level2Group>()
            for (l2 in l2s) {
                val l3s = l3List.filter { it.level2Id == l2.id }
                val l3Groups = mutableListOf<Level3Group>()
                for (l3 in l3s) {
                    val l3Items = itemsByL3.getOrDefault(l3.id, emptyList())
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

        val checkedItems = items.filter { it.isChecked }

        return ActiveListUiState(
            activeGroups = groups,
            checkedItems = checkedItems,
            error = null
        )
    }

    fun onEvent(event: UiEvent) {
        when (event) {
            is UiEvent.CheckOff -> handleCheckOff(event.itemId, event.isChecked)
            is UiEvent.EditItem -> handleEditItem(event.item)
            is UiEvent.DeleteItem -> handleDeleteItem(event.itemId)
            is UiEvent.NavigateToVoice -> { }
            is UiEvent.NavigateToHistory -> { }
            is UiEvent.ShareList -> { }
            is UiEvent.Checkout -> handleCheckout()
            is UiEvent.AddItemManual -> { }
            is UiEvent.UndoDelete -> handleUndoDelete()
            is UiEvent.UndoCheckout -> handleUndoCheckout()
        }
    }

    fun onAddItemConfirmed(confirmed: AddItemConfirmed) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingRepository.insert(confirmed.item)
            }
        }
    }

    private fun findItem(itemId: String): ShoppingItem? {
        return _uiState.value.activeGroups
            .flatMap { group -> group.level2Groups.flatMap { l2 -> l2.level3Groups.flatMap { l3 -> l3.items } } }
            .find { it.id == itemId }
            ?: _uiState.value.checkedItems.find { it.id == itemId }
    }

    private fun handleCheckOff(itemId: String, isChecked: Boolean) {
        val item = findItem(itemId) ?: return

        if (isChecked) {
            _uiState.value = _uiState.value.copy(pendingCheckOffItemId = itemId)

            viewModelScope.launch {
                delay(300)
                _uiState.value = _uiState.value.copy(
                    activeGroups = _uiState.value.activeGroups.map { group ->
                        group.copy(
                            level2Groups = group.level2Groups.map { l2 ->
                                l2.copy(
                                    level3Groups = l2.level3Groups
                                        .map { l3 ->
                                            l3.copy(items = l3.items.filter { it.id != itemId })
                                        }
                                        .filter { it.items.isNotEmpty() }
                                )
                            }.filter { it.level3Groups.isNotEmpty() }
                        )
                    },
                    pendingCheckOffItemId = null,
                    checkingOffItemId = itemId
                )

                withContext(Dispatchers.IO) {
                    shoppingRepository.update(item.copyWith(isChecked = true))
                }
                delay(300)
                _uiState.value = _uiState.value.copy(checkingOffItemId = null)
            }
        } else {
            val updated = item.copyWith(isChecked = false)
            _uiState.value = _uiState.value.copy(
                activeGroups = _uiState.value.activeGroups.map { group ->
                    group.copy(
                        level2Groups = group.level2Groups.map { l2 ->
                            l2.copy(
                                level3Groups = l2.level3Groups.map { l3 ->
                                    if (l3.items.any { it.id == itemId }) {
                                        l3.copy(items = (l3.items + updated).sortedBy { it.productId })
                                    } else {
                                        l3
                                    }
                                }.filter { it.items.isNotEmpty() }
                            )
                        }.filter { it.level3Groups.isNotEmpty() }
                    )
                },
                checkedItems = _uiState.value.checkedItems.filter { it.id != itemId }
            )

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    shoppingRepository.update(updated)
                }
            }
        }
    }

    private fun handleEditItem(item: ShoppingItem) {
        // Edit handled by ActiveListScreen via UiEvent
    }

    private fun handleDeleteItem(itemId: String) {
        val item = findItem(itemId) ?: return

        _deletedItemForUndo.value = item

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    shoppingRepository.delete(itemId)
                }
                showUndoSnackbar("Voce eliminata")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Eliminazione fallita: ${e.message}")
            }
        }
    }

    private fun handleUndoDelete() {
        _deletedItemForUndo.value?.let { item ->
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

    private fun showUndoSnackbar(message: String) {
        val currentCount = _snackbarCounter.incrementAndGet()
        _snackbarMessage.value = message
        viewModelScope.launch {
            delay(5000)
            if (_snackbarCounter.get() == currentCount && _snackbarMessage.value != null) {
                _snackbarMessage.value = null
            }
        }
    }

    private fun showCheckoutSnackbar(message: String) {
        val currentCount = _checkoutSnackbarCounter.incrementAndGet()
        _uiState.value = _uiState.value.copy(checkoutSnackbarMessage = message)
        viewModelScope.launch {
            delay(5000)
            if (_checkoutSnackbarCounter.get() == currentCount && _uiState.value.checkoutSnackbarMessage != null) {
                _uiState.value = _uiState.value.copy(checkoutSnackbarMessage = null)
            }
        }
    }

    private fun handleCheckout() {
        if (_isCheckingOut) return
        _isCheckingOut = true

        val items = _uiState.value.activeGroups
            .flatMap { it.level2Groups.flatMap { it.level3Groups.flatMap { it.items } } }
            .filter { !it.isChecked }

        if (items.isEmpty()) {
            _isCheckingOut = false
            return
        }

        _uiState.value = _uiState.value.copy(
            checkedItemsForUndo = items
        )

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    items.forEach { item ->
                        shoppingRepository.update(item.copyWith(isChecked = true))
                    }
                }
                showCheckoutSnackbar("Spesa conclusa (${items.size} voci)")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Checkout fallito: ${e.message}")
            } finally {
                _isCheckingOut = false
            }
        }
    }

    private fun handleUndoCheckout() {
        _uiState.value.checkedItemsForUndo.forEach { item ->
            val unchecked = item.copyWith(isChecked = false)
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    shoppingRepository.update(unchecked)
                }
            }
        }
        _uiState.value = _uiState.value.copy(checkedItemsForUndo = emptyList())
    }

    private data class TaxonomyCache(
        val l1: List<TaxonomyLevel.Level1>,
        val l2: List<TaxonomyLevel.Level2>,
        val l3: List<TaxonomyLevel.Level3>,
    ) {
        fun component1() = l1
        fun component2() = l2
        fun component3() = l3
    }
}
