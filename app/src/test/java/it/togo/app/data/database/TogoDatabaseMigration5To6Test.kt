package it.togo.app.data.database

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test della migrazione 5→6 (Story 8.4).
 *
 * La variante di questa storia **non droppa** `SHOPPING_ITEM` e
 * `HISTORICAL_ITEM`: i test verificano che restino presenti e invariati.
 *
 * `6.json` deve essere generato dal build KSP/Room, non creato manualmente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TogoDatabaseMigration5To6Test {

    private companion object {
        const val TEST_DB = "togo-migration-5-6-test.db"
        const val MIGRATION_6_7_DB = "togo-migration-6-7-test.db"
        const val NEW_INSTALL_DB = "togo-new-install-v7-test.db"
        const val SCHEMA_VERSION = 6
        const val SCHEMA_ASSET_PATH =
            "it.togo.app.data.database.TogoDatabase/6.json"
        const val CANONICAL_PRODUCT_COUNT = 748L
        const val TAXONOMY_L1_COUNT = 8L
        const val TAXONOMY_L2_COUNT = 37L
        const val TAXONOMY_L3_COUNT = 98L
        const val TASK_CATEGORY_COUNT = 16L
    }

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TogoDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(TEST_DB)
        context.deleteDatabase(MIGRATION_6_7_DB)
        context.deleteDatabase(NEW_INSTALL_DB)
    }

    @Test
    fun `migration 5 to 6 copies legacy rows and keeps old tables`() {
        requireSchema6Asset()

        val v5 = migrationHelper.createDatabase(TEST_DB, 5)
        try {
            insertLegacyRows(v5)
        } finally {
            v5.close()
        }

        val db = migrationHelper.runMigrationsAndValidate(
            TEST_DB,
            SCHEMA_VERSION,
            true,
            MIGRATION_5_6,
        )

        try {
            assertEquals(SCHEMA_VERSION, db.version)

            val tables = db.tableNames()
            assertTrue(
                "tabelle mancanti dopo la 5→6: " +
                    (setOf("LIST_ITEM", "TASK_CATEGORY", "TODO_TASK") - tables),
                tables.containsAll(setOf("LIST_ITEM", "TASK_CATEGORY", "TODO_TASK")),
            )
            assertTrue(
                "le tabelle legacy non devono essere eliminate",
                tables.containsAll(setOf("SHOPPING_ITEM", "HISTORICAL_ITEM")),
            )

            assertEquals("righe LIST_ITEM", 5L, db.count("LIST_ITEM"))
            assertEquals("categorie Task", TASK_CATEGORY_COUNT, db.count("TASK_CATEGORY"))
            assertEquals("TODO_TASK deve essere vuota", 0L, db.count("TODO_TASK"))
            assertEquals("righe SHOPPING_ITEM originali", 3L, db.count("SHOPPING_ITEM"))
            assertEquals("righe HISTORICAL_ITEM originali", 2L, db.count("HISTORICAL_ITEM"))

            assertListItem(
                db = db,
                id = "shop-zero",
                productId = 1L,
                quantity = 1.0,
                unit = "KILOGRAM",
                brand = "BrandZero",
                variant = "VariantZero",
                condition = "ConditionZero",
                status = "TO_BUY",
                purchaseCount = 0,
                createdAt = 100L,
                statusChangedAt = 100L,
                updatedAt = 110L,
            )

            assertListItem(
                db = db,
                id = "shared-id",
                productId = 2L,
                quantity = 2.5,
                unit = "PIECE",
                brand = "BrandShop",
                variant = "VariantShop",
                condition = "ConditionShop",
                status = "PURCHASED",
                purchaseCount = 1,
                createdAt = 200L,
                statusChangedAt = 250L,
                updatedAt = 250L,
            )

            assertListItem(
                db = db,
                id = "shop-empty-unit",
                productId = 3L,
                quantity = 3.0,
                unit = "PIECE",
                brand = null,
                variant = null,
                condition = null,
                status = "TO_BUY",
                purchaseCount = 0,
                createdAt = 300L,
                statusChangedAt = 300L,
                updatedAt = 300L,
            )

            assertListItem(
                db = db,
                id = "legacy-hist:shared-id",
                productId = 4L,
                quantity = 5.0,
                unit = "GRAM",
                brand = "BrandHist",
                variant = "VariantHist",
                condition = "ConditionHist",
                status = "PURCHASED",
                purchaseCount = 7,
                createdAt = 400L,
                statusChangedAt = 400L,
                updatedAt = 400L,
            )

            assertListItem(
                db = db,
                id = "legacy-hist:hist-empty-unit",
                productId = 5L,
                quantity = 1.0,
                unit = "PIECE",
                brand = null,
                variant = null,
                condition = null,
                status = "PURCHASED",
                purchaseCount = 1,
                createdAt = 500L,
                statusChangedAt = 500L,
                updatedAt = 500L,
            )

            assertEquals(
                "stesso id legacy deve produrre due righe distinte",
                2L,
                db.count("LIST_ITEM WHERE id IN ('shared-id', 'legacy-hist:shared-id')"),
            )

            assertLegacyRowsUnchanged(db)
            assertTaskCategories(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun `migration 6 to 7 removes legacy tables and preserves unified rows`() {
        requireSchema6Asset()

        val v5 = migrationHelper.createDatabase(MIGRATION_6_7_DB, 5)
        try {
            insertLegacyRows(v5)
        } finally {
            v5.close()
        }

        val v6 = migrationHelper.runMigrationsAndValidate(
            MIGRATION_6_7_DB,
            SCHEMA_VERSION,
            true,
            MIGRATION_5_6,
        )
        v6.close()

        val db = migrationHelper.runMigrationsAndValidate(
            MIGRATION_6_7_DB,
            7,
            true,
            MIGRATION_6_7,
        )
        try {
            assertEquals("versione dopo la 6→7", 7, db.version)
            assertEquals("le cinque righe migrate devono restare", 5L, db.count("LIST_ITEM"))
            assertListItem(
                db = db,
                id = "shop-zero",
                productId = 1L,
                quantity = 1.0,
                unit = "KILOGRAM",
                brand = "BrandZero",
                variant = "VariantZero",
                condition = "ConditionZero",
                status = "TO_BUY",
                purchaseCount = 0,
                createdAt = 100L,
                statusChangedAt = 100L,
                updatedAt = 110L,
            )
            assertListItem(
                db = db,
                id = "legacy-hist:shared-id",
                productId = 4L,
                quantity = 5.0,
                unit = "GRAM",
                brand = "BrandHist",
                variant = "VariantHist",
                condition = "ConditionHist",
                status = "PURCHASED",
                purchaseCount = 7,
                createdAt = 400L,
                statusChangedAt = 400L,
                updatedAt = 400L,
            )
            val tables = db.tableNames()
            assertTrue(
                "le tabelle legacy devono essere rimosse nella v7",
                (setOf("SHOPPING_ITEM", "HISTORICAL_ITEM") intersect tables).isEmpty(),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun `new installation from catalog asset v1 migrates through v7`() {
        requireSchema6Asset()

        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(NEW_INSTALL_DB)

        val db = Room.databaseBuilder(
            context,
            TogoDatabase::class.java,
            NEW_INSTALL_DB,
        )
            .createFromAsset("catalog.db")
            .allowMainThreadQueries()
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                migration4To5(context),
                MIGRATION_5_6,
                MIGRATION_6_7,
            )
            .build()

        try {
            val sqlite = db.openHelper.writableDatabase

            assertEquals("versione dopo la catena 1→7", 7, sqlite.version)
            assertEquals(
                "prodotti canonici",
                CANONICAL_PRODUCT_COUNT,
                sqlite.count("CANONICAL_PRODUCT"),
            )
            assertEquals("categorie L1", TAXONOMY_L1_COUNT, sqlite.count("TAXONOMY_LEVEL_1"))
            assertEquals("categorie L2", TAXONOMY_L2_COUNT, sqlite.count("TAXONOMY_LEVEL_2"))
            assertEquals("categorie L3", TAXONOMY_L3_COUNT, sqlite.count("TAXONOMY_LEVEL_3"))
            assertEquals("categorie Task", TASK_CATEGORY_COUNT, sqlite.count("TASK_CATEGORY"))
            assertEquals("TODO_TASK deve essere vuota", 0L, sqlite.count("TODO_TASK"))
            assertEquals("LIST_ITEM deve essere vuota nell'asset", 0L, sqlite.count("LIST_ITEM"))

            val tables = sqlite.tableNames()
            assertTrue(
                "nuove tabelle mancanti dopo la catena 1→7: " +
                    (setOf("LIST_ITEM", "TASK_CATEGORY", "TODO_TASK") - tables),
                tables.containsAll(setOf("LIST_ITEM", "TASK_CATEGORY", "TODO_TASK")),
            )
            assertTrue(
                "le tabelle legacy devono essere rimosse nella v7",
                (setOf("SHOPPING_ITEM", "HISTORICAL_ITEM") intersect tables).isEmpty(),
            )
        } finally {
            db.close()
            context.deleteDatabase(NEW_INSTALL_DB)
        }
    }

    private fun insertLegacyRows(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO SHOPPING_ITEM " +
                "(id, productId, quantity, unit, brand, variant, condition, isChecked, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                "shop-zero", 1L, 0.0, "KILOGRAM",
                "BrandZero", "VariantZero", "ConditionZero", 0, 100L, 110L,
            ),
        )
        db.execSQL(
            "INSERT INTO SHOPPING_ITEM " +
                "(id, productId, quantity, unit, brand, variant, condition, isChecked, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                "shared-id", 2L, 2.5, "PIECE",
                "BrandShop", "VariantShop", "ConditionShop", 1, 200L, 250L,
            ),
        )
        db.execSQL(
            "INSERT INTO SHOPPING_ITEM " +
                "(id, productId, quantity, unit, brand, variant, condition, isChecked, createdAt, updatedAt) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                "shop-empty-unit", 3L, 3.0, "",
                null, null, null, 0, 300L, 300L,
            ),
        )

        db.execSQL(
            "INSERT INTO HISTORICAL_ITEM " +
                "(id, productId, lastQuantity, lastUnit, brand, variant, condition, purchasedAt, purchaseCount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                "shared-id", 4L, 5.0, "GRAM",
                "BrandHist", "VariantHist", "ConditionHist", 400L, 7,
            ),
        )
        db.execSQL(
            "INSERT INTO HISTORICAL_ITEM " +
                "(id, productId, lastQuantity, lastUnit, brand, variant, condition, purchasedAt, purchaseCount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf(
                "hist-empty-unit", 5L, 0.0, "",
                null, null, null, 500L, 0,
            ),
        )
    }

    private fun assertLegacyRowsUnchanged(db: SupportSQLiteDatabase) {
        db.query(
            "SELECT quantity, unit, isChecked, updatedAt FROM SHOPPING_ITEM WHERE id = 'shop-zero'",
        ).use { cursor ->
            assertTrue("SHOPPING_ITEM shop-zero mancante", cursor.moveToFirst())
            assertEquals("quantity legacy invariata", 0.0, cursor.getDouble(0), 0.0)
            assertEquals("unit legacy invariata", "KILOGRAM", cursor.getString(1))
            assertEquals("isChecked legacy invariato", 0, cursor.getInt(2))
            assertEquals("updatedAt legacy invariato", 110L, cursor.getLong(3))
        }

        db.query(
            "SELECT purchaseCount, purchasedAt, lastUnit FROM HISTORICAL_ITEM WHERE id = 'shared-id'",
        ).use { cursor ->
            assertTrue("HISTORICAL_ITEM shared-id mancante", cursor.moveToFirst())
            assertEquals("purchaseCount legacy invariato", 7, cursor.getInt(0))
            assertEquals("purchasedAt legacy invariato", 400L, cursor.getLong(1))
            assertEquals("lastUnit legacy invariata", "GRAM", cursor.getString(2))
        }
    }

    private fun assertTaskCategories(db: SupportSQLiteDatabase) {
        val names = mutableListOf<String>()
        db.query("SELECT name FROM TASK_CATEGORY ORDER BY id").use { cursor ->
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0))
            }
        }

        assertEquals(
            "seed TASK_CATEGORY completo",
            listOf(
                "Casa", "Commissioni", "Personale", "Lavoro", "Altro",
                "Pulizie", "Manutenzione", "Organizzazione",
                "Ritiri", "Spedizioni", "Pratiche",
                "Appuntamenti", "Benessere", "Varie",
                "Attività", "Scadenze",
            ),
            names,
        )
        assertEquals(
            "Pulizie deve appartenere a Casa",
            1L,
            db.singleLong("SELECT parentId FROM TASK_CATEGORY WHERE id = 101"),
        )
    }

    private fun assertListItem(
        db: SupportSQLiteDatabase,
        id: String,
        productId: Long,
        quantity: Double,
        unit: String,
        brand: String?,
        variant: String?,
        condition: String?,
        status: String,
        purchaseCount: Int,
        createdAt: Long,
        statusChangedAt: Long,
        updatedAt: Long,
    ) {
        db.query(
            "SELECT id, productId, quantity, unit, brand, variant, condition, note, " +
                "status, purchaseCount, createdAt, statusChangedAt, updatedAt " +
                "FROM LIST_ITEM WHERE id = ?",
            arrayOf(id),
        ).use { cursor ->
            assertTrue("LIST_ITEM $id mancante", cursor.moveToFirst())
            assertEquals(id, cursor.getString(0))
            assertEquals(productId, cursor.getLong(1))
            assertEquals(quantity, cursor.getDouble(2), 0.0)
            assertEquals(unit, cursor.getString(3))
            assertEquals(brand, cursor.stringOrNull(4))
            assertEquals(variant, cursor.stringOrNull(5))
            assertEquals(condition, cursor.stringOrNull(6))
            assertNull("note non deve essere popolata dalla migrazione", cursor.stringOrNull(7))
            assertEquals(status, cursor.getString(8))
            assertEquals(purchaseCount, cursor.getInt(9))
            assertEquals(createdAt, cursor.getLong(10))
            assertEquals(statusChangedAt, cursor.getLong(11))
            assertEquals(updatedAt, cursor.getLong(12))
        }
    }

    private fun requireSchema6Asset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = runCatching {
            context.assets.open(SCHEMA_ASSET_PATH).use { it.readBytes() }
        }.getOrElse { error ->
            throw AssertionError(
                "Schema v6 non disponibile come asset: $SCHEMA_ASSET_PATH. " +
                    "Esegui il build (`assembleDebug` o `:app:kspDebugKotlin`) per " +
                    "generare `app/schemas/.../6.json`; non crearlo manualmente.",
                error,
            )
        }

        assertTrue("lo schema 6.json è vuoto", bytes.isNotEmpty())
        val database = JSONObject(bytes.toString(Charsets.UTF_8)).getJSONObject("database")
        assertEquals("versione dichiarata in 6.json", SCHEMA_VERSION, database.getInt("version"))

        val tables = buildSet {
            val entities = database.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                add(entities.getJSONObject(index).getString("tableName"))
            }
        }
        assertTrue(
            "tabelle mancanti in 6.json: " +
                (setOf("LIST_ITEM", "TASK_CATEGORY", "TODO_TASK") - tables),
            tables.containsAll(setOf("LIST_ITEM", "TASK_CATEGORY", "TODO_TASK")),
        )
    }

    private fun SupportSQLiteDatabase.count(tableOrWhere: String): Long =
        query("SELECT COUNT(*) FROM $tableOrWhere").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }

    private fun SupportSQLiteDatabase.singleLong(sql: String): Long =
        query(sql).use { cursor ->
            assertTrue("query senza risultati: $sql", cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun SupportSQLiteDatabase.tableNames(): Set<String> =
        query(
            "SELECT name FROM sqlite_master " +
                "WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0))
                }
            }
        }

    private fun Cursor.stringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)
}
