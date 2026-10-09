package it.togo.app.data.repository

import it.togo.app.data.database.dao.ShoppingItemDao
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import androidx.room.Transaction

class RoomShoppingListRepository(
    private val shoppingItemDao: ShoppingItemDao,
    private val historyDao: HistoryDao
) : ShoppingListRepository {

    override fun getActiveItems(): Flow<List<ShoppingItem>> =
        shoppingItemDao.getAll()
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .map { items -> items.filter { !it.isChecked } }

    override fun getCheckedItems(): Flow<List<ShoppingItem>> =
        shoppingItemDao.getAll()
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .map { items -> items.filter { it.isChecked } }

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

        return withContext(Dispatchers.IO) {
            // Batch read all checked items in one query
            val checkedItems = shoppingItemDao.getByIds(checkedItemIds)
                .map { it.toDomain() }

            if (checkedItems.isEmpty()) return@withContext true

            // Batch read history items
            val productIds = checkedItems.map { it.productId }.distinct()
            val historyMap = historyDao.getByProductIds(productIds)
                .associateBy { it.productId }

            val now = System.currentTimeMillis()
            val historicalItems = checkedItems.map { item ->
                val existing = historyMap[item.productId]
                if (existing != null) {
                    existing.toDomain().increment(item.quantity, item.unit).toEntity()
                } else {
                    it.togo.app.domain.model.HistoricalItem(
                        id = item.id,
                        productId = item.productId,
                        lastQuantity = item.quantity,
                        lastUnit = item.unit,
                        purchasedAt = System.currentTimeMillis(),
                        purchaseCount = 1
                    ).toEntity()
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