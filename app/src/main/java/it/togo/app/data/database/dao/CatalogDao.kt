package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.togo.app.data.database.CatalogQueries
import it.togo.app.data.database.entity.CanonicalProductEntity
import it.togo.app.data.database.entity.CanonicalProductRow
import it.togo.app.data.database.entity.TaxonomyLevel1Entity
import it.togo.app.data.database.entity.TaxonomyLevel2Entity
import it.togo.app.data.database.entity.TaxonomyLevel3Entity
import it.togo.app.data.database.entity.TaxonomyLevel3PathRow
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
    fun search(query: String): Flow<List<CanonicalProductRow>>

    /**
     * Tutti i Livelli 3 con percorso tassonomico e numero di prodotti.
     *
     * Il filtro testuale è fatto in Kotlin (`CatalogQueries.normalize`) perché
     * SQLite `LIKE` non fa accent-fold: la tassonomia è composta da poche
     * decine di righe, quindi il costo è trascurabile e "caffe" trova "Caffè".
     * La query resta un Flow: le categorie create dall'utente compaiono subito.
     */
    @Query(
        """
        SELECT t3.id AS id,
               t3.name AS name,
               t2.name AS level2Name,
               t1.name AS level1Name,
               (SELECT COUNT(*) FROM CANONICAL_PRODUCT cp WHERE cp.level3_id = t3.id) AS productCount
        FROM TAXONOMY_LEVEL_3 t3
        JOIN TAXONOMY_LEVEL_2 t2 ON t2.id = t3.level2_id
        JOIN TAXONOMY_LEVEL_1 t1 ON t1.id = t2.level1_id
        ORDER BY t1.sortOrder, t2.sortOrder, t3.sortOrder
        """
    )
    fun getLevel3WithPath(): Flow<List<TaxonomyLevel3PathRow>>

    @Query(
        """
        SELECT cp.*,
               t3.name AS level3Name,
               t2.name AS level2Name,
               t2.id AS level2Id,
               t1.name AS level1Name,
               t1.id AS level1Id
        FROM CANONICAL_PRODUCT cp
        LEFT JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
        LEFT JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
        LEFT JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
        WHERE cp.id = :id
        LIMIT 1
        """
    )
    suspend fun getById(id: Long): CanonicalProductRow?

    /**
     * Match esatto sul sinonimo già normalizzato (minuscolo + accent-fold).
     * `DISTINCT` evita duplicati se lo stesso termine è ripetuto per prodotto;
     * il chiamante deve trattare più prodotti distinti come non risolvibili.
     */
    @Query(
        """
        SELECT DISTINCT cp.*,
               t3.name AS level3Name,
               t2.name AS level2Name,
               t2.id AS level2Id,
               t1.name AS level1Name,
               t1.id AS level1Id
        FROM CANONICAL_PRODUCT cp
        JOIN SYNONYM s ON s.product_id = cp.id
        LEFT JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
        LEFT JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
        LEFT JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
        WHERE s.term = :term
        LIMIT 2
        """
    )
    suspend fun getByExactSynonym(term: String): List<CanonicalProductRow>

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
    @Query(
        """
        SELECT cp.*,
               t3.name AS level3Name,
               t2.name AS level2Name,
               t2.id AS level2Id,
               t1.name AS level1Name,
               t1.id AS level1Id
        FROM CANONICAL_PRODUCT cp
        LEFT JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
        LEFT JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
        LEFT JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
        WHERE cp.level3_id = :level3Id
        ORDER BY cp.name
        """
    )
    suspend fun getByLevel3(level3Id: Long): List<CanonicalProductRow>

    /**
     * Inserisce un prodotto canonico. Con `id = 0` Room usa NULL e SQLite
     * assegna il rowid (`autoGenerate`). Nessun conflitto UNIQUE sul nome:
     * l'avviso di duplicato è applicativo e consente di procedere.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(entity: CanonicalProductEntity): Long

    /**
     * Prodotti canonici creati dall'utente (`is_user_defined = 1`), con il
     * percorso tassonomico. Alimenta la sezione "I miei prodotti" delle
     * Impostazioni.
     */
    @Query(
        """
        SELECT cp.*,
               t3.name AS level3Name,
               t2.name AS level2Name,
               t2.id AS level2Id,
               t1.name AS level1Name,
               t1.id AS level1Id
        FROM CANONICAL_PRODUCT cp
        JOIN TAXONOMY_LEVEL_3 t3 ON cp.level3_id = t3.id
        JOIN TAXONOMY_LEVEL_2 t2 ON t3.level2_id = t2.id
        JOIN TAXONOMY_LEVEL_1 t1 ON t2.level1_id = t1.id
        WHERE cp.is_user_defined = 1
        ORDER BY cp.name COLLATE NOCASE
        """
    )
    fun getUserDefinedProducts(): Flow<List<CanonicalProductRow>>

    /**
     * Rinomina/sposta un prodotto SOLO se è user-defined. Il guard su
     * `is_user_defined` impedisce di modificare il catalogo preinstallato.
     * Ritorna il numero di righe aggiornate (0 = prodotto non trovato o default).
     */
    @Query(
        "UPDATE CANONICAL_PRODUCT SET name = :name, level3_id = :level3Id " +
            "WHERE id = :id AND is_user_defined = 1"
    )
    suspend fun updateUserProduct(id: Long, name: String, level3Id: Long): Int

    /** Elimina un prodotto SOLO se è user-defined. */
    @Query("DELETE FROM CANONICAL_PRODUCT WHERE id = :id AND is_user_defined = 1")
    suspend fun deleteUserProduct(id: Long): Int

    /** Rimuove i sinonimi di un prodotto (usato a rinomina/eliminazione). */
    @Query("DELETE FROM SYNONYM WHERE product_id = :productId")
    suspend fun deleteSynonymsForProduct(productId: Long)

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
