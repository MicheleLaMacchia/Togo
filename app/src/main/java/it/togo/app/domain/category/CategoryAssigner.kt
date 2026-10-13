package it.togo.app.domain.category

import it.togo.app.data.database.CatalogQueries
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.flow.firstOrNull
import android.util.Log

/**
 * Servizio puro per l'assegnazione della categoria canonica a un prodotto.
 *
 * Priorità di assegnazione:
 * 1. **Learned Rule attiva** per l'espressione utente normalizzata + productId (confidence=0.9)
 * 2. **Default del prodotto** (CanonicalProduct.level3Id) (confidence=1.0)
 * 3. **Fallback** primo L3 non user-defined della tassonomia (confidence=0.5)
 *
 * Non modifica stato persistente; ritorna solo [CategoryAssignment].
 */
class CategoryAssigner(
    private val catalogRepository: CatalogRepository,
    private val learnedRulesRepository: LearnedRulesRepository
) {

    companion object {
        private const val TAG = "CategoryAssigner"
    }

    /**
     * Assegna la categoria canonica al prodotto risolto.
     *
     * @param resolvedProduct Prodotto canonico risolto (da ProductResolver)
     * @param userExpression Espressione libera dell'utente (es. "latte", "caffè macinato"); se null/blank, salta lookup rule
     * @return CategoryAssignment con categoria suggerita, confidence e origine
     */
    suspend fun assign(
        resolvedProduct: CanonicalProduct,
        userExpression: String? = null
    ): CategoryAssignment {
        val productLevel3Id = resolvedProduct.level3Id
        val normalizedExpression = userExpression?.let { normalizeSafely(it) }

        // Cache tassonomia per chiamata (evita multiple emissioni Flow)
        val taxonomy = getTaxonomySnapshot()

        // 1. Prova regola appresa per l'espressione normalizzata + productId (scoping per prodotto)
        if (normalizedExpression != null && normalizedExpression.isNotEmpty()) {
            val learnedRule = learnedRulesRepository.getByExpressionAndProductId(normalizedExpression, resolvedProduct.id)
            if (learnedRule != null && learnedRule.isActive) {
                // Verifica che la rule punti a un L3 valido e non user-defined
                val ruleLevel3 = findLevel3InTaxonomy(taxonomy, learnedRule.level3Id)
                if (ruleLevel3 != null && !ruleLevel3.isUserDefined) {
                    val alternatives = buildAlternatives(taxonomy, productLevel3Id, learnedRule.level3Id)
                    return CategoryAssignment(
                        suggestedLevel3Id = learnedRule.level3Id,
                        confidence = 0.9,
                        source = AssignmentSource.LEARNED,
                        product = resolvedProduct,
                        alternatives = alternatives,
                        userExpression = normalizedExpression
                    )
                } else if (ruleLevel3 != null && ruleLevel3.isUserDefined) {
                    Log.w(TAG, "Learned rule '$normalizedExpression' for product ${resolvedProduct.id} points to user-defined L3 ${learnedRule.level3Id}, ignoring")
                }
            }
        }

        // 2. Default del prodotto (se valido e non user-defined)
        val productLevel3 = findLevel3InTaxonomy(taxonomy, productLevel3Id)
        if (productLevel3 != null && !productLevel3.isUserDefined) {
            val alternatives = if (normalizedExpression != null) {
                buildAlternatives(taxonomy, productLevel3Id, productLevel3Id)
            } else {
                emptyList()
            }
            return CategoryAssignment(
                suggestedLevel3Id = productLevel3Id,
                confidence = 1.0,
                source = AssignmentSource.DEFAULT,
                product = resolvedProduct,
                alternatives = alternatives,
                userExpression = normalizedExpression
            )
        }

        // 3. Fallback: primo L3 non user-defined disponibile
        val fallbackLevel3 = findFirstValidLevel3(taxonomy)
        return CategoryAssignment(
            suggestedLevel3Id = fallbackLevel3?.id ?: productLevel3Id,
            confidence = 0.5,
            source = AssignmentSource.FALLBACK,
            product = resolvedProduct,
            alternatives = buildFallbackAlternatives(taxonomy, fallbackLevel3?.id ?: productLevel3Id),
            userExpression = normalizedExpression
        )
    }

    /** Normalizza in modo sicuro, ritorna null se fallisce */
    private fun normalizeSafely(expression: String): String? = try {
        val normalized = CatalogQueries.normalize(expression)
        if (normalized.isEmpty()) null else normalized
    } catch (e: Exception) {
        Log.w(TAG, "Normalization failed for '$expression': ${e.message}")
        null
    }

    /** Snapshot tassonomia: firstOrNull con fallback a lista vuota */
    private suspend fun getTaxonomySnapshot(): List<TaxonomyLevel> =
        catalogRepository.getTaxonomy().firstOrNull() ?: emptyList()

    /** Trova L3 in tassonomia snapshot */
    private fun findLevel3InTaxonomy(taxonomy: List<TaxonomyLevel>, level3Id: Long): TaxonomyLevel.Level3? =
        taxonomy.firstOrNull { it is TaxonomyLevel.Level3 && it.id == level3Id } as TaxonomyLevel.Level3?

    /** Trova primo L3 valido in tassonomia snapshot */
    private fun findFirstValidLevel3(taxonomy: List<TaxonomyLevel>): TaxonomyLevel.Level3? =
        taxonomy.firstOrNull { it is TaxonomyLevel.Level3 && !it.isUserDefined } as TaxonomyLevel.Level3?

    /** Costruisce alternative: default + rule + sibling L3s nello stesso L2 */
    private fun buildAlternatives(
        taxonomy: List<TaxonomyLevel>,
        defaultLevel3Id: Long,
        suggestedLevel3Id: Long
    ): List<TaxonomyLevel.Level3> {
        if (defaultLevel3Id == suggestedLevel3Id) return buildSiblingAlternatives(taxonomy, defaultLevel3Id)

        val result = mutableListOf<TaxonomyLevel.Level3>()

        // Aggiungi default e rule
        listOf(defaultLevel3Id, suggestedLevel3Id).distinct().forEach { id ->
            findLevel3InTaxonomy(taxonomy, id)?.let { if (!it.isUserDefined) result.add(it) }
        }

        // Aggiungi sibling L3s (stesso L2) per scelta informata
        result.forEach { l3 ->
            val siblings = getSiblingLevel3s(taxonomy, l3.level2Id)
            siblings.forEach { if (it.id !in result.map { it.id }) result.add(it) }
        }

        return result
    }

    /** Alternative per fallback: tutti i L3 validi nello stesso L2 del fallback */
    private fun buildFallbackAlternatives(taxonomy: List<TaxonomyLevel>, fallbackLevel3Id: Long): List<TaxonomyLevel.Level3> {
        findLevel3InTaxonomy(taxonomy, fallbackLevel3Id)?.let { fallback ->
            return getSiblingLevel3s(taxonomy, fallback.level2Id)
        }
        return emptyList()
    }

    /** Recupera tutti i L3 non user-defined nello stesso L2 */
    private fun getSiblingLevel3s(taxonomy: List<TaxonomyLevel>, level2Id: Long): List<TaxonomyLevel.Level3> {
        return taxonomy
            .filter { it is TaxonomyLevel.Level3 && it.level2Id == level2Id && !it.isUserDefined }
            .map { it as TaxonomyLevel.Level3 }
            .sortedBy { it.sortOrder }
    }
}