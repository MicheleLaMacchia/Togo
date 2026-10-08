package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import it.togo.app.data.database.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingItemDao {

    @Query("SELECT * FROM SHOPPING_ITEM WHERE isChecked = 0 ORDER BY createdAt ASC")
    fun getActive(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM SHOPPING_ITEM WHERE productId = :productId AND isChecked = 0 LIMIT 1")
    suspend fun getActiveByProductId(productId: Long): ShoppingItemEntity?

    @Query("SELECT * FROM SHOPPING_ITEM WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg items: ShoppingItemEntity): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingItemEntity): Long

    @Update
    suspend fun updateAll(vararg items: ShoppingItemEntity): Int

    @Update
    suspend fun update(item: ShoppingItemEntity): Int

    @Delete
    suspend fun delete(item: ShoppingItemEntity): Int

    @Query("DELETE FROM SHOPPING_ITEM WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE SHOPPING_ITEM SET isChecked = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsChecked(id: String, updatedAt: Long): Int

    @Query("UPDATE SHOPPING_ITEM SET isChecked = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markAsUnchecked(id: String, updatedAt: Long): Int

    @Query("SELECT * FROM SHOPPING_ITEM WHERE isChecked = 1")
    suspend fun getCheckedItems(): List<ShoppingItemEntity>

    @Query("DELETE FROM SHOPPING_ITEM WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int
}