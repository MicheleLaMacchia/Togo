package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import it.togo.app.data.database.entity.LearnedRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedRulesDao {

    @Query("SELECT * FROM LEARNED_RULE WHERE isActive = 1 ORDER BY lastAppliedAt DESC")
    fun getAll(): Flow<List<LearnedRuleEntity>>

    @Query("SELECT * FROM LEARNED_RULE WHERE userExpression = :expression LIMIT 1")
    suspend fun getByExpression(expression: String): LearnedRuleEntity?

    @Query("SELECT * FROM LEARNED_RULE WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LearnedRuleEntity?

    @Query("SELECT * FROM LEARNED_RULE WHERE productId = :productId AND isActive = 1 LIMIT 1")
    suspend fun getActiveByProductId(productId: Long): LearnedRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: LearnedRuleEntity): Long

    @Update
    suspend fun update(rule: LearnedRuleEntity): Int

    @Delete
    suspend fun delete(rule: LearnedRuleEntity): Int

    @Query("DELETE FROM LEARNED_RULE WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE LEARNED_RULE SET isActive = 0 WHERE id = :id")
    suspend fun deactivate(id: String): Int

    @Query("UPDATE LEARNED_RULE SET lastAppliedAt = :lastAppliedAt WHERE id = :id")
    suspend fun updateLastAppliedAt(id: String, lastAppliedAt: Long): Int
}