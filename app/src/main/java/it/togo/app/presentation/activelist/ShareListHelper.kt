package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.first

/**
 * Helper legacy per il testo condiviso: nell'app non ha chiamanti.
 *
 * La produzione passa da
 * [it.togo.app.domain.usecase.ShareListUseCase]: questo helper è mantenuto
 * allineato al use case per non avere due formati diversi se venisse
 * riutilizzato.
 */
suspend fun buildSharedListText(
    activeItems: List<ShoppingItem>,
    catalogRepository: CatalogRepository
): String {
    val taxonomy = catalogRepository.getTaxonomy().first()
    val level1Map = taxonomy.filterIsInstance<TaxonomyLevel.Level1>().associateBy { it.id }
    val level2Map = taxonomy.filterIsInstance<TaxonomyLevel.Level2>().associateBy { it.id }
    val level3Map = taxonomy.filterIsInstance<TaxonomyLevel.Level3>().associateBy { it.id }
    val productsById = mutableMapOf<String, it.togo.app.domain.model.CanonicalProduct>()
    for (item in activeItems) {
        val product = catalogRepository.getById(item.productId) ?: continue
        productsById[item.id] = product
    }

    val sb = StringBuilder()

    val itemsByL1 = activeItems.groupBy { item ->
        val product = productsById[item.id]
        val l3 = product?.level3Id?.let { level3Map[it] }
        val l2 = l3?.let { level2Map[it.level2Id] }
        l2?.let { level1Map[it.level1Id] }
    }

    level1Map.values.sortedBy { it.sortOrder }.forEach l1@{ l1 ->
        val l1Items = itemsByL1[l1] ?: return@l1
        sb.appendLine("[${l1.name.uppercase()}]")

        val itemsByL2 = l1Items.groupBy { item ->
            val product = productsById[item.id]
            val l3 = product?.level3Id?.let { level3Map[it] }
            l3?.let { level2Map[it.level2Id] }
        }

        level2Map.values.filter { it.level1Id == l1.id }.sortedBy { it.sortOrder }.forEach l2@{ l2 ->
            val l2Items = itemsByL2[l2] ?: return@l2
            sb.appendLine("+ ${l2.name.uppercase()}")

            val itemsByL3 = l2Items.groupBy { item ->
                val product = productsById[item.id]
                product?.level3Id?.let { level3Map[it] }
            }

            level3Map.values.filter { it.level2Id == l2.id }.sortedBy { it.sortOrder }.forEach l3@{ l3 ->
                val l3Items = itemsByL3[l3] ?: return@l3
                sb.appendLine("- ${l3.name.uppercase()}")
                l3Items.forEach { item ->
                    val product = productsById[item.id] ?: return@forEach
                    sb.appendLine("  ${formatItem(item, product)}")
                }
            }
        }
        sb.appendLine()
    }

    return sb.toString().trim()
}

/**
 * Riga voce: prodotto, marca e variante (quando presenti), quantità e unità.
 * Es. "Spaghetti Barilla integrale 2 pz".
 */
private fun formatItem(
    item: ShoppingItem,
    product: it.togo.app.domain.model.CanonicalProduct,
): String {
    val parts = mutableListOf(product.name)
    item.brand?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
    item.variant?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
    parts.add(item.unit.format(item.quantity))
    return parts.joinToString(" ")
}
