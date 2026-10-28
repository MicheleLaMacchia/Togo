package it.togo.app.domain.model

/**
 * Stato esplicito di una riga della lista unificata.
 *
 * `PURCHASED` è terminale: una riga acquistata non può essere riaperta né
 * eliminata. Le uniche transizioni ammesse sono:
 * `TO_BUY -> PURCHASED`, `TO_BUY -> DELETED`, `DELETED -> TO_BUY`.
 */
enum class ListItemStatus {

    TO_BUY,
    PURCHASED,
    DELETED;

    fun canTransitionTo(target: ListItemStatus): Boolean = when (this) {
        TO_BUY -> target == PURCHASED || target == DELETED
        DELETED -> target == TO_BUY
        PURCHASED -> false
    }
}
