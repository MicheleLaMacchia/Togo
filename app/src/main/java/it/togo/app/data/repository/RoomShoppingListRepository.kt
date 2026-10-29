package it.togo.app.data.repository

import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.ItemAttributes
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.ListItemRepository
import it.togo.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Adapter legacy tra [ShoppingListRepository] e la nuova fonte di verità
 * [ListItemRepository] (Story 8.5).
 *
 * La UI esistente continua a usare `ShoppingItem`, ma ogni lettura/scrittura
 * passa da `LIST_ITEM`. Le voci spuntate spariscono subito dagli attivi e la
 * vecchia sezione “Presi” resta vuota: checkout e staging saranno rimossi
 * definitivamente nella Story 8.7.
 */
class RoomShoppingListRepository(
    private val listItemRepository: ListItemRepository,
) : ShoppingListRepository {

    override fun getAllItems(): Flow<List<ShoppingItem>> =
        listItemRepository.observeActive().map { items ->
            items.map { it.toShoppingItem() }
        }

    override fun getActiveItems(): Flow<List<ShoppingItem>> = getAllItems()

    override fun getCheckedItems(): Flow<List<ShoppingItem>> =
        listItemRepository.observePurchased().map { items ->
            items.map { it.toShoppingItem() }
        }

    override suspend fun insert(item: ShoppingItem) {
        val status = if (item.isChecked) {
            ListItemStatus.PURCHASED
        } else {
            ListItemStatus.TO_BUY
        }

        val listItem = ListItem.create(
            id = item.id,
            productId = item.productId,
            quantity = validQuantity(item.quantity),
            unit = item.unit.name,
            createdAt = item.createdAt,
            brand = item.brand,
            variant = item.variant,
            condition = item.condition,
            status = status,
            purchaseCount = if (status == ListItemStatus.PURCHASED) 1 else 0,
            statusChangedAt = if (status == ListItemStatus.PURCHASED) {
                item.updatedAt
            } else {
                item.createdAt
            },
            updatedAt = item.updatedAt,
        )

        listItemRepository.add(listItem)
    }

    override suspend fun update(item: ShoppingItem) {
        val existing = listItemRepository.getById(item.id) ?: return

        // PURCHASED è terminale: la UI legacy non può riaprire una voce già
        // acquistata tramite questo adapter.
        if (existing.status != ListItemStatus.TO_BUY) return

        if (item.isChecked) {
            listItemRepository.complete(item.id)
            return
        }

        listItemRepository.updateDetails(
            existing.copy(
                quantity = validQuantity(item.quantity),
                unit = item.unit.name,
                brand = item.brand,
                variant = item.variant,
                condition = item.condition,
            ),
        )
    }

    override suspend fun delete(itemId: String) {
        listItemRepository.delete(itemId)
    }

    override suspend fun getByProductId(productId: Long): ShoppingItem? =
        listItemRepository.getActiveByProductId(productId)?.toShoppingItem()

    /**
     * Compatibilità temporanea con la UI pre-Story 8.7.
     *
     * È un no-op: le voci sono già `PURCHASED` in `LIST_ITEM`; non si elimina
     * nulla e non si crea storico. Il metodo verrà rimosso nella Story 8.7.
     */
    @Deprecated("Checkout legacy: rimosso nella Story 8.7.")
    override suspend fun checkout(checkedItemIds: List<String>): Boolean = true

    override fun getAttributeSuggestions(): Flow<AttributeSuggestions> =
        listItemRepository.observeAttributeSuggestions()

    override suspend fun getLastAttributes(productId: Long): ItemAttributes? =
        listItemRepository.getLastAttributes(productId)

    private fun ListItem.toShoppingItem(): ShoppingItem = ShoppingItem(
        id = id,
        productId = productId,
        quantity = quantity,
        unit = StandardUnit.fromCode(unit.trim()) ?: StandardUnit.PIECE,
        brand = brand,
        variant = variant,
        condition = condition,
        isChecked = status == ListItemStatus.PURCHASED,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun validQuantity(quantity: Double): Double =
        if (quantity.isFinite() && quantity > 0.0) quantity else 1.0
}
