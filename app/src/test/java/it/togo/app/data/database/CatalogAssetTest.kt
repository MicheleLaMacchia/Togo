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
 * e controlla gli AC di seed secondo la proposta tassonomica:
 *  - 892 prodotti canonici (607 mantenuti e 285 nuovi)
 *  - 22 L1, 100 L2, 455 L3, 64 L3 senza prodotti predefiniti
 *  - ogni prodotto: level3_id NOT NULL, name non vuoto, is_user_defined = 0
 *  - nomi duplicati globali == 0
 *  - sinonimi: nessun orfano, ogni prodotto ne ha almeno uno
 *  - schema: 8 tabelle presenti, user_version = 1
 *  - ordine tassonomia L1 come nella sezione 3 della proposta
 *  - ricerca (SQL condiviso [CatalogQueries.SEARCH]): match accent-fold su sinonimo
 *  - i sinonimi approvati e le espressioni Bio risolvono al prodotto base
 *
 * L'asset resta user_version = 1 per la verifica dello schema Room.
 * Il seed vale per le nuove installazioni; il generatore non produce migrazioni.
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

    @Test
    fun `asset catalog contains maintained and proposed products`() {
        withDb { conn ->
            conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM CANONICAL_PRODUCT").use { rs ->
                    rs.next()
                    assertEquals("prodotti canonici", 892, rs.getInt(1))
                }
                st.executeQuery("SELECT COUNT(*) FROM CANONICAL_PRODUCT WHERE id <= 1460").use { rs ->
                    rs.next()
                    assertEquals("ID mantenuti", 607, rs.getInt(1))
                }
                st.executeQuery("SELECT COUNT(*) FROM CANONICAL_PRODUCT WHERE name LIKE '% Bio' OR name LIKE '% Biologico' OR name LIKE '% Biologica' OR name LIKE '% Biologici' OR name LIKE '% Biologiche'").use { rs ->
                    rs.next()
                    assertEquals("nessun prodotto Bio canonico separato", 0, rs.getInt(1))
                }
            }
        }
    }

    @Test
    fun `taxonomy contains all proposal categories and the declared empty leaves`() {
        withDb { conn ->
            fun count(table: String): Int = conn.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            }
            assertEquals("categorie L1", 22, count("TAXONOMY_LEVEL_1"))
            assertEquals("categorie L2", 100, count("TAXONOMY_LEVEL_2"))
            assertEquals("categorie L3", 455, count("TAXONOMY_LEVEL_3"))

            conn.createStatement().use { st ->
                st.executeQuery(
                    """
                    SELECT COUNT(*) FROM TAXONOMY_LEVEL_3 t3
                    WHERE t3.is_user_defined = 0
                      AND NOT EXISTS (
                          SELECT 1 FROM CANONICAL_PRODUCT cp WHERE cp.level3_id = t3.id
                      )
                    """.trimIndent()
                ).use { rs ->
                    rs.next()
                    assertEquals("categorie L3 vuote previste", 64, rs.getInt(1))
                }
            }
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
    fun `taxonomy level 1 order follows the proposal`() {
        withDb { conn ->
            val order = mutableListOf<String>()
            conn.createStatement().use { st ->
                st.executeQuery("SELECT name FROM TAXONOMY_LEVEL_1 ORDER BY sortOrder").use { rs ->
                    while (rs.next()) order.add(rs.getString(1))
                }
            }
            val expected = listOf(
                "Ortofrutta", "Pane, pizza e pasticceria", "Carne e salumi", "Pesce e frutti di mare",
                "Latte, uova e gastronomia", "Surgelati", "Dispensa", "Bevande",
                "Pulizia e consumabili casa", "Cucina e tavola", "Casa e arredamento",
                "Igiene e cura della persona", "Salute e benessere", "Infanzia", "Animali",
                "Ferramenta, manutenzione e giardino", "Elettronica e ufficio",
                "Cartoleria, scuola e creatività", "Abbigliamento e accessori",
                "Sport, viaggi e mobilità", "Giochi, cultura, feste e regali", "Altri acquisti"
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
            // Il nome è "Caffè in Grani": la query accent-fold "caffe in grani"
            // deve trovarlo tramite il sinonimo normalizzato (minuscolo, senza accento).
            val names = runSearch(conn, "Caffè in Grani")
            assertTrue("probe accentato 'Caffè in Grani' deve restituire risultati", names.isNotEmpty())
            assertTrue(
                "deve esistere un prodotto 'Caffè in Grani': $names",
                names.any { it == "Caffè in Grani" }
            )
        }
    }

    @Test
    fun `proposal synonyms and Bio expressions resolve to base products`() {
        withDb { conn ->
            assertTrue("padella griglia deve trovare Bistecchiera", runSearch(conn, "padella griglia").contains("Bistecchiera"))
            assertTrue("Fragola Bio deve trovare Fragola", runSearch(conn, "Fragola Biologica").contains("Fragola"))
            assertTrue("Spritz Hugo pronto deve trovare Hugo pronto da bere", runSearch(conn, "Spritz Hugo pronto").contains("Hugo pronto da bere"))
        }
    }
}
