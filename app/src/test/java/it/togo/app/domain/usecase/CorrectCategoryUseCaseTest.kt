package it.togo.app.domain.usecase

import io.mockk.*
import it.togo.app.domain.category.AssignmentSource
import it.togo.app.domain.category.CategoryAssignment
import it.togo.app.domain.category.CategoryCorrection
import it.togo.app.domain.category.CategoryCorrector
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CorrectCategoryUseCaseTest {

    private lateinit var corrector: CategoryCorrector
    private lateinit var learnedRulesRepository: LearnedRulesRepository
    private lateinit var useCase: CorrectCategoryUseCase

    private val productLatte = CanonicalProduct(
        id = 1L, name = "Latte UHT", level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER)
    )

    private val assignment = CategoryAssignment(
        suggestedLevel3Id = 5L, confidence = 1.0,
        source = AssignmentSource.DEFAULT, product = productLatte, userExpression = "latte"
    )

    @Before
    fun setup() {
        corrector = mockk<CategoryCorrector>()
        learnedRulesRepository = mockk<LearnedRulesRepository>()
        useCase = CorrectCategoryUseCase(corrector, learnedRulesRepository)
    }

    /**
     * Repository reale e privo di I/O: i test di performance non devono
     * misurare la latenza di inizializzazione di MockK.
     */
    private fun fakeLearnedRulesRepository(): LearnedRulesRepository =
        object : LearnedRulesRepository {
            override fun getAll(): Flow<List<LearnedRule>> =
                flowOf(emptyList())

            override suspend fun insert(rule: LearnedRule) = Unit

            override suspend fun update(rule: LearnedRule) = Unit

            override suspend fun delete(ruleId: String) = Unit

            override suspend fun getByExpression(expression: String): LearnedRule? = null

            override suspend fun getById(ruleId: String): LearnedRule? = null

            override suspend fun getByExpressionAndProductId(
                expression: String,
                productId: Long
            ): LearnedRule? = null

            override suspend fun upsertAtomic(rule: LearnedRule): LearnedRule = rule
        }

    @Test
    fun `correzione delega a CategoryCorrector`(): Unit = runBlocking(Dispatchers.IO) {
        val expectedCorrection = CategoryCorrection(5L, 12L, 5L, productLatte)
        coEvery { corrector.correct(assignment, 12L) } returns expectedCorrection

        val correction = useCase(assignment, 12L)

        assertEquals(expectedCorrection, correction)
        coVerify { corrector.correct(assignment, 12L) }
    }

    @Test
    fun `undo delega a CategoryCorrector`() {
        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        val expectedUndone = correction.undo()
        every { corrector.undo(correction) } returns expectedUndone

        val undone = useCase.undo(correction)

        assertEquals(expectedUndone, undone)
        verify { corrector.undo(correction) }
    }

    @Test
    fun `generateDiff delega a CategoryCorrector`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        coEvery { corrector.generateDiff(correction) } returns "Categoria suggerita: Latticini → La tua scelta: Bevande"

        val diff = useCase.generateDiff(correction)

        assertEquals("Categoria suggerita: Latticini → La tua scelta: Bevande", diff)
        coVerify { corrector.generateDiff(correction) }
    }

    @Test
    fun `canShowRememberPrompt true quando userExpression presente e correzione effettiva`() {
        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        assertTrue(useCase.canShowRememberPrompt(assignment, correction))
    }

    @Test
    fun `canShowRememberPrompt false quando userExpression mancante`() {
        val assignmentSenzaExpr = assignment.copy(userExpression = null)
        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        assertFalse(useCase.canShowRememberPrompt(assignmentSenzaExpr, correction))
    }

    @Test
    fun `canShowRememberPrompt false quando correzione non effettiva`() {
        val correction = CategoryCorrection(5L, 5L, 5L, productLatte)
        assertFalse(useCase.canShowRememberPrompt(assignment, correction))
    }

    @Test
    fun `confirmRemember usa upsertAtomic per evitare race condition`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        coEvery { learnedRulesRepository.upsertAtomic(any()) } answers { firstArg<LearnedRule>() }

        val rule = useCase.confirmRemember(assignment, correction)

        assertEquals("latte", rule.userExpression)
        assertEquals(1L, rule.productId)
        assertEquals(12L, rule.level3Id)
        assertTrue(rule.isActive)
        coVerify { learnedRulesRepository.upsertAtomic(any()) }
    }

    @Test
    fun `confirmRemember aggiorna rule esistente per STESSO prodotto`(): Unit = runBlocking(Dispatchers.IO) {
        coEvery { learnedRulesRepository.upsertAtomic(any()) } answers {
            firstArg<LearnedRule>().copy(id = "existing-rule")
        }

        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        val rule = useCase.confirmRemember(assignment, correction)

        assertEquals("existing-rule", rule.id)
        assertEquals(1L, rule.productId)
        assertEquals(12L, rule.level3Id)
        assertTrue(rule.isActive)
    }

    @Test(expected = IllegalStateException::class)
    fun `confirmRemember senza userExpression lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        val assignmentSenzaExpr = assignment.copy(userExpression = null)
        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        useCase.confirmRemember(assignmentSenzaExpr, correction)
    }

    @Test
    fun `declineRemember ritorna true`() {
        assertTrue(useCase.declineRemember())
    }

    // Il timeout JUnit resta ampio per assorbire l'avvio del runner e il
    // classloading MockK; la soglia di 100ms è misurata dopo un warm-up.
    @Test(timeout = 15_000)
    fun `correzione completata sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) {
        val expectedCorrection = CategoryCorrection(5L, 12L, 5L, productLatte)
        coEvery { corrector.correct(assignment, 12L) } returns expectedCorrection
        useCase(assignment, 12L) // warm-up
        val start = System.nanoTime()
        useCase(assignment, 12L)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("correzione ha impiegato ${elapsedMs}ms", elapsedMs < 100)
    }

    @Test(timeout = 15_000)
    fun `confirmRemember completato sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) {
        val correction = CategoryCorrection(5L, 12L, 5L, productLatte)
        useCase = CorrectCategoryUseCase(corrector, fakeLearnedRulesRepository())
        useCase.confirmRemember(assignment, correction) // warm-up
        val start = System.nanoTime()
        useCase.confirmRemember(assignment, correction)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("confirmRemember ha impiegato ${elapsedMs}ms", elapsedMs < 100)
    }
}