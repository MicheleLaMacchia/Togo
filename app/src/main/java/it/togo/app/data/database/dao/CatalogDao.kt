package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.togo.app.data.database.CatalogQueries
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.data.database.entity.SynonymEntity
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

    /**
     * Match esatto sul sinonimo già normalizzato (minuscolo + accent-fold).
     * `DISTINCT` evita duplicati se lo stesso termine è ripetuto per prodotto;
     * il chiamante deve trattare più prodotti distinti come non risolvibili.
     */
    @Query(
        """
        SELECT DISTINCT cp.* FROM CANONICAL_PRODUCT cp
        JOIN SYNONYM s ON s.product_id = cp.id
        WHERE s.term = :term
        LIMIT 2
        """
    )
    suspend fun getByExactSynonym(term: String): List<CanonicalProductEntity>

    @Query("SELECT * FROM TAXONOMY_LEVEL_1 ORDER BY sortOrder")
    fun getLevel1(): Flow<List<TaxonomyLevel1Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_2 ORDER BY sortOrder")
    fun getLevel2(): Flow<List<TaxonomyLevel2Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_3 ORDER BY sortOrder")
    fun getLevel3(): Flow<List<TaxonomyLevel3Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_2 WHERE level1_id = :level1Id ORDER BY sortOrder")
    fun getLevel2ByLevel1(level1Id: Long): Flow<List<TaxonomyLevel2Entity>>

    @Query("SELECT * FROM TAXONOMY_LEVEL_3 WHERE level2_id = :level2Id ORDER BY sortOrder")
    fun getLevel3ByLevel2(level2Id: Long): Flow<List<TaxonomyLevel3Entity>>

    /**
     * Prodotti canonici di un Livello 3, ordinati per nome.
     * Usato dal controllo duplicati applicativo (Story 7.1).
     */
    @Query("SELECT * FROM CANONICAL_PRODUCT WHERE level3_id = :level3Id ORDER BY name")
    suspend fun getByLevel3(level3Id: Long): List<CanonicalProductEntity>

    /**
     * Inserisce un prodotto canonico. Con `id = 0` Room usa NULL e SQLite
     * assegna il rowid (`autoGenerate`). Nessun conflitto UNIQUE sul nome:
     * l'avviso di duplicato è applicativo e consente di procedere.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(entity: CanonicalProductEntity): Long

    /**
     * Inserisce un sinonimo (termine già normalizzato) per un prodotto.
     * Usato alla creazione di un prodotto user-defined per rendere il nome
     * accent-fold ricercabile: `SEARCH` normalizza solo la query, quindi
     * senza sinonimo "Caffè" non sarebbe trovato digitando "caffe".
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSynonym(entity: SynonymEntity): Long

    /**
     * Prossimo `sortOrder` libero nello stesso Livello 2: il nuovo Livello 3
     * viene inserito in fondo alla categoria. Calcolo in due statement
     * (max + insert), non transazionale: rischio di race trascurabile
     * (utente singolo + guardia nel ViewModel).
     */
    @Query("SELECT COALESCE(MAX(sortOrder), 0) + 1 FROM TAXONOMY_LEVEL_3 WHERE level2_id = :level2Id")
    suspend fun nextLevel3SortOrder(level2Id: Long): Int

    /**
     * Inserisce un Livello 3. Con `id = 0` Room usa NULL e SQLite assegna il
     * rowid (`autoGenerate`). Nessun conflitto UNIQUE sul nome: l'avviso di
     * duplicato è applicativo e consente di procedere.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLevel3(entity: TaxonomyLevel3Entity): Long
}
