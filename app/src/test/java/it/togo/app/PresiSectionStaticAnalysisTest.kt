package it.togo.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica: verifica che il pulsante "Concludi spesa" sia dentro la Column.
 *
 * Fix 8: Pulsante "Concludi spesa" dentro Column — spostato dopo items.forEach, non sibling esterno
 */
class PresiSectionStaticAnalysisTest {

    private val sourceFile: File
        get() = File("app/src/main/java/it/togo/app/presentation/activelist/PresiSection.kt").let {
            if (it.exists()) it
            else File("src/main/java/it/togo/app/presentation/activelist/PresiSection.kt")
        }

    @Test
    fun `pulsante checkout e dentro la Column non sibling`() {
        val source = sourceFile.readText()

        val forEachPos = source.indexOf("items.forEach")
        // Usa il testo del Button, non il commento nel KDoc (che è prima di forEach)
        val buttonTextPos = source.indexOf("text = \"Concludi spesa\"")

        assertTrue("items.forEach deve essere presente", forEachPos >= 0)
        assertTrue("pulsante 'Concludi spesa' deve essere presente", buttonTextPos >= 0)
        assertTrue(
            "Button deve essere DOPO items.forEach (dentro la Column)",
            buttonTextPos > forEachPos
        )
    }

    @Test
    fun `pulsante checkout non ha padding orizzontale esterno`() {
        val source = sourceFile.readText()

        // La fix rimuove padding(horizontal = spacing.space2) dal Button:
        // ispeziona il blocco dal Button( fino al testo "Concludi spesa".
        val buttonTextPos = source.indexOf("text = \"Concludi spesa\"")
        val buttonStart = source.lastIndexOf("Button(", buttonTextPos)
        assertTrue("Button( deve precedere il testo", buttonStart >= 0 && buttonStart < buttonTextPos)

        val buttonBlock = source.substring(buttonStart, buttonTextPos)
        assertFalse(
            "Button non deve avere padding orizzontale esterno (rimosso dalla fix)",
            buttonBlock.contains("padding(horizontal")
        )
    }
}
