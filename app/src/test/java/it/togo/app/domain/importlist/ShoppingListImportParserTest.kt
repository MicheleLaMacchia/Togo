package it.togo.app.domain.importlist

import it.togo.app.domain.model.StandardUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListImportParserTest {

    private val validText = """
        [FRUTTA E VERDURA]
        + FRUTTA FRESCA
        - MELE
          Golden Delicious 2 pz

        [PASTA, RISO E CEREALI]
        + PASTA SECCA
        - SPAGHETTI
          Spaghetti Barilla integrale 2 pz
    """.trimIndent()

    @Test
    fun `legge categorie voci quantita e unita`() {
        val result = ShoppingListImportParser.parse(validText)

        assertTrue(result is ImportParseResult.Success)
        val list = (result as ImportParseResult.Success).list
        assertEquals(2, list.sections.size)
        assertEquals(2, list.itemCount)

        val mele = list.sections[0].level2s[0].level3s[0].items[0]
        assertEquals("Golden Delicious", mele.description)
        assertEquals(2.0, mele.quantity, 0.0)
        assertEquals(StandardUnit.PIECE, mele.unit)

        val spaghetti = list.sections[1].level2s[0].level3s[0].items[0]
        assertEquals("Spaghetti Barilla integrale", spaghetti.description)
        assertEquals(2.0, spaghetti.quantity, 0.0)
        assertEquals(StandardUnit.PIECE, spaghetti.unit)
    }

    @Test
    fun `accetta CRLF e righe vuote`() {
        val text = validText.replace("\n", "\r\n") + "\r\n\r\n"

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Success)
        assertEquals(2, (result as ImportParseResult.Success).list.itemCount)
    }

    @Test
    fun `voce senza quantita e valida`() {
        val text = """
            [FRUTTA E VERDURA]
            + FRUTTA FRESCA
            - MELE
              Golden Delicious
        """.trimIndent()

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Success)
        val item = (result as ImportParseResult.Success)
            .list.sections[0].level2s[0].level3s[0].items[0]
        assertEquals("Golden Delicious", item.description)
        assertEquals(0.0, item.quantity, 0.0)
        assertEquals(null, item.unit)
    }

    @Test
    fun `accetta decimali con virgola e unita kg`() {
        val text = """
            [FRUTTA E VERDURA]
            + FRUTTA FRESCA
            - MELE
              Golden Delicious 1,5 kg
        """.trimIndent()

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Success)
        val item = (result as ImportParseResult.Success)
            .list.sections[0].level2s[0].level3s[0].items[0]
        assertEquals(1.5, item.quantity, 0.0)
        assertEquals(StandardUnit.KILOGRAM, item.unit)
    }

    @Test
    fun `testo vuoto viene rifiutato`() {
        val result = ShoppingListImportParser.parse("   ")

        assertTrue(result is ImportParseResult.Error)
    }

    @Test
    fun `testo libero senza categorie viene rifiutato`() {
        val result = ShoppingListImportParser.parse("Pane 1 pz\nLatte 2 L")

        assertTrue(result is ImportParseResult.Error)
    }

    @Test
    fun `voce prima della categoria viene rifiutata`() {
        val text = """
            [FRUTTA E VERDURA]
              Golden Delicious 2 pz
        """.trimIndent()

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Error)
        assertTrue((result as ImportParseResult.Error).message.contains("Riga 2"))
    }

    @Test
    fun `categoria senza prodotti viene rifiutata`() {
        val text = """
            [FRUTTA E VERDURA]
            + FRUTTA FRESCA
            - MELE
        """.trimIndent()

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Error)
        assertTrue((result as ImportParseResult.Error).message.contains("non contiene prodotti"))
    }

    @Test
    fun `indentazione con un solo spazio viene rifiutata`() {
        val text = """
            [FRUTTA E VERDURA]
            + FRUTTA FRESCA
            - MELE
             Golden Delicious 2 pz
        """.trimIndent()

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Error)
    }
}
