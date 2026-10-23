package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.domain.model.StandardUnit
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
 * Test del percorso di produzione (DAO Room + repository) per i prodotti
 * user-defined della Story 7.1.
 *
 * Copre ciò che i fake non possono dimostrare: persistenza di
 * `is_user_defined`, scoping per Livello 3 e ricercabilità immediata del
 * nome, inclusi i nomi accentati.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomCatalogRepositoryUserProductTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: CatalogDao
    private lateinit var repository: RoomCatalogRepository

    private val level3Bevande = 12L
    private val level3Cereali = 13L

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.catalogDao()
        repository = RoomCatalogRepository(dao)
        insertTaxonomy()
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    private fun insertTaxonomy() {
        val writable = db.openHelper.writableDatabase
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_1 (id, name, sortOrder) VALUES (1, 'Alimentari', 1)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_2 (id, level1_id, name, sortOrder) VALUES (21, 1, 'Bevande', 1)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_2 (id, level1_id, name, sortOrder) VALUES (22, 1, 'Cereali', 2)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_3 (id, level2_id, name, sortOrder, is_user_defined) " +
                "VALUES (12, 21, 'Bevande vegetali', 1, 0)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_3 (id, level2_id, name, sortOrder, is_user_defined) " +
                "VALUES (13, 22, 'Cereali da colazione', 1, 0)"
        )
    }

    @Test
    fun `insertProduct persiste user-defined, normalizza il nome e rispetta il L3`() = runBlocking {
        val created = repository.insertProduct("  Latte di avena bio  ", level3Bevande)

        assertTrue("Room deve assegnare un id > 0", created.id > 0L)
        assertEquals("Latte di avena bio", created.name)
        assertEquals(level3Bevande, created.level3Id)
        assertTrue(created.isUserDefined)

        val loaded = repository.getById(created.id)
        assertNotNull(loaded)
        assertTrue(loaded!!.isUserDefined)
        assertEquals("Latte di avena bio", loaded.name)
        assertEquals("Bevande vegetali", loaded.level3Name)
        assertEquals("Bevande", loaded.level2Name)
        assertEquals("Alimentari", loaded.level1Name)
        assertTrue(loaded.compatibleUnits.contains(StandardUnit.LITER))

        val inCategory = repository.getByLevel3(level3Bevande)
        assertTrue(inCategory.any { it.id == created.id })
        assertTrue(
            "Il prodotto non deve comparire in un altro Livello 3",
            repository.getByLevel3(level3Cereali).none { it.id == created.id },
        )
    }

    @Test
    fun `prodotto appena creato e subito trovabile dalla ricerca`() = runBlocking {
        val created = repository.insertProduct("Latte di avena bio", level3Bevande)

        val results = repository.search("latte di avena").first()
        val result = results.first { it.id == created.id }
        assertEquals("Bevande vegetali", result.level3Name)
        assertTrue(result.compatibleUnits.contains(StandardUnit.LITER))
    }

    @Test
    fun `nome accentato e trovabile con query senza accenti`() = runBlocking {
        // Regresione: SEARCH normalizza solo la query e confronta il nome
        // grezzo, quindi serve il sinonimo accent-fold scritto in insertProduct.
        val created = repository.insertProduct("Caffè d'orzo", level3Bevande)

        assertTrue(
            "La ricerca 'caffe' deve trovare 'Caffè d'orzo'",
            repository.search("caffe").first().any { it.id == created.id },
        )
        assertTrue(
            "La ricerca accentata deve trovare lo stesso prodotto",
            repository.search("caffè").first().any { it.id == created.id },
        )
    }

    @Test
    fun `due prodotti con lo stesso nome nello stesso L3 restano entrambi`() = runBlocking {
        // Nessun vincolo UNIQUE: l'override "Procedi" del dialog deve funzionare.
        val first = repository.insertProduct("Latte di avena", level3Bevande)
        val second = repository.insertProduct("Latte di avena", level3Bevande)

        assertTrue(first.id > 0L)
        assertTrue(second.id > 0L)
        assertEquals(
            2,
            repository.getByLevel3(level3Bevande).count { it.name == "Latte di avena" },
        )
    }
}
