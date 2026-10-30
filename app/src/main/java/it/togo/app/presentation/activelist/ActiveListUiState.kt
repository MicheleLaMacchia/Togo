package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.TaxonomyLevel

/**
 * UI State per la schermata Lista Attiva (MVI pattern).
 *
 * - [activeGroups]: gruppi di voci `TO_BUY` per categoria L1→L2→L3 ordinati
 *   per sortOrder
 * - [error]: messaggio errore se repository fallisce
 *
 * Dalla Story 8.7 non esistono più "Presi", checkout e undo checkout: la
 * spunta porta la riga direttamente a `PURCHASED` e la riga sparisce dagli
 * attivi perché `observeActive()` non la emette più.
 */
data class ActiveListUiState(
    val activeGroups: List<CategoryGroup> = emptyList(),
    val productNames: Map<Long, String> = emptyMap(),
    /**
     * Etichetta di categoria canonica (L1, L2, L3) per ogni prodotto.
     */
    val categoryLabels: Map<Long, String> = emptyMap(),
    val error: String? = null,
    val checkingOffItemId: String? = null,      // Item in corso di animazione check-off
    val pendingCheckOffItemId: String? = null,  // Item in attesa di check-off (300ms delay)
) {
    /** Totale voci attive */
    val activeCount: Int = activeGroups.sumOf { it.items.size }

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
    val items: List<ListItem> = level2Groups.flatMap { it.items }
}

/** Sottogruppo per Level2 con i suoi gruppi Level3. */
data class Level2Group(
    val level2: TaxonomyLevel.Level2,
    val level3Groups: List<Level3Group>
) {
    /** Tutte le voci di questo gruppo (flattened per conteggio) */
    val items: List<ListItem> = level3Groups.flatMap { it.items }
}

/** Sottogruppo per Level3 con le voci. */
data class Level3Group(
    val level3: TaxonomyLevel.Level3,
    val items: List<ListItem>
)

/**
 * Eventi UI emessi dalla schermata verso il ViewModel.
 *
 * Sigillata per exhaustiveness checking in when().
 */
sealed interface UiEvent {
    /** Spunta una voce: `isChecked = false` è un no-op (non c'è più "Presi"). */
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

    /** Aggiungi voce manuale (pulsante stato vuoto) */
    object AddItemManual : UiEvent

    /** Annulla eliminazione (Snackbar undo) */
    object UndoDelete : UiEvent
}
