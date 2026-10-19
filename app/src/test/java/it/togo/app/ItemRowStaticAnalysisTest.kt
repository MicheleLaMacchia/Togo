package it.togo.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica: verifica che ItemRow.kt NON usi alpha per le righe spuntate.
 *
 * Fix 6: Trasparenza righe checked — rimosso val alpha e tutti i .copy(alpha = alpha)
 */
class ItemRowStaticAnalysisTest {

    private val sourceFile: File
        get() = File("app/src/main/java/it/togo/app/presentation/activelist/ItemRow.kt").let {
            if (it.exists()) it
            else File("src/main/java/it/togo/app/presentation/activelist/ItemRow.kt")
        }

    @Test
    fun `ItemRow non usa alpha per righe spuntate`() {
        val source = sourceFile.readText()

        assertFalse(
            "ItemRow non deve contenere 'copy(alpha' — rimosso dalla fix",
            source.contains("copy(alpha")
        )
        assertFalse(
            "ItemRow non deve contenere '0.45f' — valore alpha rimosso",
            source.contains("0.45f")
        )
    }

    @Test
    fun `ItemRow usa colori pieni senza alpha`() {
        val source = sourceFile.readText()

        // Verifica che i colori siano usati direttamente (non .copy)
        assertTrue(
            "background deve usare colore diretto (senza copy alpha)",
            source.contains("color = tokens.itemRow.background")
        )
        assertTrue(
            "textColor deve essere assegnato direttamente (senza copy alpha)",
            source.contains("color = textColor") || source.contains("color = colors.inkMuted")
        )
    }
}
