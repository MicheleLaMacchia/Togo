package it.togo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test JVM per StandardUnit: conversioni, normalizzazione, validazione, formattazione.
 */
class StandardUnitTest {

    // ==================== CONVERSIONI ====================

    @Test
    fun `convert kg to g`() {
        val result = StandardUnit.KILOGRAM.convert(1.5, StandardUnit.GRAM)
        assertEquals(1500.0, result!!, 0.001)
    }

    @Test
    fun `convert g to kg`() {
        val result = StandardUnit.GRAM.convert(1500.0, StandardUnit.KILOGRAM)
        assertEquals(1.5, result!!, 0.001)
    }

    @Test
    fun `convert l to ml`() {
        val result = StandardUnit.LITER.convert(2.5, StandardUnit.MILLILITER)
        assertEquals(2500.0, result!!, 0.001)
    }

    @Test
    fun `convert ml to l`() {
        val result = StandardUnit.MILLILITER.convert(500.0, StandardUnit.LITER)
        assertEquals(0.5, result!!, 0.001)
    }

    @Test
    fun `convert incompatible units returns null`() {
        val result = StandardUnit.PIECE.convert(1.0, StandardUnit.GRAM)
        assertNull(result)
    }

    @Test
    fun `convert zero returns null`() {
        val result = StandardUnit.GRAM.convert(0.0, StandardUnit.KILOGRAM)
        assertNull(result)
    }

    @Test
    fun `convert negative returns null`() {
        val result = StandardUnit.GRAM.convert(-5.0, StandardUnit.KILOGRAM)
        assertNull(result)
    }

    @Test
    fun `convert NaN returns null`() {
        val result = StandardUnit.GRAM.convert(Double.NaN, StandardUnit.KILOGRAM)
        assertNull(result)
    }

    @Test
    fun `convert Infinity returns null`() {
        val result = StandardUnit.GRAM.convert(Double.POSITIVE_INFINITY, StandardUnit.KILOGRAM)
        assertNull(result)
    }

    // ==================== NORMALIZZAZIONE ====================

    @Test
    fun `normalize kg to base unit g`() {
        val result = StandardUnit.KILOGRAM.normalize(2.0)
        assertEquals(2000.0, result!!.first, 0.001)
        assertEquals(StandardUnit.GRAM, result.second)
    }

    @Test
    fun `normalize l to base unit ml`() {
        val result = StandardUnit.LITER.normalize(1.5)
        assertEquals(1500.0, result!!.first, 0.001)
        assertEquals(StandardUnit.MILLILITER, result.second)
    }

    @Test
    fun `normalize g stays g`() {
        val result = StandardUnit.GRAM.normalize(500.0)
        assertEquals(500.0, result!!.first, 0.001)
        assertEquals(StandardUnit.GRAM, result.second)
    }

    @Test
    fun `normalize piece stays piece`() {
        val result = StandardUnit.PIECE.normalize(3.0)
        assertEquals(3.0, result!!.first, 0.001)
        assertEquals(StandardUnit.PIECE, result.second)
    }

    @Test
    fun `normalize zero returns null`() {
        val result = StandardUnit.GRAM.normalize(0.0)
        assertNull(result)
    }

    // ==================== FORMATTAZIONE ====================

    @Test
    fun `format 1500 g as kg`() {
        val result = StandardUnit.GRAM.format(1500.0)
        assertEquals("1.5 kg", result)
    }

    @Test
    fun `format 500 g as g`() {
        val result = StandardUnit.GRAM.format(500.0)
        assertEquals("500 g", result)
    }

    @Test
    fun `format 2500 ml as L`() {
        val result = StandardUnit.MILLILITER.format(2500.0)
        assertEquals("2.5 L", result)
    }

    @Test
    fun `format 500 ml as ml`() {
        val result = StandardUnit.MILLILITER.format(500.0)
        assertEquals("500 ml", result)
    }

    @Test
    fun `format piece`() {
        val result = StandardUnit.PIECE.format(1.0)
        assertEquals("1 pz", result)
    }

    @Test
    fun `format zero returns 0`() {
        val result = StandardUnit.GRAM.format(0.0)
        assertEquals("0 g", result)
    }

    // ==================== VALIDAZIONE ====================

    @Test
    fun `isValidQuantity positive`() {
        assertTrue(StandardUnit.GRAM.isValidQuantity(1.0))
    }

    @Test
    fun `isValidQuantity zero`() {
        assertFalse(StandardUnit.GRAM.isValidQuantity(0.0))
    }

    @Test
    fun `isValidQuantity negative`() {
        assertFalse(StandardUnit.GRAM.isValidQuantity(-1.0))
    }

    @Test
    fun `isValidQuantity NaN`() {
        assertFalse(StandardUnit.GRAM.isValidQuantity(Double.NaN))
    }

    @Test
    fun `isValidQuantity Infinity`() {
        assertFalse(StandardUnit.GRAM.isValidQuantity(Double.POSITIVE_INFINITY))
    }

    // ==================== COMPATIBILITÀ ====================

    @Test
    fun `isCompatibleWith same unit`() {
        assertTrue(StandardUnit.GRAM.isCompatibleWith(StandardUnit.GRAM))
    }

    @Test
    fun `isCompatibleWith kg and g`() {
        assertTrue(StandardUnit.KILOGRAM.isCompatibleWith(StandardUnit.GRAM))
    }

    @Test
    fun `isCompatibleWith l and ml`() {
        assertTrue(StandardUnit.LITER.isCompatibleWith(StandardUnit.MILLILITER))
    }

    @Test
    fun `isCompatibleWith incompatible`() {
        assertFalse(StandardUnit.PIECE.isCompatibleWith(StandardUnit.GRAM))
    }

    // ==================== FROM CODE ====================

    @Test
    fun `fromCode G`() {
        assertEquals(StandardUnit.GRAM, StandardUnit.fromCode("G"))
    }

    @Test
    fun `fromCode KG`() {
        assertEquals(StandardUnit.KILOGRAM, StandardUnit.fromCode("KG"))
    }

    @Test
    fun `fromCode lowercase`() {
        assertEquals(StandardUnit.GRAM, StandardUnit.fromCode("g"))
    }

    @Test
    fun `fromCode invalid returns null`() {
        assertNull(StandardUnit.fromCode("INVALID"))
    }
}