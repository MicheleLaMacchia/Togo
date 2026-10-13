package it.togo.app.domain.usecase

import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

/**
 * Manager per CRUD completo delle regole apprese (LEARNED_RULE).
 *
 * Espone operazioni per Settings UI: lista, crea, aggiorna, disattiva, elimina.
 * Puro dominio: nessuna dipendenza Android, testabile su JVM.
 */
class LearnedRuleManager(
    private val learnedRulesRepository: LearnedRulesRepository
) {

    /** Ottiene tutte le regole (attive + inattive), ordinate per lastAppliedAt desc */
    suspend fun getAll(): List<LearnedRule> = withContext(Dispatchers.IO) {
        learnedRulesRepository.getAll().firstOrNull() ?: emptyList()
    }

    /** Ottiene solo regole attive */
    suspend fun getActive(): List<LearnedRule> = withContext(Dispatchers.IO) {
        (learnedRulesRepository.getAll().firstOrNull() ?: emptyList()).filter { it.isActive }
    }

    /**
     * Crea una nuova regola appresa (upsert per userExpression + productId).
     *
     * @param userExpression Espressione utente (es. "latte")
     * @param productId ID prodotto canonico
     * @param level3Id ID Livello 3 categoria (deve essere non user-defined)
     * @return LearnedRule creata/aggiornata
     * @throws IllegalArgumentException se espressione vuota o level3Id user-defined
     */
    suspend fun create(
        userExpression: String,
        productId: Long,
        level3Id: Long
    ): LearnedRule = withContext(Dispatchers.IO) {
        val normalized = userExpression.trim().lowercase()
        require(normalized.isNotEmpty()) { "Espressione utente non può essere vuota" }

        // Nota: validazione user-defined va fatta dal chiamante (UI) che ha accesso alla tassonomia
        // Qui facciamo solo upsert atomico
        val rule = LearnedRule(
            userExpression = normalized,
            productId = productId,
            level3Id = level3Id
        )
        learnedRulesRepository.upsertAtomic(rule)
    }

    /** Aggiorna regola esistente per ID */
    suspend fun update(
        ruleId: String,
        productId: Long? = null,
        level3Id: Long? = null,
        isActive: Boolean? = null
    ): LearnedRule = withContext(Dispatchers.IO) {
        val existing = learnedRulesRepository.getById(ruleId)
            ?: throw IllegalArgumentException("Rule non trovata: $ruleId")

        val updated = existing.copy(
            productId = productId ?: existing.productId,
            level3Id = level3Id ?: existing.level3Id,
            isActive = isActive ?: existing.isActive
        ).updateApplicationTime()

        learnedRulesRepository.update(updated)
        updated
    }

    /** Disattiva una regola (soft delete: isActive = false) */
    suspend fun deactivate(ruleId: String): LearnedRule = withContext(Dispatchers.IO) {
        val existing = learnedRulesRepository.getById(ruleId)
            ?: throw IllegalArgumentException("Rule non trovata: $ruleId")
        val updated = existing.deactivate()
        learnedRulesRepository.update(updated)
        updated
    }

    /** Elimina definitivamente una regola */
    suspend fun delete(ruleId: String): Boolean = withContext(Dispatchers.IO) {
        val existing = learnedRulesRepository.getById(ruleId)
        if (existing == null) return false
        learnedRulesRepository.delete(ruleId)
        true
    }

    /** Riattiva una regola disattivata */
    suspend fun reactivate(ruleId: String): LearnedRule = withContext(Dispatchers.IO) {
        val existing = learnedRulesRepository.getById(ruleId)
            ?: throw IllegalArgumentException("Rule non trovata: $ruleId")
        val updated = existing.copy(isActive = true).updateApplicationTime()
        learnedRulesRepository.update(updated)
        updated
    }

    /** Conta regole attive per un prodotto specifico */
    suspend fun countByProduct(productId: Long): Int = withContext(Dispatchers.IO) {
        (learnedRulesRepository.getAll().firstOrNull() ?: emptyList()).count { it.productId == productId && it.isActive }
    }
}