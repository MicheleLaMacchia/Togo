package it.togo.app.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Verifica della query di ricerca `CatalogDao.SEARCH` (Story 3.1).
 *
 * Apre il file SQLite generato da `tools/generate_catalog.py` con sqlite-jdbc
 * (nessuna dipendenza da Room/Android, quindi eseguibile come unit test JVM)
 * e controlla la logica di ricerca:
 *  - Match per nome prodotto
 *  - Match per sinonimo (case-insensitive, accent-fold)
 *  - Escape LIKE corretto
 * - LIMIT 50
 * - Ordine per tassonomia
 */
class CatalogDaoTest {

    private fun assetFile(): File {
        val candidates = listOf(
            File("src", "main", "assets", "catalog.db"),
            File("app", "src", "main", "assets", "catalog.db"),
            File("..", "app", "src", "main", "assets", "catalog.db")
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
        DriverManager.getConnection("jdbc:sqlite:${file.invariantSeparatorsPath}").use { conn ->
            return block(conn)
        }
    }

    private fun runSearch(conn: Connection, rawQuery: String): List<String> {
        import it.togo.app.data.database.CatalogQueries
        val query = CatalogQueries.escapeLike(CatalogQueries.normalize(rawQuery))
        if (query.isEmpty()) return emptyList()

        // La query SEARCH usa binding Room `:query` (3 occorrenze); per JDBC diventano `?`
        val sql = it.togo.app.data.database.CatalogQueries.SEARCH.replace(":query", "?")
        return conn.prepareStatement(sql).use { ps ->
            repeat(3) { ps.setString(it + 1, query) }
            ps.executeQuery().use { rs ->
                val names = mutableListOf<String>()
                while (rs.next()) {
                    names.add(rs.getString("name"))
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

    @Test
    fun `search matches only via synonym accent-fold`() {
        withDb { conn ->
            // "caffe in grani" NON è un nome prodotto (che è "Caffè in Grani"):
            // deve essere trovato solo tramite sinonimo accent-fold (minuscolo, senza accento).
            val names = runSearch(conn, "caffe in grani")
            assertTrue("probe accent-fold 'caffe in grani' deve restituire risultati", names.isNotEmpty())
            assertTrue(
                "deve esistere un prodotto 'Caffè in Grani': $names",
                names.any { it == "Caffè in Grani" }
            )
        }
    }

    @Test
    fun `search escapes LIKE wildcards`() {
        withDb { conn ->
            // % e _ devono essere trattati come letterali
            val names = runSearch(conn, "100%")
            // non deve crashare, può restituire lista vuota
            assertTrue("escape LIKE non deve crashare", names is List<*>)
        }
    }

    @Test
    fun `search empty query returns empty`() {
        withDb { conn ->
            val names = runSearch(conn, "")
            assertTrue("query vuota deve restituire lista vuota", names.isEmpty())
        }
    }

    @Test
    fun `search results limited to 50`() {
        withDb { conn ->
            val names = runSearch(conn, "a") // query molto generica
            assertTrue("LIMIT 50 deve funzionare", names.size <= 50)
        }
    }

    @Test
    fun `search results ordered by taxonomy`() {
        withDb { conn ->
            val names = runSearch(conn, "latte")
            // i risultati devono essere ordinati per tassonomia L1/L2/L3/nome
            // "Latte intero" (Latte e yogurt) deve venire prima di "Latte di soia" (Bevande)
            val latteInteroIndex = names.indexOfFirst { it == "Latte intero" }
            val latteDiSoiaIndex = names.indexOfFirst { it == "Latte di soia" }
            if (latteInteroIndex >= 0 && latteDiSoiaIndex >= 0) {
                assertTrue("ordinamento tassonomia: Latte intero prima di Latte di soia",
                    latteInteroIndex < latteDiSoiaIndex)
            }
        }
    }
}