package it.togo.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.data.database.CatalogQueries
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ListItemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

/** ViewModel della cronologia shopping basata sul modello unificato ListItem. */
class HistoryViewModel(
    private val listItemRepository: ListItemRepository,
    private val catalogRepository: CatalogRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val snackbarMessage = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            combine(
                listItemRepository.observeAll(),
                searchQuery,
                snackbarMessage,
            ) { allItems, query, message ->
                val items = allItems
                    .filter { it.status == ListItemStatus.PURCHASED || it.status == ListItemStatus.DELETED }
                    .sortedByDescending { it.statusChangedAt }
                val productNames = items.mapNotNull { item ->
                    catalogRepository.getById(item.productId)?.let { item.productId to it.name }
                }.toMap()
                val filtered = if (query.isBlank()) items else filterItems(items, query, productNames)
                HistoryUiState(
                    items = items,
                    filteredItems = filtered,
                    productNames = productNames,
                    searchQuery = query,
                    isEmpty = filtered.isEmpty(),
                    snackbarMessage = message,
                )
            }.distinctUntilChanged().collect { _uiState.value = it }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    private fun filterItems(
        items: List<ListItem>,
        query: String,
        productNames: Map<Long, String>,
    ): List<ListItem> {
        val normalizedQuery = CatalogQueries.normalize(query).lowercase(Locale.ROOT)
        return items.filter { item ->
            productNames[item.productId]?.let(CatalogQueries::normalize)
                ?.lowercase(Locale.ROOT)?.contains(normalizedQuery) == true ||
                listOfNotNull(item.brand, item.variant, item.condition, item.note).any { value ->
                    CatalogQueries.normalize(value).lowercase(Locale.ROOT).contains(normalizedQuery)
                }
        }
    }

    fun reAddToActiveList(item: ListItem) {
        if (item.status != ListItemStatus.PURCHASED) return
        viewModelScope.launch(ioDispatcher) {
            try {
                val result = listItemRepository.reAdd(item.id)
                snackbarMessage.value = if (result == null) "Voce non più disponibile" else "Aggiunto alla lista"
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                snackbarMessage.value = "Aggiunta non riuscita"
            }
        }
    }

    fun restore(item: ListItem) {
        if (item.status != ListItemStatus.DELETED) return
        viewModelScope.launch(ioDispatcher) {
            try {
                val result = listItemRepository.restore(item.id)
                snackbarMessage.value = if (result == null) "Ripristino non riuscito" else "Voce ripristinata"
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                snackbarMessage.value = "Ripristino non riuscito"
            }
        }
    }

    fun showSnackbar(message: String) {
        snackbarMessage.value = message
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }
}
