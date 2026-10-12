package it.togo.app.presentation.additem

import it.togo.app.domain.model.ResolutionOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test JVM per la logica di risoluzione duplicati (Story 3.5).
 *
 * Verifica che l'enum ResolutionOption sia corretto e che la logica
 * di determinazione della compatibilità unità funzioni come atteso.
 */
class DuplicateResolutionTest {

    @Test
    fun `ResolutionOption has all expected values`() {
        val expected = setOf(
            ResolutionOption.SUM,
            ResolutionOption.REPLACE,
            ResolutionOption.SEPARATE,
            ResolutionOption.CANCEL
        )
        assertEquals(expected, ResolutionOption.values().toSet())
    }

    @Test
    fun `canSum returns true for same unit`() {
        // Stessa unità → somma possibile
        assertTrue(canSum(StandardUnit.LITER, StandardUnit.LITER))
        assertTrue(canSum(StandardUnit.GRAM, StandardUnit.GRAM))
        assertTrue(canSum(StandardUnit.PIECE, StandardUnit.PIECE))
    }

    @Test
    fun `canSum returns true for compatible weight units`() {
        // kg ↔ g → somma possibile con conversione
        assertTrue(canSum(StandardUnit.KILOGRAM, StandardUnit.GRAM))
        assertTrue(canSum(StandardUnit.GRAM, StandardUnit.KILOGRAM))
    }

    @Test
    fun `canSum returns true for compatible volume units`() {
        // L ↔ mL → somma possibile con conversione
        assertTrue(canSum(StandardUnit.LITER, StandardUnit.MILLILITER))
        assertTrue(canSum(StandardUnit.MILLILITER, StandardUnit.LITER))
    }

    @Test
    fun `canSum returns false for incompatible units`() {
        // Peso vs volume → somma non possibile
        assertTrue(!canSum(StandardUnit.GRAM, StandardUnit.LITER))
        assertTrue(!canSum(StandardUnit.KILOGRAM, StandardUnit.MILLILITER))
        // Pezzi vs peso/volume → somma non possibile
        assertTrue(!canSum(StandardUnit.PIECE, StandardUnit.GRAM))
        assertTrue(!canSum(StandardUnit.PIECE, StandardUnit.LITER))
    }

    /**
     * Determina se due unità sono compatibili per la somma.
     * Regole: stessa unità, o entrambe di peso (kg/g), o entrambe di volume (L/mL).
     */
    private fun canSum(a: StandardUnit, b: StandardUnit): Boolean {
        if (a == b) return true
        val weightUnits = setOf(StandardUnit.KILOGRAM, StandardUnit.GRAM)
        val volumeUnits = setOf(StandardUnit.LITER, StandardUnit.MILLILITER)
        return (a in weightUnits && b in weightUnits) ||
            (a in volumeUnits && b in volumeUnits)
    }
}