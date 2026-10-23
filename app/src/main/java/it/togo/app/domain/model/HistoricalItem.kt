package it.togo.app.domain.model

import java.util.UUID

data class HistoricalItem(
    val id: String = UUID.randomUUID().toString(),
    val productId: Long,
    val lastQuantity: Double,
    val lastUnit: StandardUnit,
    val brand: String? = null,
    val variant: String? = null,
    val condition: String? = null,
    val purchasedAt: Long = System.currentTimeMillis(),
    val purchaseCount: Int = 1
) {
    fun increment(
        quantity: Double,
        unit: StandardUnit,
        brand: String? = this.brand,
        variant: String? = this.variant,
        condition: String? = this.condition
    ): HistoricalItem = copy(
        lastQuantity = quantity,
        lastUnit = unit,
        brand = brand,
        variant = variant,
        condition = condition,
        purchasedAt = System.currentTimeMillis(),
        purchaseCount = purchaseCount + 1
    )

    fun toEntity(): it.togo.app.data.database.entity.HistoricalItemEntity = it.togo.app.data.database.entity.HistoricalItemEntity(
        id = id,
        productId = productId,
        lastQuantity = lastQuantity,
        lastUnit = lastUnit,
        brand = brand,
        variant = variant,
        condition = condition,
        purchasedAt = purchasedAt,
        purchaseCount = purchaseCount
    )
}