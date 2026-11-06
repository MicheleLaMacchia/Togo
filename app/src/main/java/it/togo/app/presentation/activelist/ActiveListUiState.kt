package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel

/**
 * Pagina mostrata dal pager orizzontale della Lista Attiva (AD-8).
 *
 * L'app parte sempre da [SHOPPING] a freddo. L'ordine degli elementi è anche
 * l'ordine delle pagine: Spesa a sinistra, Da fare a destra.
 */
enum class ActiveListPage {
    SHOPPING,
    ACTIONS;

    /** L'altra pagina rispetto a questa. */
    fun other(): ActiveListPage = if (this == SHOPPING) ACTIONS else SHOPPING

    /** Nome leggibile usato nell'intestazione e dai lettori di schermo. */
    val label: String
        get() = when (this) {
            SHOPPING -> "Spesa"
            ACTIONS -> "Da fare"
        }
}

/**
 * UI State per la schermata Lista Attiva (MVI pattern).
 *
 * - [activeGroups]: gruppi di voci per categoria L1→L2→L3 ordinati per sortOrder
 * - [checkedItems]: voci spuntate (per sezione "Presi")
 * - [actions]: azioni non spuntate della pagina "Da fare"
 * - [checkedActions]: azioni spuntate (sezione "Fatte", resa in una storia successiva)
 * - [currentPage]: pagina corrente del pager (AD-8)
 * - [error]: messaggio errore se repository fallisce
 */
data class ActiveListUiState(
    val activeGroups: List<CategoryGroup> = emptyList(),
    val checkedItems: List<ShoppingItem> = emptyList(),
    val productNames: Map<Long, String> = emptyMap(),
    /**
     * Etichetta di categoria canonica (L1, L2, L3) per ogni prodotto.
     *
     * Necessaria anche per le voci spuntate in "Presi", dove la riga non passa
     * per [CategoryGroup] e quindi non conosce la tassonomia.
     */
    val categoryLabels: Map<Long, String> = emptyMap(),
    /** Azioni non spuntate: alimentano il conteggio del titolo "Da fare (N)". */
    val actions: List<ActionItem> = emptyList(),
    /** Azioni spuntate: non contribuiscono al conteggio della pagina. */
    val checkedActions: List<ActionItem> = emptyList(),
    /** Pagina corrente del pager. A freddo è sempre [ActiveListPage.SHOPPING]. */
    val currentPage: ActiveListPage = ActiveListPage.SHOPPING,
    val error: String? = null,
    val checkingOffItemId: String? = null,      // Item in corso di animazione check-off
    val pendingCheckOffItemId: String? = null,  // Item in attesa di check-off (300ms delay)
    val checkoutSnackbarMessage: String? = null, // Messaggio snackbar per checkout undo
    val checkedItemsForUndo: List<ShoppingItem> = emptyList(), // Item per undo checkout
    /**
     * Messaggio dello snackbar undo dopo l'eliminazione di una singola azione
     * (Story 8.8). Mostra "Annulla" per almeno 10 s.
     */
    val actionDeleteUndoMessage: String? = null,
    /**
     * Contatore monotono usato per ri-mostrare lo snackbar undo dopo ogni
     * eliminazione, così una nuova eliminazione rimpiazza il precedente.
     */
    val actionDeleteUndoCounter: Int = 0,
) {
    /** Totale prodotti attivi (non spuntati) mostrato nel titolo "Spesa (N)". */
    val activeCount: Int = activeGroups.sumOf { it.items.size }

    /** Totale azioni attive (non spuntate) mostrato nel titolo "Da fare (N)". */
    val activeActionsCount: Int = actions.size

    /**
     * Azioni attive pronte per la pagina "Da fare": raggruppate per ambito
     * nell'ordine fisso e ordinate per urgenza, poi alfabeticamente.
     */
    val actionGroups: List<ActionGroup>
        get() = groupActionsByScope(actions)

    /** Sezione "Presi" visibile solo se ci sono elementi checkati */
    val hasCheckedItems: Boolean = checkedItems.isNotEmpty()

    /** Se c'è un'animazione in corso */
    val isAnimating: Boolean = checkingOffItemId != null || pendingCheckOffItemId != null
}

/**
 * Gruppo di voci per categoria canonica Level1.
 *
 * Contiene gruppi Level2, che a loro volta contengono gruppi Level3 con le voci.
 */
data class CategoryGroup(
    val level1: TaxonomyLevel.Level1,
    val level2Groups: List<Level2Group>
) {
    /** Tutte le voci di questo gruppo (flattened per conteggio) */
    val items: List<ShoppingItem> = level2Groups.flatMap { it.items }
}

/** Sottogruppo per Level2 con i suoi gruppi Level3. */
data class Level2Group(
    val level2: TaxonomyLevel.Level2,
    val level3Groups: List<Level3Group>
) {
    /** Tutte le voci di questo gruppo (flattened per conteggio) */
    val items: List<ShoppingItem> = level3Groups.flatMap { it.items }
}

/** Sottogruppo per Level3 con le voci. */
data class Level3Group(
    val level3: TaxonomyLevel.Level3,
    val items: List<ShoppingItem>
)

/**
 * Eventi UI emessi dalla schermata verso il ViewModel.
 *
 * Sigillata per exhaustiveness checking in when().
 */
sealed interface UiEvent {
    /** Spunta/ripristina una voce */
    data class CheckOff(val itemId: String, val isChecked: Boolean) : UiEvent

    /** Testo confermato dalla cattura vocale. */
    data class VoiceCommand(val text: String) : UiEvent

    /** Elimina voce (swipe) */
    data class DeleteItem(val itemId: String) : UiEvent

    /** Naviga a cattura vocale (FAB microfono) */
    object NavigateToVoice : UiEvent

    /** Naviga a Storico (icona AppBar) */
    object NavigateToHistory : UiEvent

    /** Apri le Regole apprese (pulsante Impostazioni nell'App Bar) */
    object NavigateToSettings : UiEvent

    /** Condividi lista (icona AppBar) */
    object ShareList : UiEvent

    /** Concludi spesa (pulsante in sezione Presi) */
    object Checkout : UiEvent

    /** Aggiungi voce manuale (pulsante stato vuoto) */
    object AddItemManual : UiEvent

    /** Annulla eliminazione (Snackbar undo) */
    object UndoDelete : UiEvent

    /** Elimina azione (swipe cestino o azione accessibile "Elimina"). */
    data class DeleteAction(val actionId: String) : UiEvent

    /** Annulla eliminazione azione (Snackbar undo). */
    object UndoDeleteAction : UiEvent

    /** Annulla checkout (Snackbar undo) */
    object UndoCheckout : UiEvent

    /**
     * Cambia la pagina del pager (frecce, swipe sull'intestazione o back da
     * "Da fare"). Lo stato vive nel ViewModel per sopravvivere a rotazione e
     * navigazione interna (AD-8).
     */
    data class SelectPage(val page: ActiveListPage) : UiEvent
}
