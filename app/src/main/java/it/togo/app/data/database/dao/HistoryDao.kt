package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import it.togo.app.data.database.entity.HistoricalItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM HISTORICAL_ITEM ORDER BY purchasedAt DESC")
    fun getAll(): Flow<List<HistoricalItemEntity>>

    @Query("SELECT * FROM HISTORICAL_ITEM WHERE productId = :productId LIMIT 1")
    suspend fun getByProductId(productId: Long): HistoricalItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoricalItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg items: HistoricalItemEntity): List<Long>

    @Update
    suspend fun update(item: HistoricalItemEntity): Int

    @Query("""
        INSERT INTO HISTORICAL_ITEM (id, productId, lastQuantity, lastUnit, purchasedAt, purchaseCount)
        VALUES (:id, :productId, :lastQuantity, :lastUnit, :purchasedAt, :purchaseCount)
        ON CONFLICT(id) DO UPDATE SET
            lastQuantity = excluded.lastQuantity,
            lastUnit = excluded.lastUnit,
            purchasedAt = excluded.purchasedAt,
            purchaseCount = purchaseCount + 1
    """)
    suspend fun upsertHistorical(item: HistoricalItemEntity): Long
}