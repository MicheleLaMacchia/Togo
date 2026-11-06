package it.togo.app.domain.usecase

import it.togo.app.domain.repository.ShoppingListRepository

/**
 * Spunta o deseleziona un'azione (CAP-3).
 *
 * La spunta non elimina l'azione: la riga resta persistita e si sposta nella
 * sezione Fatte lato presentazione. La scrittura è atomica, su thread IO.
 */
class CheckOffActionUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(actionId: String, checked: Boolean) {
        if (actionId.isBlank()) return
        repository.checkOffAction(actionId.trim(), checked)
    }
}
