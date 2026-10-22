package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test DAO per i prodotti canonici user-defined (Story 7.1).
 *
 * Verifica che `autoGenerate` assegni l'ID con `id = 0`, che
 * `is_user_defined = 1` venga persistito, che il prodotto sia subito
 * ricercabile e che non esista alcun vincolo UNIQUE sul nome.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogDaoUserProductTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: CatalogDao

    private val level3Id = 12L

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.catalogDao()
        insertTaxonomy()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    /** Inserisce la tassonomia minima con SQL diretto (nessun metodo insert nella DAO). */
    private fun insertTaxonomy() {
        val writable = db.openHelper.writableDatabase
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_1 (id, name, sortOrder) VALUES (1, 'Alimentari', 1)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_2 (id, level1_id, name, sortOrder) VALUES (21, 1, 'Bevande', 1)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_3 (id, level2_id, name, sortOrder, is_user_defined) " +
                "VALUES (12, 21, 'Bevande vegetali', 1, 0)"
        )
    }

    @Test
    fun `insertProduct con id zero genera id user-defined e prodotto ricercabile`() = runBlocking {
        val id = dao.insertProduct(
            CanonicalProductEntity(
                id = 0L,
                level3Id = level3Id,
                name = "Latte di avena bio",
                isUserDefined = true,
            )
        )

        assertTrue("Room deve assegnare un id > 0", id > 0L)

        val loaded = dao.getById(id)
        assertNotNull(loaded)
        assertEquals(true, loaded!!.isUserDefined)
        assertEquals("Latte di avena bio", loaded.name)
        assertEquals(level3Id, loaded.level3Id)

        val byLevel3 = dao.getByLevel3(level3Id)
        assertTrue(byLevel3.any { it.id == id })

        // La query SEARCH deve trovare subito il nuovo prodotto (cp.name LIKE).
        val searchResults = dao.search(CatalogQueries.normalize("Latte di avena bio")).first()
        assertTrue(searchResults.any { it.id == id })
    }

    @Test
    fun `secondo insert con id zero ottiene un id maggiore`() = runBlocking {
        val firstId = dao.insertProduct(
            CanonicalProductEntity(id = 0L, level3Id = level3Id, name = "Primo prodotto", isUserDefined = true)
        )
        val secondId = dao.insertProduct(
            CanonicalProductEntity(id = 0L, level3Id = level3Id, name = "Secondo prodotto", isUserDefined = true)
        )

        assertTrue(secondId > firstId)
    }

    @Test
    fun `due prodotti con lo stesso nome nello stesso L3 sono entrambi ammessi`() = runBlocking {
        // Nessun vincolo UNIQUE: l'avviso di duplicato è applicativo e
        // l'utente deve poter scegliere "Procedi".
        val firstId = dao.insertProduct(
            CanonicalProductEntity(id = 0L, level3Id = level3Id, name = "Latte di avena", isUserDefined = true)
        )
        val secondId = dao.insertProduct(
            CanonicalProductEntity(id = 0L, level3Id = level3Id, name = "Latte di avena", isUserDefined = true)
        )

        assertTrue(firstId > 0L)
        assertTrue(secondId > 0L)
        assertEquals(2, dao.getByLevel3(level3Id).count { it.name == "Latte di avena" })
    }
}
