package it.togo.app.data.database

import java.text.Normalizer

/**
 * SQL e normalizzazioni condivisi per la ricerca nel catalogo.
 *
 * Unica fonte di verità della query di ricerca (`SEARCH`), usata sia dal
 * [it.togo.app.data.database.dao.CatalogDao] (via Room) sia dal test JVM
 * `CatalogAssetTest` (via sqlite-jdbc) per evitare drift tra le due.
 *
 * Il parametro `:query` va normalizzato (minuscolo + accent-fold) e "escapato"
 * per `LIKE ... ESCAPE` prima del binding: la ricerca su "caffe" deve
 * trovare "Caffè" grazie ai sinonimi accent-fold generati in `catalog.db`.
 */
object CatalogQueries {

    const val SEARCH: String = """
        SELECT cp.* FROM CANONICAL_PRODUCT cp
        JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
        JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
        JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
        WHERE :query != ''
           AND (
               cp.name LIKE '%' || :query || '%' ESCAPE '\'
                OR EXISTS (
                    SELECT 1 FROM SYNONYM s
                    WHERE s.product_id = cp.id
                      AND s.term LIKE '%' || :query || '%' ESCAPE '\'
                )
           )
        ORDER BY t1.sortOrder, t2.sortOrder, t3.sortOrder, cp.name
        LIMIT 50
    """

    private const val LIKE_ESCAPE_CHAR = '\\'

    /**
     * "accent-fold" della query: trim, minuscole, decomposizione NFKD e
     * rimozione dei segni diacritici (es. "Caffè" -> "caffe").
     */
    fun normalize(raw: String): String {
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

    /** Escapa `\`, `%` e `_` affinché il testo dell'utente sia letterale in LIKE. */
    fun escapeLike(input: String): String = buildString(input.length * 2) {
        for (ch in input) {
            when (ch) {
                LIKE_ESCAPE_CHAR -> append("\\\\")
                '%' -> append("\\%")
                '_' -> append("\\_")
                else -> append(ch)
            }
        }
    }

    /** Ritorna true se la query normalizzata è vuota (nessun risultato). */
    fun isBlank(raw: String): Boolean = normalize(raw).isEmpty()
}