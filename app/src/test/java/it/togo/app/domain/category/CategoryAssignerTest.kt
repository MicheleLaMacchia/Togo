package it.togo.app.domain.category

import it.togo.app.data.database.CatalogQueries
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Test JVM per CategoryAssigner.
 * Verifica logica di priorità: learned rule (scoped by product) → default prodotto → fallback.
 */
class CategoryAssignerTest {

    private lateinit var catalogRepository: CatalogRepository
    private lateinit var learnedRulesRepository: LearnedRulesRepository
    private lateinit var assigner: CategoryAssigner

    private val productLatte = CanonicalProduct(
        id = 1L,
        name = "Latte UHT",
        level3Id = 5L, // Latticini
        compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER),
        commonBrands = listOf("Coop", "Esselunga")
    )

    private val productLatteSoia = CanonicalProduct(
        id = 2L,
        name = "Latte di Soia",
        level3Id = 12L, // Bevande
        compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER)
    )

    private val taxonomy = listOf(
        TaxonomyLevel.Level1(1, "Alimentari", 1),
        TaxonomyLevel.Level2(10, "Bevande", 1, 1),
        TaxonomyLevel.Level2(20, "Latticini", 2, 1),
        TaxonomyLevel.Level3(5, "Latticini", 1, 20, false),
        TaxonomyLevel.Level3(6, "Yogurt", 2, 20, false),
        TaxonomyLevel.Level3(12, "Bevande", 2, 10, false),
        TaxonomyLevel.Level3(13, "Succhi", 3, 10, false),
        TaxonomyLevel.Level3(99, "User Custom", 99, 20, true)
    )

    @Before
    fun setup() {
        catalogRepository = mock()
        learnedRulesRepository = mock()
        assigner = CategoryAssigner(catalogRepository, learnedRulesRepository)

        `when`(catalogRepository.getTaxonomy()).thenReturn(flowOf(taxonomy))
    }

    @Test
    fun `assegnazione default prodotto quando nessuna rule`() = runBlocking(Dispatchers.IO) {
        `when`(learnedRulesRepository.getByExpressionAndProductId(any(), any())).thenReturn(null)

        val assignment = assigner.assign(productLatte, "latte")

        assertEquals(AssignmentSource.DEFAULT, assignment.source)
        assertEquals(1.0, assignment.confidence, 0.001)
        assertEquals(5L, assignment.suggestedLevel3Id)
        assertFalse(assignment.requiresConfirmation)
    }

    @Test
    fun `assegnazione da rule appresa per STESSO prodotto prevale su default`() = runBlocking(Dispatchers.IO) {
        val rule = LearnedRule(
            userExpression = "latte",
            productId = 1L, // STESSO productId
            level3Id = 12L, // Bevande (diverso da default 5)
            isActive = true
        )
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 1L)).thenReturn(rule)

        val assignment = assigner.assign(productLatte, "latte")

        assertEquals(AssignmentSource.LEARNED, assignment.source)
        assertEquals(0.9, assignment.confidence, 0.001)
        assertEquals(12L, assignment.suggestedLevel3Id)
        assertTrue(assignment.requiresConfirmation) // 0.9 <= 0.9
    }

    @Test
    fun `rule per PRODOTTO DIVERSO ignorata usa default`() = runBlocking(Dispatchers.IO) {
        // Rule per productId=2 (Latte di Soia), ma assegniamo per productId=1 (Latte UHT)
        val rule = LearnedRule(
            userExpression = "latte",
            productId = 2L,
            level3Id = 12L,
            isActive = true
        )
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 1L)).thenReturn(null)

        val assignment = assigner.assign(productLatte, "latte")

        assertEquals(AssignmentSource.DEFAULT, assignment.source)
        assertEquals(5L, assignment.suggestedLevel3Id)
    }

    @Test
    fun `rule inattiva ignorata usa default`() = runBlocking(Dispatchers.IO) {
        val rule = LearnedRule(
            userExpression = "latte",
            productId = 1L,
            level3Id = 12L,
            isActive = false
        )
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 1L)).thenReturn(rule)

        val assignment = assigner.assign(productLatte, "latte")

        assertEquals(AssignmentSource.DEFAULT, assignment.source)
        assertEquals(5L, assignment.suggestedLevel3Id)
    }

    @Test
    fun `rule per L3 user-defined ignorata usa default`() = runBlocking(Dispatchers.IO) {
        val rule = LearnedRule(
            userExpression = "latte",
            productId = 1L,
            level3Id = 99L, // User-defined
            isActive = true
        )
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 1L)).thenReturn(rule)

        val assignment = assigner.assign(productLatte, "latte")

        assertEquals(AssignmentSource.DEFAULT, assignment.source)
        assertEquals(5L, assignment.suggestedLevel3Id)
    }

    @Test
    fun `espressione vuota o null salta lookup rule`() = runBlocking(Dispatchers.IO) {
        val assignment1 = assigner.assign(productLatte, null)
        val assignment2 = assigner.assign(productLatte, "")
        val assignment3 = assigner.assign(productLatte, "   ")

        assertEquals(AssignmentSource.DEFAULT, assignment1.source)
        assertEquals(AssignmentSource.DEFAULT, assignment2.source)
        assertEquals(AssignmentSource.DEFAULT, assignment3.source)
        verify(learnedRulesRepository, never()).getByExpressionAndProductId(any(), any())
    }

    @Test
    fun `normalizzazione accent-fold su espressione`() = runBlocking(Dispatchers.IO) {
        val rule = LearnedRule(
            userExpression = CatalogQueries.normalize("caffè"), // "caffe"
            productId = 2L,
            level3Id = 12L,
            isActive = true
        )
        `when`(learnedRulesRepository.getByExpressionAndProductId("caffe", 2L)).thenReturn(rule)

        val assignment = assigner.assign(productLatteSoia, "caffè")

        assertEquals(AssignmentSource.LEARNED, assignment.source)
        verify(learnedRulesRepository).getByExpressionAndProductId("caffe", 2L)
    }

    @Test
    fun `alternative includono default + rule + sibling L3s`() = runBlocking(Dispatchers.IO) {
        val rule = LearnedRule(
            userExpression = "latte",
            productId = 1L,
            level3Id = 12L, // Bevande
            isActive = true
        )
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 1L)).thenReturn(rule)

        val assignment = assigner.assign(productLatte, "latte")

        // Default (5 Latticini) + Rule (12 Bevande) + Siblings (6 Yogurt nello stesso L2=20)
        assertTrue(assignment.alternatives.size >= 3)
        assertTrue(assignment.alternatives.any { it.id == 5L }) // default
        assertTrue(assignment.alternatives.any { it.id == 12L }) // rule
        assertTrue(assignment.alternatives.any { it.id == 6L }) // sibling Yogurt
    }

    @Test
    fun `fallback primo L3 valido quando default invalido`() = runBlocking(Dispatchers.IO) {
        val productSenzaL3 = productLatte.copy(level3Id = 999L)
        `when`(learnedRulesRepository.getByExpressionAndProductId(any(), any())).thenReturn(null)

        val assignment = assigner.assign(productSenzaL3, "test")

        assertEquals(0.5, assignment.confidence, 0.001)
        assertEquals(AssignmentSource.FALLBACK, assignment.source)
        assertTrue(assignment.isFallback)
        assertTrue(assignment.requiresConfirmation) // 0.5 <= 0.9
        // Fallback dovrebbe essere primo L3 non user-defined = 5 (Latticini) o 12 (Bevande)
        assertTrue(listOf(5L, 12L, 6L, 13L).contains(assignment.suggestedLevel3Id))
    }

    @Test
    fun `fallback alternatives includono sibling L3s`() = runBlocking(Dispatchers.IO) {
        val productSenzaL3 = productLatte.copy(level3Id = 999L)
        `when`(learnedRulesRepository.getByExpressionAndProductId(any(), any())).thenReturn(null)

        val assignment = assigner.assign(productSenzaL3, "test")

        // Fallback alternatives = tutti L3 validi nello stesso L2 del fallback
        assertTrue(assignment.alternatives.isNotEmpty())
        assignment.alternatives.forEach { assertFalse(it.isUserDefined) }
    }

    @Test(timeout = 100)
    fun `assegnazione completata sotto 100ms`() = runBlocking(Dispatchers.IO) {
        `when`(learnedRulesRepository.getByExpressionAndProductId(any(), any())).thenReturn(null)
        assigner.assign(productLatte, "latte")
    }
}