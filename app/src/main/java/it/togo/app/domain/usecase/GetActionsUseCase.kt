package it.togo.app.domain.usecase

import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow

/**
 * Lettura reattiva di tutte le azioni (attive e spuntate).
 *
 * Il raggruppamento per ambito/urgenza e l'ordinamento alfabetico sono
 * responsabilità della presentazione (Story 8.10); il repository espone la
 * lista così come è persistita, come unico flusso di verità.
 */
class GetActionsUseCase(
    private val repository: ShoppingListRepository,
) {
    operator fun invoke(): Flow<List<ActionItem>> = repository.getActions()
}
