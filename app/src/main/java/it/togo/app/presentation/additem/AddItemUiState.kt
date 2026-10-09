package it.togo.app.presentation.additem

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import kotlinx.coroutines.flow.Flow

/**
 * UI State per il Bottom Sheet "Aggiungi Voce Manuale".
 *
 * Gestisce il flusso multi-step:
 * 1. Ricerca prodotto (query → risultati)
 * 2. Selezione prodotto → quantità + unità
 * 3. Attributi opzionali (marca, variante, conservazione)
 * 4. Conferma → emette AddItemConfirmed
 */
data class AddItemUiState(
    // Step 1: Ricerca
    val searchQuery: String = "",
    val searchResults: List<CanonicalProduct> = emptyList(),
    val isSearching: Boolean = false,

    // Step 2: Prodotto selezionato + quantità/unità
    val selectedProduct: CanonicalProduct? = null,
    val quantity: Double = 1.0,
    val selectedUnit: StandardUnit? = null,
    val compatibleUnits: List<StandardUnit> = emptyList(),

    // Step 3: Attributi opzionali
    val brand: String = "",
    val variant: String = "",
    val condition: String = "",

    // Validazione
    val errors: Map<String, String> = emptyMap(),

    // Stato UI
    val isLoading: Boolean = false,
    val currentStep: Int = 1, // 1=ricerca, 2=quantità/unità, 3=attributi, 4=conferma
) {
    /** Prodotto selezionato valido per procedere allo step 2 */
    val canProceedToStep2: Boolean
        get() = selectedProduct != null

    /** Quantità > 0 e unità selezionata per procedere allo step 3 */
    val canProceedToStep3: Boolean
        get() = quantity > 0 && selectedUnit != null

    /** Tutti i campi obbligatori validi per conferma */
    val isValidForConfirm: Boolean
        get() = canProceedToStep3 && selectedProduct != null
}

/**
 * Eventi UI per AddItemViewModel.
 */
sealed interface AddItemUiEvent {
    /** Cambio query ricerca (debounced in ViewModel) */
    data class SearchQueryChanged(val query: String) : AddItemUiEvent

    /** Selezione prodotto dai risultati ricerca */
    data class ProductSelected(val product: CanonicalProduct) : AddItemUiEvent

    /** Cambio quantità (stepper/input) */
    data class QuantityChanged(val quantity: Double) : AddItemUiEvent

    /** Cambio unità selezionata */
    data class UnitChanged(val unit: StandardUnit) : AddItemUiEvent

    /** Cambio marca */
    data class BrandChanged(val brand: String) : AddItemUiEvent

    /** Cambio variante */
    data class VariantChanged(val variant: String) : AddItemUiEvent

    /** Cambio conservazione */
    data class ConditionChanged(val condition: String) : AddItemUiEvent

    /** Avanti allo step successivo */
    object NextStep : AddItemUiEvent

    /** Indietro allo step precedente */
    object PreviousStep : AddItemUiEvent

    /** Conferma creazione voce */
    object Confirm : AddItemUiEvent

    /** Annulla e chiudi */
    object Cancel : AddItemUiEvent

    /** Reset completo (nuova voce) */
    object Reset : AddItemUiEvent
}

/** Risultato conferma: ShoppingItem pronto per insert */
data class AddItemConfirmed(
    val item: ShoppingItem
)