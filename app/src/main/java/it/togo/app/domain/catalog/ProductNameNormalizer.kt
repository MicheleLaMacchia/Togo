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

    /**
     * Forma "capitalized" usata per salvare prodotti, marche e varianti:
     * iniziale maiuscola, resto minuscolo, spazi interni collassati.
     *
     * "pasta", "PASTA" e "  pasta  " diventano tutte "Pasta", così la stessa
     * cosa non genera valori diversi nel catalogo. Per le stringhe che iniziano
     * con cifre o simboli viene resa maiuscola la prima lettera (es. "3 pane"
     * -> "3 Pane").
     */
    fun capitalizeFirstWord(raw: String): String {
        val collapsed = WHITESPACE.replace(raw.trim(), " ").trim()
        if (collapsed.isEmpty()) return ""

        val lower = collapsed.lowercase(Locale.ROOT)
        val firstLetter = lower.indexOfFirst { it.isLetter() }
        if (firstLetter < 0) return lower

        return lower.substring(0, firstLetter) +
            lower[firstLetter].uppercaseChar() +
            lower.substring(firstLetter + 1)
    }
}
