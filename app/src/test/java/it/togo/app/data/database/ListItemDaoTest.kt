package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.ListItemDao
import it.togo.app.data.database.entity.ListItemEntity
import it.togo.app.domain.model.ListItemStatus
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
 * Test del DAO della lista unificata. Test scritti, non eseguiti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ListItemDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: ListItemDao

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.listItemDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert e query per stato restituiscono le righe attese`() = runBlocking {
        dao.insert(entity(id = "to-buy", status = ListItemStatus.TO_BUY))
        dao.insert(entity(id = "purchased", status = ListItemStatus.PURCHASED))
        dao.insert(entity(id = "deleted", status = ListItemStatus.DELETED))

        assertEquals(3, dao.observeAll().first().size)
        assertEquals("to-buy", dao.getById("to-buy")?.id)

        val active = dao.observeByStatus(ListItemStatus.TO_BUY.name).first()
        assertEquals(listOf("to-buy"), active.map { it.id })

        val purchased = dao.observeByStatus(ListItemStatus.PURCHASED.name).first()
        assertEquals(listOf("purchased"), purchased.map { it.id })

        val deleted = dao.observeByStatus(ListItemStatus.DELETED.name).first()
        assertEquals(listOf("deleted"), deleted.map { it.id })
    }

    @Test
    fun `observeBrands e observeVariants includono attivi e acquistati ma non eliminati`() =
        runBlocking {
            dao.insert(
                entity(
                    id = "active",
                    status = ListItemStatus.TO_BUY,
                    brand = "Coop",
                    variant = "Intero",
                ),
            )
            dao.insert(
                entity(
                    id = "purchased",
                    status = ListItemStatus.PURCHASED,
                    brand = "Barilla",
                    variant = "Integrale",
                ),
            )
            dao.insert(
                entity(
                    id = "deleted",
                    status = ListItemStatus.DELETED,
                    brand = "Da eliminare",
                    variant = "Da eliminare",
                ),
            )
            dao.insert(
                entity(
                    id = "blank",
                    status = ListItemStatus.TO_BUY,
                    brand = "   ",
                    variant = "",
                ),
            )

            val brands = dao.observeBrands().first().toSet()
            val variants = dao.observeVariants().first().toSet()

            assertEquals(setOf("Coop", "Barilla"), brands)
            assertEquals(setOf("Intero", "Integrale"), variants)
        }

    @Test
    fun `markActiveDeleted aggiorna solo le righe TO_BUY`() = runBlocking {
        dao.insert(entity(id = "active", status = ListItemStatus.TO_BUY))
        dao.insert(entity(id = "purchased", status = ListItemStatus.PURCHASED))

        val updated = dao.markActiveDeleted(at = 5_000L)

        assertEquals(1, updated)
        val active = dao.getById("active")!!
        assertEquals(ListItemStatus.DELETED.name, active.status)
        assertEquals(0, active.purchaseCount)
        assertEquals(5_000L, active.statusChangedAt)
        assertEquals(5_000L, active.updatedAt)

        val purchased = dao.getById("purchased")!!
        assertEquals(ListItemStatus.PURCHASED.name, purchased.status)
        assertEquals(1, purchased.purchaseCount)
        assertEquals(100L, purchased.statusChangedAt)
    }

    @Test
    fun `deleteDeleted elimina solo le righe DELETED`() = runBlocking {
        dao.insert(entity(id = "active", status = ListItemStatus.TO_BUY))
        dao.insert(entity(id = "purchased", status = ListItemStatus.PURCHASED))
        dao.insert(entity(id = "deleted", status = ListItemStatus.DELETED))

        val deleted = dao.deleteDeleted()

        assertEquals(1, deleted)
        assertNull(dao.getById("deleted"))
        assertTrue(dao.getById("active") != null)
        assertTrue(dao.getById("purchased") != null)
        assertEquals(2, dao.observeAll().first().size)
    }

    private fun entity(
        id: String,
        status: ListItemStatus,
        brand: String? = null,
        variant: String? = null,
    ) = ListItemEntity(
        id = id,
        productId = 1L,
        quantity = 1.0,
        unit = "PIECE",
        brand = brand,
        variant = variant,
        condition = null,
        note = null,
        status = status.name,
        purchaseCount = if (status == ListItemStatus.PURCHASED) 1 else 0,
        createdAt = 100L,
        statusChangedAt = 100L,
        updatedAt = 100L,
    )
}
