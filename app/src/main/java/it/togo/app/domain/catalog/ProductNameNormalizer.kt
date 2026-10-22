package it.togo.app.domain.catalog

import java.text.Normalizer
import java.util.Locale

/**
 * Normalizzazione dei nomi prodotto per il confronto dei duplicati (Story 7.1).
 *
 * La definizione di "nome semanticamente identico" è vincolante:
 * uguaglianza dopo normalizzazione (trim, minuscole, accent-fold, spazi
 * collassati). Non è una similarità fuzzy: nessuna soglia, nessun Levenshtein.
 */
object ProductNameNormalizer {

    /** Compilata una sola volta: `normalize` è chiamata per ogni prodotto del L3. */
    private val WHITESPACE = Regex("\\s+")

    /**
     * Ritorna il nome normalizzato:
     * - trim iniziale/finale;
     * - stringa vuota se non resta nulla;
     * - minuscole con [Locale.ROOT];
     * - decomposizione NFKD e rimozione dei segni diacritici;
     * - spazi interni collassati a uno solo.
     */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        val decomposed = Normalizer.normalize(trimmed.lowercase(Locale.ROOT), Normalizer.Form.NFKD)
        val withoutMarks = buildString(decomposed.length) {
            for (ch in decomposed) {
                val type = Character.getType(ch)
                if (type != Character.NON_SPACING_MARK.toInt() &&
                    type != Character.COMBINING_SPACING_MARK.toInt() &&
                    type != Character.ENCLOSING_MARK.toInt()
                ) {
                    append(ch)
                }
            }
        }
        return WHITESPACE.replace(withoutMarks, " ").trim()
    }
}
