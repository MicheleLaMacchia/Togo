package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import it.togo.app.data.database.entity.ListItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accesso alla tabella unificata `LIST_ITEM` (Story 8.5).
 *
 * Le query di stato usano le stringhe di [it.togo.app.domain.model.ListItemStatus]
 * per restare allineate a [ListItemEntity.status], che è persistito come testo.
 * Dalla Story 8.7 le tabelle legacy `SHOPPING_ITEM` e `HISTORICAL_ITEM` non
 * esistono più: `LIST_ITEM` è l'unica fonte di lista attiva e storico.
 */
@Dao
interface ListItemDao {

    @Query("SELECT * FROM LIST_ITEM ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM LIST_ITEM WHERE status = :status ORDER BY createdAt ASC")
    fun observeByStatus(status: String): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM LIST_ITEM WHERE status = :status ORDER BY statusChangedAt DESC")
    fun observeByStatusNewest(status: String): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM LIST_ITEM WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ListItemEntity?

    @Query(
        """
        SELECT * FROM LIST_ITEM
        WHERE productId = :productId AND status = 'TO_BUY'
        ORDER BY createdAt ASC
        """
    )
    suspend fun getActiveByProductId(productId: Long): List<ListItemEntity>

    @Query(
        """
        SELECT * FROM LIST_ITEM
        WHERE productId = :productId AND status = 'PURCHASED'
        ORDER BY statusChangedAt DESC LIMIT 1
        """
    )
    suspend fun getLatestPurchasedByProductId(productId: Long): ListItemEntity?

    @Query("SELECT * FROM LIST_ITEM WHERE status = 'TO_BUY'")
    suspend fun getActive(): List<ListItemEntity>

    @Query(
        """
        UPDATE LIST_ITEM
        SET status = 'DELETED', purchaseCount = 0,
            statusChangedAt = :at, updatedAt = :at
        WHERE status = 'TO_BUY'
        """
    )
    suspend fun markActiveDeleted(at: Long): Int

    @Query("DELETE FROM LIST_ITEM WHERE status = 'DELETED'")
    suspend fun deleteDeleted(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ListItemEntity)

    @Update
    suspend fun update(entity: ListItemEntity): Int

    @Query("DELETE FROM LIST_ITEM WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query(
        """
        SELECT DISTINCT brand FROM LIST_ITEM
        WHERE status IN ('TO_BUY', 'PURCHASED')
          AND brand IS NOT NULL AND TRIM(brand) <> ''
        """
    )
    fun observeBrands(): Flow<List<String>>

    @Query(
        """
        SELECT DISTINCT variant FROM LIST_ITEM
        WHERE status IN ('TO_BUY', 'PURCHASED')
          AND variant IS NOT NULL AND TRIM(variant) <> ''
        """
    )
    fun observeVariants(): Flow<List<String>>
}
