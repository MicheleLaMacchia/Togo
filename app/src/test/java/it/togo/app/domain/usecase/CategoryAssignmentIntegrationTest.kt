package it.togo.app.domain.usecase

import io.mockk.*
import it.togo.app.domain.category.AssignmentSource
import it.togo.app.domain.category.CategoryAssigner
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

class CategoryAssignmentIntegrationTest {

    private lateinit var catalogRepository: CatalogRepository
    private lateinit var learnedRulesRepository: LearnedRulesRepository
    private lateinit var assigner: CategoryAssigner
    private lateinit var corrector: CategoryCorrector
    private lateinit var assignUseCase: AssignCanonicalCategoryUseCase
    private lateinit var correctUseCase: CorrectCategoryUseCase

    private val productLatte = CanonicalProduct(
        id = 1L, name = "Latte UHT", level3Id = 5L,
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
        catalogRepository = mockk<CatalogRepository>()
        learnedRulesRepository = mockk<LearnedRulesRepository>()

        every { catalogRepository.getTaxonomy() } returns flowOf(taxonomy)
        coEvery { learnedRulesRepository.getByExpressionAndProductId(any(), any()) } returns null
        coEvery { learnedRulesRepository.upsertAtomic(any()) } answers { firstArg<LearnedRule>() }

        assigner = CategoryAssigner(catalogRepository, learnedRulesRepository)
        corrector = CategoryCorrector(catalogRepository)
        assignUseCase = AssignCanonicalCategoryUseCase(assigner)
        correctUseCase = CorrectCategoryUseCase(corrector, learnedRulesRepository)
    }

    @Test
    fun `flusso completo assign default correct confirmRemember reassign usa learned rule`(): Unit = runBlocking(Dispatchers.IO) {
        val assignment1 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.DEFAULT, assignment1.source)
        assertEquals(5L, assignment1.suggestedLevel3Id)
        assertEquals("latte", assignment1.userExpression)

        val correction = correctUseCase(assignment1, 12L)
        assertTrue(correction.isEffective)
        assertEquals(5L, correction.originalLevel3Id)
        assertEquals(12L, correction.correctedLevel3Id)

        val learnedRule = correctUseCase.confirmRemember(assignment1, correction)
        assertEquals("latte", learnedRule.userExpression)
        assertEquals(1L, learnedRule.productId)
        assertEquals(12L, learnedRule.level3Id)
        assertTrue(learnedRule.isActive)

        coEvery { learnedRulesRepository.getByExpressionAndProductId("latte", 1L) } returns learnedRule

        val assignment2 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.LEARNED, assignment2.source)
        assertEquals(12L, assignment2.suggestedLevel3Id)
        assertEquals(0.9, assignment2.confidence, 0.001)
        assertTrue(assignment2.requiresConfirmation)
    }

    @Test
    fun `flusso con declineRemember rule NON creata nuovo assign usa default`(): Unit = runBlocking(Dispatchers.IO) {
        val assignment1 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.DEFAULT, assignment1.source)

        val correction = correctUseCase(assignment1, 12L)
        assertTrue(correction.isEffective)

        val declined = correctUseCase.declineRemember()
        assertTrue(declined)

        coVerify(exactly = 0) { learnedRulesRepository.upsertAtomic(any()) }

        val assignment2 = assignUseCase(productLatte, "latte")
        assertEquals(AssignmentSource.DEFAULT, assignment2.source)
        assertEquals(5L, assignment2.suggestedLevel3Id)
    }

    @Test
    fun `rule per prodotto A non influenza prodotto B`(): Unit = runBlocking(Dispatchers.IO) {
        val productLatteSoia = CanonicalProduct(
            id = 2L, name = "Latte di Soia", level3Id = 12L,
            compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER)
        )

        val assignment1 = assignUseCase(productLatte, "latte")
        val correction = correctUseCase(assignment1, 12L)
        val learnedRule = correctUseCase.confirmRemember(assignment1, correction)
        coEvery { learnedRulesRepository.getByExpressionAndProductId("latte", 1L) } returns learnedRule
        coEvery { learnedRulesRepository.getByExpressionAndProductId("latte", 2L) } returns null

        val assignment2 = assignUseCase(productLatteSoia, "latte")

        assertEquals(AssignmentSource.DEFAULT, assignment2.source)
        assertEquals(12L, assignment2.suggestedLevel3Id)
    }
}