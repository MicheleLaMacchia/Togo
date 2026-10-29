package it.togo.app.domain.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di validazione e normalizzazione delle Task. Test scritti, non eseguiti.
 */
class TaskValidationTest {

    @Test
    fun `titolo vuoto o di soli spazi viene rifiutato`() {
        for (raw in listOf("", "   ", "\n\t", "\t \n ")) {
            val error = assertThrows("titolo=[$raw]", IllegalArgumentException::class.java) {
                TaskValidation.validateAndTrimTitle(raw)
            }
            assertTrue(error.message.orEmpty().contains("titolo", ignoreCase = true))
        }
    }

    @Test
    fun `titolo di 200 code point e ammesso e 201 no`() {
        val valid = "a".repeat(TaskValidation.MAX_TITLE_CODE_POINTS)

        assertEquals(valid, TaskValidation.validateAndTrimTitle(valid))

        val tooLong = "a".repeat(TaskValidation.MAX_TITLE_CODE_POINTS + 1)
        val error = assertThrows(IllegalArgumentException::class.java) {
            TaskValidation.validateAndTrimTitle(tooLong)
        }
        assertTrue(error.message.orEmpty().contains("200"))
    }

    @Test
    fun `emoji e surrogate pair contano come un solo code point`() {
        val valid = "😀".repeat(TaskValidation.MAX_TITLE_CODE_POINTS)

        assertEquals(valid, TaskValidation.validateAndTrimTitle(valid))

        val tooLong = "😀".repeat(TaskValidation.MAX_TITLE_CODE_POINTS + 1)
        assertThrows(IllegalArgumentException::class.java) {
            TaskValidation.validateAndTrimTitle(tooLong)
        }
    }

    @Test
    fun `titolo con caratteri di controllo viene rifiutato`() {
        val invalidTitles = listOf(
            "Prima riga\nSeconda riga",
            "Prima colonna\tSeconda colonna",
            "Titolo\u0000con null",
            "Titolo\u0007con bell",
            "Titolo\u007Fcon delete",
        )

        for (title in invalidTitles) {
            assertThrows("titolo=[$title]", IllegalArgumentException::class.java) {
                TaskValidation.validateAndTrimTitle(title)
            }
        }
    }

    @Test
    fun `titolo viene ripulito dagli spazi ai bordi`() {
        assertEquals("Comprare latte", TaskValidation.validateAndTrimTitle("  Comprare latte  "))
    }

    @Test
    fun `nota null o blank diventa null`() {
        assertNull(TaskValidation.validateAndNormalizeNote(null))
        assertNull(TaskValidation.validateAndNormalizeNote(""))
        assertNull(TaskValidation.validateAndNormalizeNote("   "))
        assertNull(TaskValidation.validateAndNormalizeNote("\n\t"))
    }

    @Test
    fun `nota di 1000 code point e ammessa e 1001 no`() {
        val valid = "a".repeat(TaskValidation.MAX_NOTE_CODE_POINTS)

        assertEquals(valid, TaskValidation.validateAndNormalizeNote(valid))

        val tooLong = "a".repeat(TaskValidation.MAX_NOTE_CODE_POINTS + 1)
        val error = assertThrows(IllegalArgumentException::class.java) {
            TaskValidation.validateAndNormalizeNote(tooLong)
        }
        assertTrue(error.message.orEmpty().contains("1000"))
    }

    @Test
    fun `nota ammette newline e tab`() {
        val note = "  Prima riga\nSeconda\tcolonna  "

        assertEquals(
            "Prima riga\nSeconda\tcolonna",
            TaskValidation.validateAndNormalizeNote(note),
        )
    }

    @Test
    fun `nota con altri caratteri di controllo viene rifiutata`() {
        val invalidNotes = listOf(
            "Nota\u0000con null",
            "Nota\u0007con bell",
            "Nota\u007Fcon delete",
            "Nota con ritorno\rcarrello",
            "Nota con vertical\u000Btab",
        )

        for (note in invalidNotes) {
            assertThrows("nota=[$note]", IllegalArgumentException::class.java) {
                TaskValidation.validateAndNormalizeNote(note)
            }
        }
    }

    @Test
    fun `normalizeTitle applica trim lowercase e spazi collassati`() {
        assertEquals(
            "caffè latte intero",
            TaskValidation.normalizeTitle("  Caffè   Latte    Intero  "),
        )
    }

    @Test
    fun `normalizeTitle preserva gli accenti e non e univoco`() {
        assertEquals("perché", TaskValidation.normalizeTitle("  PERCHÉ  "))
        assertEquals(
            TaskValidation.normalizeTitle("Latte"),
            TaskValidation.normalizeTitle("  latte  "),
        )
    }
}
