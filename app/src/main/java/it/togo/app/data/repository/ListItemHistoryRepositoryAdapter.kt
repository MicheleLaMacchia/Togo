package it.togo.app.data.repository

import it.togo.app.domain.model.HistoricalItem
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.HistoryRepository
import it.togo.app.domain.repository.ListItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Adapter legacy tra [HistoryRepository] e [ListItemRepository] (Story 8.5).
 *
 * La vecchia cronologia mostra solo le righe `PURCHASED`. Le righe `DELETED`
 * non compaiono: la cronologia unificata è scope della Story 8.7.
 */
class ListItemHistoryRepositoryAdapter(
    private val listItemRepository: ListItemRepository,
) : HistoryRepository {

    override fun getAll(): Flow<List<HistoricalItem>> =
        listItemRepository.observePurchased().map { items ->
            items.map { it.toHistoricalItem() }
                .sortedByDescending { it.purchasedAt }
        }

    override suspend fun insert(item: HistoricalItem) {
        listItemRepository.add(item.toPurchasedListItem())
    }

    override suspend fun upsertHistorical(item: HistoricalItem) {
        listItemRepository.add(item.toPurchasedListItem())
    }

    override suspend fun getByProductId(productId: Long): HistoricalItem? =
        listItemRepository.getLatestPurchasedByProductId(productId)
            ?.toHistoricalItem()

    private fun HistoricalItem.toPurchasedListItem(): ListItem = ListItem.create(
        id = id,
        productId = productId,
        quantity = validQuantity(lastQuantity),
        unit = lastUnit.name,
        createdAt = purchasedAt,
        brand = brand,
        variant = variant,
        condition = condition,
        status = ListItemStatus.PURCHASED,
        purchaseCount = maxOf(1, purchaseCount),
        statusChangedAt = purchasedAt,
        updatedAt = purchasedAt,
    )

    private fun ListItem.toHistoricalItem(): HistoricalItem = HistoricalItem(
        id = id,
        productId = productId,
        lastQuantity = quantity,
        lastUnit = StandardUnit.fromCode(unit.trim()) ?: StandardUnit.PIECE,
        brand = brand,
        variant = variant,
        condition = condition,
        purchasedAt = statusChangedAt,
        purchaseCount = maxOf(1, purchaseCount),
    )

    private fun validQuantity(quantity: Double): Double =
        if (quantity.isFinite() && quantity > 0.0) quantity else 1.0
}
