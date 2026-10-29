package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.time.FakeClock
import it.togo.app.domain.model.HistoricalItem
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.model.StandardUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test dell'adapter legacy `HistoryRepository` sopra `LIST_ITEM`.
 * Test scritti, non eseguiti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListItemHistoryRepositoryAdapterTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var listItemRepository: RoomListItemRepository
    private lateinit var adapter: ListItemHistoryRepositoryAdapter

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        listItemRepository = RoomListItemRepository(
            database = db,
            dao = db.listItemDao(),
            clock = FakeClock(now = 1_000L),
        )
        adapter = ListItemHistoryRepositoryAdapter(listItemRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getAll mappa solo le righe PURCHASED`() = runBlocking {
        db.listItemDao().insert(
            item(
                id = "active",
                status = ListItemStatus.TO_BUY,
                createdAt = 300L,
            ).toEntity(),
        )
        db.listItemDao().insert(
            item(
                id = "purchased",
                status = ListItemStatus.PURCHASED,
                quantity = 2.5,
                unit = "KILOGRAM",
                brand = "Coop",
                variant = "Intero",
                condition = "Fresco",
                purchaseCount = 4,
                createdAt = 100L,
                statusChangedAt = 500L,
                updatedAt = 500L,
            ).toEntity(),
        )
        db.listItemDao().insert(
            item(
                id = "deleted",
                status = ListItemStatus.DELETED,
                createdAt = 200L,
            ).toEntity(),
        )

        val history = adapter.getAll().first()

        assertEquals(1, history.size)
        val mapped = history.single()
        assertEquals("purchased", mapped.id)
        assertEquals(1L, mapped.productId)
        assertEquals(2.5, mapped.lastQuantity, 0.0)
        assertEquals(StandardUnit.KILOGRAM, mapped.lastUnit)
        assertEquals("Coop", mapped.brand)
        assertEquals("Intero", mapped.variant)
        assertEquals("Fresco", mapped.condition)
        assertEquals(500L, mapped.purchasedAt)
        assertEquals(4, mapped.purchaseCount)
    }

    @Test
    fun `getAll non mostra le righe DELETED`() = runBlocking {
        db.listItemDao().insert(
            item(
                id = "deleted",
                status = ListItemStatus.DELETED,
                createdAt = 100L,
                statusChangedAt = 900L,
                updatedAt = 900L,
            ).toEntity(),
        )

        assertTrue(adapter.getAll().first().isEmpty())
    }

    @Test
    fun `getByProductId restituisce l'ultima PURCHASED`() = runBlocking {
        db.listItemDao().insert(
            item(
                id = "older",
                status = ListItemStatus.PURCHASED,
                createdAt = 100L,
                statusChangedAt = 200L,
                updatedAt = 200L,
            ).toEntity(),
        )
        db.listItemDao().insert(
            item(
                id = "newer",
                status = ListItemStatus.PURCHASED,
                createdAt = 150L,
                statusChangedAt = 800L,
                updatedAt = 800L,
            ).toEntity(),
        )
        db.listItemDao().insert(
            item(
                id = "active",
                status = ListItemStatus.TO_BUY,
                createdAt = 950L,
                updatedAt = 950L,
            ).toEntity(),
        )

        assertEquals("newer", adapter.getByProductId(1L)?.id)
        assertEquals(800L, adapter.getByProductId(1L)?.purchasedAt)
        assertNull(adapter.getByProductId(2L))
    }

    @Test
    fun `insert scrive una riga PURCHASED in LIST_ITEM`() = runBlocking {
        val historical = HistoricalItem(
            id = "history-new",
            productId = 1L,
            lastQuantity = 0.0,
            lastUnit = StandardUnit.LITER,
            brand = "Coop",
            variant = "Intero",
            condition = "Fresco",
            purchasedAt = 900L,
            purchaseCount = 0,
        )

        adapter.insert(historical)

        val stored = db.listItemDao().getById("history-new")!!
        assertEquals(ListItemStatus.PURCHASED.name, stored.status)
        assertEquals(1.0, stored.quantity, 0.0)
        assertEquals("LITER", stored.unit)
        assertEquals("Coop", stored.brand)
        assertEquals("Intero", stored.variant)
        assertEquals("Fresco", stored.condition)
        assertEquals(1, stored.purchaseCount)
        assertEquals(900L, stored.createdAt)
        assertEquals(900L, stored.statusChangedAt)
        assertEquals(900L, stored.updatedAt)

        val mapped = adapter.getAll().first().single()
        assertEquals(1.0, mapped.lastQuantity, 0.0)
        assertEquals(StandardUnit.LITER, mapped.lastUnit)
        assertEquals(900L, mapped.purchasedAt)
        assertEquals(1, mapped.purchaseCount)
    }

    @Test
    fun `upsertHistorical sostituisce la riga esistente restando PURCHASED`() = runBlocking {
        val first = HistoricalItem(
            id = "history-upsert",
            productId = 1L,
            lastQuantity = 1.0,
            lastUnit = StandardUnit.KILOGRAM,
            purchasedAt = 100L,
            purchaseCount = 1,
        )
        adapter.insert(first)

        val updated = first.copy(
            lastQuantity = 2.0,
            lastUnit = StandardUnit.LITER,
            brand = "Nuova marca",
            purchasedAt = 500L,
            purchaseCount = 3,
        )
        adapter.upsertHistorical(updated)

        val stored = db.listItemDao().getById("history-upsert")!!
        assertEquals(2.0, stored.quantity, 0.0)
        assertEquals("LITER", stored.unit)
        assertEquals("Nuova marca", stored.brand)
        assertEquals(3, stored.purchaseCount)
        assertEquals(500L, stored.createdAt)
        assertEquals(500L, stored.statusChangedAt)
        assertEquals(500L, stored.updatedAt)

        val all = db.listItemDao().observeAll().first()
        assertEquals(1, all.size)
    }

    private fun item(
        id: String,
        status: ListItemStatus,
        productId: Long = 1L,
        quantity: Double = 1.0,
        unit: String = "PIECE",
        brand: String? = null,
        variant: String? = null,
        condition: String? = null,
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
        status = status,
        purchaseCount = purchaseCount,
        statusChangedAt = statusChangedAt,
        updatedAt = updatedAt,
    )
}
