package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import it.togo.app.data.database.entity.TodoTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accesso alla tabella `TODO_TASK` (Story 8.6).
 *
 * Non esiste alcun DELETE fisico: l'eliminazione è sempre logica tramite lo
 * stato `DELETED`.
 */
@Dao
interface TodoTaskDao {

    @Query(
        """
        SELECT * FROM TODO_TASK
        WHERE status = 'OPEN'
        ORDER BY CASE WHEN priority = 'URGENT' THEN 0 ELSE 1 END,
                 createdAt DESC
        """
    )
    fun observeOpen(): Flow<List<TodoTaskEntity>>

    @Query(
        """
        SELECT * FROM TODO_TASK
        WHERE status = 'COMPLETED'
        ORDER BY statusChangedAt DESC
        """
    )
    fun observeCompleted(): Flow<List<TodoTaskEntity>>

    @Query(
        """
        SELECT * FROM TODO_TASK
        WHERE status = 'DELETED'
        ORDER BY statusChangedAt DESC
        """
    )
    fun observeDeleted(): Flow<List<TodoTaskEntity>>

    @Query("SELECT * FROM TODO_TASK ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TodoTaskEntity>>

    @Query("SELECT * FROM TODO_TASK WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoTaskEntity?

    @Insert
    suspend fun insert(entity: TodoTaskEntity): Long

    @Update
    suspend fun update(entity: TodoTaskEntity): Int
}
