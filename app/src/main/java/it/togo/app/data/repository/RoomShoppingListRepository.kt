package it.togo.app.data.repository

import it.togo.app.data.database.dao.ShoppingItemDao
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.HistoricalItem
import it.togo.app.domain.model.ItemAttributes
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.room.Transaction

class RoomShoppingListRepository(
    private val shoppingItemDao: ShoppingItemDao,
    private val historyDao: HistoryDao
) : ShoppingListRepository {

    override fun getAllItems(): Flow<List<ShoppingItem>> =
        shoppingItemDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getActiveItems(): Flow<List<ShoppingItem>> =
        getAllItems().map { items -> items.filter { !it.isChecked } }

    override fun getCheckedItems(): Flow<List<ShoppingItem>> =
        getAllItems().map { items -> items.filter { it.isChecked } }

    override suspend fun insert(item: ShoppingItem) {
        withContext(Dispatchers.IO) {
            shoppingItemDao.insert(item.toEntity())
        }
    }

    override suspend fun update(item: ShoppingItem) {
        withContext(Dispatchers.IO) {
            shoppingItemDao.update(item.toEntity())
        }
    }

    override suspend fun delete(itemId: String) {
        withContext(Dispatchers.IO) {
            shoppingItemDao.deleteById(itemId)
        }
    }

    override suspend fun getByProductId(productId: Long): ShoppingItem? =
        withContext(Dispatchers.IO) {
            shoppingItemDao.getActiveByProductId(productId)?.toDomain()
        }

    override fun getAttributeSuggestions(): Flow<AttributeSuggestions> =
        combine(
            shoppingItemDao.getBrands(),
            shoppingItemDao.getVariants(),
            historyDao.getBrands(),
            historyDao.getVariants(),
        ) { shoppingBrands, shoppingVariants, historyBrands, historyVariants ->
            AttributeSuggestions(
                brands = mergeValues(shoppingBrands, historyBrands),
                variants = mergeValues(shoppingVariants, historyVariants),
            )
        }

    override suspend fun getLastAttributes(productId: Long): ItemAttributes? =
        withContext(Dispatchers.IO) {
            // La lista attiva riflette l'ultima intenzione dell'utente: se la
            // voce è ancora lì, i suoi attributi sono i più recenti. Solo in
            // mancanza si guarda l'acquisto più recente nello storico.
            val attributes = shoppingItemDao.getActiveByProductId(productId)?.let {
                ItemAttributes(it.brand, it.variant, it.condition)
            } ?: historyDao.getByProductIds(listOf(productId))
                .maxByOrNull { it.purchasedAt }
                ?.let { ItemAttributes(it.brand, it.variant, it.condition) }
            attributes?.takeUnless { it.isEmpty }
        }

    /**
     * Deduplica case-insensitive ("Coop" e "coop" sono la stessa marca) e
     * ordina alfabeticamente i valori ricordati.
     */
    private fun mergeValues(vararg lists: List<String>): List<String> =
        lists.asSequence()
            .flatten()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedBy { it.lowercase(Locale.ROOT) }
            .toList()

    @Transaction
    override suspend fun checkout(checkedItemIds: List<String>): Boolean {
        if (checkedItemIds.isEmpty()) return true

        // Batch read all checked items in one query
        val checkedItems = shoppingItemDao.getByIds(checkedItemIds)
            .map { it.toDomain() }

        if (checkedItems.isEmpty()) return true

        // Lo storico distingue le varianti: due voci dello stesso prodotto con
        // marca/variante differenti restano due righe separate.
        val checkedGroups = checkedItems.groupBy {
            HistoryKey(it.productId, it.brand, it.variant, it.condition)
        }
        val productIds = checkedGroups.keys.map { it.productId }.distinct()
        val existingRows = historyDao.getByProductIds(productIds)
        val existingByKey = existingRows.associateBy {
            HistoryKey(it.productId, it.brand, it.variant, it.condition)
        }.toMutableMap()

        // Le righe create prima della migrazione non hanno attributi: possono
        // essere riutilizzate per il primo acquisto di una variante.
        val legacyByProduct = existingRows
            .filter { it.brand == null && it.variant == null && it.condition == null }
            .associateBy { it.productId }
            .toMutableMap()

        val now = System.currentTimeMillis()
        val historicalItems = checkedGroups.map { (key, items) ->
            val exactMatch = existingByKey.remove(key)
            val existing = exactMatch ?: legacyByProduct.remove(key.productId)
            if (existing != null) {
                // La stessa riga non deve essere aggiornata due volte se nello
                // stesso checkout convivono variante e voce senza attributi.
                if (existing.brand == null && existing.variant == null && existing.condition == null) {
                    legacyByProduct.remove(key.productId)
                }
                existingByKey.remove(
                    HistoryKey(existing.productId, existing.brand, existing.variant, existing.condition)
                )
                val totalQuantity = items.sumOf { it.quantity }
                val firstItem = items.first()
                existing.toDomain().increment(
                    quantity = totalQuantity,
                    unit = firstItem.unit,
                    brand = key.brand,
                    variant = key.variant,
                    condition = key.condition,
                ).toEntity()
            } else {
                val totalQuantity = items.sumOf { it.quantity }
                val firstItem = items.first()
                HistoricalItem(
                    id = firstItem.id,
                    productId = key.productId,
                    lastQuantity = totalQuantity,
                    lastUnit = firstItem.unit,
                    brand = key.brand,
                    variant = key.variant,
                    condition = key.condition,
                    purchasedAt = now,
                    purchaseCount = 1
                ).toEntity()
            }
        }

        // Upsert to history
        historicalItems.forEach { historyDao.upsertHistorical(it) }

        // Delete from shopping list - verify all items deleted
        val deletedCount = shoppingItemDao.deleteByIds(checkedItemIds)
        if (deletedCount != checkedItemIds.size) {
            throw IllegalStateException("Expected to delete ${checkedItemIds.size} items but deleted $deletedCount")
        }

        return true
    }
}

private data class HistoryKey(
    val productId: Long,
    val brand: String?,
    val variant: String?,
    val condition: String?,
)
