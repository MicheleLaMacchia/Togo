package it.togo.app.data.repository

import it.togo.app.data.database.dao.ShoppingItemDao
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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

    @Transaction
    override suspend fun checkout(checkedItemIds: List<String>): Boolean {
        if (checkedItemIds.isEmpty()) return true

        // Batch read all checked items in one query
        val checkedItems = shoppingItemDao.getByIds(checkedItemIds)
            .map { it.toDomain() }

        if (checkedItems.isEmpty()) return true

        // Batch read history items - use distinct productIds to avoid duplicates
        val productIds = checkedItems.map { it.productId }.distinct()
        val historyMap = historyDao.getByProductIds(productIds)
            .associateBy { it.productId }

        val now = System.currentTimeMillis()
        val historicalItems = checkedItems.groupBy { it.productId }.map { (productId, items) ->
            val existing = historyMap[productId]
            if (existing != null) {
                // Use first item as reference, sum quantities
                val totalQuantity = items.sumOf { it.quantity }
                val firstItem = items.first()
                existing.toDomain().increment(totalQuantity, firstItem.unit).toEntity()
            } else {
                // Create new historical item with summed quantities
                val totalQuantity = items.sumOf { it.quantity }
                val firstItem = items.first()
                it.togo.app.domain.model.HistoricalItem(
                    id = firstItem.id,
                    productId = firstItem.productId,
                    lastQuantity = totalQuantity,
                    lastUnit = firstItem.unit,
                    purchasedAt = now,
                    purchaseCount = 1
                ).toEntity()
            }
        }.toList()

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