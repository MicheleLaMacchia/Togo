package it.togo.app.domain.repository

import it.togo.app.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    fun getActiveItems(): Flow<List<ShoppingItem>>
    suspend fun insert(item: ShoppingItem)
    suspend fun update(item: ShoppingItem)
    suspend fun delete(itemId: String)
    suspend fun getByProductId(productId: Long): ShoppingItem?
    suspend fun checkout(checkedItemIds: List<String>): Boolean
}