package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.entity.ActionItemEntity
import it.togo.app.data.database.entity.HistoricalItemEntity
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.domain.model.StandardUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomShoppingListRepositoryCheckoutTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var repository: RoomShoppingListRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomShoppingListRepository(
            db.shoppingItemDao(),
            db.historyDao(),
            db.actionItemDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `checkout conserva gli attributi delle varianti`() = runBlocking {
        val now = System.currentTimeMillis()
        db.shoppingItemDao().insert(
            shoppingItem(
                id = "coop",
                brand = "Coop",
                variant = "Intero",
                quantity = 1.0,
                now = now,
            )
        )
        db.shoppingItemDao().insert(
            shoppingItem(
                id = "esselunga",
                brand = "Esselunga",
                variant = "Parzialmente scremato",
                quantity = 2.0,
                now = now,
            )
        )

        repository.checkout(listOf("coop", "esselunga"))

        val history = db.historyDao().getAll().first()
        assertEquals(2, history.size)

        val coop = history.single { it.brand == "Coop" }
        assertEquals("Intero", coop.variant)
        assertEquals(1.0, coop.lastQuantity, 0.0)

        val esselunga = history.single { it.brand == "Esselunga" }
        assertEquals("Parzialmente scremato", esselunga.variant)
        assertEquals(2.0, esselunga.lastQuantity, 0.0)
    }

    @Test
    fun `checkout aggiorna la riga della stessa variante`() = runBlocking {
        val now = System.currentTimeMillis()
        db.historyDao().insert(
            HistoricalItemEntity(
                id = "old-history",
                productId = 1L,
                lastQuantity = 1.0,
                lastUnit = StandardUnit.LITER,
                brand = "Coop",
                variant = "Intero",
                purchasedAt = now,
                purchaseCount = 1,
            )
        )
        db.shoppingItemDao().insert(
            shoppingItem(
                id = "coop-2",
                brand = "Coop",
                variant = "Intero",
                quantity = 2.0,
                now = now,
            )
        )

        repository.checkout(listOf("coop-2"))

        val stored = db.historyDao().getAll().first().single()
        assertEquals("old-history", stored.id)
        assertEquals(2, stored.purchaseCount)
        assertEquals(2.0, stored.lastQuantity, 0.0)
        assertTrue(stored.brand == "Coop" && stored.variant == "Intero")
    }

    @Test
    fun `eliminazione azioni spuntate preserva attive spesa e storico`() = runBlocking {
        val now = System.currentTimeMillis()
        db.actionItemDao().insert(action("checked", checked = true, now = now))
        db.actionItemDao().insert(action("active", checked = false, now = now))
        db.shoppingItemDao().insert(shoppingItem("shopping", null, null, 1.0, now))
        db.historyDao().insert(
            HistoricalItemEntity(
                id = "history",
                productId = 1L,
                lastQuantity = 2.0,
                lastUnit = StandardUnit.LITER,
                purchasedAt = now,
                purchaseCount = 3,
            ),
        )

        val deleted = repository.deleteCheckedActions()

        assertEquals(1, deleted)
        assertEquals(listOf("active"), db.actionItemDao().getAll().first().map { it.id })
        assertEquals(listOf("shopping"), db.shoppingItemDao().getAll().first().map { it.id })
        val history = db.historyDao().getAll().first().single()
        assertEquals("history", history.id)
        assertEquals(3, history.purchaseCount)
    }

    @Test
    fun `eliminazione azioni spuntate senza righe e innocua`() = runBlocking {
        db.actionItemDao().insert(action("active", checked = false, now = 1L))

        assertEquals(0, repository.deleteCheckedActions())
        assertEquals(listOf("active"), db.actionItemDao().getAll().first().map { it.id })
    }

    private fun action(id: String, checked: Boolean, now: Long) = ActionItemEntity(
        id = id,
        testo = "Azione $id",
        statoSpunta = checked,
        createdAt = now,
        updatedAt = now,
    )

    private fun shoppingItem(
        id: String,
        brand: String?,
        variant: String?,
        quantity: Double,
        now: Long,
    ) = ShoppingItemEntity(
        id = id,
        productId = 1L,
        quantity = quantity,
        unit = StandardUnit.LITER,
        brand = brand,
        variant = variant,
        isChecked = true,
        createdAt = now,
        updatedAt = now,
    )
}
