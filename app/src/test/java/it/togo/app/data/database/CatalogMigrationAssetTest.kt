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
 * Verifica lo script SQL generato da `tools/generate_catalog.py` e usato dalla
 * migrazione Room 4→5.
 *
 * Lo script viene applicato a una copia dell'asset nuovo: non serve un DB Room
 * né Android. Prima dell'applicazione vengono inseriti riferimenti sintetici a
 * un ID fuso (753) e a uno dei suoi sinonimi di confezione, così il test copre
 * anche il remapping di lista, storico e regole apprese.
 */
class CatalogMigrationAssetTest {

    private fun projectFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("..", relativePath),
            File("../..", relativePath),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("File non trovato: $relativePath")
    }

    private fun <T> withMigratedDatabase(block: (Connection) -> T): T {
        Class.forName("org.sqlite.JDBC")
        val source = projectFile("app/src/main/assets/catalog.db")
        val sqlFile = projectFile("app/src/main/assets/catalog_migration_v2.sql")
        val statements = sqlFile.readLines(Charsets.UTF_8)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("--") }
        assertTrue("script di migrazione vuoto", statements.isNotEmpty())

        val temp = Files.createTempFile("togo-catalog-migration-", ".db")
        Files.copy(source.toPath(), temp, StandardCopyOption.REPLACE_EXISTING)
        try {
            DriverManager.getConnection("jdbc:sqlite:${temp.toFile().invariantSeparatorsPath}").use { conn ->
                conn.createStatement().use { st ->
                    // Simula due prodotti predefiniti del vecchio catalogo che
                    // verranno fusi: 753 -> 739 e 754 -> 731.
                    st.executeUpdate(
                        "INSERT INTO CANONICAL_PRODUCT (id, level3_id, name, is_user_defined) " +
                            "VALUES (753, 1, 'Vecchio prodotto 753', 0)"
                    )
                    st.executeUpdate(
                        "INSERT INTO CANONICAL_PRODUCT (id, level3_id, name, is_user_defined) " +
                            "VALUES (754, 1, 'Vecchio prodotto 754', 0)"
                    )
                    st.executeUpdate(
                        "INSERT INTO SHOPPING_ITEM " +
                            "(id, productId, quantity, unit, isChecked, createdAt, updatedAt) " +
                            "VALUES ('migration-shop', 753, 1.0, 'pz', 0, 0, 0)"
                    )
                    st.executeUpdate(
                        "INSERT INTO HISTORICAL_ITEM " +
                            "(id, productId, lastQuantity, lastUnit, purchasedAt, purchaseCount) " +
                            "VALUES ('migration-history', 754, 2.0, 'pz', 0, 1)"
                    )
                    st.executeUpdate(
                        "INSERT INTO LEARNED_RULE " +
                            "(id, userExpression, productId, level3Id, lastAppliedAt, isActive) " +
                            "VALUES ('migration-rule', 'migration expression', 753, 1, 0, 1)"
                    )
                    // Un prodotto utente con ID 1461 (ID che nel vecchio catalogo
                    // apparteneva a una confezione fusa) non deve essere rimappato
                    // né cancellato.
                    st.executeUpdate(
                        "INSERT INTO CANONICAL_PRODUCT (id, level3_id, name, is_user_defined) " +
                            "VALUES (1461, 1, 'Prodotto Utente Test', 1)"
                    )
                    st.executeUpdate(
                        "INSERT INTO SYNONYM (product_id, term) " +
                            "VALUES (1461, 'prodotto utente test')"
                    )
                    st.executeUpdate(
                        "INSERT INTO SHOPPING_ITEM " +
                            "(id, productId, quantity, unit, isChecked, createdAt, updatedAt) " +
                            "VALUES ('migration-user', 1461, 1.0, 'pz', 0, 0, 0)"
                    )
                }

                conn.autoCommit = false
                try {
                    statements.forEach { statement ->
                        conn.createStatement().use { st -> st.execute(statement) }
                    }
                    conn.commit()
                } catch (error: Throwable) {
                    conn.rollback()
                    throw error
                } finally {
                    conn.autoCommit = true
                }

                return block(conn)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun Connection.queryLong(sql: String): Long =
        createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }

    private fun Connection.queryString(sql: String): String? =
        createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                if (rs.next()) rs.getString(1) else null
            }
        }

    @Test(timeout = 120_000)
    fun `migration SQL rebuilds catalog and remaps user references`() {
        withMigratedDatabase { conn ->
            assertEquals(
                "prodotti predefiniti del nuovo catalogo",
                748L,
                conn.queryLong("SELECT COUNT(*) FROM CANONICAL_PRODUCT WHERE is_user_defined = 0"),
            )
            assertEquals(
                "l'ID fuso 753 non deve più esistere",
                0L,
                conn.queryLong("SELECT COUNT(*) FROM CANONICAL_PRODUCT WHERE id = 753"),
            )
            assertEquals(
                "il survivor di 753 deve essere Lasagne (739)",
                "Lasagne",
                conn.queryString("SELECT name FROM CANONICAL_PRODUCT WHERE id = 739"),
            )
            assertEquals(
                "la lista attiva deve puntare al survivor",
                739L,
                conn.queryLong("SELECT productId FROM SHOPPING_ITEM WHERE id = 'migration-shop'"),
            )
            assertEquals(
                "lo storico deve puntare al survivor",
                731L,
                conn.queryLong("SELECT productId FROM HISTORICAL_ITEM WHERE id = 'migration-history'"),
            )
            assertEquals(
                "le regole apprese devono puntare al survivor",
                739L,
                conn.queryLong("SELECT productId FROM LEARNED_RULE WHERE id = 'migration-rule'"),
            )
            assertEquals(
                "il prodotto utente non deve essere cancellato",
                1L,
                conn.queryLong(
                    "SELECT COUNT(*) FROM CANONICAL_PRODUCT " +
                        "WHERE id = 1461 AND is_user_defined = 1"
                ),
            )
            assertEquals(
                "il sinonimo del prodotto utente deve restare collegato",
                1L,
                conn.queryLong(
                    "SELECT COUNT(*) FROM SYNONYM " +
                        "WHERE product_id = 1461 AND term = 'prodotto utente test'"
                ),
            )
            assertEquals(
                "il riferimento al prodotto utente non deve essere rimappato",
                1461L,
                conn.queryLong("SELECT productId FROM SHOPPING_ITEM WHERE id = 'migration-user'"),
            )
            assertEquals(
                "nessun sinonimo orfano",
                0L,
                conn.queryLong(
                    "SELECT COUNT(*) FROM SYNONYM s " +
                        "LEFT JOIN CANONICAL_PRODUCT p ON p.id = s.product_id WHERE p.id IS NULL"
                ),
            )
            assertEquals(
                "ogni prodotto deve avere almeno un sinonimo",
                0L,
                conn.queryLong(
                    "SELECT COUNT(*) FROM CANONICAL_PRODUCT p " +
                        "WHERE NOT EXISTS (SELECT 1 FROM SYNONYM s WHERE s.product_id = p.id)"
                ),
            )
            assertEquals(
                "i termini sinonimo devono restare univoci",
                0L,
                conn.queryLong(
                    "SELECT COUNT(*) FROM " +
                        "(SELECT term FROM SYNONYM GROUP BY term HAVING COUNT(*) > 1)"
                ),
            )
        }
    }
}
