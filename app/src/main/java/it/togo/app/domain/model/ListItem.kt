package it.togo.app.domain.model

import java.util.UUID

/**
 * Riga della lista unificata nel dominio.
 *
 * La quantità è sempre finita e maggiore di zero; il valore `0.0` usato dal
 * vecchio modello come sentinella non è valido. L'unità è testo libero e viene
 * normalizzata con `trim()` dalla factory [create].
 *
 * `purchaseCount` vale almeno `1` se e solo se lo stato è [ListItemStatus.PURCHASED];
 * per [ListItemStatus.TO_BUY] e [ListItemStatus.DELETED] vale `0`.
 */
data class ListItem(
    val id: String,
    val productId: Long,
    val quantity: Double,
    val unit: String,
    val brand: String?,
    val variant: String?,
    val condition: String?,
    val note: String?,
    val status: ListItemStatus,
    val purchaseCount: Int,
    val createdAt: Long,
    val statusChangedAt: Long,
    val updatedAt: Long,
) {

    init {
        require(id.isNotBlank()) { "id non può essere vuoto" }
        require(productId > 0) { "productId deve essere maggiore di zero" }
        require(quantity.isFinite() && quantity > 0.0) {
            "quantity deve essere finita e maggiore di zero, ricevuto: $quantity"
        }
        require(unit.isNotBlank()) { "unit non può essere vuota" }
        require(
            if (status == ListItemStatus.PURCHASED) {
                purchaseCount >= 1
            } else {
                purchaseCount == 0
            },
        ) {
            "purchaseCount non coerente con lo stato $status: $purchaseCount"
        }
    }

    /**
     * Applica una transizione di stato.
     *
     * Se lo stato richiesto è già quello corrente la chiamata è idempotente e
     * restituisce la stessa istanza, senza modificare i timestamp.
     */
    fun transitionTo(target: ListItemStatus, at: Long): ListItem {
        if (status == target) return this

        require(status.canTransitionTo(target)) {
            "Transizione non ammessa: $status -> $target"
        }

        return when (target) {
            ListItemStatus.PURCHASED -> copy(
                status = ListItemStatus.PURCHASED,
                purchaseCount = maxOf(1, purchaseCount),
                statusChangedAt = at,
                updatedAt = at,
            )

            ListItemStatus.DELETED -> copy(
                status = ListItemStatus.DELETED,
                purchaseCount = 0,
                statusChangedAt = at,
                updatedAt = at,
            )

            ListItemStatus.TO_BUY -> copy(
                status = ListItemStatus.TO_BUY,
                purchaseCount = 0,
                statusChangedAt = at,
                updatedAt = at,
            )
        }
    }

    fun toEntity(): it.togo.app.data.database.entity.ListItemEntity =
        it.togo.app.data.database.entity.ListItemEntity(
            id = id,
            productId = productId,
            quantity = quantity,
            unit = unit,
            brand = brand,
            variant = variant,
            condition = condition,
            note = note,
            status = status.name,
            purchaseCount = purchaseCount,
            createdAt = createdAt,
            statusChangedAt = statusChangedAt,
            updatedAt = updatedAt,
        )

    companion object {

        /**
         * Factory per il caso nominale.
         *
         * `createdAt` è obbligatorio per non introdurre una nuova lettura
         * diretta dell'orologio nel dominio: il chiamante usa il `Clock`
         * iniettato (Story 8.2). `statusChangedAt` e `updatedAt` derivano da
         * `createdAt` salvo indicazione diversa.
         */
        fun create(
            productId: Long,
            quantity: Double,
            unit: String,
            createdAt: Long,
            id: String = UUID.randomUUID().toString(),
            brand: String? = null,
            variant: String? = null,
            condition: String? = null,
            note: String? = null,
            status: ListItemStatus = ListItemStatus.TO_BUY,
            purchaseCount: Int = if (status == ListItemStatus.PURCHASED) 1 else 0,
            statusChangedAt: Long = createdAt,
            updatedAt: Long = createdAt,
        ): ListItem = ListItem(
            id = id,
            productId = productId,
            quantity = quantity,
            unit = unit.trim(),
            brand = brand,
            variant = variant,
            condition = condition,
            note = note,
            status = status,
            purchaseCount = purchaseCount,
            createdAt = createdAt,
            statusChangedAt = statusChangedAt,
            updatedAt = updatedAt,
        )
    }
}
