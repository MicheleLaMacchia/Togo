package it.togo.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica: verifica che l'indicatore del wizard disegni cerchio prima della linea.
 *
 * Fix 9: Wizard step indicator — ordine invertito (cerchio → linea, prima era linea → cerchio)
 */
class WizardStepIndicatorTest {

    private val sourceFile: File
        get() = File("app/src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt").let {
            if (it.exists()) it
            else File("src/main/java/it/togo/app/presentation/additem/AddItemBottomSheet.kt")
        }

    @Test
    fun `wizard indicatore disegna cerchio prima della linea`() {
        val source = sourceFile.readText()

        // Estrai il blocco indicatore (tra il commento e la fine del Row)
        val startIndex = source.indexOf("Progress indicator")
        val endIndex = source.indexOf("Contenuto step", startIndex)
        assertTrue("Blocco 'Progress indicator' trovato", startIndex >= 0)
        assertTrue("Marcatore 'Contenuto step' trovato", endIndex >= 0)

        val bloccoIndicatore = source.substring(startIndex, endIndex)

        val circlePos = bloccoIndicatore.indexOf("CircleShape")
        val linePos = bloccoIndicatore.indexOf("height(2.dp)")

        assertTrue("Cerchio (CircleShape) trovato nel blocco", circlePos >= 0)
        assertTrue("Linea (height(2.dp)) trovata nel blocco", linePos >= 0)
        assertTrue(
            "Cerchio deve essere disegnato PRIMA della linea",
            circlePos < linePos
        )
    }

    @Test
    fun `wizard indicatore linea usa colore per step completati`() {
        val source = sourceFile.readText()

        val startIndex = source.indexOf("Progress indicator")
        val endIndex = source.indexOf("Contenuto step", startIndex)
        val bloccoIndicatore = source.substring(startIndex, endIndex)

        // La fix cambia >= step → > step per il colore della linea
        assertTrue(
            "Linea deve usare currentStep > step (non >=)",
            bloccoIndicatore.contains("uiState.currentStep > step")
        )
    }
}
