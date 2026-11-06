package it.togo.app.domain.usecase

import it.togo.app.domain.action.ActionTextNormalizer
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.repository.ShoppingListRepository

/**
 * Verifica se esiste già un'azione con la stessa chiave di deduplica:
 * testo normalizzato + ambito + urgenza.
 *
 * La normalizzazione vive nel domain e preserva gli accenti. Le azioni
 * spuntate contano come presenti. [excludeActionId] esclude l'azione che si
 * sta modificando, che non è duplicata di sé stessa.
 */
class CheckDuplicateActionUseCase(
    private val repository: ShoppingListRepository,
) {
    suspend operator fun invoke(
        text: String,
        scope: ActionScope,
        urgency: ActionUrgency,
        excludeActionId: String? = null,
    ): Boolean {
        val normalizedText = ActionTextNormalizer.normalize(text)
        if (normalizedText.isEmpty()) return false
        return repository.checkDuplicateAction(
            normalizedText = normalizedText,
            scope = scope,
            urgency = urgency,
            excludeActionId = excludeActionId,
        )
    }
}
