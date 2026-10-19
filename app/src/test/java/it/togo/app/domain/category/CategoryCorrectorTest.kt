package it.togo.app.domain.category

import io.mockk.*
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test JVM per CategoryCorrector.
 * Verifica correzione, undo, generazione diff.
 */
class CategoryCorrectorTest {

    private lateinit var catalogRepository: CatalogRepository
    private lateinit var corrector: CategoryCorrector

    private val productLatte = CanonicalProduct(
        id = 1L,
        name = "Latte UHT",
        level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER)
    )

    private val assignment = CategoryAssignment(
        suggestedLevel3Id = 5L, // Latticini
        confidence = 1.0,
        source = AssignmentSource.DEFAULT,
        product = productLatte,
        userExpression = "latte"
    )

    private val taxonomy = listOf(
        TaxonomyLevel.Level1(1, "Alimentari", 1),
        TaxonomyLevel.Level2(10, "Bevande", 1, 1),
        TaxonomyLevel.Level2(20, "Latticini", 2, 1),
        TaxonomyLevel.Level3(5, "Latticini", 1, 20, false),
        TaxonomyLevel.Level3(6, "Yogurt", 2, 20, false),
        TaxonomyLevel.Level3(12, "Bevande", 2, 10, false),
        TaxonomyLevel.Level3(99, "User Custom", 99, 20, true)
    )

    @Before
    fun setup() {
        catalogRepository = mockk<CatalogRepository>()
        corrector = CategoryCorrector(catalogRepository)

        every { catalogRepository.getTaxonomy() } returns flowOf(taxonomy)
    }

    @Test
    fun `correzione valida crea CategoryCorrection con diff`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = corrector.correct(assignment, 12L) // Bevande

        assertTrue(correction.isEffective)
        assertEquals(5L, correction.originalLevel3Id)
        assertEquals(12L, correction.correctedLevel3Id)
        assertEquals(5L, correction.rootOriginalLevel3Id)
        assertEquals(productLatte, correction.product)
    }

    @Test
    fun `correzione stesso L3 non e effettiva`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = corrector.correct(assignment, 5L) // Stesso Latticini

        assertFalse(correction.isEffective)
        assertEquals(5L, correction.originalLevel3Id)
        assertEquals(5L, correction.correctedLevel3Id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `correzione L3 inesistente lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        corrector.correct(assignment, 999L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `correzione L3 user-defined lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        corrector.correct(assignment, 99L) // User-defined
    }

    @Test
    fun `undo ripristina categoria originale immediata`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = corrector.correct(assignment, 12L)
        val undone = corrector.undo(correction)

        assertEquals(5L, undone.correctedLevel3Id)
        assertEquals(5L, undone.originalLevel3Id)
        assertEquals(5L, undone.rootOriginalLevel3Id)
        assertFalse(undone.isEffective)
    }

    @Test
    fun `undoFull ripristina radice dopo catena correzioni`(): Unit = runBlocking(Dispatchers.IO) {
        // A (5) -> B (12) -> C (6)
        val step1 = corrector.correct(assignment, 12L) // 5 -> 12
        val step2 = step1.nextCorrection(6L) // 12 -> 6

        assertEquals(5L, step2.rootOriginalLevel3Id)
        assertEquals(12L, step2.originalLevel3Id)
        assertEquals(6L, step2.correctedLevel3Id)
        assertTrue(step2.hasCorrectionChain)

        val fullUndo = step2.undoFull() // undo completo a radice
        assertEquals(5L, fullUndo.correctedLevel3Id)
        assertEquals(5L, fullUndo.rootOriginalLevel3Id)
    }

    @Test
    fun `generateDiff ritorna stringa formattata`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = corrector.correct(assignment, 12L)
        val diff = corrector.generateDiff(correction)

        assertTrue(diff.contains("Categoria suggerita: Latticini"))
        assertTrue(diff.contains("La tua scelta: Bevande"))
        assertTrue(diff.contains("→"))
    }

    @Test(timeout = 100)
    fun `correzione completata sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) {
        corrector.correct(assignment, 12L)
    }

    @Test(timeout = 100)
    fun `generateDiff completata sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = corrector.correct(assignment, 12L)
        corrector.generateDiff(correction)
    }
}