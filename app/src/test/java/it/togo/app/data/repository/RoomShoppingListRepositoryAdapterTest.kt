package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.time.FakeClock
import it.togo.app.domain.model.ListItemStatus
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
 * Test dell'adapter legacy `ShoppingListRepository` sopra `LIST_ITEM`.
 * Test scritti, non eseguiti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomShoppingListRepositoryAdapterTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var listItemRepository: RoomListItemRepository
    private lateinit var repository: RoomShoppingListRepository
    private lateinit var clock: FakeClock

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = FakeClock(now = 1_000L)
        listItemRepository = RoomListItemRepository(db, db.listItemDao(), clock)
        repository = RoomShoppingListRepository(listItemRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert converte ShoppingItem in una riga LIST_ITEM TO_BUY`() = runBlocking {
        repository.insert(
            shoppingItem(
                id = "item-1",
                quantity = 0.0,
                unit = StandardUnit.KILOGRAM,
                brand = "Coop",
                variant = "Intero",
                createdAt = 100L,
                updatedAt = 150L,
            ),
        )

        val stored = db.listItemDao().getById("item-1")!!

        assertEquals(ListItemStatus.TO_BUY.name, stored.status)
        assertEquals(1.0, stored.quantity, 0.0)
        assertEquals("KILOGRAM", stored.unit)
        assertEquals("Coop", stored.brand)
        assertEquals("Intero", stored.variant)
        assertEquals(0, stored.purchaseCount)
        assertEquals(100L, stored.createdAt)
        assertEquals(150L, stored.updatedAt)

        val active = repository.getAllItems().first().single()
        assertFalse(active.isChecked)
        assertEquals(StandardUnit.KILOGRAM, active.unit)
    }

    @Test
    fun `update isChecked true porta la riga a PURCHASED`() = runBlocking {
        repository.insert(shoppingItem(id = "item-1", createdAt = 100L))
        val active = repository.getAllItems().first().single()
        clock.now = 5_000L

        repository.update(active.copy(isChecked = true))

        val stored = db.listItemDao().getById("item-1")!!
        assertEquals(ListItemStatus.PURCHASED.name, stored.status)
        assertEquals(1, stored.purchaseCount)
        assertEquals(5_000L, stored.statusChangedAt)
        assertEquals(5_000L, stored.updatedAt)
        assertTrue(repository.getAllItems().first().isEmpty())

        val checked = repository.getCheckedItems().first().single()
        assertTrue(checked.isChecked)
    }

    @Test
    fun `getAllItems non mostra le righe PURCHASED`() = runBlocking {
        repository.insert(shoppingItem(id = "active", createdAt = 100L))
        repository.insert(
            shoppingItem(
                id = "purchased",
                isChecked = true,
                createdAt = 100L,
                updatedAt = 100L,
            ),
        )

        val all = repository.getAllItems().first()

        assertEquals(listOf("active"), all.map { it.id })
        assertTrue(all.none { it.isChecked })
    }

    @Test
    fun `getCheckedItems mostra solo le righe PURCHASED`() = runBlocking {
        repository.insert(shoppingItem(id = "active", createdAt = 100L))
        repository.insert(
            shoppingItem(
                id = "purchased",
                isChecked = true,
                createdAt = 100L,
                updatedAt = 100L,
            ),
        )

        val checked = repository.getCheckedItems().first()

        assertEquals(listOf("purchased"), checked.map { it.id })
        assertTrue(checked.single().isChecked)
    }

    @Test
    fun `delete porta la riga a DELETED e la rimuove dagli attivi`() = runBlocking {
        repository.insert(shoppingItem(id = "item-1", createdAt = 100L))

        repository.delete("item-1")

        val stored = db.listItemDao().getById("item-1")!!
        assertEquals(ListItemStatus.DELETED.name, stored.status)
        assertTrue(repository.getAllItems().first().isEmpty())
    }

    @Test
    fun `checkout e un no-op e non cancella la riga PURCHASED`() = runBlocking {
        repository.insert(shoppingItem(id = "item-1", createdAt = 100L))
        val active = repository.getAllItems().first().single()
        repository.update(active.copy(isChecked = true))

        val result = repository.checkout(listOf("item-1"))

        assertTrue(result)
        assertNotNull(db.listItemDao().getById("item-1"))
        assertEquals(
            ListItemStatus.PURCHASED.name,
            db.listItemDao().getById("item-1")?.status,
        )
        assertEquals(1, repository.getCheckedItems().first().size)
        assertTrue(db.historyDao().getAll().first().isEmpty())
    }

    @Test
    fun `suggerimenti e ultimi attributi delegano al nuovo repository`() = runBlocking {
        repository.insert(
            shoppingItem(
                id = "active",
                quantity = 2.0,
                unit = StandardUnit.LITER,
                brand = "Coop",
                variant = "Intero",
                createdAt = 100L,
            ),
        )
        repository.insert(
            shoppingItem(
                id = "deleted",
                brand = "Da eliminare",
                variant = "Da eliminare",
                createdAt = 200L,
            ),
        )
        repository.delete("deleted")

        val suggestions = repository.getAttributeSuggestions().first()
        val attributes = repository.getLastAttributes(1L)
        val byProduct = repository.getByProductId(1L)

        assertEquals(setOf("Coop"), suggestions.brands.toSet())
        assertEquals(setOf("Intero"), suggestions.variants.toSet())
        assertEquals("Coop", attributes?.brand)
        assertEquals("Intero", attributes?.variant)
        assertNotNull(byProduct)
        assertEquals("active", byProduct?.id)
        assertEquals(StandardUnit.LITER, byProduct?.unit)
    }

    @Test
    fun `getByProductId ignora le righe acquistate`() = runBlocking {
        repository.insert(
            shoppingItem(
                id = "purchased",
                isChecked = true,
                createdAt = 100L,
                updatedAt = 100L,
            ),
        )

        assertNull(repository.getByProductId(1L))
    }

    private fun shoppingItem(
        id: String,
        productId: Long = 1L,
        quantity: Double = 1.0,
        unit: StandardUnit = StandardUnit.PIECE,
        brand: String? = null,
        variant: String? = null,
        isChecked: Boolean = false,
        createdAt: Long = 100L,
        updatedAt: Long = 100L,
    ): ShoppingItem = ShoppingItem(
        id = id,
        productId = productId,
        quantity = quantity,
        unit = unit,
        brand = brand,
        variant = variant,
        isChecked = isChecked,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
