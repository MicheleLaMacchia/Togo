package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.HistoryDao
import it.togo.app.data.database.entity.HistoricalItemEntity
import it.togo.app.domain.model.StandardUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: HistoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.historyDao()
    }

    @Test
    fun `insert and getAll emits item`() = runBlocking {
        val item = HistoricalItemEntity(
            id = "hist-1",
            productId = 10,
            lastQuantity = 1.5,
            lastUnit = StandardUnit.KILOGRAM,
            purchasedAt = System.currentTimeMillis(),
            purchaseCount = 1
        )

        dao.insert(item)

        val allItems = dao.getAll().first()
        assertEquals(1, allItems.size)
        assertEquals("hist-1", allItems[0].id)
        assertEquals(10, allItems[0].productId)
    }

    @Test
    fun `upsertHistorical increments purchaseCount`() = runBlocking {
        val item = HistoricalItemEntity(
            id = "hist-2",
            productId = 20,
            lastQuantity = 1.0,
            lastUnit = StandardUnit.LITER,
            purchasedAt = System.currentTimeMillis(),
            purchaseCount = 1
        )

        dao.insert(item)

        var allItems = dao.getAll().first()
        assertEquals(1, allItems[0].purchaseCount)

        // Upsert with same ID should increment purchaseCount
        val updated = item.copy(
            lastQuantity = 2.0,
            purchasedAt = System.currentTimeMillis(),
            purchaseCount = 2
        )
        dao.upsertHistorical(updated)

        allItems = dao.getAll().first()
        assertEquals(2, allItems[0].purchaseCount)
        assertEquals(2.0, allItems[0].lastQuantity, 0.001)
    }

    @Test
    fun `getByProductId finds item`() = runBlocking {
        val item = HistoricalItemEntity(
            id = "hist-3",
            productId = 30,
            lastQuantity = 0.5,
            lastUnit = StandardUnit.KILOGRAM,
            purchasedAt = System.currentTimeMillis(),
            purchaseCount = 3
        )

        dao.insert(item)

        val found = dao.getByProductId(30)
        assertNotNull(found)
        assertEquals("hist-3", found?.id)

        val notFound = dao.getByProductId(999)
        assertNull(notFound)
    }
}