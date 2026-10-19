package it.togo.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica: verifica che CategoryHeader.kt disegni solo il bordo sinistro.
 *
 * Fix 7: Bordo CategoryHeader solo sinistro — .border() (4 lati) → .drawBehind { drawLine(...) } (solo sinistro)
 */
class CategoryHeaderStaticAnalysisTest {

    private val sourceFile: File
        get() = File("app/src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt").let {
            if (it.exists()) it
            else File("src/main/java/it/togo/app/presentation/activelist/CategoryHeader.kt")
        }

    @Test
    fun `CategoryHeader usa drawBehind per bordo sinistro`() {
        val source = sourceFile.readText()

        assertTrue(
            "CategoryHeader deve usare drawBehind per disegnare il bordo",
            source.contains("drawBehind")
        )
        assertTrue(
            "CategoryHeader deve usare drawLine per il bordo sinistro",
            source.contains("drawLine")
        )
        assertFalse(
            "CategoryHeader non deve più usare Modifier.border(",
            source.contains("Modifier.border(")
        )
        assertFalse(
            "CategoryHeader non deve importare .border",
            source.contains("import androidx.compose.foundation.border")
        )
    }

    @Test
    fun `CategoryHeader disegna linea verticale sul lato sinistro`() {
        val source = sourceFile.readText()

        // La linea va dall'alto (0,0) al basso (0, height) — solo lato sinistro
        assertTrue(
            "drawLine deve partire da Offset(0f, 0f)",
            source.contains("Offset(0f, 0f)")
        )
        assertTrue(
            "drawLine deve terminare a Offset(0f, size.height)",
            source.contains("Offset(0f, size.height)")
        )
    }
}
