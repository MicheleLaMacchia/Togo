package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.ShoppingItemDao
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.domain.model.StandardUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ShoppingItemDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: ShoppingItemDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.shoppingItemDao()
    }

    @Test
    fun `insert and getActive emits item`() = runBlocking {
        val item = ShoppingItemEntity(
            id = "test-1",
            productId = 1,
            quantity = 1.0,
            unit = StandardUnit.KILOGRAM,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        dao.insert(item)

        val activeItems = dao.getActive().first()
        assertEquals(1, activeItems.size)
        assertEquals("test-1", activeItems[0].id)
        assertEquals(1, activeItems[0].productId)
        assertEquals(StandardUnit.KILOGRAM, activeItems[0].unit)
    }

    @Test
    fun `update isChecked emits updated list`() = runBlocking {
        val item = ShoppingItemEntity(
            id = "test-2",
            productId = 2,
            quantity = 2.0,
            unit = StandardUnit.LITER,
            isChecked = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        dao.insert(item)

        var activeItems = dao.getActive().first()
        assertEquals(1, activeItems.size)
        assertFalse(activeItems[0].isChecked)

        dao.markAsChecked("test-2", System.currentTimeMillis())

        activeItems = dao.getActive().first()
        assertEquals(0, activeItems.size) // Item moved out of active
    }

    @Test
    fun `getActiveByProductId finds item`() = runBlocking {
        val item = ShoppingItemEntity(
            id = "test-3",
            productId = 3,
            quantity = 0.5,
            unit = StandardUnit.KILOGRAM,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        dao.insert(item)

        val found = dao.getActiveByProductId(3)
        assertNotNull(found)
        assertEquals("test-3", found?.id)

        val notFound = dao.getActiveByProductId(999)
        assertNull(notFound)
    }

    @Test
    fun `deleteById removes item`() = runBlocking {
        val item = ShoppingItemEntity(
            id = "test-4",
            productId = 4,
            quantity = 1.0,
            unit = StandardUnit.PIECE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        dao.insert(item)
        var activeItems = dao.getActive().first()
        assertEquals(1, activeItems.size)

        dao.deleteById("test-4")

        activeItems = dao.getActive().first()
        assertEquals(0, activeItems.size)
    }
}