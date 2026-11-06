package it.togo.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import it.togo.app.domain.action.ActionTextNormalizer

/**
 * Azione in testo libero della pagina "Da fare" (Epic 8, Story 8.1).
 *
 * La tabella è indipendente da `SHOPPING_ITEM` e dal catalogo prodotti: testo,
 * ambito e urgenza non sono un prodotto, una categoria o un attributo.
 *
 * `testo_normalizzato` è la chiave usata dalla deduplica (testo + ambito +
 * urgenza). Viene calcolata da [ActionTextNormalizer], che preserva gli
 * accenti; non va usato [it.togo.app.domain.catalog.ProductNameNormalizer].
 *
 * Attenzione: `copy()` non ricalcola i valori di default. Quando si modifica
 * `testo`, aggiornare esplicitamente anche `testoNormalizzato`.
 */
@Entity(
    tableName = "ACTION_ITEM",
    indices = [
        Index(
            value = ["testo_normalizzato", "ambito", "urgenza"],
            name = "idx_action_item_dedupe"
        ),
        Index(value = ["stato_spunta"], name = "idx_action_item_stato_spunta")
    ]
)
data class ActionItemEntity(
    @PrimaryKey
    val id: String,
    val testo: String,
    val ambito: String = DEFAULT_AMBITO,
    val urgenza: String = DEFAULT_URGENZA,
    @ColumnInfo(name = "stato_spunta", defaultValue = "0")
    val statoSpunta: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "testo_normalizzato")
    val testoNormalizzato: String = ActionTextNormalizer.normalize(testo),
) {
    companion object {
        /** Ambito iniziale per una nuova azione (CAP-8). */
        const val DEFAULT_AMBITO = "Altro"

        /** Urgenza iniziale per una nuova azione (CAP-7). */
        const val DEFAULT_URGENZA = "A tempo perso"
    }
}
