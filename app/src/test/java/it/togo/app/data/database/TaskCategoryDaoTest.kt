package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.TaskCategoryDao
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
 * Test del DAO read-only delle categorie Task. Test scritti, non eseguiti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TaskCategoryDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: TaskCategoryDao

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        TaskCategoryTestSeed.insert(db)
        dao = db.taskCategoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `observeAll restituisce le 16 categorie del seed ordinate`() = runBlocking {
        val categories = dao.observeAll().first()

        assertEquals(16, categories.size)
        assertEquals(
            listOf(
                1L, 2L, 3L, 4L, 5L,
                101L, 201L, 301L, 401L,
                102L, 202L, 302L, 402L,
                103L, 203L, 303L,
            ),
            categories.map { it.id },
        )
    }

    @Test
    fun `getById getChildren e getByLevel usano il seed stabile`() = runBlocking {
        val pulizie = dao.getById(101L)
        assertEquals("Pulizie", pulizie?.name)
        assertEquals(1L, pulizie?.parentId)
        assertEquals(2, pulizie?.level)
        assertEquals(10, pulizie?.sortOrder)

        assertEquals(listOf(101L, 102L, 103L), dao.getChildren(1L).map { it.id })
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), dao.getByLevel(1).map { it.id })
        assertEquals(
            listOf(101L, 201L, 301L, 401L, 102L, 202L, 302L, 402L, 103L, 203L, 303L),
            dao.getByLevel(2).map { it.id },
        )
        assertTrue(dao.getChildren(5L).isEmpty())
    }

    @Test
    fun `il seed contiene aree e sottocategorie con ID stabili`() = runBlocking {
        val categories = dao.observeAll().first().associate { it.id to it.name }

        assertEquals("Casa", categories[1L])
        assertEquals("Commissioni", categories[2L])
        assertEquals("Personale", categories[3L])
        assertEquals("Lavoro", categories[4L])
        assertEquals("Altro", categories[5L])
        assertEquals("Pulizie", categories[101L])
        assertEquals("Manutenzione", categories[102L])
        assertEquals("Organizzazione", categories[103L])
        assertEquals("Ritiri", categories[201L])
        assertEquals("Spedizioni", categories[202L])
        assertEquals("Pratiche", categories[203L])
        assertEquals("Appuntamenti", categories[301L])
        assertEquals("Benessere", categories[302L])
        assertEquals("Varie", categories[303L])
        assertEquals("Attività", categories[401L])
        assertEquals("Scadenze", categories[402L])
    }
}
