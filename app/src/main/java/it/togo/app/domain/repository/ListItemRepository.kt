package it.togo.app.domain.repository

import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.ItemAttributes
import it.togo.app.domain.model.ListItem
import kotlinx.coroutines.flow.Flow

/**
 * Fonte di verità della lista unificata.
 *
 * Il contratto è volutamente privo di checkout, staging e “Presi”: queste
 * responsabilità restano negli adapter legacy fino alla Story 8.7. Le
 * transizioni di stato sono esplicite (`complete`, `delete`, `restore`,
 * `reAdd`) e usano il [it.togo.app.domain.time.Clock] iniettato nel repository.
 */
interface ListItemRepository {

    fun observeAll(): Flow<List<ListItem>>
    fun observeActive(): Flow<List<ListItem>>
    fun observePurchased(): Flow<List<ListItem>>
    fun observeDeleted(): Flow<List<ListItem>>

    suspend fun getById(id: String): ListItem?
    suspend fun getActiveByProductId(productId: Long): ListItem?
    suspend fun getLatestPurchasedByProductId(productId: Long): ListItem?

    suspend fun add(item: ListItem): ListItem
    suspend fun updateDetails(item: ListItem): ListItem?
    suspend fun complete(id: String): ListItem?
    suspend fun delete(id: String): ListItem?
    suspend fun restore(id: String): ListItem?
    suspend fun reAdd(purchasedId: String): ListItem?

    suspend fun clearActive(): Int
    suspend fun purgeDeleted(): Int

    fun observeAttributeSuggestions(): Flow<AttributeSuggestions>
    suspend fun getLastAttributes(productId: Long): ItemAttributes?
}
