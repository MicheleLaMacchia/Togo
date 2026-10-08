package it.togo.app.domain.model

import java.util.UUID

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val productId: Long,
    val quantity: Double,
    val unit: StandardUnit,
    val brand: String? = null,
    val variant: String? = null,
    val condition: String? = null,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun copyWith(
        quantity: Double? = null,
        unit: StandardUnit? = null,
        brand: String? = null,
        variant: String? = null,
        condition: String? = null,
        isChecked: Boolean? = null
    ): ShoppingItem = copy(
        quantity = quantity ?: this.quantity,
        unit = unit ?: this.unit,
        brand = brand ?? this.brand,
        variant = variant ?? this.variant,
        condition = condition ?? this.condition,
        isChecked = isChecked ?: this.isChecked,
        updatedAt = System.currentTimeMillis()
    )

    fun toEntity(): it.togo.app.data.database.entity.ShoppingItemEntity = it.togo.app.data.database.entity.ShoppingItemEntity(
        id = id,
        productId = productId,
        quantity = quantity,
        unit = unit,
        brand = brand,
        variant = variant,
        condition = condition,
        isChecked = isChecked,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}