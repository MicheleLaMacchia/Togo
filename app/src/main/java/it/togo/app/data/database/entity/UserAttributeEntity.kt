package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.togo.app.domain.model.UserAttribute
import it.togo.app.domain.model.UserAttributeType

/**
 * Marca o variante aggiunta dall'utente.
 *
 * La tabella contiene SOLO valori user-defined: le marche predefinite
 * (Coop, Conad, ...) restano in [it.togo.app.domain.model.ProductAttribute] e
 * non vengono registrate. In questo modo le sezioni delle Impostazioni possono
 * mostrare esclusivamente ciò che l'utente ha davvero creato.
 *
 * `normalized` è la forma accent-fold/minuscola usata per i confronti
 * case-insensitive e per l'indice univoco `(type, normalized)`.
 */
@Entity(
    tableName = "USER_ATTRIBUTE",
    indices = [
        Index(
            value = ["type", "normalized"],
            unique = true,
            name = "idx_user_attribute_type_normalized",
        )
    ]
)
data class UserAttributeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val value: String,
    val normalized: String,
) {
    fun toDomain(): UserAttribute = UserAttribute(
        id = id,
        type = UserAttributeType.valueOf(type),
        value = value,
    )
}
