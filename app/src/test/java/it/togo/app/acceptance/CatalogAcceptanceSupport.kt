package it.togo.app.acceptance

import it.togo.app.data.database.CatalogQueries
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Supporto condiviso per gli audit di accettazione della Story 6.4.
 *
 * Il catalogo viene letto da `app/src/main/assets/catalog.db` con sqlite-jdbc,
 * lo stesso asset usato in produzione. La query di ricerca è
 * [CatalogQueries.SEARCH], quindi non esiste un secondo motore di ricerca che
 * possa divergere da Room. Nessuna dipendenza da Android/Room.
 */
class SqliteCatalogRepository private constructor(
    private val connection: Connection,
) : CatalogRepository, AutoCloseable {

    private val taxonomy: List<TaxonomyLevel> by lazy { loadTaxonomy() }

    override fun search(query: String): Flow<List<CanonicalProduct>> = flow {
        val normalized = CatalogQueries.escapeLike(CatalogQueries.normalize(query))
        if (normalized.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        emit(
            withContext(Dispatchers.IO) {
                synchronized(connection) {
                    val sql = CatalogQueries.SEARCH.replace(":query", "?")
                    connection.prepareStatement(sql).use { statement ->
                        repeat(3) { index -> statement.setString(index + 1, normalized) }
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        CanonicalProduct(
                                            id = rs.getLong("id"),
                                            name = rs.getString("name").trim(),
                                            level3Id = rs.getLong("level3_id"),
                                            isUserDefined = rs.getBoolean("is_user_defined"),
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    override suspend fun getById(id: Long): CanonicalProduct? = withContext(Dispatchers.IO) {
        synchronized(connection) {
            connection.prepareStatement(
                "SELECT id, name, level3_id, is_user_defined FROM CANONICAL_PRODUCT WHERE id = ? LIMIT 1"
            ).use { statement ->
                statement.setLong(1, id)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) return@withContext null
                    CanonicalProduct(
                        id = rs.getLong("id"),
                        name = rs.getString("name").trim(),
                        level3Id = rs.getLong("level3_id"),
                        isUserDefined = rs.getBoolean("is_user_defined"),
                    )
                }
            }
        }
    }

    override suspend fun getByExactSynonym(term: String): CanonicalProduct? =
        withContext(Dispatchers.IO) {
            val normalized = CatalogQueries.normalize(term)
            if (normalized.isEmpty()) return@withContext null
            synchronized(connection) {
                connection.prepareStatement(
                    """
                    SELECT DISTINCT cp.id, cp.name, cp.level3_id, cp.is_user_defined
                    FROM CANONICAL_PRODUCT cp
                    JOIN SYNONYM s ON s.product_id = cp.id
                    WHERE s.term = ?
                    LIMIT 2
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, normalized)
                    statement.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    CanonicalProduct(
                                        id = rs.getLong("id"),
                                        name = rs.getString("name").trim(),
                                        level3Id = rs.getLong("level3_id"),
                                        isUserDefined = rs.getBoolean("is_user_defined"),
                                    )
                                )
                            }
                        }.singleOrNull()
                    }
                }
            }
        }

    override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = flow { emit(taxonomy) }

    /**
     * Corpus derivato dal catalogo reale: per ogni prodotto il nome canonico e
     * tutti i sinonimi. Ogni caso conserva il prodotto e il L3 attesi.
     */
    fun acceptanceCases(): List<AcceptanceCase> = synchronized(connection) {
        val cases = mutableListOf<AcceptanceCase>()

        connection.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT id, name, level3_id FROM CANONICAL_PRODUCT ORDER BY id"
            ).use { rs ->
                while (rs.next()) {
                    cases.add(
                        AcceptanceCase(
                            kind = AcceptanceCaseKind.CANONICAL_NAME,
                            utterance = rs.getString("name").trim(),
                            expectedProductId = rs.getLong("id"),
                            expectedLevel3Id = rs.getLong("level3_id"),
                        )
                    )
                }
            }
        }

        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT s.term, p.id AS product_id, p.level3_id
                FROM SYNONYM s
                JOIN CANONICAL_PRODUCT p ON p.id = s.product_id
                ORDER BY s.product_id, s.id
                """.trimIndent()
            ).use { rs ->
                while (rs.next()) {
                    cases.add(
                        AcceptanceCase(
                            kind = AcceptanceCaseKind.SYNONYM,
                            utterance = rs.getString("term").trim(),
                            expectedProductId = rs.getLong("product_id"),
                            expectedLevel3Id = rs.getLong("level3_id"),
                        )
                    )
                }
            }
        }

        cases
    }

    override fun close() {
        synchronized(connection) {
            if (!connection.isClosed) connection.close()
        }
    }

    private fun loadTaxonomy(): List<TaxonomyLevel> = synchronized(connection) {
        val result = mutableListOf<TaxonomyLevel>()
        val level1 = mutableListOf<TaxonomyLevel.Level1>()
        val level2 = mutableListOf<TaxonomyLevel.Level2>()
        val level3 = mutableListOf<TaxonomyLevel.Level3>()

        connection.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT id, name, sortOrder FROM TAXONOMY_LEVEL_1 ORDER BY sortOrder, id"
            ).use { rs ->
                while (rs.next()) {
                    level1.add(
                        TaxonomyLevel.Level1(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            sortOrder = rs.getInt("sortOrder"),
                        )
                    )
                }
            }
        }
        connection.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT id, level1_id, name, sortOrder FROM TAXONOMY_LEVEL_2 ORDER BY sortOrder, id"
            ).use { rs ->
                while (rs.next()) {
                    level2.add(
                        TaxonomyLevel.Level2(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            sortOrder = rs.getInt("sortOrder"),
                            level1Id = rs.getLong("level1_id"),
                        )
                    )
                }
            }
        }
        connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT id, level2_id, name, sortOrder, is_user_defined
                FROM TAXONOMY_LEVEL_3
                WHERE is_user_defined = 0
                ORDER BY sortOrder, id
                """.trimIndent()
            ).use { rs ->
                while (rs.next()) {
                    level3.add(
                        TaxonomyLevel.Level3(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            sortOrder = rs.getInt("sortOrder"),
                            level2Id = rs.getLong("level2_id"),
                            isUserDefined = rs.getBoolean("is_user_defined"),
                        )
                    )
                }
            }
        }

        level1.forEach { l1 ->
            result.add(l1)
            level2.filter { it.level1Id == l1.id }.forEach { l2 ->
                result.add(l2)
                level3.filter { it.level2Id == l2.id }.forEach { result.add(it) }
            }
        }
        result
    }

    companion object {
        fun open(): SqliteCatalogRepository {
            Class.forName("org.sqlite.JDBC")
            val file = projectFile("app/src/main/assets/catalog.db")
            check(file.exists()) { "catalog.db non trovato in ${file.absolutePath}" }
            val connection = DriverManager.getConnection(
                "jdbc:sqlite:${file.invariantSeparatorsPath}"
            )
            return SqliteCatalogRepository(connection)
        }
    }
}

data class AcceptanceCase(
    val kind: AcceptanceCaseKind,
    val utterance: String,
    val expectedProductId: Long,
    val expectedLevel3Id: Long,
)

enum class AcceptanceCaseKind {
    CANONICAL_NAME,
    SYNONYM,
}

/** Repository regole vuoto: l'audit misura il comportamento di default. */
object EmptyLearnedRulesRepository : LearnedRulesRepository {
    override fun getAll(): Flow<List<LearnedRule>> = flowOf(emptyList())
    override suspend fun insert(rule: LearnedRule) = Unit
    override suspend fun update(rule: LearnedRule) = Unit
    override suspend fun delete(ruleId: String) = Unit
    override suspend fun getByExpression(expression: String): LearnedRule? = null
    override suspend fun getById(ruleId: String): LearnedRule? = null
    override suspend fun getByExpressionAndProductId(
        expression: String,
        productId: Long,
    ): LearnedRule? = null
    override suspend fun upsertAtomic(rule: LearnedRule): LearnedRule = rule
}

/**
 * Risolve un path dalla root del progetto indipendentemente da dove Gradle
 * esegue i test (root del repository o modulo `app`).
 */
fun projectFile(relativePath: String): File {
    val candidates = listOf(
        File(relativePath),
        File("..", relativePath),
        File("../..", relativePath),
    )
    return candidates.firstOrNull { it.exists() }
        ?: File(relativePath).absoluteFile
}
