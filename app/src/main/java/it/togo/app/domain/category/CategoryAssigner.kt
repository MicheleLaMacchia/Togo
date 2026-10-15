package it.togo.app.domain.category

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Assegna la categoria canonica a un prodotto risolto.
 *
 * Priorità:
 * 1. Regola appresa attiva per (espressione, prodotto) → confidence 0.9
 * 2. Default del prodotto (level3Id) → confidence 1.0
 * 3. Fallback primo L3 valido → confidence 0.5
 */
class CategoryAssigner(
    private val catalogRepository: CatalogRepository,
    private val learnedRulesRepository: LearnedRulesRepository
) {

    suspend fun assign(
        resolvedProduct: CanonicalProduct,
        userExpression: String? = null
    ): CategoryAssignment = withContext(Dispatchers.IO) {
        val taxonomy = catalogRepository.getTaxonomy().firstOrNull().orEmpty()
        val normalizedExpression = userExpression?.trim()?.lowercase()

        // 1. Regola appresa attiva per (espressione, prodotto)
        if (normalizedExpression != null && normalizedExpression.isNotEmpty()) {
            val rule = learnedRulesRepository.getByExpression(normalizedExpression)
            if (rule != null && rule.isActive && rule.productId == resolvedProduct.id) {
                val ruleLevel3 = taxonomy.findLevel3(rule.level3Id)
                if (ruleLevel3 != null && !ruleLevel3.isUserDefined) {
                    return@withContext CategoryAssignment(
                        suggestedLevel3Id = rule.level3Id,
                        confidence = 0.9,
                        source = AssignmentSource.LEARNED,
                        product = resolvedProduct,
                        alternatives = taxonomy.findLevel3(resolvedProduct.level3Id)
                            ?.let { if (it.id != rule.level3Id) listOf(it) else emptyList() }
                            .orEmpty(),
                        userExpression = normalizedExpression
                    )
                }
            }
        }

        // 2. Default del prodotto
        val productLevel3 = taxonomy.findLevel3(resolvedProduct.level3Id)
        if (productLevel3 != null && !productLevel3.isUserDefined) {
            return@withContext CategoryAssignment(
                suggestedLevel3Id = resolvedProduct.level3Id,
                confidence = 1.0,
                source = AssignmentSource.DEFAULT,
                product = resolvedProduct,
                alternatives = emptyList(),
                userExpression = normalizedExpression
            )
        }

        // 3. Fallback primo L3 valido
        val fallbackLevel3 = taxonomy.firstOrNull { it is TaxonomyLevel.Level3 && !it.isUserDefined } as? TaxonomyLevel.Level3
        CategoryAssignment(
            suggestedLevel3Id = fallbackLevel3?.id ?: resolvedProduct.level3Id,
            confidence = 0.5,
            source = AssignmentSource.FALLBACK,
            product = resolvedProduct,
            alternatives = emptyList(),
            userExpression = normalizedExpression
        )
    }
}

private fun List<TaxonomyLevel>.findLevel3(id: Long): TaxonomyLevel.Level3? =
    firstOrNull { it is TaxonomyLevel.Level3 && it.id == id } as? TaxonomyLevel.Level3
