package it.togo.app.domain.usecase

import it.togo.app.domain.category.AssignmentSource
import it.togo.app.domain.category.CategoryAssignment
import it.togo.app.domain.category.CategoryAssigner
import it.togo.app.domain.category.CategoryCorrection
import it.togo.app.domain.category.CategoryCorrector
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
 * Integration test JVM per il flusso completo:
 * assign → correct → confirmRemember → reassign with same expression
 */
class CategoryAssignmentIntegrationTest {

    private lateinit var catalogRepository: CatalogRepository
    private lateinit var learnedRulesRepository: LearnedRulesRepository
    private lateinit var assigner: CategoryAssigner
    private lateinit var corrector: CategoryCorrector
    private lateinit var assignUseCase: AssignCanonicalCategoryUseCase
    private lateinit var correctUseCase: CorrectCategoryUseCase

    private val productLatte = CanonicalProduct(
        id = 1L,
        name = "Latte UHT",
        level3Id = 5L, // Latticini (default)
        compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER)
    )

    private val taxonomy = listOf(
        TaxonomyLevel.Level1(1, "Alimentari", 1),
        TaxonomyLevel.Level2(10, "Bevande", 1, 1),
        TaxonomyLevel.Level2(20, "Latticini", 2, 1),
        TaxonomyLevel.Level3(5, "Latticini", 1, 20, false),
        TaxonomyLevel.Level3(12, "Bevande", 2, 10, false),
        TaxonomyLevel.Level3(99, "User Custom", 99, 20, true)
    )

    @Before
    fun setup() {
        catalogRepository = mock()
        learnedRulesRepository = mock()

        `when`(catalogRepository.getTaxonomy()).thenReturn(flowOf(taxonomy))
        runBlocking {
            `when`(learnedRulesRepository.getByExpressionAndProductId(any(), any())).thenReturn(null)
            `when`(learnedRulesRepository.upsertAtomic(any())).thenAnswer { invocation ->
                invocation.arguments[0] as LearnedRule
            }
        }

        assigner = CategoryAssigner(catalogRepository, learnedRulesRepository)
        corrector = CategoryCorrector(catalogRepository)
        assignUseCase = AssignCanonicalCategoryUseCase(assigner)
        correctUseCase = CorrectCategoryUseCase(corrector, learnedRulesRepository)
    }

    @Test
    fun `flusso completo assign default correct confirmRemember reassign usa learned rule`() = runBlocking(Dispatchers.IO) {
        // 1. Primo assegnamento: default prodotto (Latticini)
        val assignment1 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.DEFAULT, assignment1.source)
        assertEquals(5L, assignment1.suggestedLevel3Id)
        assertEquals("latte", assignment1.userExpression)

        // 2. Utente corregge: Latticini -> Bevande
        val correction = correctUseCase(assignment1, 12L)
        assertTrue(correction.isEffective)
        assertEquals(5L, correction.originalLevel3Id)
        assertEquals(12L, correction.correctedLevel3Id)

        // 3. Utente conferma "Ricorda questa scelta"
        val learnedRule = correctUseCase.confirmRemember(assignment1, correction)
        assertEquals("latte", learnedRule.userExpression)
        assertEquals(1L, learnedRule.productId)
        assertEquals(12L, learnedRule.level3Id)
        assertTrue(learnedRule.isActive)

        // 4. Mock repository per restituire la rule appena creata
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 1L)).thenReturn(learnedRule)

        // 5. Nuovo assegnamento con stessa espressione: deve usare LEARNED rule
        val assignment2 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.LEARNED, assignment2.source)
        assertEquals(12L, assignment2.suggestedLevel3Id) // Bevande, non Latticini
        assertEquals(0.9, assignment2.confidence, 0.001)
        assertTrue(assignment2.requiresConfirmation) // 0.9 <= 0.9
    }

    @Test
    fun `flusso con declineRemember rule NON creata nuovo assign usa default`() = runBlocking(Dispatchers.IO) {
        // 1. Assegnamento default
        val assignment1 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.DEFAULT, assignment1.source)

        // 2. Correzione
        val correction = correctUseCase(assignment1, 12L)
        assertTrue(correction.isEffective)

        // 3. Utente RIFIUTA "Ricorda"
        val declined = correctUseCase.declineRemember()
        assertTrue(declined)

        // 4. Repository non chiamato per upsert
        verify(learnedRulesRepository, never()).upsertAtomic(any())

        // 5. Nuovo assegnamento: ancora default (nessuna rule creata)
        val assignment2 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.DEFAULT, assignment2.source)
        assertEquals(5L, assignment2.suggestedLevel3Id)
    }

    @Test
    fun `rule per prodotto A non influenza prodotto B`() = runBlocking(Dispatchers.IO) {
        val productLatteSoia = CanonicalProduct(
            id = 2L,
            name = "Latte di Soia",
            level3Id = 12L, // Bevande (default)
            compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER)
        )

        // 1. Crea rule per Latte UHT (productId=1): "latte" -> Bevande
        val assignment1 = assignUseCase(productLatte, "latte")
        val correction = correctUseCase(assignment1, 12L)
        val learnedRule = correctUseCase.confirmRemember(assignment1, correction)
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 1L)).thenReturn(learnedRule)

        // 2. Assegna per Latte di Soia (productId=2) con stessa espressione
        `when`(learnedRulesRepository.getByExpressionAndProductId("latte", 2L)).thenReturn(null)

        val assignment2 = assignUseCase(productLatteSoia, "latte")

        // Deve usare DEFAULT di Latte di Soia (Bevande), non la rule di Latte UHT
        assertEquals(AssignmentSource.DEFAULT, assignment2.source)
        assertEquals(12L, assignment2.suggestedLevel3Id)
    }
}