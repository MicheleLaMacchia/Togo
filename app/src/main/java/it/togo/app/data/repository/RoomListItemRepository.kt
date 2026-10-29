package it.togo.app.data.repository

import androidx.room.withTransaction
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.ListItemDao
import it.togo.app.data.database.entity.ListItemEntity
import it.togo.app.domain.model.AttributeSuggestions
import it.togo.app.domain.model.ItemAttributes
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.repository.ListItemRepository
import it.togo.app.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.UUID

/**
 * Implementazione Room di [ListItemRepository].
 *
 * Tutte le operazioni che leggono e poi scrivono (merge compreso) girano in una
 * transazione. I timestamp di transizione arrivano dal [Clock] iniettato: il
 * repository non legge mai direttamente l'orologio di sistema.
 */
class RoomListItemRepository(
    private val database: TogoDatabase,
    private val dao: ListItemDao,
    private val clock: Clock,
) : ListItemRepository {

    override fun observeAll(): Flow<List<ListItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActive(): Flow<List<ListItem>> =
        dao.observeByStatus(ListItemStatus.TO_BUY.name)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observePurchased(): Flow<List<ListItem>> =
        dao.observeByStatusNewest(ListItemStatus.PURCHASED.name)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeDeleted(): Flow<List<ListItem>> =
        dao.observeByStatus(ListItemStatus.DELETED.name)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): ListItem? =
        dao.getById(id)?.toDomain()

    override suspend fun getActiveByProductId(productId: Long): ListItem? =
        dao.getActiveByProductId(productId).firstOrNull()?.toDomain()

    override suspend fun getLatestPurchasedByProductId(productId: Long): ListItem? =
        dao.getLatestPurchasedByProductId(productId)?.toDomain()

    override suspend fun add(item: ListItem): ListItem = database.withTransaction {
        val incoming = item.toEntity()

        if (item.status == ListItemStatus.TO_BUY) {
            val compatible = dao.getActiveByProductId(item.productId)
                .firstOrNull { it.hasSameMergeKeyAs(incoming) }

            if (compatible != null) {
                val merged = compatible.copy(
                    quantity = compatible.quantity + item.quantity,
                    updatedAt = clock.nowMillis(),
                )
                dao.update(merged)
                return@withTransaction merged.toDomain()
            }
        }

        dao.insert(incoming)
        item
    }

    override suspend fun updateDetails(item: ListItem): ListItem? = database.withTransaction {
        val existing = dao.getById(item.id) ?: return@withTransaction null
        if (existing.status != ListItemStatus.TO_BUY.name) return@withTransaction null

        val updated = existing.copy(
            quantity = item.quantity,
            unit = item.unit.trim(),
            brand = item.brand,
            variant = item.variant,
            condition = item.condition,
            note = item.note,
            updatedAt = clock.nowMillis(),
        )
        dao.update(updated)
        updated.toDomain()
    }

    override suspend fun complete(id: String): ListItem? = database.withTransaction {
        val existing = dao.getById(id) ?: return@withTransaction null
        val current = existing.toDomain()

        when (current.status) {
            ListItemStatus.PURCHASED -> current
            ListItemStatus.DELETED -> null
            ListItemStatus.TO_BUY -> {
                val updated = current.transitionTo(
                    target = ListItemStatus.PURCHASED,
                    at = clock.nowMillis(),
                )
                dao.update(updated.toEntity())
                updated
            }
        }
    }

    override suspend fun delete(id: String): ListItem? = database.withTransaction {
        val existing = dao.getById(id) ?: return@withTransaction null
        val current = existing.toDomain()

        when (current.status) {
            ListItemStatus.DELETED -> current
            ListItemStatus.PURCHASED -> null
            ListItemStatus.TO_BUY -> {
                val updated = current.transitionTo(
                    target = ListItemStatus.DELETED,
                    at = clock.nowMillis(),
                )
                dao.update(updated.toEntity())
                updated
            }
        }
    }

    override suspend fun restore(id: String): ListItem? = database.withTransaction {
        val existing = dao.getById(id) ?: return@withTransaction null
        val current = existing.toDomain()

        when (current.status) {
            ListItemStatus.TO_BUY -> current
            ListItemStatus.PURCHASED -> null
            ListItemStatus.DELETED -> {
                val compatible = dao.getActiveByProductId(current.productId)
                    .firstOrNull { it.hasSameMergeKeyAs(existing) }

                if (compatible != null) {
                    val merged = compatible.copy(
                        quantity = compatible.quantity + current.quantity,
                        updatedAt = clock.nowMillis(),
                    )
                    dao.update(merged)
                    dao.deleteById(existing.id)
                    merged.toDomain()
                } else {
                    val restored = current.transitionTo(
                        target = ListItemStatus.TO_BUY,
                        at = clock.nowMillis(),
                    )
                    dao.update(restored.toEntity())
                    restored
                }
            }
        }
    }

    override suspend fun reAdd(purchasedId: String): ListItem? = database.withTransaction {
        val existing = dao.getById(purchasedId) ?: return@withTransaction null
        val purchased = existing.toDomain()
        if (purchased.status != ListItemStatus.PURCHASED) return@withTransaction null

        val compatible = dao.getActiveByProductId(purchased.productId)
            .firstOrNull { it.hasSameMergeKeyAs(existing) }

        if (compatible != null) {
            val merged = compatible.copy(
                quantity = compatible.quantity + purchased.quantity,
                updatedAt = clock.nowMillis(),
            )
            dao.update(merged)
            return@withTransaction merged.toDomain()
        }

        val now = clock.nowMillis()
        val readded = purchased.copy(
            id = UUID.randomUUID().toString(),
            status = ListItemStatus.TO_BUY,
            purchaseCount = 0,
            createdAt = now,
            statusChangedAt = now,
            updatedAt = now,
        )
        dao.insert(readded.toEntity())
        readded
    }

    override suspend fun clearActive(): Int = database.withTransaction {
        dao.markActiveDeleted(clock.nowMillis())
    }

    override suspend fun purgeDeleted(): Int = database.withTransaction {
        dao.deleteDeleted()
    }

    override fun observeAttributeSuggestions(): Flow<AttributeSuggestions> =
        combine(
            dao.observeBrands(),
            dao.observeVariants(),
        ) { brands, variants ->
            AttributeSuggestions(
                brands = mergeValues(brands),
                variants = mergeValues(variants),
            )
        }

    override suspend fun getLastAttributes(productId: Long): ItemAttributes? {
        // La lista attiva riflette l'ultima intenzione dell'utente; solo in
        // mancanza si guarda l'acquisto più recente.
        val active = dao.getActiveByProductId(productId).maxByOrNull { it.updatedAt }
        val source = active
            ?: dao.getLatestPurchasedByProductId(productId)
            ?: return null

        return ItemAttributes(
            brand = source.brand,
            variant = source.variant,
            condition = source.condition,
        ).takeUnless { it.isEmpty }
    }

    /**
     * Chiave di merge: stesso prodotto e stessa unità (trim + case-insensitive).
     * Marca, variante e conservazione sono confrontate ignorando spazi e case;
     * `null` e stringa vuota sono equivalenti. Le note non partecipano.
     */
    private fun ListItemEntity.hasSameMergeKeyAs(other: ListItemEntity): Boolean =
        productId == other.productId &&
            normalize(unit) == normalize(other.unit) &&
            normalize(brand) == normalize(other.brand) &&
            normalize(variant) == normalize(other.variant) &&
            normalize(condition) == normalize(other.condition)

    private fun normalize(value: String?): String? =
        value?.trim()?.lowercase(Locale.ROOT)?.ifEmpty { null }

    /**
     * Deduplica case-insensitive e ordina alfabeticamente i suggerimenti.
     * La query SQL esclude già `DELETED` e i valori vuoti.
     */
    private fun mergeValues(values: List<String>): List<String> =
        values.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedBy { it.lowercase(Locale.ROOT) }
            .toList()
}
