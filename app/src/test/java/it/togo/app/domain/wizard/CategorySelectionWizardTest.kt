package it.togo.app.domain.wizard

import io.mockk.*
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CategorySelectionWizardTest {

    private lateinit var catalogRepository: CatalogRepository
    private lateinit var wizard: CategorySelectionWizard

    private val taxonomy = listOf(
        TaxonomyLevel.Level1(1, "Alimentari", 1),
        TaxonomyLevel.Level1(2, "Non alimentari", 2),
        TaxonomyLevel.Level2(10, "Bevande", 1, 1),
        TaxonomyLevel.Level2(20, "Latticini", 2, 1),
        TaxonomyLevel.Level2(30, "Detergenti", 1, 2),
        TaxonomyLevel.Level3(5, "Latticini", 1, 20, false),
        TaxonomyLevel.Level3(6, "Yogurt", 2, 20, false),
        TaxonomyLevel.Level3(12, "Bevande", 2, 10, false),
        TaxonomyLevel.Level3(13, "Succhi", 3, 10, false),
        TaxonomyLevel.Level3(99, "User Custom", 99, 20, true)
    )

    @Before
    fun setup() {
        catalogRepository = mockk<CatalogRepository>()
        wizard = CategorySelectionWizard(catalogRepository)
        every { catalogRepository.getTaxonomy() } returns flowOf(taxonomy)
    }

    @Test
    fun `getLevel1Options ritorna L1 ordinati per sortOrder`(): Unit = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel1Options()
        assertEquals(2, options.size)
        assertEquals("Alimentari", options[0].name)
        assertEquals("Non alimentari", options[1].name)
    }

    @Test
    fun `getLevel2Options ritorna solo figli di L1 specifico`(): Unit = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel2Options(1L)
        assertEquals(2, options.size)
        assertTrue(options.all { it.level1Id == 1L })
        assertEquals("Bevande", options[0].name)
        assertEquals("Latticini", options[1].name)
    }

    @Test
    fun `getLevel2Options con L1 senza figli ritorna lista vuota`(): Unit = runBlocking(Dispatchers.IO) {
        assertTrue(wizard.getLevel2Options(999L).isEmpty())
    }

    @Test
    fun `getLevel3Options ritorna solo figli di L2 specifico`(): Unit = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel3Options(20L)
        assertEquals(2, options.size)
        assertTrue(options.all { it.level2Id == 20L })
        assertEquals("Latticini", options[0].name)
        assertEquals("Yogurt", options[1].name)
    }

    @Test
    fun `getLevel3Options include user-defined di default`(): Unit = runBlocking(Dispatchers.IO) {
        assertTrue(wizard.getLevel3Options(20L, includeUserDefined = true).any { it.isUserDefined })
    }

    @Test
    fun `getLevel3Options esclude user-defined se richiesto`(): Unit = runBlocking(Dispatchers.IO) {
        assertTrue(wizard.getLevel3Options(20L, includeUserDefined = false).none { it.isUserDefined })
    }

    @Test
    fun `createNewLevel3Option crea marker con ID -1`() {
        val newOption = wizard.createNewLevel3Option(20L)
        assertEquals(-1L, newOption.id)
        assertEquals("+ Nuovo...", newOption.name)
        assertTrue(newOption.isUserDefined)
    }

    @Test
    fun `level3NameExists true se nome esiste gia`(): Unit = runBlocking(Dispatchers.IO) {
        assertTrue(wizard.level3NameExists(20L, "Latticini"))
    }

    @Test
    fun `level3NameExists false se nome non esiste`(): Unit = runBlocking(Dispatchers.IO) {
        assertFalse(wizard.level3NameExists(20L, "NonEsistente"))
    }

    @Test
    fun `level3NameExists case insensitive`(): Unit = runBlocking(Dispatchers.IO) {
        assertTrue(wizard.level3NameExists(20L, "LATTICINI"))
    }

    @Test
    fun `level3NameExists con spazi bianchi ritorna true`(): Unit = runBlocking(Dispatchers.IO) {
        assertTrue(wizard.level3NameExists(20L, "  Latticini  "))
    }

    @Test
    fun `level3NameExists con solo spazi ritorna false`(): Unit = runBlocking(Dispatchers.IO) {
        assertFalse(wizard.level3NameExists(20L, "   "))
    }

    @Test
    fun `level3NameExists con stringa vuota ritorna false`(): Unit = runBlocking(Dispatchers.IO) {
        assertFalse(wizard.level3NameExists(20L, ""))
    }

    @Test
    fun `getTaxonomySnapshot con Flow vuoto ritorna lista vuota`(): Unit = runBlocking(Dispatchers.IO) {
        every { catalogRepository.getTaxonomy() } returns flowOf(emptyList())
        assertTrue(wizard.getLevel1Options().isEmpty())
    }

    @Test(timeout = 100)
    fun `getLevel1Options completato sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) { wizard.getLevel1Options() }

    @Test(timeout = 100)
    fun `getLevel2Options completato sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) { wizard.getLevel2Options(1L) }

    @Test(timeout = 100)
    fun `getLevel3Options completato sotto 100ms`(): Unit = runBlocking(Dispatchers.IO) { wizard.getLevel3Options(20L) }
}