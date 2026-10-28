package it.togo.app.data.database

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrazione 4→5: sostituisce il catalogo storico con i 748 prodotti canonici
 * del mapping `tools/togo_catalog_migration_mapping_v2.csv`.
 *
 * Lo script SQL è generato da `tools/generate_catalog.py` e:
 *  - rimappa i riferimenti di lista, storico e regole apprese dagli ID fusi
 *    verso il relativo `proposed_survivor_id`;
 *  - elimina solo i prodotti predefiniti (`is_user_defined = 0`);
 *  - reinserisce prodotti e sinonimi del nuovo catalogo;
 *  - lascia intatti i prodotti creati dall'utente.
 *
 * La migrazione è saltata quando il database contiene già i 748 prodotti
 * canonici (es. primo avvio da `createFromAsset("catalog.db")`).
 */
fun migration4To5(context: Context): Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (!needsCatalogV2(db)) return

        context.applicationContext.assets
            .open(CATALOG_MIGRATION_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .useLines { lines ->
                lines.forEach { line ->
                    val statement = line.trim()
                    if (statement.isNotEmpty() && !statement.startsWith("--")) {
                        db.execSQL(statement)
                    }
                }
            }
    }
}

private const val CATALOG_MIGRATION_ASSET = "catalog_migration_v2.sql"
private const val CANONICAL_PRODUCTS_V2 = 748L
private const val LEGACY_PRODUCT_ID = 753L

private fun needsCatalogV2(db: SupportSQLiteDatabase): Boolean {
    val canonicalCount = db.query(
        "SELECT COUNT(*) FROM CANONICAL_PRODUCT WHERE is_user_defined = 0"
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getLong(0) else 0L
    }
    if (canonicalCount != CANONICAL_PRODUCTS_V2) return true

    val legacyProductCount = db.query(
        "SELECT COUNT(*) FROM CANONICAL_PRODUCT WHERE id = $LEGACY_PRODUCT_ID"
    ).use { cursor ->
        if (cursor.moveToFirst()) cursor.getLong(0) else 0L
    }
    return legacyProductCount > 0L
}
