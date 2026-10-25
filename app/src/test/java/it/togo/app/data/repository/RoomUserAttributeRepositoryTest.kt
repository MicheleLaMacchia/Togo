package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.UserAttributeDao
import it.togo.app.domain.model.RenameUserAttributeResult
import it.togo.app.domain.model.UserAttributeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test del registro marche/varianti user-defined.
 *
 * Copre ciò che i fake non possono dimostrare: persistenza Room, forma
 * "capitalized", confronto case/accent-insensitive e filtro delle marche
 * predefinite (che non devono comparire nelle Impostazioni).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomUserAttributeRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: UserAttributeDao
    private lateinit var repository: RoomUserAttributeRepository

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.userAttributeDao()
        repository = RoomUserAttributeRepository(dao)
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) {
            db.close()
        }
    }

    @Test
    fun `remember salva la forma capitalized`() = runBlocking {
        repository.remember(UserAttributeType.BRAND, "  barilla due  ")

        val brands = repository.observe(UserAttributeType.BRAND).first()
        assertEquals(listOf("Barilla due"), brands.map { it.value })
    }

    @Test
    fun `remember non registra le marche predefinite`() = runBlocking {
        repository.remember(UserAttributeType.BRAND, "coop")
        repository.remember(UserAttributeType.BRAND, "  CONAD ")

        assertTrue(
            "Coop e Conad sono predefinite e non devono entrare nel registro",
            repository.observe(UserAttributeType.BRAND).first().isEmpty(),
        )
    }

    @Test
    fun `remember deduplica maiuscole e accenti`() = runBlocking {
        repository.remember(UserAttributeType.VARIANT, "Caffè")
        repository.remember(UserAttributeType.VARIANT, "caffe")
        repository.remember(UserAttributeType.VARIANT, "  CAFFÈ  ")

        val variants = repository.observe(UserAttributeType.VARIANT).first()
        assertEquals(1, variants.size)
        assertEquals("Caffè", variants.single().value)
    }

    @Test
    fun `rename rifiuta un duplicato case-insensitive`() = runBlocking {
        // "Alfa"/"Beta" non hanno altre maiuscole interne: la forma salvata
        // resta identica e il confronto è solo sul case.
        repository.remember(UserAttributeType.BRAND, "Alfa")
        repository.remember(UserAttributeType.BRAND, "Beta")
        val brands = repository.observe(UserAttributeType.BRAND).first()
        val first = brands.first { it.value == "Alfa" }

        val result = repository.rename(first.id, UserAttributeType.BRAND, "beta")

        assertTrue(result is RenameUserAttributeResult.Duplicate)
        assertEquals(
            listOf("Alfa", "Beta"),
            repository.observe(UserAttributeType.BRAND).first().map { it.value },
        )
    }

    @Test
    fun `rename aggiorna il valore e normalizza la forma`() = runBlocking {
        repository.remember(UserAttributeType.BRAND, "marca a")
        val first = repository.observe(UserAttributeType.BRAND).first().single()

        val result = repository.rename(first.id, UserAttributeType.BRAND, "  NUOVA MARCA ")

        assertTrue(result is RenameUserAttributeResult.Renamed)
        assertEquals(
            listOf("Nuova marca"),
            repository.observe(UserAttributeType.BRAND).first().map { it.value },
        )
    }

    @Test
    fun `delete rimuove il valore dal registro`() = runBlocking {
        repository.remember(UserAttributeType.VARIANT, "Integrale")
        val stored = repository.observe(UserAttributeType.VARIANT).first().single()

        assertTrue(repository.delete(stored.id))

        assertFalse(repository.delete(stored.id))
        assertTrue(repository.observe(UserAttributeType.VARIANT).first().isEmpty())
    }
}
