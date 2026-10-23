package it.togo.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Aggiunge allo storico gli attributi della voce acquistata.
 *
 * L'asset `catalog.db` resta alla versione 1: le migrazioni 1→2 e 2→3
 * vengono applicate anche al database copiato dagli asset al primo avvio.
 * Le colonne sono nullable perché gli acquisti già registrati non hanno
 * un attributo valorizzato.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE HISTORICAL_ITEM ADD COLUMN brand TEXT")
        db.execSQL("ALTER TABLE HISTORICAL_ITEM ADD COLUMN variant TEXT")
        db.execSQL("ALTER TABLE HISTORICAL_ITEM ADD COLUMN condition TEXT")
    }
}
