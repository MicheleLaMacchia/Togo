package it.togo.app.presentation.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel per il Bottom Sheet "Aggiungi Voce Manuale".
 *
 * Gestisce:
 * - Ricerca prodotti con debounce 300ms
 * - Validazione step-by-step
 * - Mapping a ShoppingItem su conferma
 */
class AddItemViewModel(
    private val catalogRepository: CatalogRepository,
    private val shoppingRepository: ShoppingListRepository
) : ViewModel() {

    // ---- State ----
    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState = _uiState
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), AddItemUiState())

    // ---- Ricerca prodotti con debounce 300ms ----
    private val _searchQuery = MutableStateFlow("")
    val searchResults = _searchQuery
        .debounce(300) // debounce 300ms
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.length >= 2) {
                catalogRepository.search(query)
                    .map { results -> results }
                    .onStart { _uiState.update { it.copy(isLoading = true) } }
                    .onCompletion { _uiState.update { it.copy(isLoading = false) } }
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), emptyList())

    init {
        _searchQuery.value = ""
    }

    /** Handle UI events */
    fun onEvent(event: AddItemUiEvent) {
        when (event) {
            is AddItemUiEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is AddItemUiEvent.ProductSelected -> handleProductSelected(event.product)
            is AddItemUiEvent.QuantityChanged -> handleQuantityChanged(event.quantity)
            is AddItemUiEvent.UnitChanged -> handleUnitChanged(event.unit)
            is AddItemUiEvent.BrandChanged -> handleBrandChanged(event.brand)
            is AddItemUiEvent.VariantChanged -> handleVariantChanged(event.variant)
            is AddItemUiEvent.ConditionChanged -> handleConditionChanged(event.condition)
            is AddItemUiEvent.NextStep -> handleNextStep()
            is AddItemUiEvent.PreviousStep -> handlePreviousStep()
            is AddItemUiEvent.Confirm -> handleConfirm()
            is AddItemUiEvent.Cancel -> handleCancel()
            is AddItemUiEvent.Reset -> handleReset()
        }
    }

    private fun handleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, errors = emptyMap()) }
        _searchQuery.value = query
    }

    private fun handleProductSelected(product: CanonicalProduct) {
        // Determina unità compatibili per il prodotto
        val compatible = product.compatibleUnits
            .ifEmpty { product.defaultUnit?.let { listOf(it) } ?: listOf(StandardUnit.PIECE) }

        val defaultUnit = product.defaultUnit ?: compatible.firstOrNull { it == StandardUnit.PIECE } ?: compatible.first()

        _uiState.update {
            it.copy(
                selectedProduct = product,
                searchResults = emptyList(),
                searchQuery = product.name,
                compatibleUnits = compatible,
                selectedUnit = defaultUnit,
                quantity = 1.0,
                currentStep = 2,
                errors = emptyMap(),
            )
        }
    }

    private fun handleQuantityChanged(quantity: Double) {
        _uiState.update {
            it.copy(
                quantity = maxOf(0.0, quantity),
                errors = it.errors.minus("quantity"),
            )
        }
    }

    private fun handleUnitChanged(unit: StandardUnit) {
        _uiState.update {
            it.copy(selectedUnit = unit, errors = it.errors.minus("unit"))
        }
    }

    private fun handleBrandChanged(brand: String) {
        _uiState.update { it.copy(brand = brand) }
    }

    private fun handleVariantChanged(variant: String) {
        _uiState.update { it.copy(variant = variant) }
    }

    private fun handleConditionChanged(condition: String) {
        _uiState.update { it.copy(condition = condition) }
    }

    private fun handleNextStep() {
        _uiState.update { state ->
            val nextStep = when (state.currentStep) {
                1 -> if (state.canProceedToStep2) 2 else 1
                2 -> if (state.canProceedToStep3) 3 else 2
                3 -> 4
                else -> state.currentStep
            }
            // Validazione step corrente
            val errors = validateStep(state.currentStep, state)
            state.copy(currentStep = nextStep, errors = errors)
        }
    }

    private fun handlePreviousStep() {
        _uiState.update { state ->
            state.copy(currentStep = maxOf(1, state.currentStep - 1))
        }
    }

    private fun handleConfirm() {
        val state = _uiState.value
        // Valida sempre step 4 (conferma) indipendentemente da currentStep
        val errors = validateStep(4, state)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, currentStep = 4) }
            return
        }

        // Crea ShoppingItem con null safety
        val product = state.selectedProduct ?: return
        val unit = state.selectedUnit ?: return
        val item = ShoppingItem(
            id = UUID.randomUUID().toString(),
            productId = product.id,
            quantity = state.quantity,
            unit = unit,
            brand = state.brand.ifBlank { null },
            variant = state.variant.ifBlank { null },
            condition = state.condition.ifBlank { null },
            isChecked = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )

        // Inserisci in repository
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                shoppingRepository.insert(item)
            }
        }

        // Reset per nuova voce
        handleReset()
    }

    private fun handleCancel() {
        // Il bottom sheet gestisce la chiusura, qui facciamo reset
        handleReset()
    }

    private fun handleReset() {
        _uiState.update { AddItemUiState() }
    }

    /** Validazione per step corrente */
    private fun validateStep(step: Int, state: AddItemUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        when (step) {
            1 -> {
                if (!state.canProceedToStep2) errors["product"] = "Seleziona un prodotto"
            }
            2 -> {
                if (state.quantity <= 0) errors["quantity"] = "Quantità deve essere > 0"
                if (state.selectedUnit == null) errors["unit"] = "Seleziona un'unità"
            }
            3 -> {} // Attributi opzionali
            4 -> {
                if (!state.isValidForConfirm) {
                    if (!state.canProceedToStep2) errors["product"] = "Seleziona un prodotto"
                    if (state.quantity <= 0) errors["quantity"] = "Quantità deve essere > 0"
                    if (state.selectedUnit == null) errors["unit"] = "Seleziona un'unità"
                }
            }
        }
        return errors
    }
}