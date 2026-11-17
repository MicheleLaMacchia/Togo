package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.entity.ActionItemEntity
import it.togo.app.data.database.entity.HistoricalItemEntity
import it.togo.app.data.database.entity.ShoppingItemEntity
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.RenameUserAttributeResult
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.UserAttribute
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.UserAttributeRepository
import it.togo.app.domain.usecase.ImportExecutionResult
import it.togo.app.domain.usecase.ImportPlan
import it.togo.app.domain.usecase.ImportShoppingListUseCase
import it.togo.app.domain.usecase.PlannedImportItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
            db,
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
    fun `import fallito annulla catalogo lista azioni e aggiornamenti insieme`() = runBlocking {
        val now = System.currentTimeMillis()
        val writable = db.openHelper.writableDatabase
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_1 (id, name, sortOrder) VALUES (1, 'Alimentari', 1)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_2 (id, level1_id, name, sortOrder) " +
                "VALUES (2, 1, 'Bevande', 1)"
        )
        val catalogRepository = RoomCatalogRepository(db.catalogDao())
        val existingLevel3 = catalogRepository.insertLevel3("Cereali", 2L)
        val existingProduct = catalogRepository.insertProduct("Riso", existingLevel3.id)
        val existingItem = ShoppingItem(
            id = "existing-item",
            productId = existingProduct.id,
            quantity = 1.0,
            unit = StandardUnit.PIECE,
            createdAt = now,
            updatedAt = now,
        )
        repository.insert(existingItem)
        db.actionItemDao().insert(
            ActionItemEntity(
                id = "existing-action",
                testo = "Azione già presente",
                createdAt = now,
                updatedAt = now,
            )
        )
        val failingShoppingRepository = object : ShoppingListRepository by repository {
            private var addedActions = 0

            override suspend fun addAction(action: ActionItem) {
                if (++addedActions == 2) error("errore simulato sull'azione")
                repository.addAction(action)
            }
        }
        val useCase = ImportShoppingListUseCase(
            catalogRepository = catalogRepository,
            shoppingListRepository = failingShoppingRepository,
            userAttributeRepository = object : UserAttributeRepository {
                override fun observe(type: UserAttributeType) = flowOf(emptyList<UserAttribute>())
                override suspend fun remember(type: UserAttributeType, rawValue: String) = Unit
                override suspend fun rename(
                    id: Long,
                    type: UserAttributeType,
                    rawValue: String,
                ) = RenameUserAttributeResult.Invalid
                override suspend fun delete(id: Long) = false
            },
        )
        val plan = ImportPlan(
            items = listOf(
                PlannedImportItem(
                    level2Id = 2L,
                    level2Name = "Bevande",
                    level3Id = null,
                    level3Name = "Bevande bio",
                    level1Name = "Alimentari",
                    productId = null,
                    productName = "Latte vegetale",
                    quantity = 1.0,
                    unit = StandardUnit.PIECE,
                    brand = null,
                    variant = null,
                    lineNumber = 1,
                ),
                PlannedImportItem(
                    level2Id = 2L,
                    level2Name = "Bevande",
                    level3Id = existingLevel3.id,
                    level3Name = existingLevel3.name,
                    level1Name = "Alimentari",
                    productId = existingProduct.id,
                    productName = existingProduct.name,
                    quantity = 2.0,
                    unit = StandardUnit.PIECE,
                    brand = null,
                    variant = null,
                    lineNumber = 2,
                ),
            ),
            actions = listOf(
                ActionItem(text = "Prima azione"),
                ActionItem(text = "Seconda azione"),
            ),
        )

        val result = useCase.execute(plan)

        assertTrue(result is ImportExecutionResult.Error)
        assertEquals(1, db.catalogDao().getLevel3ByLevel2(2L).first().size)
        assertEquals(1, db.catalogDao().getByLevel3(existingLevel3.id).size)
        assertEquals(listOf("Riso"), db.catalogDao().getUserDefinedProducts().first().map { it.name })
        assertTrue(db.catalogDao().getByExactSynonym("latte vegetale").isEmpty())
        val items = db.shoppingItemDao().getAll().first()
        assertEquals(1, items.size)
        assertEquals("existing-item", items.single().id)
        assertEquals(1.0, items.single().quantity, 0.0)
        assertEquals(
            listOf("Azione già presente"),
            db.actionItemDao().getAll().first().map { it.testo },
        )
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
