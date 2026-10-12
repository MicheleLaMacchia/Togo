package it.togo.app.presentation.search

import it.togo.app.domain.model.CanonicalProduct
import kotlinx.coroutines.flow.Flow

/**
 * UI State per la ricerca nel catalogo.
 *
 * - [query]: query corrente
 * - [results]: risultati ricerca (max 50)
 * - [isLoading]: true durante ricerca
 * - [error]: messaggio errore se repository fallisce
 * - [recentQueries]: ultime query per suggerimenti (opzionale futuro)
 */
data class SearchUiState(
    val query: String = "",
    val results: List<CanonicalProduct> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentQueries: List<String> = emptyList(),
) {
    /** Query normalizzata non vuota */
    val hasQuery: Boolean
        get() = query.trim().isNotEmpty()

    /** Ci sono risultati */
    val hasResults: Boolean
        get() = results.isNotEmpty()

    /** Numero risultati (per UI "altri X risultati") */
    val resultsCount: Int
        get() = results.size
}

/**
 * Eventi UI per la ricerca.
 */
sealed interface SearchUiEvent {
    /** Cambio query (debounced in ViewModel) */
    data class QueryChanged(val query: String) : SearchUiEvent

    /** Pulisci query */
    object Clear : SearchUiEvent

    /** Selezione item risultato */
    data class ItemSelected(val product: it.togo.app.domain.model.CanonicalProduct) : SearchUiEvent
}