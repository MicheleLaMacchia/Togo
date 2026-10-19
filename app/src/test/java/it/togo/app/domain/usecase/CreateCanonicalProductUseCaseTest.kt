package it.togo.app.domain.usecase

import io.mockk.*
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.wizard.NewProductDraft
import it.togo.app.domain.wizard.WizardStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CreateCanonicalProductUseCaseTest {

    private lateinit var catalogRepository: CatalogRepository
    private lateinit var useCase: CreateCanonicalProductUseCase

    private val level1 = TaxonomyLevel.Level1(1, "Alimentari", 1)
    private val level2 = TaxonomyLevel.Level2(10, "Bevande", 1, 1)
    private val level3 = TaxonomyLevel.Level3(12, "Bevande", 2, 10, false)

    private val validDraft = NewProductDraft.empty()
        .withLevel1(level1).withLevel2(level2).withLevel3(level3)
        .withProductName("Latte di avena bio")
        .withUnits(listOf(StandardUnit.LITER, StandardUnit.MILLILITER))
        .withSelectedUnit(StandardUnit.LITER).withQuantity(1.0)
        .withAttributes("Coop", "bio", null).withStep(WizardStep.CONFIRM)

    @Before
    fun setup() {
        catalogRepository = mockk<CatalogRepository>()
        useCase = CreateCanonicalProductUseCase(catalogRepository)
    }

    @Test
    fun `crea prodotto user-defined e verifica tutti i campi`(): Unit = runBlocking(Dispatchers.IO) {
        val product = useCase(validDraft)
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
    fun `crea prodotto con nuovo L3`(): Unit = runBlocking(Dispatchers.IO) {
        val draft = NewProductDraft.empty()
            .withLevel1(level1).withLevel2(level2).withNewLevel3("Latte di avena")
            .withProductName("Latte di avena").withUnits(listOf(StandardUnit.LITER))
            .withSelectedUnit(StandardUnit.LITER).withQuantity(1.0).withStep(WizardStep.CONFIRM)
        val product = useCase(draft)
        assertEquals("Latte di avena", product.name)
        assertEquals("Latte di avena", product.level3Name)
        assertEquals(-1L, product.level3Id)
        assertTrue(product.isUserDefined)
    }

    @Test
    fun `prodotto creato con unita compatibili corrette`(): Unit = runBlocking(Dispatchers.IO) {
        assertEquals(listOf(StandardUnit.LITER, StandardUnit.MILLILITER), useCase(validDraft).compatibleUnits)
    }

    @Test
    fun `prodotto creato con attributi corretti`(): Unit = runBlocking(Dispatchers.IO) {
        assertEquals(listOf("Coop"), useCase(validDraft).commonBrands)
    }

    @Test
    fun `nome prodotto vuoto lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(validDraft.withProductName("")) }
        }
        assertTrue(ex.message?.contains("Nome prodotto") == true)
    }

    @Test
    fun `quantita zero lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(validDraft.withQuantity(0.0)) }
        }
        assertTrue(ex.message?.contains("Quantità") == true)
    }

    @Test
    fun `draft non a CONFIRM lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(validDraft.withStep(WizardStep.ENTER_DETAILS)) }
        }
    }

    @Test
    fun `senza L1 lancia eccezione`(): Unit = runBlocking(Dispatchers.IO) {
        val invalidDraft = NewProductDraft.empty()
            .withLevel2(level2).withLevel3(level3).withProductName("Test")
            .withUnits(listOf(StandardUnit.LITER)).withSelectedUnit(StandardUnit.LITER)
            .withQuantity(1.0).withStep(WizardStep.CONFIRM)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(invalidDraft) }
        }
    }

    @Test(timeout = 100)
    fun `creazione prodotto completata sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) {
        useCase(validDraft)
    }
}