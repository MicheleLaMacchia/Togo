package it.togo.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import it.togo.app.data.database.entity.TaskCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Accesso read-only alle categorie Task seminate dalla migrazione `5→6`.
 *
 * In v1 non esistono categorie personalizzate: il DAO non espone insert,
 * update o delete.
 */
@Dao
interface TaskCategoryDao {

    @Query(
        """
        SELECT * FROM TASK_CATEGORY
        ORDER BY level ASC, sortOrder ASC, id ASC
        """
    )
    fun observeAll(): Flow<List<TaskCategoryEntity>>

    @Query("SELECT * FROM TASK_CATEGORY WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskCategoryEntity?

    @Query(
        """
        SELECT * FROM TASK_CATEGORY
        WHERE parentId = :parentId
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun getChildren(parentId: Long): List<TaskCategoryEntity>

    @Query(
        """
        SELECT * FROM TASK_CATEGORY
        WHERE level = :level
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun getByLevel(level: Int): List<TaskCategoryEntity>
}
