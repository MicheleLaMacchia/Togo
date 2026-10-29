package it.togo.app.domain.task

import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoStatus
import it.togo.app.domain.model.TodoTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Test delle transizioni di stato delle Task. Test scritti, non eseguiti.
 */
class TaskTransitionsTest {

    private fun openTask(): TodoTask = TodoTask(
        id = 42L,
        title = "Comprare latte",
        normalizedTitle = "comprare latte",
        categoryId = null,
        priority = TodoPriority.NORMAL,
        dueDate = null,
        note = null,
        status = TodoStatus.OPEN,
        createdAt = 1_000L,
        statusChangedAt = 1_000L,
        completedAt = null,
    )

    private fun completedTask(): TodoTask =
        TaskTransitions.complete(openTask(), at = 2_000L)

    private fun deletedTask(): TodoTask =
        TaskTransitions.delete(openTask(), at = 2_000L)

    @Test
    fun `complete porta OPEN a COMPLETED e usa at`() {
        val completed = TaskTransitions.complete(openTask(), at = 2_000L)

        assertEquals(TodoStatus.COMPLETED, completed.status)
        assertEquals(2_000L, completed.completedAt)
        assertEquals(2_000L, completed.statusChangedAt)
        assertEquals(42L, completed.id)
        assertEquals(1_000L, completed.createdAt)
    }

    @Test
    fun `complete due volte e idempotente e non cambia i timestamp`() {
        val completed = completedTask()

        val again = TaskTransitions.complete(completed, at = 9_000L)

        assertSame(completed, again)
        assertEquals(2_000L, again.statusChangedAt)
        assertEquals(2_000L, again.completedAt)
    }

    @Test
    fun `reopen porta COMPLETED a OPEN e azzera completedAt`() {
        val reopened = TaskTransitions.reopen(completedTask(), at = 3_000L)

        assertEquals(TodoStatus.OPEN, reopened.status)
        assertNull(reopened.completedAt)
        assertEquals(3_000L, reopened.statusChangedAt)
        assertEquals(42L, reopened.id)
        assertEquals(1_000L, reopened.createdAt)
    }

    @Test
    fun `reopen due volte e idempotente`() {
        val reopened = TaskTransitions.reopen(completedTask(), at = 3_000L)

        val again = TaskTransitions.reopen(reopened, at = 9_000L)

        assertSame(reopened, again)
        assertEquals(3_000L, again.statusChangedAt)
        assertNull(again.completedAt)
    }

    @Test
    fun `delete porta OPEN a DELETED`() {
        val deleted = TaskTransitions.delete(openTask(), at = 2_000L)

        assertEquals(TodoStatus.DELETED, deleted.status)
        assertNull(deleted.completedAt)
        assertEquals(2_000L, deleted.statusChangedAt)
        assertEquals(42L, deleted.id)
        assertEquals(1_000L, deleted.createdAt)
    }

    @Test
    fun `delete due volte e idempotente`() {
        val deleted = deletedTask()

        val again = TaskTransitions.delete(deleted, at = 9_000L)

        assertSame(deleted, again)
        assertEquals(2_000L, again.statusChangedAt)
    }

    @Test
    fun `delete di una Task COMPLETED non e ammesso`() {
        assertThrows(IllegalStateException::class.java) {
            TaskTransitions.delete(completedTask(), at = 3_000L)
        }
    }

    @Test
    fun `restore porta DELETED a OPEN e azzera completedAt`() {
        val restored = TaskTransitions.restore(deletedTask(), at = 3_000L)

        assertEquals(TodoStatus.OPEN, restored.status)
        assertNull(restored.completedAt)
        assertEquals(3_000L, restored.statusChangedAt)
        assertEquals(42L, restored.id)
        assertEquals(1_000L, restored.createdAt)
    }

    @Test
    fun `restore due volte e idempotente`() {
        val restored = TaskTransitions.restore(deletedTask(), at = 3_000L)

        val again = TaskTransitions.restore(restored, at = 9_000L)

        assertSame(restored, again)
        assertEquals(3_000L, again.statusChangedAt)
        assertNull(again.completedAt)
    }

    @Test
    fun `restore di una Task COMPLETED non e ammesso`() {
        assertThrows(IllegalStateException::class.java) {
            TaskTransitions.restore(completedTask(), at = 3_000L)
        }
    }

    @Test
    fun `complete e reopen di una Task DELETED non sono ammessi`() {
        assertThrows(IllegalStateException::class.java) {
            TaskTransitions.complete(deletedTask(), at = 3_000L)
        }
        assertThrows(IllegalStateException::class.java) {
            TaskTransitions.reopen(deletedTask(), at = 3_000L)
        }
    }

    @Test
    fun `reopen su OPEN e restore su OPEN restituiscono la stessa istanza`() {
        val open = openTask()

        assertSame(open, TaskTransitions.reopen(open, at = 9_000L))
        assertSame(open, TaskTransitions.restore(open, at = 9_000L))
    }
}
