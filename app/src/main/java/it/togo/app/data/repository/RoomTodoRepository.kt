package it.togo.app.data.repository

import androidx.room.withTransaction
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.TaskCategoryDao
import it.togo.app.data.database.dao.TodoTaskDao
import it.togo.app.domain.model.TaskCategory
import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoStatus
import it.togo.app.domain.model.TodoTask
import it.togo.app.domain.model.toDomain
import it.togo.app.domain.repository.TodoRepository
import it.togo.app.domain.task.TaskTransitions
import it.togo.app.domain.task.TaskValidation
import it.togo.app.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Implementazione Room di [TodoRepository].
 *
 * Le operazioni che leggono e poi scrivono girano in una transazione. I
 * timestamp arrivano sempre dal [Clock] iniettato: il repository non legge
 * mai direttamente l'orologio di sistema.
 */
class RoomTodoRepository(
    private val database: TogoDatabase,
    private val todoTaskDao: TodoTaskDao,
    private val taskCategoryDao: TaskCategoryDao,
    private val clock: Clock,
) : TodoRepository {

    override fun observeAll(): Flow<List<TodoTask>> =
        todoTaskDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeOpen(): Flow<List<TodoTask>> =
        todoTaskDao.observeOpen().map { entities -> entities.map { it.toDomain() } }

    override fun observeCompleted(): Flow<List<TodoTask>> =
        todoTaskDao.observeCompleted().map { entities -> entities.map { it.toDomain() } }

    override fun observeDeleted(): Flow<List<TodoTask>> =
        todoTaskDao.observeDeleted().map { entities -> entities.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<TaskCategory>> =
        taskCategoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): TodoTask? =
        todoTaskDao.getById(id)?.toDomain()

    override suspend fun getCategory(id: Long): TaskCategory? =
        taskCategoryDao.getById(id)?.toDomain()

    override suspend fun create(
        title: String,
        priority: TodoPriority,
        categoryId: Long?,
        dueDate: LocalDate?,
        note: String?,
    ): TodoTask = database.withTransaction {
        val trimmedTitle = TaskValidation.validateAndTrimTitle(title)
        val normalizedTitle = TaskValidation.normalizeTitle(trimmedTitle)
        val normalizedNote = TaskValidation.validateAndNormalizeNote(note)
        validateCategory(categoryId)

        val now = clock.nowMillis()
        val task = TodoTask(
            id = 0,
            title = trimmedTitle,
            normalizedTitle = normalizedTitle,
            categoryId = categoryId,
            priority = priority,
            dueDate = dueDate,
            note = normalizedNote,
            status = TodoStatus.OPEN,
            createdAt = now,
            statusChangedAt = now,
            completedAt = null,
        )
        val id = todoTaskDao.insert(task.toEntity())
        task.copy(id = id)
    }

    override suspend fun updateDetails(
        id: Long,
        title: String,
        priority: TodoPriority,
        categoryId: Long?,
        dueDate: LocalDate?,
        note: String?,
    ): TodoTask? = database.withTransaction {
        val existing = todoTaskDao.getById(id) ?: return@withTransaction null
        if (existing.status != TodoStatus.OPEN) return@withTransaction null

        val trimmedTitle = TaskValidation.validateAndTrimTitle(title)
        val normalizedTitle = TaskValidation.normalizeTitle(trimmedTitle)
        val normalizedNote = TaskValidation.validateAndNormalizeNote(note)
        validateCategory(categoryId)

        val updated = existing.toDomain().copy(
            title = trimmedTitle,
            normalizedTitle = normalizedTitle,
            categoryId = categoryId,
            priority = priority,
            dueDate = dueDate,
            note = normalizedNote,
        )
        todoTaskDao.update(updated.toEntity())
        updated
    }

    override suspend fun complete(id: Long): TodoTask? =
        transition(id) { task, at -> TaskTransitions.complete(task, at) }

    override suspend fun reopen(id: Long): TodoTask? =
        transition(id) { task, at -> TaskTransitions.reopen(task, at) }

    override suspend fun delete(id: Long): TodoTask? =
        transition(id) { task, at -> TaskTransitions.delete(task, at) }

    override suspend fun restore(id: Long): TodoTask? =
        transition(id) { task, at -> TaskTransitions.restore(task, at) }

    private suspend fun transition(
        id: Long,
        operation: (TodoTask, Long) -> TodoTask,
    ): TodoTask? = database.withTransaction {
        val existing = todoTaskDao.getById(id) ?: return@withTransaction null
        val current = existing.toDomain()

        val updated = try {
            operation(current, clock.nowMillis())
        } catch (_: IllegalStateException) {
            return@withTransaction null
        }

        if (updated !== current) {
            todoTaskDao.update(updated.toEntity())
        }
        updated
    }

    private suspend fun validateCategory(categoryId: Long?) {
        if (categoryId == null) return

        val category = taskCategoryDao.getById(categoryId)
            ?: throw IllegalArgumentException("Categoria Task inesistente: $categoryId")

        when (category.level) {
            1 -> {
                if (category.parentId != null) {
                    throw IllegalArgumentException(
                        "Una categoria Task di livello 1 non può avere un parent",
                    )
                }
            }

            2 -> {
                val parentId = category.parentId
                    ?: throw IllegalArgumentException(
                        "Una categoria Task di livello 2 deve avere un parent",
                    )
                val parent = taskCategoryDao.getById(parentId)
                    ?: throw IllegalArgumentException(
                        "Il parent della categoria Task non esiste: $parentId",
                    )
                if (parent.level != 1 || parent.parentId != null) {
                    throw IllegalArgumentException(
                        "Il parent della categoria Task deve essere di livello 1",
                    )
                }
            }

            else -> throw IllegalArgumentException(
                "Il livello della categoria Task deve essere 1 o 2, ricevuto: ${category.level}",
            )
        }
    }
}
