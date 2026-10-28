package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus

/**
 * Entità Room di transizione per la lista unificata.
 *
 * Non è ancora registrata in `TogoDatabase.entities`: la tabella `LIST_ITEM` e
 * gli indici verranno creati nella Story 8.4. In questa storia l'entità serve
 * solo a definire e testare il mapping con il dominio.
 *
 * `status` è una stringa per non dipendere da un converter non ancora
 * introdotto; i valori ammessi sono quelli di [ListItemStatus].
 */
@Entity(tableName = "LIST_ITEM")
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
