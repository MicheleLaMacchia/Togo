package it.togo.app.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Verifica dell'asset precompilato `app/src/main/assets/catalog.db` (Story 1.4).
 *
 * Apre il file SQLite generato da `tools/generate_catalog.py` con sqlite-jdbc
 * (nessuna dipendenza da Room/Android, quindi eseguibile come unit test JVM)
 * e controlla gli AC di seed:
 *  - >= 1000 prodotti alimentari, >= 400 non alimentari (totale >= 1400)
 *  - ogni prodotto: level3_id NOT NULL, name non vuoto, is_user_defined = 0
 *  - nomi duplicati globali == 0
 *  - sinonimi: nessun orfano, ogni prodotto ne ha almeno uno
 *  - schema: 8 tabelle presenti, user_version = 1
 *  - ordine tassonomia L1: Ortofrutta -> ... -> Banco Frigo -> Dispensa -> Non alimentare
 *  - ricerca (SQL condiviso [CatalogQueries.SEARCH]): match accent-fold su sinonimo
 *
 * Il DB di asset è l'unica parte che NON viene verificata da un build KSP/Room su
 * questa macchina (SDK assente); Room validerebbe lo schema al primo open su device.
 */
class CatalogAssetTest {

    private fun assetFile(): File {
        val candidates = listOf(
            File("src", "main/assets/catalog.db"),
            File("app", "src/main/assets/catalog.db"),
            File("..", "app/src/main/assets/catalog.db")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error(
                "catalog.db non trovato (cercato in: " +
                    candidates.joinToString(", ") { it.absolutePath } + ")"
            )
    }

    private fun <T> withDb(block: (Connection) -> T): T {
        Class.forName("org.sqlite.JDBC")
        val file = assetFile()
        // invariantSeparatorsPath => percorsi "/" compatibili con l'URL JDBC su Windows
        DriverManager.getConnection("jdbc:sqlite:${file.invariantSeparatorsPath}").use { conn ->
            return block(conn)
        }
    }

    private fun countByAisle(conn: Connection, isNonFood: Boolean): Int {
        conn.createStatement().use { st ->
            st.executeQuery(
                """
                SELECT COUNT(*) FROM CANONICAL_PRODUCT cp
                JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
                JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
                JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
                WHERE t1.name ${if (isNonFood) "=" else "!="} 'Non alimentare'
                """.trimIndent()
            ).use { rs -> rs.next(); return rs.getInt(1) }
        }
    }

    @Test
    fun `asset catalog has at least 1000 food and 400 non-food products`() {
        withDb { conn ->
            val food = countByAisle(conn, isNonFood = false)
            val nonFood = countByAisle(conn, isNonFood = true)
            assertTrue("food=$food deve essere >= 1000", food >= 1000)
            assertTrue("nonFood=$nonFood deve essere >= 400", nonFood >= 400)
            assertTrue("totale ${food + nonFood} deve essere >= 1400", food + nonFood >= 1400)
        }
    }

    @Test
    fun `every canonical product is valid and unique`() {
        withDb { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(
                    """
                    SELECT COUNT(*) FROM CANONICAL_PRODUCT
                    WHERE level3_id IS NULL OR TRIM(name) = '' OR is_user_defined != 0
                    """.trimIndent()
                ).use { rs ->
                    rs.next()
                    assertEquals("nessuna riga seed invalida", 0, rs.getInt(1))
                }
            }
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT COUNT(*) FROM (SELECT name FROM CANONICAL_PRODUCT GROUP BY name HAVING COUNT(*) > 1)"
                ).use { rs ->
                    rs.next()
                    assertEquals("nessun nome duplicato nel catalogo", 0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `synonyms are referentially valid and complete`() {
        withDb { conn ->
            conn.createStatement().use { st ->
                st.executeQuery(
                    """
                    SELECT COUNT(*) FROM SYNONYM s
                    LEFT JOIN CANONICAL_PRODUCT p ON p.id = s.product_id
                    WHERE p.id IS NULL
                    """.trimIndent()
                ).use { rs ->
                    rs.next()
                    assertEquals("nessun sinonimo orfano", 0, rs.getInt(1))
                }
            }
            conn.createStatement().use { st ->
                st.executeQuery(
                    """
                    SELECT COUNT(*) FROM CANONICAL_PRODUCT p
                    WHERE NOT EXISTS (SELECT 1 FROM SYNONYM s WHERE s.product_id = p.id)
                    """.trimIndent()
                ).use { rs ->
                    rs.next()
                    assertEquals("ogni prodotto deve avere almeno un sinonimo", 0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `database schema matches Room expectations`() {
        withDb { conn ->
            val tables = mutableSetOf<String>()
            conn.createStatement().use { st ->
                st.executeQuery("SELECT name FROM sqlite_master WHERE type='table'").use { rs ->
                    while (rs.next()) tables.add(rs.getString(1))
                }
            }
            val expected = setOf(
                "TAXONOMY_LEVEL_1", "TAXONOMY_LEVEL_2", "TAXONOMY_LEVEL_3",
                "CANONICAL_PRODUCT", "SYNONYM",
                "SHOPPING_ITEM", "HISTORICAL_ITEM", "LEARNED_RULE"
            )
            assertTrue("tabelle mancanti: ${expected - tables}", tables.containsAll(expected))
            conn.createStatement().use { st ->
                st.executeQuery("PRAGMA user_version").use { rs ->
                    rs.next()
                    assertEquals("user_version deve essere 1", 1, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `taxonomy level 1 order follows aisle anchors`() {
        withDb { conn ->
            val order = mutableListOf<String>()
            conn.createStatement().use { st ->
                st.executeQuery("SELECT name FROM TAXONOMY_LEVEL_1 ORDER BY sortOrder").use { rs ->
                    while (rs.next()) order.add(rs.getString(1))
                }
            }
            val expected = listOf(
                "Ortofrutta", "Panetteria", "Carne", "Pesce",
                "Bevande", "Banco Frigo", "Dispensa", "Non alimentare"
            )
            assertEquals("ordine L1 completo", expected, order)
        }
    }

    /** La query è la stessa usata da [CatalogDao.search] (nessun drift tra test e codice). */
    private fun runSearch(conn: Connection, rawQuery: String): List<String> {
        val query = CatalogQueries.escapeLike(CatalogQueries.normalize(rawQuery))
        assertTrue("query normalizzata non vuota: '$rawQuery'", query.isNotEmpty())
        // SEARCH usa i binding Room `:query` (3 occorrenze); per JDBC diventano `?`
        val sql = CatalogQueries.SEARCH.replace(":query", "?")
        conn.prepareStatement(sql).use { ps ->
            repeat(3) { ps.setString(it + 1, query) }
            ps.executeQuery().use { rs ->
                val names = mutableListOf<String>()
                while (rs.next()) {
                    names.add(rs.getString("name").trim())
                    assertTrue("nome non vuoto", rs.getString("name").trim().isNotEmpty())
                }
                return names
            }
        }
    }

    @Test
    fun `search matches by product name`() {
        withDb { conn ->
            val names = runSearch(conn, "mela")
            assertTrue("ricerca 'mela' deve restituire risultati", names.isNotEmpty())
            assertTrue("almeno un risultato deve contenere 'Mela'", names.any { it.contains("Mela") })
        }
    }

    @Test
    fun `search matches a synonym-only accent-folded term`() {
        withDb { conn ->
            // "caffe in grani" NON è un nome prodotto (che è "Caffè in Grani"):
            // deve essere trovato solo tramite sinonimo accent-fold (minuscolo, senza accento).
            val names = runSearch(conn, "Caffè in Grani")
            assertTrue("probe accentato 'Caffè in Grani' deve restituire risultati", names.isNotEmpty())
            assertTrue(
                "deve esistere un prodotto 'Caffè in Grani': $names",
                names.any { it == "Caffè in Grani" }
            )
        }
    }
}