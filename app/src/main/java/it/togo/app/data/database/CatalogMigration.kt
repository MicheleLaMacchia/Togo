package it.togo.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Passaggio Room 4→5 mantenuto per compatibilità del grafo delle versioni.
 * Non modifica il catalogo: gli asset nuovi usano la proposta tassonomica e i
 * database già installati conservano il proprio catalogo e i propri riferimenti.
 * Lo script `catalog_migration_v2.sql` resta un artefatto storico, non applicato
 * da questa migrazione.
 */
val migration4To5 = object : Migration(4, 5) {
    @Suppress("UNUSED_PARAMETER")
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}
