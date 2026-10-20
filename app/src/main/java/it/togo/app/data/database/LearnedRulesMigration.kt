package it.togo.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import it.togo.app.domain.text.normalizeExpression

/** Riga minimale necessaria alla migrazione delle espressioni apprese. */
data class LearnedRuleMigrationRow(
    val id: String,
    val expression: String,
    val lastAppliedAt: Long,
)

/**
 * Calcola le espressioni normalizzate da mantenere.
 *
 * Se due espressioni raw collidono dopo l'accent-fold, vince quella con
 * `lastAppliedAt` più recente; l'id è il tie-breaker deterministico.
 * Le righe con espressione vuota vengono lasciate invariate.
 */
fun normalizeLearnedRules(rows: List<LearnedRuleMigrationRow>): List<Pair<String, String>> =
    rows.groupBy { normalizeExpression(it.expression) }
        .flatMap { (normalized, group) ->
            if (normalized.isEmpty()) {
                // Non cancellare righe vuote: non hanno una forma normalizzata.
                group.map { it.id to it.expression }
            } else {
                val keep = group.maxWithOrNull(
                    compareBy<LearnedRuleMigrationRow> { it.lastAppliedAt }.thenBy { it.id }
                )
                listOfNotNull(keep?.let { it.id to normalized })
            }
        }

/**
 * Migrazione 1→2: allinea le regole già salvate alla normalizzazione
 * accent-fold usata dalle nuove query.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val rows = mutableListOf<LearnedRuleMigrationRow>()
        db.query("SELECT id, userExpression, lastAppliedAt FROM LEARNED_RULE").use { cursor ->
            while (cursor.moveToNext()) {
                rows += LearnedRuleMigrationRow(
                    id = cursor.getString(0),
                    expression = cursor.getString(1),
                    lastAppliedAt = cursor.getLong(2),
                )
            }
        }

        val kept = normalizeLearnedRules(rows)
        val keptIds = kept.map { it.first }.toSet()

        // Prima elimina le collisioni, poi aggiorna: l'indice unico su
        // userExpression non deve vedere due righe con lo stesso valore.
        rows.filterNot { it.id in keptIds }.forEach { row ->
            db.execSQL("DELETE FROM LEARNED_RULE WHERE id = ?", arrayOf<Any>(row.id))
        }
        kept.forEach { (id, normalized) ->
            db.execSQL(
                "UPDATE LEARNED_RULE SET userExpression = ? WHERE id = ?",
                arrayOf<Any>(normalized, id),
            )
        }
    }
}
