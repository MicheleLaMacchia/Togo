package it.togo.app.domain.wizard

import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * Servizio puro per navigazione tassonomia nel wizard L1 → L2 → L3.
 *
 * Stateless: non mantiene stato, solo query su tassonomia.
 * Usa snapshot `firstOrNull()` per robustezza (no Flow cold hangs).
 */
class CategorySelectionWizard(
    private val catalogRepository: CatalogRepository
) {

    /** Recupera tutti i Livello 1 ordinati per sortOrder */
    suspend fun getLevel1Options(): List<TaxonomyLevel.Level1> =
        getTaxonomySnapshot()
            .filter { it is TaxonomyLevel.Level1 }
            .map { it as TaxonomyLevel.Level1 }
            .sortedBy { it.sortOrder }

    /** Recupera Livello 2 figli di un Level1 ordinati per sortOrder */
    suspend fun getLevel2Options(level1Id: Long): List<TaxonomyLevel.Level2> =
        getTaxonomySnapshot()
            .filter { it is TaxonomyLevel.Level2 && (it as TaxonomyLevel.Level2).level1Id == level1Id }
            .map { it as TaxonomyLevel.Level2 }
            .sortedBy { it.sortOrder }

    /** Recupera Livello 3 figli di un Level2 (include user-defined per "Nuovo...") ordinati per sortOrder */
    suspend fun getLevel3Options(level2Id: Long, includeUserDefined: Boolean = true): List<TaxonomyLevel.Level3> =
        getTaxonomySnapshot()
            .filter { it is TaxonomyLevel.Level3 && (it as TaxonomyLevel.Level3).level2Id == level2Id }
            .map { it as TaxonomyLevel.Level3 }
            .filter { includeUserDefined || !it.isUserDefined }
            .sortedBy { it.sortOrder }

    /** Crea opzione "Nuovo L3" per UI (non persistito, solo marker) */
    fun createNewLevel3Option(level2Id: Long): TaxonomyLevel.Level3 =
        TaxonomyLevel.Level3(
            id = -1L, // Marker per "nuovo"
            name = "+ Nuovo...",
            sortOrder = Int.MAX_VALUE, // Sempre ultimo
            level2Id = level2Id,
            isUserDefined = true
        )

    /** Verifica se un nome L3 esiste già sotto un Level2 (per validazione duplicati) */
    suspend fun level3NameExists(level2Id: Long, name: String): Boolean {
        val trimmedName = name.trim().lowercase()
        if (trimmedName.isEmpty()) return false
        return getTaxonomySnapshot()
            .any { it is TaxonomyLevel.Level3 && it.level2Id == level2Id && it.name.trim().lowercase() == trimmedName }
    }

    /** Snapshot tassonomia robusto (firstOrNull + fallback vuoto) */
    private suspend fun getTaxonomySnapshot(): List<TaxonomyLevel> =
        catalogRepository.getTaxonomy().firstOrNull() ?: emptyList()
}