package it.togo.app.domain.model

import it.togo.app.domain.action.ActionTextNormalizer
import java.util.UUID

/**
 * Azione in testo libero della pagina "Da fare" (Epic 8).
 *
 * Non è un prodotto: non ha quantità, unità, marca, variante, categoria o
 * riferimento al catalogo. Ambito e urgenza sono valori di elenchi chiusi e
 * sono sempre valorizzati.
 *
 * [normalizedText] è la chiave usata dalla deduplica insieme ad ambito e
 * urgenza. La normalizzazione vive nel domain e preserva gli accenti.
 */
data class ActionItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val scope: ActionScope = ActionScope.ALTRO,
    val urgency: ActionUrgency = ActionUrgency.A_TEMPO_PERSO,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val normalizedText: String
        get() = ActionTextNormalizer.normalize(text)
}
