package it.togo.app

import it.togo.app.domain.model.StandardUnit
import it.togo.app.presentation.activelist.formatQuantity
import it.togo.app.presentation.activelist.quantityLabel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test comportamentali della formattazione quantità usata dalla riga.
 */
class ItemRowFormattingTest {

    @Test
    fun `quantity label uses italian comma and full unit name`() {
        assertEquals("1,5 chilogrammi", quantityLabel(1.5, StandardUnit.KILOGRAM))
        assertEquals("2 pezzi", quantityLabel(2.0, StandardUnit.PIECE))
    }

    @Test
    fun `quantity label uses singular for one unit`() {
        assertEquals("1 pezzo", quantityLabel(1.0, StandardUnit.PIECE))
        assertEquals("1 confezione", quantityLabel(1.0, StandardUnit.PACKAGE))
    }

    @Test
    fun `formatQuantity strips trailing zeros and handles invalid values`() {
        assertEquals("2", formatQuantity(2.0))
        assertEquals("0", formatQuantity(0.0))
        assertEquals("0", formatQuantity(Double.NaN))
        assertEquals("0", formatQuantity(Double.POSITIVE_INFINITY))
    }
}
