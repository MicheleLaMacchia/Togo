package it.togo.app.domain.task

import java.util.Locale

/**
 * Validazione e normalizzazione dei campi testuali di una Task.
 *
 * I limiti sono espressi in code point, non in unità UTF-16: una emoji
 * composta da una surrogate pair conta come un solo carattere.
 */
object TaskValidation {

    const val MAX_TITLE_CODE_POINTS = 200
    const val MAX_NOTE_CODE_POINTS = 1000

    fun validateAndTrimTitle(raw: String): String {
        val title = raw.trim()
        require(title.isNotBlank()) { "Il titolo è obbligatorio" }
        require(title.codePointCount(0, title.length) <= MAX_TITLE_CODE_POINTS) {
            "Il titolo non può superare $MAX_TITLE_CODE_POINTS code point"
        }
        require(!containsControlCharacters(title, allowNewlineAndTab = false)) {
            "Il titolo contiene caratteri di controllo non ammessi"
        }
        return title
    }

    fun validateAndNormalizeNote(raw: String?): String? {
        val note = raw?.trim() ?: return null
        if (note.isBlank()) return null

        require(note.codePointCount(0, note.length) <= MAX_NOTE_CODE_POINTS) {
            "La nota non può superare $MAX_NOTE_CODE_POINTS code point"
        }
        require(!containsControlCharacters(note, allowNewlineAndTab = true)) {
            "La nota contiene caratteri di controllo non ammessi"
        }
        return note
    }

    fun normalizeTitle(title: String): String =
        title.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")

    private fun containsControlCharacters(
        value: String,
        allowNewlineAndTab: Boolean,
    ): Boolean {
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            val isAllowedWhitespace = allowNewlineAndTab &&
                (codePoint == '\n'.code || codePoint == '\t'.code)

            if (Character.isISOControl(codePoint) && !isAllowedWhitespace) return true
            index += Character.charCount(codePoint)
        }
        return false
    }
}
