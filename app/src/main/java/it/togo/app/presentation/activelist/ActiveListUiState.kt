package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import kotlinx.coroutines.flow.Flow

/**
 * UI State per la schermata Lista Attiva (MVI pattern).
 *
 * - [activeGroups]: gruppi di voci per categoria L1→L2→L3 ordinati per sortOrder
 * - [checkedItems]: voci spuntate (per sezione "Presi")
 * - [error]: messaggio errore se repository fallisce
 */
data class ActiveListUiState(
    val activeGroups: List<CategoryGroup> = emptyList(),
    val checkedItems: List<ShoppingItem> = emptyList(),
    val error: String? = null
) {
    /** Totale voci attive (non checkate) */
    val activeCount: Int = activeGroups.sumOf { it.items.size }

    /** Sezione "Presi" visibile solo se ci sono elementi checkati */
    val hasCheckedItems: Boolean = checkedItems.isNotEmpty()
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

    /** Modifica voce (tap su riga) */
    data class EditItem(val item: ShoppingItem) : UiEvent

    /** Elimina voce (swipe) */
    data class DeleteItem(val itemId: String) : UiEvent

    /** Naviga a cattura vocale (FAB microfono) */
    object NavigateToVoice : UiEvent

    /** Naviga a Storico (icona AppBar) */
    object NavigateToHistory : UiEvent

    /** Condividi lista (icona AppBar) */
    object ShareList : UiEvent

    /** Concludi spesa (pulsante in sezione Presi) */
    object Checkout : UiEvent

    /** Aggiungi voce manuale (pulsante stato vuoto) */
    object AddItemManual : UiEvent
}