package it.togo.app.domain.parser

import it.togo.app.domain.model.StandardUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test JVM per VoiceCommandParser.
 * Verifica estrazione prodotto, quantità, unità, categoria da testo grezzo.
 */
class VoiceCommandParserTest {

    private lateinit var parser: VoiceCommandParser

    @Before
    fun setup() {
        parser = VoiceCommandParser()
    }

    // --- Comando semplice ---

    @Test
    fun `comando semplice solo prodotto`() {
        val result = parser.parse("latte")

        assertEquals("latte", result.productName)
        assertNull(result.quantity)
        assertNull(result.unit)
        assertEquals(0.8, result.confidence, 0.001)
        assertTrue(result.isValid)
    }

    @Test
    fun `comando con accento preservato`() {
        val result = parser.parse("caffè")

        assertEquals("caffè", result.productName)
        assertTrue(result.confidence > 0)
    }

    // --- Quantità numerica ---

    @Test
    fun `quantità intera`() {
        val result = parser.parse("2 latte")

        assertEquals("latte", result.productName)
        assertEquals(2.0, result.quantity!!, 0.001)
        assertNull(result.unit)
        assertTrue(result.needsClarification)
        assertEquals(0.9, result.confidence, 0.001)
    }

    @Test
    fun `quantità decimale con virgola`() {
        val result = parser.parse("1,5 kg di riso")

        assertEquals("riso", result.productName)
        assertEquals(1.5, result.quantity!!, 0.001)
        assertEquals(StandardUnit.KILOGRAM, result.unit)
    }

    @Test
    fun `quantità decimale con punto`() {
        val result = parser.parse("1.5 kg di riso")

        assertEquals("riso", result.productName)
        assertEquals(1.5, result.quantity!!, 0.001)
        assertEquals(StandardUnit.KILOGRAM, result.unit)
    }

    // --- Quantità parole italiane ---

    @Test
    fun `quantità mezzo`() {
        val result = parser.parse("mezzo chilo di caffè")

        assertEquals("caffè", result.productName)
        assertEquals(0.5, result.quantity!!, 0.001)
        assertEquals(StandardUnit.KILOGRAM, result.unit)
    }

    @Test
    fun `quantità un`() {
        val result = parser.parse("un litro di latte")

        assertEquals("latte", result.productName)
        assertEquals(1.0, result.quantity!!, 0.001)
        assertEquals(StandardUnit.LITER, result.unit)
    }

    @Test
    fun `quantità due`() {
        val result = parser.parse("due mele")

        assertEquals("mele", result.productName)
        assertEquals(2.0, result.quantity!!, 0.001)
    }

    @Test
    fun `parola numerica in prodotto non estrae quantità`() {
        // "funghi" contiene "un" ma con word boundary non deve matchare
        val result = parser.parse("funghi")

        assertEquals("funghi", result.productName)
        assertNull(result.quantity)
    }

    @Test
    fun `prosciutto non estrae otto`() {
        // "prosciutto" contiene "otto" ma con word boundary non deve matchare
        val result = parser.parse("prosciutto")

        assertEquals("prosciutto", result.productName)
        assertNull(result.quantity)
    }

    // --- Unità ---

    @Test
    fun `unità litro`() {
        val result = parser.parse("2 litri di latte")

        assertEquals(StandardUnit.LITER, result.unit)
        assertEquals(1.0, result.confidence, 0.001)
    }

    @Test
    fun `unità chilogrammo`() {
        val result = parser.parse("1 kg di riso")

        assertEquals(StandardUnit.KILOGRAM, result.unit)
    }

    @Test
    fun `unità millilitro`() {
        val result = parser.parse("500 ml di latte")

        assertEquals(StandardUnit.MILLILITER, result.unit)
    }

    @Test
    fun `unità pezzi`() {
        val result = parser.parse("3 pezzi di pizza")

        assertEquals(StandardUnit.PIECE, result.unit)
    }

    @Test
    fun `unità confezione`() {
        val result = parser.parse("1 conf di pasta")

        assertEquals(StandardUnit.PACKAGE, result.unit)
    }

    // --- Categoria hint ---

    @Test
    fun `hint categoria latticini`() {
        val result = parser.parse("latte 2 litri latticini")

        assertEquals("latticini", result.categoryHint)
        assertTrue(result.confidence > 0.9)
    }

    @Test
    fun `hint categoria bevande`() {
        val result = parser.parse("succo di frutta 1 litro bevande")

        assertNotNull(result.categoryHint)
    }

    // --- Casi limite ---

    @Test
    fun `testo vuoto ritorna confidence zero`() {
        val result = parser.parse("")

        assertEquals(0.0, result.confidence, 0.001)
        assertFalse(result.isValid)
        assertTrue(result.needsClarification)
    }

    @Test
    fun `solo spazi ritorna confidence zero`() {
        val result = parser.parse("   ")

        assertEquals(0.0, result.confidence, 0.001)
        assertFalse(result.isValid)
    }

    @Test
    fun `quantità ma non unità needs clarification`() {
        val result = parser.parse("2 di latte")

        assertEquals("latte", result.productName)
        assertEquals(2.0, result.quantity!!, 0.001)
        assertNull(result.unit)
        assertTrue(result.needsClarification)
    }

    @Test
    fun `comando multi prodotto primo estratto`() {
        val result = parser.parse("latte e caffè")

        // MVP: solo primo prodotto (split su "e")
        assertEquals("latte", result.productName)
    }

    @Test
    fun `parole da rimuovere compra aggiungi`() {
        val result = parser.parse("compra latte")

        assertEquals("latte", result.productName)
        assertFalse(result.productName.contains("compra"))
    }

    @Test
    fun `frase completa`() {
        val result = parser.parse("aggiungi 2 litri di latte intero")

        assertEquals("latte intero", result.productName)
        assertEquals(2.0, result.quantity!!, 0.001)
        assertEquals(StandardUnit.LITER, result.unit)
    }

    @Test
    fun `preposizioni articolate rimosse correttamente`() {
        val result = parser.parse("2 litri della latte")

        assertEquals("latte", result.productName)
        assertEquals(2.0, result.quantity!!, 0.001)
    }

    @Test
    fun `parola che inizia con di non mutilata`() {
        // "disco" inizia con "di" ma con word boundary non deve essere rimosso
        val result = parser.parse("disco")

        assertEquals("disco", result.productName)
    }

    @Test(timeout = 100)
    fun `parsing completato sotto 100ms`() {
        parser.parse("2 litri di latte")
    }
}