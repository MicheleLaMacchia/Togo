package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import it.togo.app.data.database.CatalogQueries
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {

    /**
     * La query deve arrivare già normalizzata ed escapata ([CatalogQueries]):
     * minuscole + accent-fold, e senza jolly LIKE non letterali.
     * Query vuota (dopo normalizzazione) => nessun risultato per il guard `:query != ''`.
     */
    @Query(CatalogQueries.SEARCH)
    fun search(query: String): Flow<List<CanonicalProductEntity>>

    @Query("SELECT * FROM CANONICAL_PRODUCT WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CanonicalProductEntity?

    @Query("SELECT * FROM TAXONOMY_LEVEL_1 ORDER BY sortOrder")
    fun getLevel1(): Flow<List<TaxonomyLevel1Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_2 ORDER BY sortOrder")
    fun getLevel2(): Flow<List<TaxonomyLevel2Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_3 WHERE is_user_defined = 0 ORDER BY sortOrder")
    fun getLevel3(): Flow<List<TaxonomyLevel3Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_2 WHERE level1_id = :level1Id ORDER BY sortOrder")
    fun getLevel2ByLevel1(level1Id: Long): Flow<List<TaxonomyLevel2Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_3 WHERE level2_id = :level2Id ORDER BY sortOrder")
    fun getLevel3ByLevel2(level2Id: Long): Flow<List<TaxonomyLevel3Entity>>
}
