package it.togo.app.domain.repository

import it.togo.app.domain.model.TaskCategory
import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoTask
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contratto di dominio delle Task.
 *
 * Non contiene UI, filtri di presentazione o categorie personalizzate: le
 * categorie sono quelle seminate e il repository si limita a validarne il
 * livello e il parent.
 */
interface TodoRepository {

    fun observeAll(): Flow<List<TodoTask>>
    fun observeOpen(): Flow<List<TodoTask>>
    fun observeCompleted(): Flow<List<TodoTask>>
    fun observeDeleted(): Flow<List<TodoTask>>
    fun observeCategories(): Flow<List<TaskCategory>>

    suspend fun getById(id: Long): TodoTask?
    suspend fun getCategory(id: Long): TaskCategory?

    suspend fun create(
        title: String,
        priority: TodoPriority = TodoPriority.NORMAL,
        categoryId: Long? = null,
        dueDate: LocalDate? = null,
        note: String? = null,
    ): TodoTask

    suspend fun updateDetails(
        id: Long,
        title: String,
        priority: TodoPriority,
        categoryId: Long?,
        dueDate: LocalDate?,
        note: String?,
    ): TodoTask?

    suspend fun complete(id: Long): TodoTask?
    suspend fun reopen(id: Long): TodoTask?
    suspend fun delete(id: Long): TodoTask?
    suspend fun restore(id: Long): TodoTask?
}
