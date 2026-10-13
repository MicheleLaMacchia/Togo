package it.togo.app.domain.usecase

import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.wizard.NewProductDraft
import it.togo.app.domain.wizard.WizardStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Test JVM per CreateCanonicalProductUseCase.
 * Verifica creazione prodotto user-defined con persistenza repository.
 */
class CreateCanonicalProductUseCaseTest {

    private lateinit var catalogRepository: CatalogRepository
    private lateinit var useCase: CreateCanonicalProductUseCase

    private val level1 = TaxonomyLevel.Level1(1, "Alimentari", 1)
    private val level2 = TaxonomyLevel.Level2(10, "Bevande", 1, 1)
    private val level3 = TaxonomyLevel.Level3(12, "Bevande", 2, 10, false)

    private val validDraft = NewProductDraft.empty()
        .withLevel1(level1)
        .withLevel2(level2)
        .withLevel3(level3)
        .withProductName("Latte di avena bio")
        .withUnits(listOf(StandardUnit.LITER, StandardUnit.MILLILITER))
        .withSelectedUnit(StandardUnit.LITER)
        .withQuantity(1.0)
        .withAttributes("Coop", "bio", null)
        .withStep(WizardStep.CONFIRM)

    @Before
    fun setup() {
        catalogRepository = mock()
        useCase = CreateCanonicalProductUseCase(catalogRepository)
    }

    @Test
    fun `crea prodotto user-defined e verifica tutti i campi`() = runBlocking(Dispatchers.IO) {
        val product = useCase(validDraft)

        // Verifica campi essenziali (non solo ID > 0)
        assertEquals("Latte di avena bio", product.name)
        assertTrue(product.isUserDefined)
        assertEquals(1L, product.level1Id)
        assertEquals(10L, product.level2Id)
        assertEquals(12L, product.level3Id)
        assertEquals("Alimentari", product.level1Name)
        assertEquals("Bevande", product.level2Name)
        assertEquals("Bevande", product.level3Name)
        assertTrue(product.id > 0)
    }

    @Test
    fun `crea prodotto con nuovo L3 (isNewLevel3)`() = runBlocking(Dispatchers.IO) {
        val draft = NewProductDraft.empty()
            .withLevel1(level1)
            .withLevel2(level2)
            .withNewLevel3("Latte di avena")
            .withProductName("Latte di avena")
            .withUnits(listOf(StandardUnit.LITER))
            .withSelectedUnit(StandardUnit.LITER)
            .withQuantity(1.0)
            .withStep(WizardStep.CONFIRM)

        val product = useCase(draft)

        assertEquals("Latte di avena", product.name)
        assertEquals("Latte di avena", product.level3Name)
        assertEquals(-1L, product.level3Id) // Nuovo L3, ID assegnato da DB
        assertTrue(product.isUserDefined)
    }

    @Test
    fun `prodotto creato con unita compatibili corrette`() = runBlocking(Dispatchers.IO) {
        val product = useCase(validDraft)

        assertEquals(listOf(StandardUnit.LITER, StandardUnit.MILLILITER), product.compatibleUnits)
    }

    @Test
    fun `prodotto creato con attributi corretti`() = runBlocking(Dispatchers.IO) {
        val product = useCase(validDraft)

        assertEquals(listOf("Coop"), product.commonBrands)
    }

    @Test
    fun `nome prodotto vuoto lancia eccezione`() = runBlocking(Dispatchers.IO) {
        val invalidDraft = validDraft.withProductName("")

        val ex = assertThrows(IllegalArgumentException::class.java) {
            useCase(invalidDraft)
        }
        assertTrue(ex.message?.contains("Nome prodotto") == true)
    }

    @Test
    fun `quantita zero lancia eccezione`() = runBlocking(Dispatchers.IO) {
        val invalidDraft = validDraft.withQuantity(0.0)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            useCase(invalidDraft)
        }
        assertTrue(ex.message?.contains("Quantità") == true)
    }

    @Test
    fun `draft non a CONFIRM lancia eccezione`() = runBlocking(Dispatchers.IO) {
        val invalidDraft = validDraft.withStep(WizardStep.ENTER_DETAILS)

        assertThrows(IllegalArgumentException::class.java) {
            useCase(invalidDraft)
        }
    }

    @Test
    fun `senza L1 lancia eccezione`() = runBlocking(Dispatchers.IO) {
        val invalidDraft = NewProductDraft.empty()
            .withLevel2(level2)
            .withLevel3(level3)
            .withProductName("Test")
            .withUnits(listOf(StandardUnit.LITER))
            .withSelectedUnit(StandardUnit.LITER)
            .withQuantity(1.0)
            .withStep(WizardStep.CONFIRM)

        assertThrows(IllegalArgumentException::class.java) {
            useCase(invalidDraft)
        }
    }

    @Test(timeout = 100)
    fun `creazione prodotto completata sotto 100ms`() = runBlocking(Dispatchers.IO) {
        useCase(validDraft)
    }
}