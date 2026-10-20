package it.togo.app.domain.repository

import it.togo.app.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingListRepository {
    /** Tutte le voci in un'unica emissione, per evitare stati transitori tra query. */
    fun getAllItems(): Flow<List<ShoppingItem>>
    fun getActiveItems(): Flow<List<ShoppingItem>>
    fun getCheckedItems(): Flow<List<ShoppingItem>>
    suspend fun insert(item: ShoppingItem)
    suspend fun update(item: ShoppingItem)
    suspend fun delete(itemId: String)
    suspend fun getByProductId(productId: Long): ShoppingItem?
    suspend fun checkout(checkedItemIds: List<String>): Boolean
}