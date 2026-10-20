package it.togo.app.domain.text

import java.text.Normalizer

/**
 * Normalizzazione condivisa delle espressioni utente.
 *
 * Stessa semantica di `CatalogQueries.normalize`: trim, minuscole,
 * decomposizione NFKD e rimozione dei segni diacritici.
 */
fun normalizeExpression(raw: String): String {
    val decomposed = Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFKD)
    return buildString(decomposed.length) {
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
}
