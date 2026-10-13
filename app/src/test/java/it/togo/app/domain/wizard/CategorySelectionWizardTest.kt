package it.togo.app.domain.wizard

import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Test JVM per CategorySelectionWizard.
 * Verifica navigazione tassonomia L1 → L2 → L3.
 */
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
        catalogRepository = mock()
        wizard = CategorySelectionWizard(catalogRepository)

        `when`(catalogRepository.getTaxonomy()).thenReturn(flowOf(taxonomy))
    }

    @Test
    fun `getLevel1Options ritorna L1 ordinati per sortOrder`() = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel1Options()

        assertEquals(2, options.size)
        assertEquals("Alimentari", options[0].name)
        assertEquals("Non alimentari", options[1].name)
    }

    @Test
    fun `getLevel2Options ritorna solo figli di L1 specifico`() = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel2Options(1L)

        assertEquals(2, options.size)
        assertTrue(options.all { it.level1Id == 1L })
        assertEquals("Bevande", options[0].name)
        assertEquals("Latticini", options[1].name)
    }

    @Test
    fun `getLevel2Options con L1 senza figli ritorna lista vuota`() = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel2Options(999L)

        assertTrue(options.isEmpty())
    }

    @Test
    fun `getLevel3Options ritorna solo figli di L2 specifico`() = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel3Options(20L)

        assertEquals(2, options.size)
        assertTrue(options.all { it.level2Id == 20L })
        assertEquals("Latticini", options[0].name)
        assertEquals("Yogurt", options[1].name)
    }

    @Test
    fun `getLevel3Options include user-defined di default`() = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel3Options(20L, includeUserDefined = true)

        assertTrue(options.any { it.isUserDefined })
    }

    @Test
    fun `getLevel3Options esclude user-defined se richiesto`() = runBlocking(Dispatchers.IO) {
        val options = wizard.getLevel3Options(20L, includeUserDefined = false)

        assertTrue(options.none { it.isUserDefined })
    }

    @Test
    fun `createNewLevel3Option crea marker con ID -1`() {
        val newOption = wizard.createNewLevel3Option(20L)

        assertEquals(-1L, newOption.id)
        assertEquals("+ Nuovo...", newOption.name)
        assertTrue(newOption.isUserDefined)
    }

    @Test
    fun `level3NameExists true se nome esiste gia`() = runBlocking(Dispatchers.IO) {
        val exists = wizard.level3NameExists(20L, "Latticini")

        assertTrue(exists)
    }

    @Test
    fun `level3NameExists false se nome non esiste`() = runBlocking(Dispatchers.IO) {
        val exists = wizard.level3NameExists(20L, "NonEsistente")

        assertFalse(exists)
    }

    @Test
    fun `level3NameExists case insensitive`() = runBlocking(Dispatchers.IO) {
        val exists = wizard.level3NameExists(20L, "LATTICINI")

        assertTrue(exists)
    }

    @Test
    fun `level3NameExists con spazi bianchi ritorna true`() = runBlocking(Dispatchers.IO) {
        val exists = wizard.level3NameExists(20L, "  Latticini  ")

        assertTrue(exists)
    }

    @Test
    fun `level3NameExists con solo spazi ritorna false`() = runBlocking(Dispatchers.IO) {
        val exists = wizard.level3NameExists(20L, "   ")

        assertFalse(exists)
    }

    @Test
    fun `level3NameExists con stringa vuota ritorna false`() = runBlocking(Dispatchers.IO) {
        val exists = wizard.level3NameExists(20L, "")

        assertFalse(exists)
    }

    @Test
    fun `getTaxonomySnapshot con Flow vuoto ritorna lista vuota`() = runBlocking(Dispatchers.IO) {
        `when`(catalogRepository.getTaxonomy()).thenReturn(flowOf(emptyList()))

        val options = wizard.getLevel1Options()

        assertTrue(options.isEmpty())
    }

    @Test(timeout = 100)
    fun `getLevel1Options completato sotto 100ms`() = runBlocking(Dispatchers.IO) {
        wizard.getLevel1Options()
    }

    @Test(timeout = 100)
    fun `getLevel2Options completato sotto 100ms`() = runBlocking(Dispatchers.IO) {
        wizard.getLevel2Options(1L)
    }

    @Test(timeout = 100)
    fun `getLevel3Options completato sotto 100ms`() = runBlocking(Dispatchers.IO) {
        wizard.getLevel3Options(20L)
    }
}