package it.togo.app.domain.usecase

import it.togo.app.domain.category.AssignmentSource
import it.togo.app.domain.category.CategoryAssignment
import it.togo.app.domain.category.CategoryCorrection
import it.togo.app.domain.category.CategoryCorrector
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Test JVM per CorrectCategoryUseCase.
 * Verifica correzione, undo, prompt "Ricorda".
 */
class CorrectCategoryUseCaseTest {

    private lateinit var corrector: CategoryCorrector
    private lateinit var learnedRulesRepository: LearnedRulesRepository
    private lateinit var useCase: CorrectCategoryUseCase

    private val productLatte = CanonicalProduct(
        id = 1L,
        name = "Latte UHT",
        level3Id = 5L,
        compatibleUnits = listOf(StandardUnit.LITER, StandardUnit.MILLILITER)
    )

    private val assignment = CategoryAssignment(
        suggestedLevel3Id = 5L,
        confidence = 1.0,
        source = AssignmentSource.DEFAULT,
        product = productLatte,
        userExpression = "latte"
    )

    @Before
    fun setup() {
        corrector = mock()
        learnedRulesRepository = mock()
        useCase = CorrectCategoryUseCase(corrector, learnedRulesRepository)
    }

    @Test
    fun `correzione delega a CategoryCorrector`() = runBlocking(Dispatchers.IO) {
        val expectedCorrection = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        `when`(corrector.correct(assignment, 12L)).thenReturn(expectedCorrection)

        val correction = useCase(assignment, 12L)

        assertEquals(expectedCorrection, correction)
        verify(corrector).correct(assignment, 12L)
    }

    @Test
    fun `undo delega a CategoryCorrector`() {
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        val expectedUndone = correction.undo()
        `when`(corrector.undo(correction)).thenReturn(expectedUndone)

        val undone = useCase.undo(correction)

        assertEquals(expectedUndone, undone)
        verify(corrector).undo(correction)
    }

    @Test
    fun `generateDiff delega a CategoryCorrector`() = runBlocking(Dispatchers.IO) {
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        `when`(corrector.generateDiff(correction)).thenReturn("Categoria suggerita: Latticini → La tua scelta: Bevande")

        val diff = useCase.generateDiff(correction)

        assertEquals("Categoria suggerita: Latticini → La tua scelta: Bevande", diff)
        verify(corrector).generateDiff(correction)
    }

    @Test
    fun `canShowRememberPrompt true quando userExpression presente e correzione effettiva`() {
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        assertTrue(useCase.canShowRememberPrompt(assignment, correction))
    }

    @Test
    fun `canShowRememberPrompt false quando userExpression mancante`() {
        val assignmentSenzaExpr = assignment.copy(userExpression = null)
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        assertFalse(useCase.canShowRememberPrompt(assignmentSenzaExpr, correction))
    }

    @Test
    fun `canShowRememberPrompt false quando correzione non effettiva`() {
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 5L, // stesso
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        assertFalse(useCase.canShowRememberPrompt(assignment, correction))
    }

    @Test
    fun `confirmRemember usa upsertAtomic per evitare race condition`() = runBlocking(Dispatchers.IO) {
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        `when`(learnedRulesRepository.upsertAtomic(any())).thenAnswer { invocation ->
            invocation.arguments[0] as LearnedRule
        }

        val rule = useCase.confirmRemember(assignment, correction)

        assertEquals("latte", rule.userExpression)
        assertEquals(1L, rule.productId)
        assertEquals(12L, rule.level3Id)
        assertTrue(rule.isActive)
        verify(learnedRulesRepository).upsertAtomic(any())
    }

    @Test
    fun `confirmRemember aggiorna rule esistente per STESSO prodotto`() = runBlocking(Dispatchers.IO) {
        val existingRule = LearnedRule(
            id = "existing-rule",
            userExpression = "latte",
            productId = 1L, // STESSO prodotto
            level3Id = 5L,  // vecchia categoria
            isActive = false
        )
        `when`(learnedRulesRepository.upsertAtomic(any())).thenAnswer { invocation ->
            val rule = invocation.arguments[0] as LearnedRule
            rule.copy(id = "existing-rule")
        }

        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )

        val rule = useCase.confirmRemember(assignment, correction)

        assertEquals("existing-rule", rule.id)
        assertEquals(1L, rule.productId)
        assertEquals(12L, rule.level3Id) // nuova categoria
        assertTrue(rule.isActive) // riattivata
    }

    @Test(expected = IllegalStateException::class)
    fun `confirmRemember senza userExpression lancia eccezione`() = runBlocking(Dispatchers.IO) {
        val assignmentSenzaExpr = assignment.copy(userExpression = null)
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )

        useCase.confirmRemember(assignmentSenzaExpr, correction)
    }

    @Test
    fun `declineRemember ritorna true`() {
        val result = useCase.declineRemember()
        assertTrue(result)
    }

    @Test(timeout = 100)
    fun `correzione completata sotto 100ms`() = runBlocking(Dispatchers.IO) {
        val expectedCorrection = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        `when`(corrector.correct(assignment, 12L)).thenReturn(expectedCorrection)
        useCase(assignment, 12L)
    }

    @Test(timeout = 100)
    fun `confirmRemember completato sotto 100ms`() = runBlocking(Dispatchers.IO) {
        val correction = CategoryCorrection(
            originalLevel3Id = 5L,
            correctedLevel3Id = 12L,
            rootOriginalLevel3Id = 5L,
            product = productLatte
        )
        `when`(learnedRulesRepository.upsertAtomic(any())).thenAnswer { invocation ->
            invocation.arguments[0] as LearnedRule
        }
        useCase.confirmRemember(assignment, correction)
    }
}