package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.ListItemDao
import it.togo.app.data.time.FakeClock
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Test del nuovo repository `LIST_ITEM`. Test scritti, non eseguiti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomListItemRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: ListItemDao
    private lateinit var clock: FakeClock
    private lateinit var repository: RoomListItemRepository

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.listItemDao()
        clock = FakeClock(now = 1_000L)
        repository = RoomListItemRepository(db, dao, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `add inserisce una nuova riga TO_BUY`() = runBlocking {
        val item = item(id = "new", quantity = 2.0, createdAt = 100L)

        val added = repository.add(item)

        assertEquals(item, added)
        assertEquals(item, dao.getById("new")?.toDomain())
        assertEquals(listOf("new"), repository.observeActive().first().map { it.id })
    }

    @Test
    fun `add compatibile somma la quantita e mantiene una sola riga attiva`() = runBlocking {
        repository.add(
            item(
                id = "first",
                quantity = 1.0,
                unit = "KG",
                brand = "Coop",
                variant = "Intero",
                createdAt = 100L,
            ),
        )

        clock.now = 200L
        val merged = repository.add(
            item(
                id = "second",
                quantity = 2.0,
                unit = " kg ",
                brand = " coop ",
                variant = "intero",
                condition = "",
                createdAt = 300L,
            ),
        )

        assertEquals("first", merged.id)
        assertEquals(3.0, merged.quantity, 0.0)
        assertEquals(200L, merged.updatedAt)
        assertEquals(1, repository.observeActive().first().size)
        assertNull(dao.getById("second"))
    }

    @Test
    fun `add con unita diversa crea due righe`() = runBlocking {
        repository.add(item(id = "kg", quantity = 1.0, unit = "KG"))
        repository.add(item(id = "litri", quantity = 2.0, unit = "L"))

        assertEquals(
            setOf("kg", "litri"),
            repository.observeActive().first().map { it.id }.toSet(),
        )
    }

    @Test
    fun `add non fonde mai righe PURCHASED o DELETED`() = runBlocking {
        dao.insert(
            item(
                id = "purchased",
                status = ListItemStatus.PURCHASED,
                createdAt = 100L,
                statusChangedAt = 100L,
                updatedAt = 100L,
            ).toEntity(),
        )
        dao.insert(
            item(
                id = "deleted",
                status = ListItemStatus.DELETED,
                createdAt = 100L,
                statusChangedAt = 100L,
                updatedAt = 100L,
            ).toEntity(),
        )

        repository.add(item(id = "new-active", quantity = 2.0, createdAt = 200L))

        val all = dao.observeAll().first()
        assertEquals(3, all.size)
        assertEquals(
            listOf("new-active"),
            repository.observeActive().first().map { it.id },
        )
        assertEquals(
            ListItemStatus.PURCHASED.name,
            dao.getById("purchased")?.status,
        )
        assertEquals(
            ListItemStatus.DELETED.name,
            dao.getById("deleted")?.status,
        )
    }

    @Test
    fun `updateDetails aggiorna solo i dettagli di una riga TO_BUY`() = runBlocking {
        repository.add(
            item(
                id = "active",
                quantity = 1.0,
                unit = "KG",
                brand = "Coop",
                variant = "Intero",
                createdAt = 100L,
            ),
        )
        clock.now = 4_000L
        val current = dao.getById("active")!!.toDomain()

        val updated = repository.updateDetails(
            current.copy(
                quantity = 3.0,
                unit = "L",
                brand = "Barilla",
                variant = "Integrale",
                condition = "Fresco",
                note = "Nota",
            ),
        )!!

        assertEquals(3.0, updated.quantity, 0.0)
        assertEquals("L", updated.unit)
        assertEquals("Barilla", updated.brand)
        assertEquals("Integrale", updated.variant)
        assertEquals("Fresco", updated.condition)
        assertEquals("Nota", updated.note)
        assertEquals(ListItemStatus.TO_BUY, updated.status)
        assertEquals(0, updated.purchaseCount)
        assertEquals(100L, updated.createdAt)
        assertEquals(100L, updated.statusChangedAt)
        assertEquals(4_000L, updated.updatedAt)
    }

    @Test
    fun `updateDetails restituisce null per una riga non TO_BUY`() = runBlocking {
        repository.add(item(id = "active", createdAt = 100L))
        clock.now = 2_000L
        val purchased = repository.complete("active")!!
        clock.now = 5_000L

        assertNull(repository.updateDetails(purchased.copy(quantity = 2.0)))
        assertEquals(1.0, dao.getById("active")?.quantity ?: 0.0, 0.0)
    }

    @Test
    fun `complete porta TO_BUY a PURCHASED con timestamp dal clock`() = runBlocking {
        repository.add(item(id = "active", createdAt = 100L))
        clock.now = 5_000L

        val completed = repository.complete("active")!!

        assertEquals(ListItemStatus.PURCHASED, completed.status)
        assertEquals(1, completed.purchaseCount)
        assertEquals(100L, completed.createdAt)
        assertEquals(5_000L, completed.statusChangedAt)
        assertEquals(5_000L, completed.updatedAt)
        assertTrue(repository.observeActive().first().isEmpty())
        assertEquals(listOf("active"), repository.observePurchased().first().map { it.id })
    }

    @Test
    fun `complete due volte e idempotente e non cambia i timestamp`() = runBlocking {
        repository.add(item(id = "active", createdAt = 100L))
        clock.now = 5_000L
        val first = repository.complete("active")!!
        clock.now = 9_000L

        val second = repository.complete("active")!!

        assertEquals(first, second)
        assertEquals(5_000L, second.statusChangedAt)
        assertEquals(5_000L, second.updatedAt)
    }

    @Test
    fun `complete di una riga DELETED restituisce null`() = runBlocking {
        repository.add(item(id = "deleted", createdAt = 100L))
        clock.now = 2_000L
        repository.delete("deleted")
        clock.now = 3_000L

        assertNull(repository.complete("deleted"))
        assertEquals(ListItemStatus.DELETED, dao.getById("deleted")?.toDomain()?.status)
    }

    @Test
    fun `delete porta TO_BUY a DELETED ed e idempotente`() = runBlocking {
        repository.add(item(id = "active", createdAt = 100L))
        clock.now = 2_000L

        val deleted = repository.delete("active")!!

        assertEquals(ListItemStatus.DELETED, deleted.status)
        assertEquals(0, deleted.purchaseCount)
        assertEquals(2_000L, deleted.statusChangedAt)
        assertEquals(2_000L, deleted.updatedAt)

        clock.now = 3_000L
        val again = repository.delete("active")!!

        assertEquals(deleted, again)
        assertEquals(2_000L, again.statusChangedAt)
        assertEquals(2_000L, again.updatedAt)
    }

    @Test
    fun `delete di una riga PURCHASED restituisce null`() = runBlocking {
        repository.add(item(id = "active", createdAt = 100L))
        clock.now = 2_000L
        repository.complete("active")
        clock.now = 3_000L

        assertNull(repository.delete("active"))
        assertEquals(ListItemStatus.PURCHASED, dao.getById("active")?.toDomain()?.status)
    }

    @Test
    fun `restore riporta DELETED a TO_BUY sulla stessa identita`() = runBlocking {
        repository.add(item(id = "to-restore", createdAt = 100L))
        clock.now = 2_000L
        repository.delete("to-restore")
        clock.now = 6_000L

        val restored = repository.restore("to-restore")!!

        assertEquals("to-restore", restored.id)
        assertEquals(ListItemStatus.TO_BUY, restored.status)
        assertEquals(0, restored.purchaseCount)
        assertEquals(100L, restored.createdAt)
        assertEquals(6_000L, restored.statusChangedAt)
        assertEquals(6_000L, restored.updatedAt)
    }

    @Test
    fun `restore con riga attiva compatibile fonde la quantita e rimuove la DELETED`() =
        runBlocking {
            dao.insert(
                item(
                    id = "active",
                    quantity = 2.0,
                    unit = "KG",
                    brand = "Coop",
                    variant = "Intero",
                    createdAt = 100L,
                ).toEntity(),
            )
            dao.insert(
                item(
                    id = "deleted",
                    quantity = 3.0,
                    unit = " kg ",
                    brand = " coop ",
                    variant = "intero",
                    status = ListItemStatus.DELETED,
                    createdAt = 200L,
                    statusChangedAt = 300L,
                    updatedAt = 300L,
                ).toEntity(),
            )
            clock.now = 7_000L

            val restored = repository.restore("deleted")!!

            assertEquals("active", restored.id)
            assertEquals(5.0, restored.quantity, 0.0)
            assertEquals(7_000L, restored.updatedAt)
            assertNull(dao.getById("deleted"))
            assertEquals(1, dao.getActiveByProductId(1L).size)
        }

    @Test
    fun `restore con unita diversa non fonde le righe`() = runBlocking {
        dao.insert(item(id = "active", quantity = 2.0, unit = "KG").toEntity())
        dao.insert(
            item(
                id = "deleted",
                quantity = 3.0,
                unit = "L",
                status = ListItemStatus.DELETED,
                createdAt = 200L,
                statusChangedAt = 300L,
                updatedAt = 300L,
            ).toEntity(),
        )
        clock.now = 7_000L

        val restored = repository.restore("deleted")!!

        assertEquals("deleted", restored.id)
        assertEquals(ListItemStatus.TO_BUY, restored.status)
        assertEquals(2, repository.observeActive().first().size)
    }

    @Test
    fun `reAdd crea una nuova TO_BUY e lascia la PURCHASED immutata`() = runBlocking {
        repository.add(
            item(
                id = "purchased",
                quantity = 2.0,
                unit = "KG",
                brand = "Coop",
                variant = "Intero",
                createdAt = 100L,
            ),
        )
        clock.now = 2_000L
        repository.complete("purchased")
        val purchasedBefore = dao.getById("purchased")!!
        clock.now = 5_000L

        val readded = repository.reAdd("purchased")!!

        assertNotEquals("purchased", readded.id)
        assertEquals(ListItemStatus.TO_BUY, readded.status)
        assertEquals(0, readded.purchaseCount)
        assertEquals(2.0, readded.quantity, 0.0)
        assertEquals("KG", readded.unit)
        assertEquals("Coop", readded.brand)
        assertEquals("Intero", readded.variant)
        assertEquals(5_000L, readded.createdAt)
        assertEquals(5_000L, readded.statusChangedAt)
        assertEquals(5_000L, readded.updatedAt)
        assertEquals(purchasedBefore, dao.getById("purchased"))
    }

    @Test
    fun `reAdd con riga attiva compatibile somma la quantita`() = runBlocking {
        repository.add(item(id = "purchased", quantity = 2.0, createdAt = 100L))
        clock.now = 2_000L
        repository.complete("purchased")
        repository.add(item(id = "active", quantity = 1.0, createdAt = 3_000L))
        val purchasedBefore = dao.getById("purchased")!!
        clock.now = 5_000L

        val readded = repository.reAdd("purchased")!!

        assertEquals("active", readded.id)
        assertEquals(3.0, readded.quantity, 0.0)
        assertEquals(5_000L, readded.updatedAt)
        assertEquals(purchasedBefore, dao.getById("purchased"))
        assertEquals(1, repository.observeActive().first().size)
    }

    @Test
    fun `clearActive porta solo le righe TO_BUY a DELETED`() = runBlocking {
        dao.insert(item(id = "active", status = ListItemStatus.TO_BUY).toEntity())
        dao.insert(
            item(
                id = "purchased",
                status = ListItemStatus.PURCHASED,
                purchaseCount = 2,
                createdAt = 200L,
                statusChangedAt = 300L,
                updatedAt = 300L,
            ).toEntity(),
        )
        dao.insert(
            item(
                id = "deleted",
                status = ListItemStatus.DELETED,
                createdAt = 400L,
                statusChangedAt = 500L,
                updatedAt = 500L,
            ).toEntity(),
        )
        clock.now = 8_000L

        val updated = repository.clearActive()

        assertEquals(1, updated)
        val active = dao.getById("active")!!
        assertEquals(ListItemStatus.DELETED.name, active.status)
        assertEquals(8_000L, active.statusChangedAt)

        val purchased = dao.getById("purchased")!!
        assertEquals(ListItemStatus.PURCHASED.name, purchased.status)
        assertEquals(300L, purchased.statusChangedAt)

        val alreadyDeleted = dao.getById("deleted")!!
        assertEquals(500L, alreadyDeleted.statusChangedAt)
        assertEquals(500L, alreadyDeleted.updatedAt)
    }

    @Test
    fun `purgeDeleted elimina solo le righe DELETED`() = runBlocking {
        dao.insert(item(id = "active", status = ListItemStatus.TO_BUY).toEntity())
        dao.insert(item(id = "purchased", status = ListItemStatus.PURCHASED).toEntity())
        dao.insert(item(id = "deleted", status = ListItemStatus.DELETED).toEntity())

        val deleted = repository.purgeDeleted()

        assertEquals(1, deleted)
        assertNull(dao.getById("deleted"))
        assertTrue(dao.getById("active") != null)
        assertTrue(dao.getById("purchased") != null)
    }

    @Test
    fun `observePurchased restituisce solo le righe PURCHASED`() = runBlocking {
        dao.insert(item(id = "active", status = ListItemStatus.TO_BUY).toEntity())
        dao.insert(item(id = "purchased", status = ListItemStatus.PURCHASED).toEntity())
        dao.insert(item(id = "deleted", status = ListItemStatus.DELETED).toEntity())

        val purchased = repository.observePurchased().first()

        assertEquals(listOf("purchased"), purchased.map { it.id })
    }

    @Test
    fun `la somma di purchaseCount preserva i valori legacy`() = runBlocking {
        repository.add(
            item(
                id = "legacy-7",
                status = ListItemStatus.PURCHASED,
                purchaseCount = 7,
                createdAt = 100L,
            ),
        )
        repository.add(
            item(
                id = "legacy-2",
                status = ListItemStatus.PURCHASED,
                purchaseCount = 2,
                createdAt = 200L,
            ),
        )
        repository.add(
            item(
                id = "active",
                status = ListItemStatus.TO_BUY,
                createdAt = 300L,
            ),
        )

        val total = repository.observePurchased().first().sumOf { it.purchaseCount }

        assertEquals(9, total)
    }

    @Test
    fun `observeAttributeSuggestions esclude DELETED e deduplica case-insensitive`() =
        runBlocking {
            dao.insert(
                item(
                    id = "active",
                    status = ListItemStatus.TO_BUY,
                    brand = "Coop",
                    variant = "Intero",
                ).toEntity(),
            )
            dao.insert(
                item(
                    id = "purchased",
                    status = ListItemStatus.PURCHASED,
                    brand = "coop",
                    variant = "intero",
                ).toEntity(),
            )
            dao.insert(
                item(
                    id = "purchased-2",
                    status = ListItemStatus.PURCHASED,
                    brand = "Barilla",
                    variant = "Integrale",
                ).toEntity(),
            )
            dao.insert(
                item(
                    id = "deleted",
                    status = ListItemStatus.DELETED,
                    brand = "Da eliminare",
                    variant = "Da eliminare",
                ).toEntity(),
            )

            val suggestions = repository.observeAttributeSuggestions().first()

            assertEquals(
                setOf("coop", "barilla"),
                suggestions.brands.map { it.lowercase(Locale.ROOT) }.toSet(),
            )
            assertEquals(
                setOf("intero", "integrale"),
                suggestions.variants.map { it.lowercase(Locale.ROOT) }.toSet(),
            )
            assertEquals(
                suggestions.brands.sortedBy { it.lowercase(Locale.ROOT) },
                suggestions.brands,
            )
            assertTrue(suggestions.brands.none { it.equals("Da eliminare", ignoreCase = true) })
        }

    @Test
    fun `getLastAttributes preferisce la riga attiva e altrimenti l'ultima PURCHASED`() =
        runBlocking {
            dao.insert(
                item(
                    id = "active-old",
                    productId = 1L,
                    brand = "ActiveOld",
                    variant = "Vecchia",
                    createdAt = 100L,
                    updatedAt = 200L,
                ).toEntity(),
            )
            dao.insert(
                item(
                    id = "active-new",
                    productId = 1L,
                    brand = "ActiveNew",
                    variant = "Nuova",
                    createdAt = 150L,
                    updatedAt = 300L,
                ).toEntity(),
            )
            dao.insert(
                item(
                    id = "purchased-1",
                    productId = 1L,
                    brand = "Purchased",
                    variant = "Storico",
                    status = ListItemStatus.PURCHASED,
                    createdAt = 100L,
                    statusChangedAt = 900L,
                    updatedAt = 100L,
                ).toEntity(),
            )
            dao.insert(
                item(
                    id = "purchased-2",
                    productId = 2L,
                    brand = "SoloStorico",
                    variant = "Recente",
                    status = ListItemStatus.PURCHASED,
                    createdAt = 100L,
                    statusChangedAt = 500L,
                    updatedAt = 100L,
                ).toEntity(),
            )

            val active = repository.getLastAttributes(1L)!!
            val purchasedOnly = repository.getLastAttributes(2L)!!

            assertEquals("ActiveNew", active.brand)
            assertEquals("Nuova", active.variant)
            assertEquals("SoloStorico", purchasedOnly.brand)
            assertEquals("Recente", purchasedOnly.variant)
            assertNull(repository.getLastAttributes(3L))
        }

    private fun item(
        id: String,
        productId: Long = 1L,
        quantity: Double = 1.0,
        unit: String = "KG",
        brand: String? = "Coop",
        variant: String? = "Intero",
        condition: String? = null,
        note: String? = null,
        status: ListItemStatus = ListItemStatus.TO_BUY,
        purchaseCount: Int = if (status == ListItemStatus.PURCHASED) 1 else 0,
        createdAt: Long = 100L,
        statusChangedAt: Long = createdAt,
        updatedAt: Long = createdAt,
    ): ListItem = ListItem.create(
        id = id,
        productId = productId,
        quantity = quantity,
        unit = unit,
        createdAt = createdAt,
        brand = brand,
        variant = variant,
        condition = condition,
        note = note,
        status = status,
        purchaseCount = purchaseCount,
        statusChangedAt = statusChangedAt,
        updatedAt = updatedAt,
    )
}
