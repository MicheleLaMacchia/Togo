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
 * ViewModel per il Bottom Sheet "Aggiungi/Modifica Voce Manuale".
 *
 * Supporta due modalità:
 * - INSERT: nuova voce (default)
 * - EDIT: modifica voce esistente (pre-popolato, step 2 default)
 *
 * Gestisce:
 * - Ricerca prodotti con debounce 300ms
 * - Validazione step-by-step
 * - Mapping a ShoppingItem su conferma (insert o update)
 */
class AddItemViewModel(
    private val catalogRepository: CatalogRepository,
    private val shoppingRepository: ShoppingListRepository,
    private val editItem: ShoppingItem? = null
) : ViewModel() {

    // Flag per modalità edit - ora parte dello UI state
    val isEditMode: Boolean = editItem != null

    // ---- State ----
    private val _uiState = MutableStateFlow(AddItemUiState(isEditMode = editItem != null))
    val uiState = _uiState
        .distinctUntilChanged()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(), AddItemUiState(isEditMode = editItem != null))

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
        if (isEditMode) {
            initializeEditMode()
        } else {
            _searchQuery.value = ""
        }
    }

    /** Inizializza stato per modalità edit */
    private fun initializeEditMode() {
        editItem?.let { item ->
            // Trova prodotto canonico per l'item
            viewModelScope.launch {
                try {
                    val product = catalogRepository.getById(item.productId)
                        ?: throw IllegalStateException("Prodotto non più disponibile nel catalogo")
                    
                    val compatible = product.compatibleUnits
                        .ifEmpty { product.defaultUnit?.let { listOf(it) } ?: listOf(StandardUnit.PIECE) }
                    
                    val defaultUnit = product.defaultUnit ?: compatible.firstOrNull { it == StandardUnit.PIECE } ?: compatible.first()

                    _uiState.update {
                        it.copy(
                            selectedProduct = product,
                            searchResults = emptyList(),
                            searchQuery = product.name,
                            compatibleUnits = compatible,
                            selectedUnit = item.unit, // Singola assegnazione: usa l'unità dell'item in edit
                            quantity = item.quantity,
                            brand = item.brand ?: "",
                            variant = item.variant ?: "",
                            condition = item.condition ?: "",
                            currentStep = 2, // In modifica si parte da step 2 (quantità/unità)
                            errors = emptyMap(),
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Errore caricamento prodotto: ${e.message}") }
                }
            }
        }
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
            // In edit mode, non permettere di tornare allo step 1 (ricerca)
            val minStep = if (isEditMode) 2 else 1
            state.copy(currentStep = maxOf(minStep, state.currentStep - 1))
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

        val product = state.selectedProduct ?: return
        val unit = state.selectedUnit ?: return

        val item = if (isEditMode) {
            // Modalità EDIT: aggiorna item esistente mantenendo ID e timestamp creato
            editItem!!.copyWith(
                productId = state.selectedProduct!!.id,
                quantity = state.quantity,
                unit = unit,
                brand = state.brand.ifBlank { null },
                variant = state.variant.ifBlank { null },
                condition = state.condition.ifBlank { null },
                isChecked = editItem!!.isChecked, // mantiene stato check
            ).copyWith(updatedAt = System.currentTimeMillis()) // Aggiorna timestamp
        } else {
            // Modalità INSERT: nuova voce
            ShoppingItem(
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
        }

        // Inserisci/aggiorna in repository
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (isEditMode) {
                    shoppingRepository.update(item)
                } else {
                    shoppingRepository.insert(item)
                }
            }
        }

        // Reset per nuova voce DOPO successo DB
        handleReset()
    }

    private fun handleCancel() {
        handleReset()
    }

    private fun handleReset() {
        if (isEditMode) {
            // In edit mode, reset to original values or clear
            initializeEditMode()
        } else {
            _uiState.update { AddItemUiState(isEditMode = false) }
        }
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

    /** Factory per modalità INSERT */
    companion object {
        fun forInsert(
            catalogRepository: CatalogRepository,
            shoppingRepository: ShoppingListRepository
        ): AddItemViewModel = AddItemViewModel(catalogRepository, shoppingRepository)

        /** Factory per modalità EDIT */
        fun forEdit(
            catalogRepository: CatalogRepository,
            shoppingRepository: ShoppingListRepository,
            item: ShoppingItem
        ): AddItemViewModel = AddItemViewModel(catalogRepository, shoppingRepository, item)
    }
}