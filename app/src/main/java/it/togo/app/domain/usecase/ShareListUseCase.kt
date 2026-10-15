package it.togo.app.domain.usecase

import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShareListUseCase(
    private val shoppingListRepository: ShoppingListRepository,
    private val catalogRepository: CatalogRepository
) {

    suspend operator fun invoke(): android.content.Intent? = withContext(Dispatchers.IO) {
        val activeItems = shoppingListRepository.getActiveItems().firstOrNull()
            .orEmpty()
            .filter { !it.isChecked }

        if (activeItems.isEmpty()) return@withContext null

        val taxonomy = catalogRepository.getTaxonomy().firstOrNull().orEmpty()
        val level1Map = taxonomy.filterIsInstance<TaxonomyLevel.Level1>().associateBy { it.id }
        val level2Map = taxonomy.filterIsInstance<TaxonomyLevel.Level2>().associateBy { it.id }
        val level3Map = taxonomy.filterIsInstance<TaxonomyLevel.Level3>().associateBy { it.id }

        val text = buildSharedText(activeItems, level1Map, level2Map, level3Map)

        android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(android.content.Intent.EXTRA_TEXT, text)
            .putExtra(android.content.Intent.EXTRA_SUBJECT, "Lista spesa TOGO")
    }

    private suspend fun buildSharedText(
        items: List<ShoppingItem>,
        level1Map: Map<Long, TaxonomyLevel.Level1>,
        level2Map: Map<Long, TaxonomyLevel.Level2>,
        level3Map: Map<Long, TaxonomyLevel.Level3>
    ): String {
        val sb = StringBuilder()

        val itemsByL1 = items.groupBy { item ->
            val product = getProduct(item)
            val l3 = product?.level3Id?.let { level3Map[it] }
            val l2 = l3?.let { level2Map[it.level2Id] }
            l2?.let { level1Map[it.level1Id] }
        }

        level1Map.values.sortedBy { it.sortOrder }.forEach { l1 ->
            val l1Items = itemsByL1[l1] ?: return@forEach
            sb.appendLine("=== ${l1.name} ===")

            val itemsByL2 = l1Items.groupBy { item ->
                val product = getProduct(item)
                val l3 = product?.level3Id?.let { level3Map[it] }
                l3?.let { level2Map[it.level2Id] }
            }

            level2Map.values.filter { it.level1Id == l1.id }.sortedBy { it.sortOrder }.forEach { l2 ->
                val l2Items = itemsByL2[l2] ?: return@forEach
                sb.appendLine("--- ${l2.name} ---")

                val itemsByL3 = l2Items.groupBy { item ->
                    val product = getProduct(item)
                    product?.level3Id?.let { level3Map[it] }
                }

                level3Map.values.filter { it.level2Id == l2.id }.sortedBy { it.sortOrder }.forEach { l3 ->
                    val l3Items = itemsByL3[l3] ?: return@forEach
                    sb.appendLine("${l3.name}:")
                    l3Items.forEach { item ->
                        sb.appendLine("  - ${formatItem(item)}")
                    }
                }
            }
            sb.appendLine()
        }

        return sb.toString().trim()
    }

    private suspend fun getProduct(item: ShoppingItem): it.togo.app.domain.model.CanonicalProduct? {
        return catalogRepository.getById(item.productId)
    }

    private fun formatItem(item: ShoppingItem): String {
        return "${item.quantity} ${item.unit.displayName}"
    }
}
