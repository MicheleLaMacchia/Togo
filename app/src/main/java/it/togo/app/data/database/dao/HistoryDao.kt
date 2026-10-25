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

    @Query("SELECT * FROM HISTORICAL_ITEM WHERE productId IN (:productIds)")
    suspend fun getByProductIds(productIds: List<Long>): List<HistoricalItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoricalItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg items: HistoricalItemEntity): List<Long>

    @Update
    suspend fun update(item: HistoricalItemEntity): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistorical(item: HistoricalItemEntity): Long

    /** Marche non vuote già acquistate: suggerimenti nel form "Aggiungi voce". */
    @Query("SELECT DISTINCT brand FROM HISTORICAL_ITEM WHERE brand IS NOT NULL AND TRIM(brand) <> ''")
    fun getBrands(): Flow<List<String>>

    /** Varianti non vuote già acquistate: suggerimenti nel form "Aggiungi voce". */
    @Query("SELECT DISTINCT variant FROM HISTORICAL_ITEM WHERE variant IS NOT NULL AND TRIM(variant) <> ''")
    fun getVariants(): Flow<List<String>>
}