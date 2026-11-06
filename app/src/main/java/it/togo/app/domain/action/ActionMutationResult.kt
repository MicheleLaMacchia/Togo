package it.togo.app.domain.action

import it.togo.app.domain.model.ActionItem

/**
 * Esito di una mutazione su un'azione.
 *
 * La presentazione usa [Duplicate] per mostrare "Azione già presente" e
 * [Invalid] per il messaggio di validazione, senza duplicare la regola nel
 * layer UI.
 */
sealed interface ActionMutationResult {

    data class Success(val action: ActionItem) : ActionMutationResult

    data object Duplicate : ActionMutationResult

    data class Invalid(val message: String) : ActionMutationResult

    /** Le azioni nella sezione Fatte non sono modificabili (CAP-6). */
    data object NotEditable : ActionMutationResult
}
