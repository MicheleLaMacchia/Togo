package it.togo.app.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test della normalizzazione nomi prodotto usata dal controllo duplicati
 * della Story 7.1 (uguaglianza dopo normalizzazione, non fuzzy).
 */
class ProductNameNormalizerTest {

    @Test
    fun `accenti rimossi e minuscole`() {
        assertEquals("caffe", ProductNameNormalizer.normalize("Caffè"))
        assertEquals(
            ProductNameNormalizer.normalize("Caffè"),
            ProductNameNormalizer.normalize("caffe"),
        )
    }

    @Test
    fun `trim e spazi interni collassati`() {
        assertEquals("caffe latte", ProductNameNormalizer.normalize("  CAFFE   Latte  "))
    }

    @Test
    fun `stringa vuota o soli spazi diventa vuota`() {
        assertEquals("", ProductNameNormalizer.normalize(""))
        assertEquals("", ProductNameNormalizer.normalize("   "))
        assertEquals("", ProductNameNormalizer.normalize("\t\n"))
    }

    @Test
    fun `accenti multipli e lettere accentate diverse`() {
        assertEquals("pera e pesca", ProductNameNormalizer.normalize("Pèra é Pèsca"))
        assertEquals("creme brulee", ProductNameNormalizer.normalize("Crème Brûlée"))
    }

    @Test
    fun `nomi diversi restano diversi`() {
        assertEquals(false, ProductNameNormalizer.normalize("Latte intero") == ProductNameNormalizer.normalize("Latte parzialmente scremato"))
    }
}
