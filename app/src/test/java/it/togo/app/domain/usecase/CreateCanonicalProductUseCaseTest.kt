package it.togo.app.domain.usecase

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test della creazione prodotto canonico user-defined su L3 esistente (Story 7.1).
 *
 * Il fake implementa `getByLevel3`/`insertProduct` in memoria: il confronto
 * duplicati è uguaglianza dopo normalizzazione nello stesso Livello 3.
 */
class CreateCanonicalProductUseCaseTest {

    private val level3Id = 12L
    private val otherLevel3Id = 13L

    private class InMemoryCatalogRepository(
        initial: List<CanonicalProduct> = emptyList(),
    ) : CatalogRepository {

        val products = initial.toMutableList()
        var insertCount = 0
            private set
        private var nextId = 1_000L

        override fun search(query: String): Flow<List<CanonicalProduct>> =
            flowOf(products.filter { it.name.contains(query, ignoreCase = true) })

        override suspend fun getById(id: Long): CanonicalProduct? =
            products.firstOrNull { it.id == id }

        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = flowOf(emptyList())

        override suspend fun getByLevel3(level3Id: Long): List<CanonicalProduct> =
            products.filter { it.level3Id == level3Id }

        override suspend fun insertProduct(name: String, level3Id: Long): CanonicalProduct {
            val product = CanonicalProduct(
                id = nextId++,
                name = name.trim(),
                level3Id = level3Id,
                isUserDefined = true,
            )
            products += product
            insertCount++
            return product
        }
    }

    private fun existing(name: String, level3Id: Long = this.level3Id) = CanonicalProduct(
        id = 1L,
        name = name,
        level3Id = level3Id,
    )

    @Test
    fun `nome vuoto ritorna Invalid e non inserisce`() = runTest {
        val repository = InMemoryCatalogRepository()
        val useCase = CreateCanonicalProductUseCase(repository)

        val result = useCase(name = "   ", level3Id = level3Id)

        assertTrue(result is CreateProductResult.Invalid)
        assertEquals("Inserisci il nome del prodotto", (result as CreateProductResult.Invalid).message)
        assertEquals(0, repository.insertCount)
    }

    @Test
    fun `livello 3 non valido ritorna Invalid e non inserisce`() = runTest {
        val repository = InMemoryCatalogRepository()
        val useCase = CreateCanonicalProductUseCase(repository)

        val result = useCase(name = "Latte di avena", level3Id = 0L)

        assertTrue(result is CreateProductResult.Invalid)
        assertEquals(0, repository.insertCount)
    }

    @Test
    fun `duplicato nello stesso L3 senza conferma ritorna Duplicate e non inserisce`() = runTest {
        val candidate = existing("Latte di avena")
        val repository = InMemoryCatalogRepository(listOf(candidate))
        val useCase = CreateCanonicalProductUseCase(repository)

        val result = useCase(name = "latte di avena", level3Id = level3Id)

        assertTrue(result is CreateProductResult.Duplicate)
        assertEquals(candidate, (result as CreateProductResult.Duplicate).candidate)
        assertEquals(0, repository.insertCount)
    }

    @Test
    fun `duplicato con conferma esplicita crea il prodotto`() = runTest {
        val candidate = existing("Latte di avena")
        val repository = InMemoryCatalogRepository(listOf(candidate))
        val useCase = CreateCanonicalProductUseCase(repository)

        val result = useCase(
            name = "  Latte di avena  ",
            level3Id = level3Id,
            confirmDuplicate = true,
        )

        assertTrue(result is CreateProductResult.Created)
        assertEquals(1, repository.insertCount)
        assertEquals(2, repository.products.size)
        assertEquals("Latte di avena", (result as CreateProductResult.Created).product.name)
    }

    @Test
    fun `stesso nome in un L3 diverso viene creato`() = runTest {
        val repository = InMemoryCatalogRepository(listOf(existing("Latte di avena", level3Id)))
        val useCase = CreateCanonicalProductUseCase(repository)

        val result = useCase(name = "Latte di avena", level3Id = otherLevel3Id)

        assertTrue(result is CreateProductResult.Created)
        assertEquals(otherLevel3Id, (result as CreateProductResult.Created).product.level3Id)
        assertEquals(1, repository.insertCount)
    }

    @Test
    fun `varianti accentate e con spazi sono considerate duplicato`() = runTest {
        val repository = InMemoryCatalogRepository(listOf(existing("Caffè")))
        val useCase = CreateCanonicalProductUseCase(repository)

        val lower = useCase(name = "caffe", level3Id = level3Id)
        val spaced = useCase(name = "  CAFFÈ  ", level3Id = level3Id)

        assertTrue(lower is CreateProductResult.Duplicate)
        assertTrue(spaced is CreateProductResult.Duplicate)
        assertEquals(0, repository.insertCount)
    }

    @Test
    fun `prodotto creato e user-defined sul L3 corretto`() = runTest {
        val repository = InMemoryCatalogRepository()
        val useCase = CreateCanonicalProductUseCase(repository)

        val result = useCase(name = "  Latte di avena bio  ", level3Id = level3Id)

        assertTrue(result is CreateProductResult.Created)
        val product = (result as CreateProductResult.Created).product
        assertNotNull(product)
        assertTrue(product.isUserDefined)
        assertEquals(level3Id, product.level3Id)
        assertEquals("Latte di avena bio", product.name)
        assertTrue(product.id > 0L)
    }

    @Test
    fun `nome composto solo da spazi accentati non validi ritorna Invalid`() = runTest {
        // Il nome composto solo da segni diacritici non produce testo dopo
        // la normalizzazione: nessun insert.
        val repository = InMemoryCatalogRepository()
        val useCase = CreateCanonicalProductUseCase(repository)

        val result = useCase(name = "\u0301\u0301", level3Id = level3Id)

        assertTrue(result is CreateProductResult.Invalid)
        assertEquals(0, repository.insertCount)
    }
}
