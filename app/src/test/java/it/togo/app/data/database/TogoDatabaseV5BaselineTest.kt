package it.togo.app.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Baseline verificabile dello schema Room v5 (Story 8.1).
 *
 * L'app è ancora in sviluppo e non ha dati di produzione: l'harness inizializza
 * un database v5 **nuovo** partendo dallo schema esportato, senza eseguire la
 * catena 1→5. Il catalogo (748 prodotti canonici) viene copiato dall'asset
 * `catalog.db` per riprodurre la fotografia attesa di una installazione pulita.
 *
 * Casi coperti:
 *  - `5.json` esiste, dichiara version = 5 e contiene tutte le tabelle attese;
 *  - `5.json` è disponibile come asset del test Robolectric;
 *  - un database v5 creato da [MigrationTestHelper] ha version = 5 e le tabelle;
 *  - lo stesso database, inizializzato dal catalogo, contiene 748 prodotti
 *    canonici e la tassonomia attesa.
 *
 * Se lo schema esportato manca il test fallisce con un messaggio esplicito:
 * non viene mai saltato o reso verde artificialmente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TogoDatabaseV5BaselineTest {

    private companion object {
        const val DATABASE_NAME = "togo-v5-baseline-test.db"
        const val SCHEMA_VERSION = 5
        const val SCHEMA_RELATIVE_PATH =
            "schemas/it.togo.app.data.database.TogoDatabase/5.json"
        const val SCHEMA_ASSET_PATH =
            "it.togo.app.data.database.TogoDatabase/5.json"

        val EXPECTED_TABLES = setOf(
            "SHOPPING_ITEM",
            "HISTORICAL_ITEM",
            "CANONICAL_PRODUCT",
            "TAXONOMY_LEVEL_1",
            "TAXONOMY_LEVEL_2",
            "TAXONOMY_LEVEL_3",
            "SYNONYM",
            "LEARNED_RULE",
            "USER_ATTRIBUTE",
        )

        const val CANONICAL_PRODUCT_COUNT = 748L
        const val TAXONOMY_L1_COUNT = 8L
        const val TAXONOMY_L2_COUNT = 37L
        const val TAXONOMY_L3_COUNT = 98L
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
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `schema 5 is exported with version 5 and every expected table`() {
        val schemaFile = requireSchemaFile()

        val root = JSONObject(schemaFile.readText(Charsets.UTF_8))
        val database = root.getJSONObject("database")

        assertEquals("versione dichiarata in 5.json", SCHEMA_VERSION, database.getInt("version"))

        val entities = database.getJSONArray("entities")
        val exportedTables = buildSet {
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                add(entity.getString("tableName"))
                assertTrue(
                    "createSql vuoto per ${entity.getString("tableName")}",
                    entity.getString("createSql").isNotBlank(),
                )
            }
        }

        assertEquals(
            "tabelle esportate in 5.json",
            EXPECTED_TABLES,
            exportedTables,
        )
    }

    @Test
    fun `schema 5 is packaged as a Robolectric test asset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = runCatching {
            context.assets.open(SCHEMA_ASSET_PATH).use { it.readBytes() }
        }.getOrElse { error ->
            throw AssertionError(
                "Schema v5 non disponibile come asset dei test: $SCHEMA_ASSET_PATH. " +
                    "Verifica `android.sourceSets.debug.assets.srcDir(\"\$projectDir/schemas\")` " +
                    "(Robolectric legge gli asset della variante debug) e rigenera lo schema " +
                    "con la build.",
                error,
            )
        }

        assertTrue("lo schema 5.json esportato è vuoto", bytes.isNotEmpty())
    }

    @Test
    fun `fresh v5 database opens with version 5 and every expected table`() {
        requireSchemaAsset()
        val db = migrationHelper.createDatabase(DATABASE_NAME, SCHEMA_VERSION)

        try {
            assertEquals("versione del database appena creato", SCHEMA_VERSION, db.version)

            val tables = db.tableNames()
            assertEquals(
                "tabelle mancanti nel database v5",
                emptySet<String>(),
                EXPECTED_TABLES - tables,
            )
            assertEquals(
                "il database appena creato non deve contenere dati utente",
                0L,
                db.count("SHOPPING_ITEM"),
            )
            assertEquals(0L, db.count("HISTORICAL_ITEM"))
            assertEquals(0L, db.count("LEARNED_RULE"))
            assertEquals(0L, db.count("USER_ATTRIBUTE"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `fresh v5 database initialized from catalog asset has 748 canonical products`() {
        requireSchemaAsset()
        val db = migrationHelper.createDatabase(DATABASE_NAME, SCHEMA_VERSION)

        try {
            seedCatalogFromAsset(db)

            assertEquals("versione dopo il seed del catalogo", SCHEMA_VERSION, db.version)
            assertEquals(
                "prodotti canonici",
                CANONICAL_PRODUCT_COUNT,
                db.count("CANONICAL_PRODUCT"),
            )
            assertEquals(
                "prodotti canonici predefiniti",
                CANONICAL_PRODUCT_COUNT,
                db.count("CANONICAL_PRODUCT WHERE is_user_defined = 0"),
            )
            assertEquals("categorie L1", TAXONOMY_L1_COUNT, db.count("TAXONOMY_LEVEL_1"))
            assertEquals("categorie L2", TAXONOMY_L2_COUNT, db.count("TAXONOMY_LEVEL_2"))
            assertEquals("categorie L3", TAXONOMY_L3_COUNT, db.count("TAXONOMY_LEVEL_3"))
            assertEquals(
                "prodotti senza sinonimo",
                0L,
                db.count(
                    "CANONICAL_PRODUCT p WHERE NOT EXISTS " +
                        "(SELECT 1 FROM SYNONYM s WHERE s.product_id = p.id)"
                ),
            )
        } finally {
            db.close()
        }
    }

    /**
     * Copia i dati del catalogo dall'asset v1 nel database v5 appena creato.
     *
     * Non è una migrazione: l'app non è in esercizio e la baseline viene
     * inizializzata come una installazione nuova, senza toccare le tabelle
     * utente (`SHOPPING_ITEM`, `HISTORICAL_ITEM`, `LEARNED_RULE`,
     * `USER_ATTRIBUTE`).
     */
    private fun seedCatalogFromAsset(db: SupportSQLiteDatabase) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assetCopy = File(context.cacheDir, "catalog-v1-baseline.db")
        context.assets.open("catalog.db").use { input ->
            assetCopy.outputStream().use { output -> input.copyTo(output) }
        }

        val attachPath = assetCopy.invariantSeparatorsPath.replace("'", "''")
        try {
            db.execSQL("ATTACH DATABASE '$attachPath' AS catalog_v1")
            listOf(
                "TAXONOMY_LEVEL_1",
                "TAXONOMY_LEVEL_2",
                "TAXONOMY_LEVEL_3",
                "CANONICAL_PRODUCT",
                "SYNONYM",
            ).forEach { table -> copyTable(db, table) }
        } finally {
            runCatching { db.execSQL("DETACH DATABASE catalog_v1") }
            assetCopy.delete()
        }
    }

    private fun copyTable(db: SupportSQLiteDatabase, table: String) {
        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        assertTrue("nessuna colonna trovata per $table", columns.isNotEmpty())

        val columnList = columns.joinToString(", ")
        db.execSQL("INSERT INTO $table ($columnList) SELECT $columnList FROM catalog_v1.$table")
    }

    private fun requireSchemaFile(): File {
        val candidates = listOf(
            File(SCHEMA_RELATIVE_PATH),
            File("app", SCHEMA_RELATIVE_PATH),
            File("..", "app/$SCHEMA_RELATIVE_PATH"),
            File("../..", "app/$SCHEMA_RELATIVE_PATH"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "Schema v5 mancante. Cercato in: " +
                    candidates.joinToString(", ") { it.absolutePath } +
                    ". Esegui la build (`:app:kspDebugKotlin` o `assembleDebug`) " +
                    "per generare `app/$SCHEMA_RELATIVE_PATH`.",
            )
    }

    private fun requireSchemaAsset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val available = runCatching {
            context.assets.open(SCHEMA_ASSET_PATH).use { it.readBytes().isNotEmpty() }
        }.getOrDefault(false)
        assertTrue(
            "Schema v5 non disponibile come asset dei test ($SCHEMA_ASSET_PATH): " +
                "il database v5 non può essere aperto in modo verificabile.",
            available,
        )
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

    private fun SupportSQLiteDatabase.count(tableOrWhere: String): Long =
        query("SELECT COUNT(*) FROM $tableOrWhere").use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
        }
}
