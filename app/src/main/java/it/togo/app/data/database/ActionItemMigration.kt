package it.togo.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrazione 5→6: aggiunge la tabella delle azioni "Cose da fare".
 *
 * È non distruttiva: crea solo `ACTION_ITEM` e i suoi indici. Non tocca
 * `SHOPPING_ITEM`, `HISTORICAL_ITEM`, il catalogo precompilato né gli altri
 * dati utente, quindi non serve (e non va usato) il fallback distruttivo.
 *
 * La tabella nasce vuota, perciò `testo_normalizzato` non richiede un
 * backfill: le righe future vengono scritte dal repository con la chiave
 * calcolata da `ActionTextNormalizer`.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS ACTION_ITEM (" +
                "id TEXT NOT NULL, " +
                "testo TEXT NOT NULL, " +
                "ambito TEXT NOT NULL, " +
                "urgenza TEXT NOT NULL, " +
                "stato_spunta INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "testo_normalizzato TEXT NOT NULL, " +
                "PRIMARY KEY(id))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_action_item_dedupe " +
                "ON ACTION_ITEM(testo_normalizzato, ambito, urgenza)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_action_item_stato_spunta " +
                "ON ACTION_ITEM(stato_spunta)"
        )
    }
}
