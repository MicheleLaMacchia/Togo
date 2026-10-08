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

class RoomShoppingListRepository(
    private val shoppingItemDao: ShoppingItemDao,
    private val historyDao: HistoryDao
) : ShoppingListRepository {

    override fun getActiveItems(): Flow<List<ShoppingItem>> =
        shoppingItemDao.getActive().map { entities ->
            entities.map { it.toDomain() }
        }

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

    override suspend fun checkout(checkedItemIds: List<String>): Boolean {
        if (checkedItemIds.isEmpty()) return true

        return withContext(Dispatchers.IO) {
            val checkedItems = checkedItemIds.mapNotNull { id ->
                shoppingItemDao.getById(id)
            }

            if (checkedItems.isEmpty()) return@withContext true

            val now = System.currentTimeMillis()

            // Convert to historical items (upsert - increment purchase_count)
            val historicalItems = checkedItems.map { entity ->
                val existing = historyDao.getByProductId(entity.productId)
                if (existing != null) {
                    existing.toDomain().increment(entity.quantity, entity.unit).toEntity()
                } else {
                    entity.toDomain().let { domain ->
                        it.togo.app.domain.model.HistoricalItem(
                            id = entity.id,
                            productId = entity.productId,
                            lastQuantity = entity.quantity,
                            lastUnit = entity.unit,
                            purchasedAt = now,
                            purchaseCount = 1
                        ).toEntity()
                    }
                }
            }

            // Upsert to history
            historicalItems.forEach { historyDao.upsertHistorical(it) }

            // Delete from shopping list
            shoppingItemDao.deleteByIds(checkedItemIds)

            true
        }
    }
}