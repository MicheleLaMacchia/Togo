package it.togo.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel per la ricerca nel catalogo.
 *
 * - Espone [uiState] come StateFlow con risultati ricerca
 * - Debounce 300ms su input utente
 * - Limita risultati a 50 (già limitato lato DB)
 */
class SearchViewModel(
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    // ---- State ----
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), SearchUiState())

    // ---- Input query con debounce 300ms ----
    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchResults: Flow<List<CanonicalProduct>> = _searchQuery
        .debounce(300) // debounce 300ms
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isNotBlank()) {
                catalogRepository.search(query)
                    .onStart { _uiState.update { it.copy(isLoading = true) } }
                    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
                    .catch { e ->
                        _uiState.update { it.copy(error = e.message ?: "Errore ricerca", isLoading = false) }
                        kotlinx.coroutines.flow.flowOf(emptyList())
                    }
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
                    .onStart { _uiState.update { it.copy(isLoading = true) } }
                    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    init {
        _searchQuery.value = ""
    }

    /** Handle UI events */
    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.QueryChanged -> handleQueryChanged(event.query)
            is SearchUiEvent.Clear -> handleClear()
            is SearchUiEvent.ItemSelected -> handleItemSelected(event.product)
        }
    }

    private fun handleQueryChanged(query: String) {
        _searchQuery.value = query
    }

    private fun handleClear() {
        _searchQuery.value = ""
    }

    private fun handleItemSelected(product: CanonicalProduct) {
        // TODO: navigazione a dettaglio prodotto / aggiunta alla lista
        // Per ora emettiamo un evento che la Activity/Composable padre può gestire
        // In futuro: navigazione a dettaglio prodotto o aggiunta alla lista
    }
}