package it.togo.app.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.TaskCategoryTestSeed
import it.togo.app.data.database.TogoDatabase
import it.togo.app.data.database.dao.TodoTaskDao
import it.togo.app.data.database.entity.TaskCategoryEntity
import it.togo.app.data.time.FakeClock
import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoStatus
import it.togo.app.domain.model.toDomain
import it.togo.app.domain.repository.TodoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Test del repository Room delle Task. Test scritti, non eseguiti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomTodoRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var todoTaskDao: TodoTaskDao
    private lateinit var clock: FakeClock
    private lateinit var repository: TodoRepository

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        TaskCategoryTestSeed.insert(db)
        todoTaskDao = db.todoTaskDao()
        clock = FakeClock(now = 1_000L)
        repository = RoomTodoRepository(
            database = db,
            todoTaskDao = todoTaskDao,
            taskCategoryDao = db.taskCategoryDao(),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `create con soli titolo applica i default e usa il Clock`() = runBlocking {
        val task = repository.create("  Comprare latte  ")

        assertTrue(task.id > 0L)
        assertEquals("Comprare latte", task.title)
        assertEquals("comprare latte", task.normalizedTitle)
        assertNull(task.categoryId)
        assertEquals(TodoPriority.NORMAL, task.priority)
        assertNull(task.dueDate)
        assertNull(task.note)
        assertEquals(TodoStatus.OPEN, task.status)
        assertEquals(1_000L, task.createdAt)
        assertEquals(1_000L, task.statusChangedAt)
        assertNull(task.completedAt)
        assertEquals(task, todoTaskDao.getById(task.id)?.toDomain())
    }

    @Test
    fun `due Task con lo stesso titolo producono due righe distinte`() = runBlocking {
        val first = repository.create("Latte")
        clock.now = 2_000L
        val second = repository.create("Latte")

        assertNotEquals(first.id, second.id)
        assertEquals(2, repository.observeAll().first().size)
        assertEquals(2, todoTaskDao.observeAll().first().size)
        assertEquals(first.normalizedTitle, second.normalizedTitle)
    }

    @Test
    fun `create accetta una Area T1 e una sottocategoria T2`() = runBlocking {
        val area = repository.create("Solo Area", categoryId = 1L)
        val subcategory = repository.create("Sottocategoria", categoryId = 101L)

        assertEquals(1L, area.categoryId)
        assertEquals(101L, subcategory.categoryId)
        assertEquals("Pulizie", repository.getCategory(101L)?.name)
    }

    @Test
    fun `create rifiuta una categoria inesistente o di livello 3`() = runBlocking {
        TaskCategoryTestSeed.insert(
            db,
            listOf(TaskCategoryEntity(900L, 101L, "Livello tre", 3, 10)),
        )

        val missing = illegalArgumentFrom {
            repository.create("Categoria assente", categoryId = 999L)
        }
        assertNotNull(missing)
        assertTrue(missing!!.message.orEmpty().contains("inesistente", ignoreCase = true))

        val levelThree = illegalArgumentFrom {
            repository.create("Categoria troppo profonda", categoryId = 900L)
        }
        assertNotNull(levelThree)
        assertTrue(levelThree!!.message.orEmpty().contains("livello", ignoreCase = true))

        assertEquals(0, repository.observeAll().first().size)
    }

    @Test
    fun `create valida titolo e nota`() = runBlocking {
        val blankTitle = illegalArgumentFrom { repository.create("   ") }
        assertNotNull(blankTitle)

        val longNote = illegalArgumentFrom {
            repository.create("Titolo valido", note = "a".repeat(1001))
        }
        assertNotNull(longNote)

        assertEquals(0, repository.observeAll().first().size)
    }

    @Test
    fun `updateDetails aggiorna una Task OPEN senza cambiare stato e timestamp di stato`() =
        runBlocking {
            val created = repository.create("Titolo originale")
            clock.now = 2_000L
            val dueDate = LocalDate.of(2026, 11, 3)

            val updated = repository.updateDetails(
                id = created.id,
                title = "  Titolo aggiornato  ",
                priority = TodoPriority.URGENT,
                categoryId = 101L,
                dueDate = dueDate,
                note = "  Nota\nsu due righe  ",
            ) ?: error("updateDetails ha restituito null")

            assertEquals(created.id, updated.id)
            assertEquals("Titolo aggiornato", updated.title)
            assertEquals("titolo aggiornato", updated.normalizedTitle)
            assertEquals(TodoPriority.URGENT, updated.priority)
            assertEquals(101L, updated.categoryId)
            assertEquals(dueDate, updated.dueDate)
            assertEquals("Nota\nsu due righe", updated.note)
            assertEquals(TodoStatus.OPEN, updated.status)
            assertEquals(1_000L, updated.createdAt)
            assertEquals(1_000L, updated.statusChangedAt)
            assertNull(updated.completedAt)
            assertEquals(updated, todoTaskDao.getById(created.id)?.toDomain())
        }

    @Test
    fun `updateDetails restituisce null su COMPLETED e DELETED`() = runBlocking {
        val toComplete = repository.create("Da completare")
        clock.now = 2_000L
        val completed = repository.complete(toComplete.id)!!

        assertNull(
            repository.updateDetails(
                completed.id,
                "Non deve cambiare",
                TodoPriority.NORMAL,
                null,
                null,
                null,
            ),
        )
        assertEquals(completed, repository.getById(completed.id))

        val toDelete = repository.create("Da eliminare")
        clock.now = 3_000L
        val deleted = repository.delete(toDelete.id)!!

        assertNull(
            repository.updateDetails(
                deleted.id,
                "Non deve cambiare",
                TodoPriority.NORMAL,
                null,
                null,
                null,
            ),
        )
        assertEquals(deleted, repository.getById(deleted.id))
    }

    @Test
    fun `complete reopen delete restore sono persistiti sulla stessa riga`() = runBlocking {
        val created = repository.create("Ciclo di vita")
        clock.now = 2_000L
        val completed = repository.complete(created.id)!!

        assertEquals(TodoStatus.COMPLETED, completed.status)
        assertEquals(2_000L, completed.completedAt)
        assertEquals(2_000L, completed.statusChangedAt)
        assertEquals(created.id, completed.id)
        assertEquals(completed, todoTaskDao.getById(created.id)?.toDomain())

        clock.now = 3_000L
        val reopened = repository.reopen(created.id)!!

        assertEquals(TodoStatus.OPEN, reopened.status)
        assertNull(reopened.completedAt)
        assertEquals(3_000L, reopened.statusChangedAt)
        assertEquals(created.id, reopened.id)
        assertEquals(reopened, todoTaskDao.getById(created.id)?.toDomain())

        clock.now = 4_000L
        val deleted = repository.delete(created.id)!!

        assertEquals(TodoStatus.DELETED, deleted.status)
        assertNull(deleted.completedAt)
        assertEquals(4_000L, deleted.statusChangedAt)
        assertEquals(created.id, deleted.id)
        assertEquals(deleted, todoTaskDao.getById(created.id)?.toDomain())

        clock.now = 5_000L
        val restored = repository.restore(created.id)!!

        assertEquals(TodoStatus.OPEN, restored.status)
        assertNull(restored.completedAt)
        assertEquals(5_000L, restored.statusChangedAt)
        assertEquals(1_000L, restored.createdAt)
        assertEquals(created.id, restored.id)
        assertEquals(restored, todoTaskDao.getById(created.id)?.toDomain())
        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun `doppio complete reopen delete restore e idempotente`() = runBlocking {
        val created = repository.create("Doppio tap")

        clock.now = 2_000L
        val completed = repository.complete(created.id)!!
        clock.now = 9_000L
        val completedAgain = repository.complete(created.id)!!

        assertEquals(completed, completedAgain)
        assertEquals(2_000L, completedAgain.statusChangedAt)
        assertEquals(2_000L, completedAgain.completedAt)

        clock.now = 3_000L
        val reopened = repository.reopen(created.id)!!
        clock.now = 9_000L
        val reopenedAgain = repository.reopen(created.id)!!

        assertEquals(reopened, reopenedAgain)
        assertEquals(3_000L, reopenedAgain.statusChangedAt)
        assertNull(reopenedAgain.completedAt)

        clock.now = 4_000L
        val deleted = repository.delete(created.id)!!
        clock.now = 9_000L
        val deletedAgain = repository.delete(created.id)!!

        assertEquals(deleted, deletedAgain)
        assertEquals(4_000L, deletedAgain.statusChangedAt)

        clock.now = 5_000L
        val restored = repository.restore(created.id)!!
        clock.now = 9_000L
        val restoredAgain = repository.restore(created.id)!!

        assertEquals(restored, restoredAgain)
        assertEquals(5_000L, restoredAgain.statusChangedAt)
        assertNull(restoredAgain.completedAt)
        assertEquals(1, repository.observeAll().first().size)
    }

    @Test
    fun `delete e restore di una Task COMPLETED restituiscono null`() = runBlocking {
        val created = repository.create("Completata terminale")
        clock.now = 2_000L
        val completed = repository.complete(created.id)!!

        assertNull(repository.delete(created.id))
        assertNull(repository.restore(created.id))

        val unchanged = repository.getById(created.id)!!
        assertEquals(completed, unchanged)
        assertEquals(TodoStatus.COMPLETED, unchanged.status)
        assertEquals(2_000L, unchanged.completedAt)
        assertEquals(2_000L, unchanged.statusChangedAt)
    }

    @Test
    fun `le transizioni su un id inesistente restituiscono null`() = runBlocking {
        assertNull(repository.complete(9_999L))
        assertNull(repository.reopen(9_999L))
        assertNull(repository.delete(9_999L))
        assertNull(repository.restore(9_999L))
    }

    @Test
    fun `la cancellazione della categoria mette categoryId a null sulla Task`() = runBlocking {
        val task = repository.create("Con categoria", categoryId = 101L)

        db.openHelper.writableDatabase.execSQL(
            "DELETE FROM TASK_CATEGORY WHERE id = 101",
        )

        assertNull(todoTaskDao.getById(task.id)?.categoryId)
        assertNull(repository.getById(task.id)?.categoryId)
    }

    @Test
    fun `observeOpen observeCompleted e observeDeleted filtrano per stato`() = runBlocking {
        val open = repository.create("Aperta")
        val toComplete = repository.create("Da completare")
        val toDelete = repository.create("Da eliminare")

        clock.now = 2_000L
        val completed = repository.complete(toComplete.id)!!
        clock.now = 3_000L
        val deleted = repository.delete(toDelete.id)!!

        assertEquals(listOf(open.id), repository.observeOpen().first().map { it.id })
        assertEquals(listOf(completed.id), repository.observeCompleted().first().map { it.id })
        assertEquals(listOf(deleted.id), repository.observeDeleted().first().map { it.id })
        assertEquals(3, repository.observeAll().first().size)
    }

    @Test
    fun `observeCategories mappa il seed nel dominio`() = runBlocking {
        val categories = repository.observeCategories().first()

        assertEquals(16, categories.size)

        val casa = categories.first { it.id == 1L }
        assertEquals("Casa", casa.name)
        assertNull(casa.parentId)
        assertEquals(1, casa.level)
        assertEquals(10, casa.sortOrder)

        val pulizie = categories.first { it.id == 101L }
        assertEquals("Pulizie", pulizie.name)
        assertEquals(1L, pulizie.parentId)
        assertEquals(2, pulizie.level)
        assertEquals(10, pulizie.sortOrder)
    }

    private suspend fun illegalArgumentFrom(
        block: suspend () -> Unit,
    ): IllegalArgumentException? = try {
        block()
        null
    } catch (error: IllegalArgumentException) {
        error
    }
}
