package it.togo.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.data.database.CatalogQueries
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.HistoricalItem
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.HistoryRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * ViewModel MVI per la schermata Storico.
 *
 * Espone storico prodotti, gestisce ricerca per nome e riaggiunta alla lista attiva.
 */
class HistoryViewModel(
    private val historyRepository: HistoryRepository,
    private val catalogRepository: CatalogRepository,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _snackbarMessage = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                historyRepository.getAll(),
                _searchQuery,
                _snackbarMessage
            ) { items, query, snackbarMsg ->
                val filtered = if (query.isBlank()) {
                    items
                } else {
                    filterItemsByName(items, query)
                }
                HistoryUiState(
                    items = items,
                    filteredItems = filtered,
                    searchQuery = query,
                    isEmpty = filtered.isEmpty(),
                    snackbarMessage = snackbarMsg
                )
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Aggiorna la query di ricerca.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Cerca prodotti nello storico per nome (case-insensitive, accent-fold).
     */
    private suspend fun filterItemsByName(items: List<HistoricalItem>, query: String): List<HistoricalItem> {
        val normalizedQuery = CatalogQueries.normalize(query).lowercase()
        return items.filter { item ->
            val product = catalogRepository.getById(item.productId)
            val productName = product?.name?.lowercase() ?: ""
            productName.contains(normalizedQuery)
        }.sortedByDescending { it.purchasedAt }
    }

    /**
     * Riaggiunge un prodotto dallo storico alla lista attiva.
     */
    fun reAddToActiveList(item: HistoricalItem) {
        viewModelScope.launch(Dispatchers.IO) {
            // Verifica se già presente nella lista attiva
            val activeItems = shoppingListRepository.getActiveItems().first()
            val alreadyExists = activeItems.any { it.productId == item.productId && !it.isChecked }

            if (alreadyExists) {
                _snackbarMessage.value = "Già nella lista"
                return@launch
            }

            // Inserisce nuovo ShoppingItem con ultima quantità/unità dallo storico
            shoppingListRepository.insert(
                ShoppingItem(
                    productId = item.productId,
                    quantity = item.lastQuantity,
                    unit = item.lastUnit
                )
            )
            _snackbarMessage.value = "Aggiunto alla lista"
        }
    }

    /**
     * Mostra un messaggio snackbar.
     */
    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    /**
     * Pulisce il messaggio snackbar.
     */
    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}