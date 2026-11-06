package it.togo.app.domain.usecase

import it.togo.app.domain.action.ActionMutationResult
import it.togo.app.domain.action.ActionTextValidator
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.repository.ShoppingListRepository

/**
 * Aggiunge una nuova azione (CAP-2).
 *
 * Valori iniziali: ambito [ActionScope.ALTRO], urgenza
 * [ActionUrgency.A_TEMPO_PERSO] e stato non spuntato. La scrittura è delegata
 * al repository, che la esegue in modo atomico su thread IO. Se testo, ambito
 * e urgenza coincidono con un'azione esistente — anche spuntata — l'azione
 * non viene aggiunta e viene restituito [ActionMutationResult.Duplicate].
 */
class AddActionUseCase(
    private val repository: ShoppingListRepository,
    private val checkDuplicateAction: CheckDuplicateActionUseCase,
) {
    suspend operator fun invoke(
        text: String,
        scope: ActionScope = ActionScope.ALTRO,
        urgency: ActionUrgency = ActionUrgency.A_TEMPO_PERSO,
    ): ActionMutationResult {
        ActionTextValidator.validate(text)?.let { message ->
            return ActionMutationResult.Invalid(message)
        }

        if (checkDuplicateAction(text, scope, urgency)) {
            return ActionMutationResult.Duplicate
        }

        val now = System.currentTimeMillis()
        val action = ActionItem(
            text = ActionTextValidator.prepare(text),
            scope = scope,
            urgency = urgency,
            isChecked = false,
            createdAt = now,
            updatedAt = now,
        )

        repository.addAction(action)
        return ActionMutationResult.Success(action)
    }
}
