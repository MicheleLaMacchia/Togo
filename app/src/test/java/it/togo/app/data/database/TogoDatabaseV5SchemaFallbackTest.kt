package it.togo.app.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager

/**
 * Fallback JVM indipendente da Robolectric per la baseline v5 (Story 8.1).
 *
 * Se `MigrationTestHelper` non fosse stabile nell'ambiente Robolectric, questo
 * test resta comunque eseguibile e verifica:
 *  - l'esistenza e il contenuto minimo dello schema esportato `5.json`;
 *  - i conteggi del catalogo precompilato `catalog.db` da cui viene inizializzata
 *    una installazione nuova.
 *
 * Non applica la catena 1→5: l'app è ancora in sviluppo e la baseline viene
 * trattata come una inizializzazione pulita. Il test principale
 * [TogoDatabaseV5BaselineTest] apre il database v5 con Room.
 */
class TogoDatabaseV5SchemaFallbackTest {

    private companion object {
        const val SCHEMA_PATH = "app/schemas/it.togo.app.data.database.TogoDatabase/5.json"
        const val CANONICAL_PRODUCT_COUNT = 748L
        const val TAXONOMY_L1_COUNT = 8L
        const val TAXONOMY_L2_COUNT = 37L
        const val TAXONOMY_L3_COUNT = 98L
    }

    private fun projectFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("..", relativePath),
            File("../..", relativePath),
        )
        return candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "File non trovato: $relativePath. Cercato in: " +
                    candidates.joinToString(", ") { it.absolutePath } +
                    ". Se manca 5.json, esegui la build per esportare lo schema Room.",
            )
    }

    @Test
    fun `exported schema 5 declares version 5 and every expected table`() {
        val schema = projectFile(SCHEMA_PATH)
        val content = schema.readText(Charsets.UTF_8)

        assertTrue(
            "5.json non dichiara la versione 5",
            Regex("\"version\"\\s*:\\s*5\\b").containsMatchIn(content),
        )

        val expectedTables = listOf(
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
        expectedTables.forEach { table ->
            assertTrue(
                "5.json non contiene la tabella $table",
                Regex("\"tableName\"\\s*:\\s*\"$table\"").containsMatchIn(content),
            )
        }
    }

    @Test
    fun `catalog asset used for a fresh v5 baseline has 748 canonical products`() {
        Class.forName("org.sqlite.JDBC")
        val source = projectFile("app/src/main/assets/catalog.db")
        val temp = Files.createTempFile("togo-v5-catalog-baseline-", ".db")
        Files.copy(source.toPath(), temp, StandardCopyOption.REPLACE_EXISTING)

        try {
            DriverManager.getConnection("jdbc:sqlite:${temp.toFile().invariantSeparatorsPath}")
                .use { connection ->
                    assertEquals(
                        "user_version dell'asset catalog.db",
                        1L,
                        connection.queryLong("PRAGMA user_version"),
                    )
                    assertEquals(
                        "prodotti canonici",
                        CANONICAL_PRODUCT_COUNT,
                        connection.queryLong("SELECT COUNT(*) FROM CANONICAL_PRODUCT"),
                    )
                    assertEquals(
                        "categorie L1",
                        TAXONOMY_L1_COUNT,
                        connection.queryLong("SELECT COUNT(*) FROM TAXONOMY_LEVEL_1"),
                    )
                    assertEquals(
                        "categorie L2",
                        TAXONOMY_L2_COUNT,
                        connection.queryLong("SELECT COUNT(*) FROM TAXONOMY_LEVEL_2"),
                    )
                    assertEquals(
                        "categorie L3",
                        TAXONOMY_L3_COUNT,
                        connection.queryLong("SELECT COUNT(*) FROM TAXONOMY_LEVEL_3"),
                    )
                }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun Connection.queryLong(sql: String): Long =
        createStatement().use { statement ->
            statement.executeQuery(sql).use { resultSet ->
                resultSet.next()
                resultSet.getLong(1)
            }
        }
}
