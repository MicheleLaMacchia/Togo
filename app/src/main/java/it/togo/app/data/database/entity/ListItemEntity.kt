package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus

/**
 * Entità Room della lista unificata.
 *
 * La tabella `LIST_ITEM` viene creata dalla migrazione `5→6` (Story 8.4).
 * `SHOPPING_ITEM` e `HISTORICAL_ITEM` restano temporaneamente registrate fino
 * alla Story 8.7, che le rimuoverà con la migrazione `6→7`.
 *
 * `status` è una stringa per non dipendere da un converter: i valori ammessi
 * sono quelli di [ListItemStatus].
 */
@Entity(
    tableName = "LIST_ITEM",
    indices = [
        Index(value = ["productId"], name = "idx_list_item_product_id"),
        Index(value = ["status"], name = "idx_list_item_status"),
        Index(
            value = ["productId", "status"],
            name = "idx_list_item_product_status",
        ),
        Index(value = ["createdAt"], name = "idx_list_item_created_at"),
        Index(value = ["statusChangedAt"], name = "idx_list_item_status_changed_at"),
    ],
)
data class ListItemEntity(
    @PrimaryKey
    val id: String,
    val productId: Long,
    val quantity: Double,
    val unit: String,
    val brand: String? = null,
    val variant: String? = null,
    val condition: String? = null,
    val note: String? = null,
    val status: String,
    val purchaseCount: Int,
    val createdAt: Long,
    val statusChangedAt: Long,
    val updatedAt: Long,
) {

    fun toDomain(): ListItem {
        val domainStatus = try {
            ListItemStatus.valueOf(status)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Status ListItem non valido: '$status'. Valori ammessi: " +
                    ListItemStatus.entries.joinToString { it.name },
                exception,
            )
        }

        return ListItem(
            id = id,
            productId = productId,
            quantity = quantity,
            unit = unit,
            brand = brand,
            variant = variant,
            condition = condition,
            note = note,
            status = domainStatus,
            purchaseCount = purchaseCount,
            createdAt = createdAt,
            statusChangedAt = statusChangedAt,
            updatedAt = updatedAt,
        )
    }
}
