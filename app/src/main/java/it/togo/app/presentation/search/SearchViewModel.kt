package it.togo.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
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
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), SearchUiState())

    // ---- Input query con debounce 300ms ----
    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchResults: Flow<List<CanonicalProduct>> = _searchQuery
        .debounce(300) // debounce 300ms
        .flatMapLatest { query: String ->
            if (query.isNotBlank()) {
                catalogRepository.search(query)
                    .onStart { _uiState.update { it.copy(query = query, isLoading = true, error = null) } }
                    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
                    .catch { e: Throwable ->
                        _uiState.update { it.copy(error = e.message ?: "Errore ricerca", isLoading = false) }
                        emit(emptyList())
                    }
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList<CanonicalProduct>())
                    .onStart { _uiState.update { it.copy(isLoading = true) } }
                    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            }
        }
        .onEach { results -> _uiState.update { it.copy(results = results, isLoading = false) } }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    init {
        _searchQuery.value = ""
        searchResults.launchIn(viewModelScope)
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
        _uiState.update { it.copy(query = query, error = null) }
    }

    private fun handleClear() {
        _searchQuery.value = ""
        _uiState.update { it.copy(query = "", results = emptyList(), error = null, isLoading = false) }
    }

    private fun handleItemSelected(product: CanonicalProduct) {
        // TODO: navigazione a dettaglio prodotto / aggiunta alla lista
    }
}
