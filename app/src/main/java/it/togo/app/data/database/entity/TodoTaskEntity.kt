package it.togo.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoStatus
import java.time.LocalDate

/**
 * Entità Room di una Task.
 *
 * Le transizioni di stato e le invarianti di titolo/nota appartengono al
 * dominio della Story 8.6; qui è definita solo la persistenza.
 */
@Entity(
    tableName = "TODO_TASK",
    foreignKeys = [
        ForeignKey(
            entity = TaskCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["status"], name = "idx_todo_task_status"),
        Index(value = ["categoryId"], name = "idx_todo_task_category"),
        Index(value = ["createdAt"], name = "idx_todo_task_created_at"),
        Index(
            value = ["status", "priority", "createdAt"],
            name = "idx_todo_task_status_priority_created",
        ),
        Index(value = ["dueDate"], name = "idx_todo_task_due_date"),
    ],
)
data class TodoTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val normalizedTitle: String,
    val categoryId: Long? = null,
    val priority: TodoPriority = TodoPriority.NORMAL,
    val dueDate: LocalDate? = null,
    val note: String? = null,
    val status: TodoStatus = TodoStatus.OPEN,
    val createdAt: Long,
    val statusChangedAt: Long,
    val completedAt: Long? = null,
)
