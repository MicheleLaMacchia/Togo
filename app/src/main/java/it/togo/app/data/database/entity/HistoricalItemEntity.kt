package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import it.togo.app.domain.model.StandardUnit

@Entity(
    tableName = "HISTORICAL_ITEM",
    indices = [
        androidx.room.Index(value = ["productId"], name = "idx_historical_item_product_id"),
        androidx.room.Index(value = ["purchasedAt"], name = "idx_historical_item_purchased_at")
    ]
)
data class HistoricalItemEntity(
    @PrimaryKey
    val id: String,
    val productId: Long,
    val lastQuantity: Double,
    val lastUnit: StandardUnit,
    val purchasedAt: Long,
    val purchaseCount: Int
) {
    fun toDomain(): it.togo.app.domain.model.HistoricalItem = it.togo.app.domain.model.HistoricalItem(
        id = id,
        productId = productId,
        lastQuantity = lastQuantity,
        lastUnit = lastUnit,
        purchasedAt = purchasedAt,
        purchaseCount = purchaseCount
    )
}