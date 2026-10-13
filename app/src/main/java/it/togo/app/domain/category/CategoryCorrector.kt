package it.togo.app.domain.category

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull

/**
 * Servizio per la correzione esplicita della categoria canonica da parte dell'utente.
 *
 * Gestisce:
 * - Applicazione correzione con validazione L3
 * - Undo (ripristino categoria originale)
 * - Generazione diff testuale per UI
 */
class CategoryCorrector(
    private val catalogRepository: CatalogRepository
) {

    /**
     * Applica una correzione utente alla categoria.
     *
     * @param assignment Assegnazione originale (da CategoryAssigner)
     * @param newLevel3Id Nuovo L3 scelto dall'utente
     * @return CategoryCorrection con diff e supporto undo
     * @throws IllegalArgumentException se newLevel3Id non valido, user-defined, o assignment.product è null
     */
    suspend fun correct(
        assignment: CategoryAssignment,
        newLevel3Id: Long
    ): CategoryCorrection {
        require(assignment.product != null) { "Assignment must have product" }
        val taxonomy = getTaxonomySnapshot()
        val newLevel3 = validateLevel3(taxonomy, newLevel3Id)
        return CategoryCorrection(
            originalLevel3Id = assignment.suggestedLevel3Id,
            correctedLevel3Id = newLevel3.id,
            product = assignment.product
        )
    }

    /**
     * Annulla una correzione (ripristina categoria originale).
     *
     * @param correction Correzione da annullare
     * @return Nuova CategoryCorrection con correctedLevel3Id = originalLevel3Id
     */
    fun undo(correction: CategoryCorrection): CategoryCorrection = correction.undo()

    /**
     * Genera stringa diff per UI.
     *
     * @param correction Oggetto correzione
     * @return Stringa formattata "Categoria suggerita: X → La tua scelta: Y"
     */
    suspend fun generateDiff(correction: CategoryCorrection): String {
        ensureActive() // Handle cancellation
        val taxonomy = getTaxonomySnapshot()
        val originalName = findLevel3InTaxonomy(taxonomy, correction.originalLevel3Id)?.name ?: "Sconosciuta"
        val correctedName = findLevel3InTaxonomy(taxonomy, correction.correctedLevel3Id)?.name ?: "Sconosciuta"
        return correction.toDiffString(originalName, correctedName)
    }

    /** Snapshot tassonomia per lookup atomici */
    private suspend fun getTaxonomySnapshot(): List<TaxonomyLevel> =
        catalogRepository.getTaxonomy().firstOrNull() ?: emptyList()

    /** Valida che L3 esista in tassonomia snapshot e non sia user-defined */
    private fun validateLevel3(taxonomy: List<TaxonomyLevel>, level3Id: Long): TaxonomyLevel.Level3 {
        val level3 = findLevel3InTaxonomy(taxonomy, level3Id)
            ?: throw IllegalArgumentException("Livello 3 non trovato: $level3Id")

        if (level3.isUserDefined) {
            throw IllegalArgumentException("Non è possibile assegnare una categoria user-defined come canonica: $level3Id")
        }
        return level3
    }

    /** Trova L3 in tassonomia snapshot */
    private fun findLevel3InTaxonomy(taxonomy: List<TaxonomyLevel>, level3Id: Long): TaxonomyLevel.Level3? =
        taxonomy.firstOrNull { it is TaxonomyLevel.Level3 && it.id == level3Id } as TaxonomyLevel.Level3?
}