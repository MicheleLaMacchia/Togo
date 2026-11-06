package it.togo.app.domain.usecase

import it.togo.app.domain.repository.ShoppingListRepository

/** Elimina una singola azione (CAP-6). La scrittura è atomica, su thread IO. */
class DeleteActionUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(actionId: String) {
        if (actionId.isBlank()) return
        repository.deleteAction(actionId.trim())
    }
}
