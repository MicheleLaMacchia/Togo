package it.togo.app.presentation.additem

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import kotlinx.coroutines.flow.Flow

/**
 * UI State per il Bottom Sheet "Aggiungi Voce Manuale".
 *
 * Gestisce il flusso multi-step:
 * 1. Ricerca prodotto (query → risultati)
 * 2. Selezione prodotto → quantità + unità
 * 3. Attributi opzionali (marca, variante, conservazione)
 * 4. Conferma → insert/update diretto e chiusura della sheet
 */
data class AddItemUiState(
    // Step 1: Ricerca
    val searchQuery: String = "",
    val searchResults: List<CanonicalProduct> = emptyList(),
    val isSearching: Boolean = false,

    // Step 2: Prodotto selezionato + quantità/unità
    val selectedProduct: CanonicalProduct? = null,
    /** Testo grezzo del campo quantità: può essere vuoto mentre l'utente cancella. */
    val quantityText: String = "1",
    val selectedUnit: StandardUnit? = null,
    val compatibleUnits: List<StandardUnit> = emptyList(),

    // Step 3: Attributi opzionali
    val brand: String = "",
    val variant: String = "",
    val condition: String = "",

    // Validazione
    val errors: Map<String, String> = emptyMap(),
    val isEditMode: Boolean = false,

    // Story 7.1: creazione prodotto canonico user-defined (sotto-flusso dello step 1)
    val isNewProductFormVisible: Boolean = false,
    val newProductName: String = "",
    val newProductLevel1Id: Long? = null,
    val newProductLevel2Id: Long? = null,
    val newProductLevel3Id: Long? = null,
    val newProductError: String? = null,
    val duplicateCandidate: CanonicalProduct? = null,
    val isCreatingProduct: Boolean = false,
    val taxonomy: List<TaxonomyLevel> = emptyList(),

    // Story 7.2: proposta di un nuovo Livello 3 sotto un Livello 2 esistente
    val isLevel3ProposalVisible: Boolean = false,
    val newLevel3Name: String = "",
    val level3ProposalError: String? = null,
    val duplicateLevel3Candidate: TaxonomyLevel.Level3? = null,
    val isCreatingLevel3: Boolean = false,

    // Stato UI
    val isLoading: Boolean = false,
    val currentStep: Int = 1, // 1=ricerca, 2=quantità/unità, 3=attributi, 4=conferma
) {
    /** Quantità numerica derivata dal testo; 0 se vuota o non valida. */
    val quantity: Double
        get() = parseQuantityText(quantityText) ?: 0.0

    /** Prodotto selezionato valido per procedere allo step 2 */
    val canProceedToStep2: Boolean
        get() = selectedProduct != null

    /** Quantità > 0 e unità selezionata per procedere allo step 3 */
    val canProceedToStep3: Boolean
        get() = quantityText.isNotBlank() && quantity > 0 && selectedUnit != null

    /** Tutti i campi obbligatori validi per conferma */
    val isValidForConfirm: Boolean
        get() = canProceedToStep3 && selectedProduct != null
}

/** Converte il testo della quantità in numero, accettando anche la virgola. */
internal fun parseQuantityText(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/** Formatta un valore numerico per il campo quantità, senza zeri superflui. */
internal fun formatQuantityText(value: Double): String =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

/**
 * Eventi UI per AddItemViewModel.
 */
sealed interface AddItemUiEvent {
    /** Cambio query ricerca (debounced in ViewModel) */
    data class SearchQueryChanged(val query: String) : AddItemUiEvent

    /** Selezione prodotto dai risultati ricerca */
    data class ProductSelected(val product: CanonicalProduct) : AddItemUiEvent

    /** Cambio quantità (stepper/input), come testo grezzo */
    data class QuantityChanged(val quantityText: String) : AddItemUiEvent

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

    // ---- Story 7.1: creazione prodotto canonico user-defined ----

    /** Apre il form compatto "Nuovo prodotto" dallo step 1 */
    object ShowNewProductForm : AddItemUiEvent

    /** Chiude il form e torna alla ricerca */
    object HideNewProductForm : AddItemUiEvent

    /** Cambio nome prodotto nel form */
    data class NewProductNameChanged(val name: String) : AddItemUiEvent

    /** Selezione Livello 1 tra quelli esistenti */
    data class NewProductLevel1Selected(val level1Id: Long) : AddItemUiEvent

    /** Selezione Livello 2 tra quelli esistenti */
    data class NewProductLevel2Selected(val level2Id: Long) : AddItemUiEvent

    /** Selezione Livello 3 tra quelli esistenti */
    data class NewProductLevel3Selected(val level3Id: Long) : AddItemUiEvent

    /** Richiesta di creazione prodotto (può produrre un avviso duplicato) */
    object CreateNewProduct : AddItemUiEvent

    /** L'utente procede nonostante l'avviso di possibile duplicato */
    object ConfirmDuplicateProduct : AddItemUiEvent

    /** L'utente annulla l'avviso di possibile duplicato */
    object DismissDuplicateWarning : AddItemUiEvent

    // ---- Story 7.2: proposta di un nuovo Livello 3 ----

    /** Apre il pannello "Proponi nuovo Livello 3" (richiede un Livello 2 selezionato) */
    object ShowLevel3Proposal : AddItemUiEvent

    /** Chiude il pannello di proposta senza creare nulla */
    object HideLevel3Proposal : AddItemUiEvent

    /** Cambio nome del nuovo Livello 3 */
    data class NewLevel3NameChanged(val name: String) : AddItemUiEvent

    /** Richiesta di creazione del Livello 3 (può produrre un avviso duplicato) */
    object CreateNewLevel3 : AddItemUiEvent

    /** L'utente procede nonostante l'avviso di duplicato sul Livello 3 */
    object ConfirmDuplicateLevel3 : AddItemUiEvent

    /** L'utente annulla l'avviso di duplicato sul Livello 3 */
    object DismissDuplicateLevel3Warning : AddItemUiEvent
}

/** Risultato conferma: ShoppingItem pronto per insert */
data class AddItemConfirmed(
    val item: ShoppingItem
)
