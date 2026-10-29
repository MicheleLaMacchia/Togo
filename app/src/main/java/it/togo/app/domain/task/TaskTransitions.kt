package it.togo.app.domain.task

import it.togo.app.domain.model.TodoStatus
import it.togo.app.domain.model.TodoTask

/**
 * Transizioni di stato di una Task.
 *
 * Ogni transizione mantiene la stessa riga (stesso id) e non fa merge tra
 * Task omonime. Una richiesta già nello stato target è idempotente e
 * restituisce la stessa istanza senza toccare i timestamp.
 */
object TaskTransitions {

    fun complete(task: TodoTask, at: Long): TodoTask = when (task.status) {
        TodoStatus.OPEN -> task.copy(
            status = TodoStatus.COMPLETED,
            completedAt = at,
            statusChangedAt = at,
        )

        TodoStatus.COMPLETED -> task
        TodoStatus.DELETED -> throw IllegalStateException(
            "Non è possibile completare una Task eliminata",
        )
    }

    fun reopen(task: TodoTask, at: Long): TodoTask = when (task.status) {
        TodoStatus.COMPLETED -> task.copy(
            status = TodoStatus.OPEN,
            completedAt = null,
            statusChangedAt = at,
        )

        TodoStatus.OPEN -> task
        TodoStatus.DELETED -> throw IllegalStateException(
            "Non è possibile riaprire una Task eliminata",
        )
    }

    fun delete(task: TodoTask, at: Long): TodoTask = when (task.status) {
        TodoStatus.OPEN -> task.copy(
            status = TodoStatus.DELETED,
            completedAt = null,
            statusChangedAt = at,
        )

        TodoStatus.DELETED -> task
        TodoStatus.COMPLETED -> throw IllegalStateException(
            "Non è possibile eliminare una Task completata",
        )
    }

    fun restore(task: TodoTask, at: Long): TodoTask = when (task.status) {
        TodoStatus.DELETED -> task.copy(
            status = TodoStatus.OPEN,
            completedAt = null,
            statusChangedAt = at,
        )

        TodoStatus.OPEN -> task
        TodoStatus.COMPLETED -> throw IllegalStateException(
            "Non è possibile ripristinare una Task completata",
        )
    }
}
