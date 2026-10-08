package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import it.togo.app.domain.model.StandardUnit

@Entity(
    tableName = "SHOPPING_ITEM",
    indices = [
        androidx.room.Index(value = ["productId"], name = "idx_shopping_item_product_id"),
        androidx.room.Index(value = ["isChecked"], name = "idx_shopping_item_checked")
    ]
)
data class ShoppingItemEntity(
    @PrimaryKey
    val id: String,
    val productId: Long,
    val quantity: Double,
    val unit: StandardUnit,
    val brand: String? = null,
    val variant: String? = null,
    val condition: String? = null,
    val isChecked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): it.togo.app.domain.model.ShoppingItem = it.togo.app.domain.model.ShoppingItem(
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