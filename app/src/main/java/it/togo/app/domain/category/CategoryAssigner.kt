package it.togo.app.domain.category

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.Normalizer

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
        val taxonomy = catalogRepository.getTaxonomy().first()
        // Accent-fold come CatalogQueries.normalize: "Caffè" -> "caffe"
        val normalizedExpression = userExpression?.let { normalizeExpression(it) }

        // 1. Regola appresa attiva per (espressione, prodotto)
        if (normalizedExpression != null && normalizedExpression.isNotEmpty()) {
            val rule = learnedRulesRepository.getByExpression(normalizedExpression)
            if (rule != null && rule.isActive && rule.productId == resolvedProduct.id) {
                val ruleLevel3 = findLevel3(taxonomy, rule.level3Id)
                if (ruleLevel3 != null && !ruleLevel3.isUserDefined) {
                    val defaultLevel3 = findLevel3(taxonomy, resolvedProduct.level3Id)
                    // Alternatives: default + rule + sibling L3s (stesso L2), come da spec 3-6
                    val alternatives = buildList {
                        defaultLevel3?.takeIf { !it.isUserDefined }?.let { add(it) }
                        add(ruleLevel3)
                        defaultLevel3?.let { addAll(siblingsOf(taxonomy, it)) }
                        addAll(siblingsOf(taxonomy, ruleLevel3))
                    }.distinctBy { it.id }

                    return@withContext CategoryAssignment(
                        suggestedLevel3Id = rule.level3Id,
                        confidence = 0.9,
                        source = AssignmentSource.LEARNED,
                        product = resolvedProduct,
                        alternatives = alternatives,
                        userExpression = normalizedExpression
                    )
                }
            }
        }

        // 2. Default del prodotto
        val productLevel3 = findLevel3(taxonomy, resolvedProduct.level3Id)
        if (productLevel3 != null && !productLevel3.isUserDefined) {
            return@withContext CategoryAssignment(
                suggestedLevel3Id = resolvedProduct.level3Id,
                confidence = 1.0,
                source = AssignmentSource.DEFAULT,
                product = resolvedProduct,
                alternatives = siblingsOf(taxonomy, productLevel3),
                userExpression = normalizedExpression
            )
        }

        // 3. Fallback primo L3 valido
        val fallbackLevel3 = taxonomy
            .filterIsInstance<TaxonomyLevel.Level3>()
            .firstOrNull { !it.isUserDefined }
        CategoryAssignment(
            suggestedLevel3Id = fallbackLevel3?.id ?: resolvedProduct.level3Id,
            confidence = 0.5,
            source = AssignmentSource.FALLBACK,
            product = resolvedProduct,
            alternatives = fallbackLevel3?.let { siblingsOf(taxonomy, it) }.orEmpty(),
            userExpression = normalizedExpression
        )
    }
}

private fun findLevel3(taxonomy: List<TaxonomyLevel>, id: Long): TaxonomyLevel.Level3? =
    taxonomy.firstOrNull { it is TaxonomyLevel.Level3 && it.id == id } as? TaxonomyLevel.Level3

/**
 * Normalizzazione "accent-fold" identica a `CatalogQueries.normalize`:
 * trim, minuscole, decomposizione NFKD e rimozione dei segni diacritici.
 */
private fun normalizeExpression(raw: String): String {
    val decomposed = Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFKD)
    return buildString(decomposed.length) {
        for (ch in decomposed) {
            val type = Character.getType(ch)
            if (type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt() &&
                type != Character.ENCLOSING_MARK.toInt()
            ) {
                append(ch)
            }
        }
    }
}

/** Sibling L3 non user-defined nello stesso Level2 (escluso [level3] stesso). */
private fun siblingsOf(
    taxonomy: List<TaxonomyLevel>,
    level3: TaxonomyLevel.Level3
): List<TaxonomyLevel.Level3> =
    taxonomy.filterIsInstance<TaxonomyLevel.Level3>()
        .filter { it.level2Id == level3.level2Id && it.id != level3.id && !it.isUserDefined }
