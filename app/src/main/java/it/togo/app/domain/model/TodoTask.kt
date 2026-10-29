package it.togo.app.domain.model

import it.togo.app.data.database.entity.TodoTaskEntity
import java.time.LocalDate

/**
 * Task del dominio.
 *
 * Le modifiche di stato passano da [it.togo.app.domain.task.TaskTransitions]:
 * il modello espone solo le invarianti comuni a ogni stato.
 */
data class TodoTask(
    val id: Long,
    val title: String,
    val normalizedTitle: String,
    val categoryId: Long?,
    val priority: TodoPriority,
    val dueDate: LocalDate?,
    val note: String?,
    val status: TodoStatus,
    val createdAt: Long,
    val statusChangedAt: Long,
    val completedAt: Long?,
) {

    init {
        require(id >= 0) { "L'id della Task non può essere negativo" }
        require(title.isNotBlank()) { "Il titolo della Task non può essere vuoto" }
        require(normalizedTitle.isNotBlank()) {
            "Il titolo normalizzato della Task non può essere vuoto"
        }
        require(statusChangedAt >= createdAt) {
            "statusChangedAt deve essere maggiore o uguale a createdAt"
        }

        when (status) {
            TodoStatus.COMPLETED -> require(completedAt != null) {
                "Una Task COMPLETED deve avere completedAt valorizzato"
            }

            TodoStatus.OPEN, TodoStatus.DELETED -> require(completedAt == null) {
                "Una Task $status non può avere completedAt valorizzato"
            }
        }
    }

    fun toEntity(): TodoTaskEntity = TodoTaskEntity(
        id = id,
        title = title,
        normalizedTitle = normalizedTitle,
        categoryId = categoryId,
        priority = priority,
        dueDate = dueDate,
        note = note,
        status = status,
        createdAt = createdAt,
        statusChangedAt = statusChangedAt,
        completedAt = completedAt,
    )
}

fun TodoTaskEntity.toDomain(): TodoTask = TodoTask(
    id = id,
    title = title,
    normalizedTitle = normalizedTitle,
    categoryId = categoryId,
    priority = priority,
    dueDate = dueDate,
    note = note,
    status = status,
    createdAt = createdAt,
    statusChangedAt = statusChangedAt,
    completedAt = completedAt,
)
