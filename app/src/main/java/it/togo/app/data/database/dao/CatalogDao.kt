package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    @Query("""
        SELECT cp.* FROM CANONICAL_PRODUCT cp
        JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3Id = t3.id
        JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2Id = t2.id
        JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1Id = t1.id
        WHERE cp.name LIKE '%' || :query || '%'
           OR EXISTS (
               SELECT 1 FROM SYNONYM s
               WHERE s.productId = cp.id AND s.term LIKE '%' || :query || '%'
           )
        ORDER BY t1.sortOrder, t2.sortOrder, t3.sortOrder, cp.name
        LIMIT 50
    """)
    fun search(query: String): Flow<List<CanonicalProductEntity>>

    @Query("SELECT * FROM CANONICAL_PRODUCT WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CanonicalProductEntity?

    @Query("""
        SELECT t1.*, t2.*, t3.* FROM TAXONOMY_LEVEL_1 t1
        LEFT JOIN TAXONOMY_LEVEL_2 t2 ON t1.id = t2.level1Id
        LEFT JOIN TAXONOMY_LEVEL_3 t3 ON t2.id = t3.level2Id
        WHERE t3.isUserDefined = 0 OR t3.isUserDefined IS NULL
        ORDER BY t1.sortOrder, t2.sortOrder, t3.sortOrder
    """)
    fun getFullTaxonomy(): Flow<List<TaxonomyJoinResult>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_1 ORDER BY sortOrder")
    fun getLevel1(): Flow<List<TaxonomyLevel1Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_2 WHERE level1Id = :level1Id ORDER BY sortOrder")
    fun getLevel2ByLevel1(level1Id: Long): Flow<List<TaxonomyLevel2Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_3 WHERE level2Id = :level2Id ORDER BY sortOrder")
    fun getLevel3ByLevel2(level2Id: Long): Flow<List<TaxonomyLevel3Entity>>

    data class TaxonomyJoinResult(
        val level1: TaxonomyLevel1Entity,
        val level2: TaxonomyLevel2Entity?,
        val level3: TaxonomyLevel3Entity?
    )
}