package it.togo.app.domain.usecase

import it.togo.app.domain.action.ActionMutationResult
import it.togo.app.domain.action.ActionTextValidator
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.repository.ShoppingListRepository

/**
 * Modifica testo, ambito e urgenza di un'azione attiva (CAP-6).
 *
 * Le azioni in Fatte non sono modificabili. Il controllo di deduplica esclude
 * l'azione stessa, quindi salvare senza cambiare la chiave non è un
 * duplicato. La scrittura è delegata al repository (atomica, thread IO).
 */
class UpdateActionUseCase(
    private val repository: ShoppingListRepository,
    private val checkDuplicateAction: CheckDuplicateActionUseCase,
) {
    suspend operator fun invoke(
        action: ActionItem,
        text: String,
        scope: ActionScope,
        urgency: ActionUrgency,
    ): ActionMutationResult {
        if (action.isChecked) {
            return ActionMutationResult.NotEditable
        }

        ActionTextValidator.validate(text)?.let { message ->
            return ActionMutationResult.Invalid(message)
        }

        if (checkDuplicateAction(text, scope, urgency, excludeActionId = action.id)) {
            return ActionMutationResult.Duplicate
        }

        val updated = action.copy(
            text = ActionTextValidator.prepare(text),
            scope = scope,
            urgency = urgency,
            updatedAt = System.currentTimeMillis(),
        )

        repository.updateAction(updated)
        return ActionMutationResult.Success(updated)
    }
}
