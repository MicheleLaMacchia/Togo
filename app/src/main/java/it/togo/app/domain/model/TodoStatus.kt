package it.togo.app.domain.model

/**
 * Stato di una Task.
 *
 * `COMPLETED` è terminale rispetto all'eliminazione in v1: le transizioni
 * complete/reopen/delete/restore verranno gestite dal dominio nella Story 8.6.
 */
enum class TodoStatus {
    OPEN,
    COMPLETED,
    DELETED,
}
