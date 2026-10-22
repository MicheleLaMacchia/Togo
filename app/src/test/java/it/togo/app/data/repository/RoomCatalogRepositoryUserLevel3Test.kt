package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.CatalogDao
import it.togo.app.domain.model.TaxonomyLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test del percorso di produzione (DAO Room + repository) per i Livelli 3
 * user-defined della Story 7.2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomCatalogRepositoryUserLevel3Test {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: CatalogDao
    private lateinit var repository: RoomCatalogRepository

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
                "VALUES (5, 21, 'Bevande vegetali', 1, 0)"
        )
        writable.execSQL(
            "INSERT INTO TAXONOMY_LEVEL_3 (id, level2_id, name, sortOrder, is_user_defined) " +
                "VALUES (6, 22, 'Cereali da colazione', 1, 0)"
        )
    }

    @Test
    fun `insertLevel3 normalizza il nome, assegna id e sortOrder in fondo`() = runBlocking {
        val created = repository.insertLevel3("  Bevande bio  ", level2Bevande)

        assertTrue(created.id > 0L)
        assertEquals("Bevande bio", created.name)
        assertEquals(level2Bevande, created.level2Id)
        assertTrue(created.isUserDefined)
        assertEquals(2, created.sortOrder)

        val children = repository.getLevel3ByLevel2(level2Bevande)
        assertTrue(children.any { it.id == created.id })
        assertTrue(children.filter { !it.isUserDefined }.all { it.level2Id == level2Bevande })
    }

    @Test
    fun `il nuovo livello 3 compare in getTaxonomy`() = runBlocking {
        val created = repository.insertLevel3("Bevande bio", level2Bevande)

        val taxonomy = repository.getTaxonomy().first()

        assertTrue(taxonomy.any { it is TaxonomyLevel.Level3 && it.id == created.id })
    }

    @Test
    fun `un prodotto user-defined creato sul nuovo L3 resta ricercabile`() = runBlocking {
        val level3 = repository.insertLevel3("Bevande bio", level2Bevande)
        val product = repository.insertProduct("Caffè bio", level3.id)

        val byLevel = repository.getByLevel3(level3.id)
        assertTrue(byLevel.any { it.id == product.id })
        assertTrue(
            "Il nome accentato deve restare ricercabile (comportamento 7.1)",
            repository.search("caffe").first().any { it.id == product.id },
        )
    }

    @Test
    fun `getLevel3ByLevel2 non mescola livelli di L2 diversi`() = runBlocking {
        val onBevande = repository.insertLevel3("Bevande bio", level2Bevande)
        val onCereali = repository.insertLevel3("Cereali bio", level2Cereali)

        val bevande = repository.getLevel3ByLevel2(level2Bevande)
        val cereali = repository.getLevel3ByLevel2(level2Cereali)

        assertTrue(bevande.none { it.id == onCereali.id })
        assertTrue(bevande.any { it.id == onBevande.id })
        assertTrue(cereali.any { it.id == onCereali.id })
    }
}
