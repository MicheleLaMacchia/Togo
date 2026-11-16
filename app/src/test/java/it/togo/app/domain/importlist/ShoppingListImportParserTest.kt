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
        assertEquals(null, list.actionSection)

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
    fun `riconosce da fare come sezione azioni distinta dalla tassonomia`() {
        val text = """
            [FRUTTA E VERDURA]
            + FRUTTA FRESCA
            - MELE
              Golden Delicious 2 pz

            [DA FARE]
            + CASA
            - URGENTE
              Chiamare il medico
        """.trimIndent()

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Success)
        val list = (result as ImportParseResult.Success).list
        assertEquals(listOf("FRUTTA E VERDURA"), list.sections.map { it.name })
        assertEquals(
            listOf(
                ParsedActionLine(7, "+ CASA"),
                ParsedActionLine(8, "- URGENTE"),
                ParsedActionLine(9, "  Chiamare il medico"),
            ),
            list.actionSection?.lines,
        )
        assertEquals(6, list.actionSection?.lineNumber)
    }

    @Test
    fun `riconosce strutturalmente il solo blocco da fare`() {
        val result = ShoppingListImportParser.parse(
            "[DA FARE]\n+ CASA\n- URGENTE\n  Chiamare il medico",
        )

        assertTrue(result is ImportParseResult.Success)
        val list = (result as ImportParseResult.Success).list
        assertTrue(list.sections.isEmpty())
        assertEquals(0, list.itemCount)
        assertEquals(
            listOf(
                ParsedActionLine(2, "+ CASA"),
                ParsedActionLine(3, "- URGENTE"),
                ParsedActionLine(4, "  Chiamare il medico"),
            ),
            list.actionSection?.lines,
        )
        assertEquals(1, list.actionSection?.lineNumber)
    }

    @Test
    fun `riprende il parsing delle categorie dopo il blocco da fare`() {
        val text = """
            [DA FARE]
            + CASA
            - URGENTE
              Chiamare il medico

            [FRUTTA E VERDURA]
            + FRUTTA FRESCA
            - MELE
              Golden Delicious 2 pz
        """.trimIndent()

        val result = ShoppingListImportParser.parse(text)

        assertTrue(result is ImportParseResult.Success)
        val list = (result as ImportParseResult.Success).list
        assertEquals(listOf("FRUTTA E VERDURA"), list.sections.map { it.name })
        assertEquals(1, list.actionSection?.lineNumber)
        assertEquals(1, list.itemCount)
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
