package it.togo.app.presentation.additem

import it.togo.app.domain.category.CategoryAssignment
import it.togo.app.domain.category.CategoryCorrection
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

    // Step 3: Attributi (marca e variante sempre presenti, modificabili)
    val brand: String = "",
    val variant: String = "",
    val condition: String = "",
    /** Marche già usate dall'utente, proposte nel campo marca. */
    val brandSuggestions: List<String> = emptyList(),
    /** Varianti già usate dall'utente, proposte nel campo variante. */
    val variantSuggestions: List<String> = emptyList(),

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

    // Story 3.6: correzione categoria + regola appresa
    val isCategoryCorrectionVisible: Boolean = false,
    val categoryCorrectionLevel1Id: Long? = null,
    val categoryCorrectionLevel2Id: Long? = null,
    val categoryCorrectionLevel3Id: Long? = null,
    val categoryCorrectionExpression: String = "",
    val categoryCorrectionError: String? = null,
    /** Correzione valida in attesa della scelta "Ricorda questa scelta?". */
    val pendingCategoryCorrection: CategoryCorrection? = null,
    val pendingCategoryAssignment: CategoryAssignment? = null,
    val isSavingCategoryRule: Boolean = false,
    /** Percorso della categoria corretta, mostrato nel riepilogo. */
    val correctedCategoryPath: String? = null,
    val categoryCorrectionMessage: String? = null,

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

    /** Categoria mostrata nel riepilogo: corretta se presente, altrimenti quella del prodotto. */
    val selectedCategoryPath: String
        get() = correctedCategoryPath
            ?: selectedProduct?.let { product ->
                listOfNotNull(product.level1Name, product.level2Name, product.level3Name)
                    .joinToString(" > ")
                    .ifBlank { "Categoria #${product.level3Id}" }
            }
            ?: ""
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

    // ---- Story 3.6: correzione categoria e regola appresa ----

    /** Apre il dialog "Correggi categoria" per il prodotto selezionato. */
    object ShowCategoryCorrection : AddItemUiEvent

    /** Chiude il dialog senza applicare la correzione. */
    object HideCategoryCorrection : AddItemUiEvent

    data class CategoryCorrectionLevel1Selected(val level1Id: Long) : AddItemUiEvent

    data class CategoryCorrectionLevel2Selected(val level2Id: Long) : AddItemUiEvent

    data class CategoryCorrectionLevel3Selected(val level3Id: Long) : AddItemUiEvent

    data class CategoryCorrectionExpressionChanged(val expression: String) : AddItemUiEvent

    /** Valida la correzione e apre la richiesta "Ricorda questa scelta?". */
    object ApplyCategoryCorrection : AddItemUiEvent

    /** Crea la regola appresa per espressione + prodotto + categoria. */
    object ConfirmRememberCategory : AddItemUiEvent

    /** Chiude il prompt senza creare una regola. */
    object DismissRememberCategory : AddItemUiEvent
}

/** Risultato conferma: ShoppingItem pronto per insert */
data class AddItemConfirmed(
    val item: ShoppingItem
)
