package it.togo.app.domain.usecase

import it.togo.app.domain.category.CategoryAssignment
import it.togo.app.domain.category.CategoryCorrection
import it.togo.app.domain.category.CategoryCorrector
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.text.normalizeExpression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Use Case per la correzione esplicita della categoria canonica da parte dell'utente.
 *
 * Gestisce:
 * - Applicazione correzione con validazione
 * - Undo (ripristino)
 * - Prompt "Ricorda questa scelta?" → creazione/aggiornamento LearnedRule solo su conferma esplicita
 */
class CorrectCategoryUseCase(
    private val categoryCorrector: CategoryCorrector,
    private val learnedRulesRepository: LearnedRulesRepository
) {

    /**
     * Applica una correzione utente.
     *
     * @param assignment Assegnazione originale
     * @param newLevel3Id Nuovo L3 scelto dall'utente
     * @return CategoryCorrection con diff e supporto undo
     */
    suspend operator fun invoke(
        assignment: CategoryAssignment,
        newLevel3Id: Long
    ): CategoryCorrection = withContext(Dispatchers.IO) {
        categoryCorrector.correct(assignment, newLevel3Id)
    }

    /**
     * Annulla una correzione (ripristina categoria suggerita).
     *
     * @param correction Correzione da annullare
     * @return CategoryCorrection ripristinata
     */
    fun undo(correction: CategoryCorrection): CategoryCorrection =
        categoryCorrector.undo(correction)

    /**
     * Genera diff testuale per UI.
     *
     * @param correction Correzione applicata
     * @return Stringa "Categoria suggerita: X → La tua scelta: Y"
     */
    suspend fun generateDiff(correction: CategoryCorrection): String = withContext(Dispatchers.IO) {
        categoryCorrector.generateDiff(correction)
    }

    /**
     * Verifica se è possibile mostrare il prompt "Ricorda".
     *
     * @param assignment Assegnazione originale
     * @return true se userExpression presente e correzione effettiva
     */
    fun canShowRememberPrompt(assignment: CategoryAssignment, correction: CategoryCorrection): Boolean =
        assignment.userExpression != null && correction.isEffective

    /**
     * Conferma "Ricorda questa scelta" → crea/aggiorna LearnedRule in modo atomico.
     *
     * Chiamata SOLO dopo conferma esplicita utente (non automatica).
     *
     * @param assignment Assegnazione originale (contiene userExpression)
     * @param correction Correzione confermata dall'utente
     * @return LearnedRule creata/aggiornata
     * @throws IllegalStateException se userExpression mancante o productId mismatch
     */
    suspend fun confirmRemember(
        assignment: CategoryAssignment,
        correction: CategoryCorrection
    ): LearnedRule = withContext(Dispatchers.IO) {
        val expression = assignment.userExpression
            ?.let(::normalizeExpression)
            ?: throw IllegalStateException("userExpression mancante per creazione rule")

        // Atomic upsert via repository (handles race conditions)
        val rule = LearnedRule(
            userExpression = expression,
            productId = correction.product.id,
            level3Id = correction.correctedLevel3Id
        )
        learnedRulesRepository.upsertAtomic(rule)
    }

    /**
     * Rifiuta "Ricorda questa scelta" → nessuna rule creata.
     *
     * La correzione rimane valida solo per l'inserimento corrente.
     * Usare questo metodo quando l'utente tap "No, solo questa volta".
     *
     * @return true per confermare che l'operazione è stata processata
     */
    fun declineRemember(): Boolean = true
}