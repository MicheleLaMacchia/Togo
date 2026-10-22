package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
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
 * Test DAO per i Livelli 3 user-defined (Story 7.2).
 *
 * Verifica `autoGenerate` con `id = 0`, `is_user_defined = 1`, la rimozione
 * del filtro in `getLevel3()`, il calcolo di `sortOrder` e la compatibilità
 * con i prodotti canonici creati sul nuovo Livello 3.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogDaoUserLevel3Test {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: CatalogDao

    private val level2Bevande = 21L
    private val level2Cereali = 22L

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
                "VALUES (5, 21, 'Bevande vegetali', 1, 0)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_3 (id, level2_id, name, sortOrder, is_user_defined) " +
                "VALUES (6, 22, 'Cereali da colazione', 1, 0)"
        )
    }

    private fun newLevel3(
        name: String = "Bevande bio",
        level2Id: Long = level2Bevande,
        sortOrder: Int = 2,
    ) = TaxonomyLevel3Entity(
        id = 0L,
        level2Id = level2Id,
        name = name,
        sortOrder = sortOrder,
        isUserDefined = true,
    )

    @Test
    fun `insertLevel3 con id zero genera id e persiste is_user_defined`() = runBlocking {
        val id = dao.insertLevel3(newLevel3())

        assertTrue("Room deve assegnare un id > 0", id > 0L)

        val loaded = dao.getLevel3ByLevel2(level2Bevande).first()
            .firstOrNull { it.id == id }
        assertNotNull(loaded)
        assertEquals(true, loaded!!.isUserDefined)
        assertEquals("Bevande bio", loaded.name)
        assertEquals(level2Bevande, loaded.level2Id)
        assertEquals(2, loaded.sortOrder)
    }

    @Test
    fun `getLevel3 include i livelli user-defined`() = runBlocking {
        val id = dao.insertLevel3(newLevel3())

        val all = dao.getLevel3().first()

        assertTrue("Il filtro is_user_defined = 0 deve essere rimosso", all.any { it.id == id })
    }

    @Test
    fun `getLevel3ByLevel2 include solo i figli del livello richiesto`() = runBlocking {
        val id = dao.insertLevel3(newLevel3())

        val children = dao.getLevel3ByLevel2(level2Bevande).first()

        assertTrue(children.any { it.id == id })
        assertTrue(children.all { it.level2Id == level2Bevande })
    }

    @Test
    fun `nextLevel3SortOrder parte da max piu uno`() = runBlocking {
        // Predefinito sotto L2=21 con sortOrder = 1.
        assertEquals(2, dao.nextLevel3SortOrder(level2Bevande))

        val id = dao.insertLevel3(newLevel3(sortOrder = dao.nextLevel3SortOrder(level2Bevande)))
        val created = dao.getLevel3ByLevel2(level2Bevande).first().first { it.id == id }

        assertEquals(2, created.sortOrder)
        assertEquals(3, dao.nextLevel3SortOrder(level2Bevande))
    }

    @Test
    fun `due L2 diversi hanno sortOrder indipendenti`() = runBlocking {
        assertEquals(2, dao.nextLevel3SortOrder(level2Bevande))
        assertEquals(2, dao.nextLevel3SortOrder(level2Cereali))

        dao.insertLevel3(newLevel3(level2Id = level2Bevande, sortOrder = 2))

        assertEquals(3, dao.nextLevel3SortOrder(level2Bevande))
        assertEquals(2, dao.nextLevel3SortOrder(level2Cereali))
    }

    @Test
    fun `due L3 con lo stesso nome nello stesso L2 sono ammessi`() = runBlocking {
        val firstId = dao.insertLevel3(newLevel3(sortOrder = 2))
        val secondId = dao.insertLevel3(newLevel3(sortOrder = 3))

        assertTrue(firstId > 0L)
        assertTrue(secondId > 0L)
        assertEquals(
            2,
            dao.getLevel3ByLevel2(level2Bevande).first().count { it.name == "Bevande bio" },
        )
    }

    @Test
    fun `un prodotto canonico puo usare il nuovo livello 3`() = runBlocking {
        val level3Id = dao.insertLevel3(newLevel3())

        val productId = dao.insertProduct(
            CanonicalProductEntity(
                id = 0L,
                level3Id = level3Id,
                name = "Latte bio",
                isUserDefined = true,
            )
        )

        val products = dao.getByLevel3(level3Id)
        assertTrue(products.any { it.id == productId })
    }
}
