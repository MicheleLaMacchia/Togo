package it.togo.app.presentation.history

import it.togo.app.domain.model.HistoricalItem

/**
 * Stato UI per la schermata Storico.
 */
data class HistoryUiState(
    val items: List<HistoricalItem> = emptyList(),
    val filteredItems: List<HistoricalItem> = emptyList(),
    /** Nome prodotto per `productId`, risolto dal catalogo per la UI. */
    val productNames: Map<Long, String> = emptyMap(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val isEmpty: Boolean = true,
    val snackbarMessage: String? = null
) {
    /** True se la ricerca è attiva con query non vuota */
    val hasActiveSearch: Boolean get() = isSearchByQuery && searchQuery.isNotBlank()

    private val isSearchByQuery: Boolean get() = searchQuery.isNotBlank()
}