package it.togo.app.domain.usecase

import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ListItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Condivide la lista attiva come testo.
 *
 * Dalla Story 8.7 la fonte è [ListItemRepository]: solo le righe `TO_BUY`
 * entrano nel testo. Nessuna Task e nessuna riga `PURCHASED`/`DELETED`
 * compare nella condivisione.
 */
class ShareListUseCase(
    private val listItemRepository: ListItemRepository,
    private val catalogRepository: CatalogRepository
) {

    suspend operator fun invoke(): android.content.Intent? = withContext(Dispatchers.IO) {
        val activeItems = listItemRepository.observeActive().first()

        if (activeItems.isEmpty()) return@withContext null

        val taxonomy = catalogRepository.getTaxonomy().first()
        val level1Map = taxonomy.filterIsInstance<TaxonomyLevel.Level1>().associateBy { it.id }
        val level2Map = taxonomy.filterIsInstance<TaxonomyLevel.Level2>().associateBy { it.id }
        val level3Map = taxonomy.filterIsInstance<TaxonomyLevel.Level3>().associateBy { it.id }
        val productsById = mutableMapOf<String, it.togo.app.domain.model.CanonicalProduct>()
        for (item in activeItems) {
            val product = catalogRepository.getById(item.productId) ?: continue
            productsById[item.id] = product
        }

        val text = buildSharedText(activeItems, productsById, level1Map, level2Map, level3Map)

        android.content.Intent(android.content.Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(android.content.Intent.EXTRA_TEXT, text)
            .putExtra(android.content.Intent.EXTRA_SUBJECT, "Lista spesa TOGO")
    }

    private fun buildSharedText(
        items: List<ListItem>,
        productsById: Map<String, it.togo.app.domain.model.CanonicalProduct>,
        level1Map: Map<Long, TaxonomyLevel.Level1>,
        level2Map: Map<Long, TaxonomyLevel.Level2>,
        level3Map: Map<Long, TaxonomyLevel.Level3>
    ): String {
        val sb = StringBuilder()

        val itemsByL1 = items.groupBy { item ->
            val product = productsById[item.id]
            val l3 = product?.level3Id?.let { level3Map[it] }
            val l2 = l3?.let { level2Map[it.level2Id] }
            l2?.let { level1Map[it.level1Id] }
        }

        level1Map.values.sortedBy { it.sortOrder }.forEach { l1 ->
            val l1Items = itemsByL1[l1] ?: return@forEach
            sb.appendLine("[${l1.name.uppercase()}]")

            val itemsByL2 = l1Items.groupBy { item ->
                val product = productsById[item.id]
                val l3 = product?.level3Id?.let { level3Map[it] }
                l3?.let { level2Map[it.level2Id] }
            }

            level2Map.values.filter { it.level1Id == l1.id }.sortedBy { it.sortOrder }.forEach { l2 ->
                val l2Items = itemsByL2[l2] ?: return@forEach
                sb.appendLine("+ ${l2.name.uppercase()}")

                val itemsByL3 = l2Items.groupBy { item ->
                    val product = productsById[item.id]
                    product?.level3Id?.let { level3Map[it] }
                }

                level3Map.values.filter { it.level2Id == l2.id }.sortedBy { it.sortOrder }.forEach { l3 ->
                    val l3Items = itemsByL3[l3] ?: return@forEach
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
     *
     * L'unità è testo libero: se corrisponde a una [StandardUnit] usa la
     * formattazione standard, altrimenti è riportata così com'è.
     */
    private fun formatItem(
        item: ListItem,
        product: it.togo.app.domain.model.CanonicalProduct,
    ): String {
        val parts = mutableListOf(product.name)
        item.brand?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        item.variant?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        parts.add(formatQuantityAndUnit(item.quantity, item.unit))
        return parts.joinToString(" ")
    }
}

/**
 * Quantità + unità per il testo condiviso: la formattazione standard se
 * l'unità è riconosciuta, altrimenti quantità e testo libero.
 */
internal fun formatQuantityAndUnit(quantity: Double, unit: String): String {
    val standard = StandardUnit.fromCode(unit.trim())
    return if (standard != null) {
        standard.format(quantity)
    } else {
        "${formatShareQuantity(quantity)} ${unit.trim()}"
    }
}

/** Formatta un Double rimuovendo zeri decimali inutili (2.0 → "2", 1.5 → "1.5"). */
private fun formatShareQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
