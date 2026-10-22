package it.togo.app.domain.usecase

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test della use case di proposta di un nuovo Livello 3 (Story 7.2).
 *
 * La definizione di duplicato è vincolante: uguaglianza dopo
 * `ProductNameNormalizer.normalize` nello stesso Livello 2, nessun fuzzy.
 */
class CreateUserLevel3UseCaseTest {

    private val level2Bevande = 21L
    private val level2Cereali = 22L

    private val existing = TaxonomyLevel.Level3(
        id = 12L,
        name = "Bevande vegetali",
        sortOrder = 1,
        level2Id = level2Bevande,
        isUserDefined = false,
    )

    private class FakeCatalogRepository(
        initial: List<TaxonomyLevel.Level3> = emptyList(),
    ) : CatalogRepository {

        val level3s = initial.toMutableList()
        var insertCalls = 0
            private set
        private var nextId = 500L

        override fun search(query: String): Flow<List<CanonicalProduct>> = flowOf(emptyList())

        override suspend fun getById(id: Long): CanonicalProduct? = null

        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = flowOf(emptyList())

        override suspend fun getLevel3ByLevel2(level2Id: Long): List<TaxonomyLevel.Level3> =
            level3s.filter { it.level2Id == level2Id }

        override suspend fun insertLevel3(name: String, level2Id: Long): TaxonomyLevel.Level3 {
            insertCalls++
            val created = TaxonomyLevel.Level3(
                id = nextId++,
                name = name.trim(),
                sortOrder = level3s.count { it.level2Id == level2Id } + 1,
                level2Id = level2Id,
                isUserDefined = true,
            )
            level3s += created
            return created
        }
    }

    private fun useCase(repository: FakeCatalogRepository) =
        CreateUserLevel3UseCase(repository)

    @Test
    fun `nome vuoto ritorna Invalid e non inserisce`() = runTest {
        val repository = FakeCatalogRepository()
        val result = useCase(repository)("   ", level2Bevande)

        assertTrue(result is CreateLevel3Result.Invalid)
        assertEquals(
            "Inserisci il nome del nuovo Livello 3",
            (result as CreateLevel3Result.Invalid).message,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun `livello 2 non valido ritorna Invalid`() = runTest {
        val repository = FakeCatalogRepository()
        val result = useCase(repository)("Bevande bio", 0L)

        assertTrue(result is CreateLevel3Result.Invalid)
        assertEquals(
            "Seleziona il Livello 2",
            (result as CreateLevel3Result.Invalid).message,
        )
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun `nome duplicato nello stesso L2 ritorna Duplicate senza inserire`() = runTest {
        val repository = FakeCatalogRepository(listOf(existing))
        val result = useCase(repository)("  BEVANDE VEGETALI  ", level2Bevande)

        assertTrue(result is CreateLevel3Result.Duplicate)
        val candidate = (result as CreateLevel3Result.Duplicate).candidate
        assertEquals(existing.id, candidate.id)
        assertEquals(existing.name, candidate.name)
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun `conferma duplicato crea comunque il livello`() = runTest {
        val repository = FakeCatalogRepository(listOf(existing))
        val result = useCase(repository)(
            "Bevande vegetali",
            level2Bevande,
            confirmDuplicate = true,
        )

        assertTrue(result is CreateLevel3Result.Created)
        val created = (result as CreateLevel3Result.Created).level3
        assertTrue(created.isUserDefined)
        assertEquals(level2Bevande, created.level2Id)
        assertEquals(1, repository.insertCalls)
        assertEquals(2, repository.level3s.count { it.level2Id == level2Bevande })
    }

    @Test
    fun `stesso nome in un L2 diverso viene creato`() = runTest {
        val repository = FakeCatalogRepository(listOf(existing))
        val result = useCase(repository)("Bevande vegetali", level2Cereali)

        assertTrue(result is CreateLevel3Result.Created)
        assertEquals(level2Cereali, (result as CreateLevel3Result.Created).level3.level2Id)
        assertEquals(1, repository.insertCalls)
    }

    @Test
    fun `varianti accentate e spazi sono considerate duplicato`() = runTest {
        val repository = FakeCatalogRepository(listOf(existing))
        val createLevel3 = useCase(repository)

        assertTrue(createLevel3("bevande vegetali", level2Bevande) is CreateLevel3Result.Duplicate)
        assertTrue(createLevel3("  BEVANDE   VEGETALI  ", level2Bevande) is CreateLevel3Result.Duplicate)
        assertTrue(createLevel3("Bevande Végetali", level2Bevande) is CreateLevel3Result.Duplicate)
        assertEquals(0, repository.insertCalls)
    }

    @Test
    fun `il livello creato ha nome con trim, id assegnato e is_user_defined`() = runTest {
        val repository = FakeCatalogRepository()
        val result = useCase(repository)("  Bevande bio  ", level2Bevande)

        assertTrue(result is CreateLevel3Result.Created)
        val created = (result as CreateLevel3Result.Created).level3
        assertEquals("Bevande bio", created.name)
        assertTrue(created.id > 0L)
        assertTrue(created.isUserDefined)
        assertEquals(level2Bevande, created.level2Id)
        assertFalse(repository.level3s.isEmpty())
    }

    @Test
    fun `duplicato contro un L3 user-defined creato in precedenza`() = runTest {
        val repository = FakeCatalogRepository()
        val createLevel3 = useCase(repository)

        val first = createLevel3("Bevande bio", level2Bevande)
        assertTrue(first is CreateLevel3Result.Created)
        val firstId = (first as CreateLevel3Result.Created).level3.id

        val second = createLevel3("  BEVANDE BIO  ", level2Bevande)
        assertTrue(second is CreateLevel3Result.Duplicate)
        assertEquals(firstId, (second as CreateLevel3Result.Duplicate).candidate.id)
        // Il confronto deve coprire anche le righe user-defined, non solo quelle preinstallate.
        assertEquals(1, repository.insertCalls)
    }

    @Test
    fun `nome composto solo da segni diacritici ritorna Invalid`() = runTest {
        val repository = FakeCatalogRepository()
        val result = useCase(repository)("\u0301", level2Bevande)

        assertTrue(result is CreateLevel3Result.Invalid)
        assertEquals(
            "Inserisci un nome valido",
            (result as CreateLevel3Result.Invalid).message,
        )
        assertEquals(0, repository.insertCalls)
    }
}
