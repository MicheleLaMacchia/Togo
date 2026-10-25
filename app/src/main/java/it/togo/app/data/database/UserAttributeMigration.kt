package it.togo.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import it.togo.app.domain.catalog.ProductNameNormalizer
import it.togo.app.domain.model.ProductAttribute

/**
 * Migrazione 3→4: registro delle marche/varianti aggiunte dall'utente.
 *
 * I valori già presenti nella lista attiva e nello storico vengono importati
 * come user-defined, così i suggerimenti esistenti non si perdono dopo
 * l'aggiornamento. Le marche predefinite (Coop, Conad, ...) restano nel codice
 * e non vengono registrate: le Impostazioni mostreranno solo i valori creati
 * davvero dall'utente.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS USER_ATTRIBUTE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "value TEXT NOT NULL, " +
                "normalized TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_user_attribute_type_normalized " +
                "ON USER_ATTRIBUTE(type, normalized)"
        )

        importExistingValues(db, type = "BRAND", table = "SHOPPING_ITEM", column = "brand")
        importExistingValues(db, type = "BRAND", table = "HISTORICAL_ITEM", column = "brand")
        importExistingValues(db, type = "VARIANT", table = "SHOPPING_ITEM", column = "variant")
        importExistingValues(db, type = "VARIANT", table = "HISTORICAL_ITEM", column = "variant")
    }
}

/**
 * Copia i valori distinti di una colonna nel registro, normalizzandoli come
 * farebbe l'app (iniziale maiuscola + accent-fold per l'unicità).
 */
private fun importExistingValues(
    db: SupportSQLiteDatabase,
    type: String,
    table: String,
    column: String,
) {
    val rawValues = mutableListOf<String>()
    db.query(
        "SELECT DISTINCT $column FROM $table " +
            "WHERE $column IS NOT NULL AND TRIM($column) != ''"
    ).use { cursor ->
        while (cursor.moveToNext()) {
            cursor.getString(0)?.let(rawValues::add)
        }
    }

    rawValues.forEach { raw ->
        val value = ProductNameNormalizer.capitalizeFirstWord(raw)
        if (value.isEmpty()) return@forEach

        // Le marche predefinite non sono "create dall'utente".
        if (type == "BRAND" && ProductAttribute.defaultBrands.any { it.equals(value, ignoreCase = true) }) {
            return@forEach
        }

        val normalized = ProductNameNormalizer.normalize(value)
        if (normalized.isEmpty()) return@forEach

        db.execSQL(
            "INSERT OR IGNORE INTO USER_ATTRIBUTE (type, value, normalized) VALUES (?, ?, ?)",
            arrayOf<Any>(type, value, normalized),
        )
    }
}
