package it.togo.app.domain.wizard

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import org.junit.Assert.*
import org.junit.Test

/**
 * Test JVM per NewProductDraft.
 * Verifica accumulato stato, validazione, navigazione step.
 */
class NewProductDraftTest {

    private val level1 = TaxonomyLevel.Level1(1, "Alimentari", 1)
    private val level2 = TaxonomyLevel.Level2(10, "Bevande", 1, 1)
    private val level3 = TaxonomyLevel.Level3(12, "Bevande", 2, 10, false)

    @Test
    fun `empty draft allo step iniziale SELECT_L1`() {
        val draft = NewProductDraft.empty()

        assertEquals(WizardStep.SELECT_L1, draft.currentStep)
        assertNull(draft.selectedLevel1)
        assertFalse(draft.canProceed)
        assertTrue(draft.canGoBack.not())
    }

    @Test
    fun `withLevel1 avanza a SELECT_L2 e resetta L2-L3`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)

        assertEquals(WizardStep.SELECT_L2, draft.currentStep)
        assertEquals(level1, draft.selectedLevel1)
        assertNull(draft.selectedLevel2)
        assertNull(draft.selectedLevel3)
        assertTrue(draft.canProceed)
    }

    @Test
    fun `withLevel2 avanza a SELECT_L3 e resetta L3`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)

        assertEquals(WizardStep.SELECT_L3, draft.currentStep)
        assertEquals(level2, draft.selectedLevel2)
        assertNull(draft.selectedLevel3)
        assertTrue(draft.canProceed)
    }

    @Test
    fun `withLevel3 avanza a ENTER_DETAILS e precompila nome`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)

        assertEquals(WizardStep.ENTER_DETAILS, draft.currentStep)
        assertEquals(level3, draft.selectedLevel3)
        assertEquals("Bevande", draft.productName) // Precompilato da L3.name
    }

    @Test
    fun `withNewLevel3 richiede nome non vuoto per procedere`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withNewLevel3("")

        assertFalse(draft.canProceed) // Nome vuoto

        val draftConNome = draft.withNewLevel3("Latte di avena")
        assertTrue(draftConNome.canProceed)
    }

    @Test
    fun `withLevel1 resetta dettagli prodotto se torno indietro`() {
        // Simula: utente ha compilato dettagli, torna indietro e cambia L1
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Latte di avena")
            .withUnits(listOf(StandardUnit.LITER))
            .withAttributes("Coop", "bio", null)

        // Torna indietro e seleziona nuovo L1
        val newLevel1 = TaxonomyLevel.Level1(3, "Non alimentari", 3)
        val resetDraft = draft.withLevel1(newLevel1)

        // Verifica che i dettagli siano stati resettati
        assertEquals("", resetDraft.productName)
        assertTrue(resetDraft.compatibleUnits.isEmpty())
        assertNull(resetDraft.selectedUnit)
        assertEquals(1.0, resetDraft.quantity, 0.001)
        assertNull(resetDraft.brand)
        assertNull(resetDraft.variant)
        assertNull(resetDraft.condition)
        assertTrue(resetDraft.errors.isEmpty())
    }

    @Test
    fun `withLevel2 resetta dettagli prodotto se torno indietro`() {
        // Simula: utente ha compilato dettagli, torna indietro e cambia L2
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Latte di avena")
            .withUnits(listOf(StandardUnit.LITER))
            .withAttributes("Coop", null, null)

        // Torna indietro e seleziona nuovo L2
        val newLevel2 = TaxonomyLevel.Level2(30, "Detergenti", 1, 1)
        val resetDraft = draft.withLevel2(newLevel2)

        // Verifica che i dettagli siano stati resettati
        assertEquals("", resetDraft.productName)
        assertTrue(resetDraft.compatibleUnits.isEmpty())
        assertNull(resetDraft.selectedUnit)
        assertNull(resetDraft.brand)
    }

    @Test
    fun `ENTER_DETAILS richiede nome e unita per procedere`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Latte di avena")
            .withUnits(listOf(StandardUnit.LITER, StandardUnit.MILLILITER))

        assertTrue(draft.canProceed)
    }

    @Test
    fun `ENTER_DETAILS non procede senza nome`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("")
            .withUnits(listOf(StandardUnit.LITER))

        assertFalse(draft.canProceed)
    }

    @Test
    fun `ENTER_DETAILS non procede senza unita`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Latte di avena")
            .withUnits(emptyList())

        assertFalse(draft.canProceed)
    }

    @Test
    fun `ENTER_DETAILS non procede con quantita zero`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Latte di avena")
            .withUnits(listOf(StandardUnit.LITER))
            .withQuantity(0.0)

        assertFalse(draft.canProceed)
    }

    @Test
    fun `navigazione avanti e indietro`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)

        assertEquals(WizardStep.SELECT_L3, draft.previousStep)
        assertEquals(WizardStep.ENTER_DETAILS, draft.nextStep)

        val back = draft.withStep(draft.previousStep!!)
        assertEquals(WizardStep.SELECT_L3, back.currentStep)
    }

    @Test
    fun `primo step non puo tornare indietro`() {
        val draft = NewProductDraft.empty()

        assertFalse(draft.canGoBack)
        assertNull(draft.previousStep)
    }

    @Test
    fun `toCanonicalProduct crea prodotto user-defined`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Latte di avena bio")
            .withUnits(listOf(StandardUnit.LITER, StandardUnit.MILLILITER))
            .withSelectedUnit(StandardUnit.LITER)
            .withQuantity(1.0)
            .withAttributes("Coop", "bio", null)
            .withStep(WizardStep.CONFIRM)

        val product = draft.toCanonicalProduct()

        assertEquals("Latte di avena bio", product.name)
        assertTrue(product.isUserDefined)
        assertEquals(1L, product.level1Id)
        assertEquals(10L, product.level2Id)
        assertEquals(12L, product.level3Id)
        assertEquals(listOf(StandardUnit.LITER, StandardUnit.MILLILITER), product.compatibleUnits)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toCanonicalProduct lancia eccezione se non a CONFIRM`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Test")
            .withUnits(listOf(StandardUnit.LITER))

        draft.toCanonicalProduct() // Non a CONFIRM
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toCanonicalProduct lancia eccezione senza nome`() {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("")
            .withUnits(listOf(StandardUnit.LITER))
            .withStep(WizardStep.CONFIRM)

        draft.toCanonicalProduct()
    }

    @Test
    fun `gestione errori inline`() {
        val draft = NewProductDraft.empty()
            .withError("productName", "Nome obbligatorio")

        assertTrue(draft.errors.containsKey("productName"))
        assertEquals("Nome obbligatorio", draft.errors["productName"])

        val cleared = draft.clearError("productName")
        assertFalse(cleared.errors.containsKey("productName"))
    }

    @Test
    fun `withAttributes ignora valori blank`() {
        val draft = NewProductDraft.empty()
            .withAttributes("  ", null, "")

        assertNull(draft.brand)
        assertNull(draft.variant)
        assertNull(draft.condition)
    }

    @Test
    fun `withAttributes mantiene valori validi`() {
        val draft = NewProductDraft.empty()
            .withAttributes("Coop", "bio", "Surgelato")

        assertEquals("Coop", draft.brand)
        assertEquals("bio", draft.variant)
        assertEquals("Surgelato", draft.condition)
    }
}