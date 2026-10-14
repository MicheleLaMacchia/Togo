package it.togo.app.domain.usecase

import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use Case per la condivisione della lista attiva.
 *
 * Genera un testo formattato raggruppato per tassonomia (L1 → L2 → L3)
 * e invoca il ShareSheet di Android.
 */
class ShareListUseCase(
    private val shoppingListRepository: ShoppingListRepository,
    private val catalogRepository: CatalogRepository
) {

    /**
     * Genera il testo formattato per la condivisione e restituisce l'Intent pronto per ShareSheet.
     *
     * @return Intent.ACTION_SEND con testo formattato, o null se lista vuota
     */
    operator fun invoke(): android.content.Intent? = withContext(Dispatchers.IO) {
        val activeItems = shoppingListRepository.getActiveItems().first()
            .filter { !it.isChecked }

        if (activeItems.isEmpty()) return null

        val taxonomy = catalogRepository.getTaxonomy().first()
        val level1Map = taxonomy.filter { it is TaxonomyLevel.Level1 }
            .associateBy { it.id } { it as TaxonomyLevel.Level1 }
        val level2Map = taxonomy.filter { it is TaxonomyLevel.Level2 }
            .associateBy { it.id } { it as TaxonomyLevel.Level2 }
        val level3Map = taxonomy.filter { it is TaxonomyLevel.Level3 }
            .associateBy { it.id } { it as TaxonomyLevel.Level3 }

        val text = buildSharedText(activeItems, level1Map, level2Map, level3Map)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(android.content.Intent.EXTRA_TEXT, text)
            .putExtra(android.content.Intent.EXTRA_SUBJECT, "Lista spesa TOGO")

        return intent
    }

    /**
     * Costruisce il testo formattato raggruppato per L1 → L2 → L3.
     */
    private fun buildSharedText(
        items: List<it.togo.app.domain.model.ShoppingItem>,
        level1Map: Map<Long, TaxonomyLevel.Level1>,
        level2Map: Map<Long, TaxonomyLevel.Level2>,
        level3Map: Map<Long, TaxonomyLevel.Level3>
    ): String {
        val sb = StringBuilder()

        // Raggruppa per L1
        val itemsByL1 = items.groupBy { item ->
            val product = getProduct(item)
            val l3 = product?.level3Id?.let { level3Map[it] }
            val l2 = l3?.let { level2Map[it.level2Id] }
            l2?.let { level1Map[it.level1Id] }
        }

        level1Map.values.sortedBy { it.sortOrder }.forEach { l1 ->
            val l1Items = itemsByL1[l1] ?: continue
            if (l1Items.isEmpty()) return@forEach

            sb.appendLine("=== ${l1.name} ===")

            // Raggruppa per L2
            val itemsByL2 = l1Items.groupBy { item ->
                val product = getProduct(item)
                val l3 = product?.level3Id?.let { level3Map[it] }
                l3?.let { level2Map[it.level2Id] }
            }

            level2Map.values.filter { it.level1Id == l1.id }.sortedBy { it.sortOrder }.forEach { l2 ->
                val l2Items = itemsByL2[l2] ?: continue
                if (l2Items.isEmpty()) return@forEach

                sb.appendLine("--- ${l2.name} ---")

                // Raggruppa per L3
                val itemsByL3 = l2Items.groupBy { item ->
                    val product = getProduct(item)
                    product?.level3Id?.let { level3Map[it] }
                }

                level3Map.values.filter { it.level2Id == l2.id }.sortedBy { it.sortOrder }.forEach { l3 ->
                    val l3Items = itemsByL3[l3] ?: continue
                    if (l3Items.isEmpty()) return@forEach

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

    private suspend fun getProduct(item: it.togo.app.domain.model.ShoppingItem): it.togo.app.domain.model.CanonicalProduct? {
        return withContext(Dispatchers.IO) {
            catalogRepository.getById(item.productId)
        }
    }

    private fun formatItem(item: it.togo.app.domain.model.ShoppingItem): String {
        val sb = StringBuilder()
        sb.append("${item.quantity} ${item.unit.displayName} ")
        // Nota: attributi brand/variant/condition non disponibili in ShoppingItem senza join
        return sb.toString().trim()
    }
}