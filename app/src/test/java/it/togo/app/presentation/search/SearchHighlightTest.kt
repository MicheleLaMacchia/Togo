package it.togo.app.presentation.search

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test JVM per l'evidenziazione della ricerca (Story 6.3).
 *
 * Verifica che i colori passati da [SearchResultItem] arrivino allo [SpanStyle]
 * e che la ricerca case-insensitive non esca dagli indici della stringa.
 */
class SearchHighlightTest {

    private val background = Color(0xFF60A5FA)
    private val foreground = Color(0xFF0F172A)

    @Test
    fun `highlight applies the provided background and foreground`() {
        val annotated = buildAnnotatedStringHighlight(
            text = "Mozzarella",
            query = "zza",
            highlightBackground = background,
            highlightForeground = foreground,
        )

        val span = annotated.spanStyles.single().item
        assertEquals(background, span.background)
        assertEquals(foreground, span.color)
        assertEquals(2, annotated.spanStyles.single().start)
        assertEquals(5, annotated.spanStyles.single().end)
    }

    @Test
    fun `highlight is case insensitive`() {
        val annotated = buildAnnotatedStringHighlight(
            text = "Mozzarella",
            query = "MOZZ",
            highlightBackground = background,
            highlightForeground = foreground,
        )

        val span = annotated.spanStyles.single()
        assertEquals(0, span.start)
        assertEquals(4, span.end)
    }

    @Test
    fun `highlight finds repeated occurrences`() {
        val annotated = buildAnnotatedStringHighlight(
            text = "aba aba",
            query = "aba",
            highlightBackground = background,
            highlightForeground = foreground,
        )

        assertEquals(2, annotated.spanStyles.size)
        assertEquals(0, annotated.spanStyles[0].start)
        assertEquals(4, annotated.spanStyles[1].start)
    }

    @Test
    fun `highlight does not crash when lowercase changes the string length`() {
        // 'İ'.lowercase() produce due code point: il vecchio confronto su
        // lowerText usava indici non validi per la stringa originale.
        val annotated = buildAnnotatedStringHighlight(
            text = "İa",
            query = "a",
            highlightBackground = background,
            highlightForeground = foreground,
        )

        assertTrue(annotated.spanStyles.isNotEmpty())
        assertEquals(1, annotated.spanStyles.single().start)
        assertEquals(2, annotated.spanStyles.single().end)
    }

    @Test
    fun `blank query leaves the text unstyled`() {
        val annotated = buildAnnotatedStringHighlight(
            text = "Mozzarella",
            query = "  ",
            highlightBackground = background,
            highlightForeground = foreground,
        )

        assertTrue(annotated.spanStyles.isEmpty())
        assertEquals("Mozzarella", annotated.text)
    }
}
