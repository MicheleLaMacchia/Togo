package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.TodoTaskDao
import it.togo.app.data.database.entity.TodoTaskEntity
import it.togo.app.domain.model.TodoPriority
import it.togo.app.domain.model.TodoStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Test del DAO `TODO_TASK`. Test scritti, non eseguiti.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TodoTaskDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: TodoTaskDao

    @Before
    fun setup() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.todoTaskDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `observeOpen mette URGENT prima e ordina per createdAt DESC`() = runBlocking {
        val normalOlder = dao.insert(entity(title = "normale vecchia", createdAt = 100L))
        val urgentOlder = dao.insert(
            entity(title = "urgente vecchia", priority = TodoPriority.URGENT, createdAt = 200L),
        )
        val normalNewer = dao.insert(entity(title = "normale nuova", createdAt = 300L))
        val urgentNewer = dao.insert(
            entity(title = "urgente nuova", priority = TodoPriority.URGENT, createdAt = 400L),
        )
        dao.insert(
            entity(
                title = "completata",
                status = TodoStatus.COMPLETED,
                createdAt = 500L,
                statusChangedAt = 500L,
            ),
        )

        val open = dao.observeOpen().first()

        assertEquals(
            listOf(urgentNewer, urgentOlder, normalNewer, normalOlder),
            open.map { it.id },
        )
        assertEquals(TodoStatus.OPEN, open.first().status)
    }

    @Test
    fun `observeCompleted e observeDeleted ordinano per statusChangedAt DESC`() = runBlocking {
        val completedOlder = dao.insert(
            entity(
                title = "completata vecchia",
                status = TodoStatus.COMPLETED,
                createdAt = 100L,
                statusChangedAt = 100L,
            ),
        )
        val deletedOlder = dao.insert(
            entity(
                title = "eliminata vecchia",
                status = TodoStatus.DELETED,
                createdAt = 100L,
                statusChangedAt = 200L,
            ),
        )
        val completedNewer = dao.insert(
            entity(
                title = "completata nuova",
                status = TodoStatus.COMPLETED,
                createdAt = 100L,
                statusChangedAt = 300L,
            ),
        )
        val deletedNewer = dao.insert(
            entity(
                title = "eliminata nuova",
                status = TodoStatus.DELETED,
                createdAt = 100L,
                statusChangedAt = 400L,
            ),
        )

        assertEquals(
            listOf(completedNewer, completedOlder),
            dao.observeCompleted().first().map { it.id },
        )
        assertEquals(
            listOf(deletedNewer, deletedOlder),
            dao.observeDeleted().first().map { it.id },
        )
    }

    @Test
    fun `getById restituisce tutti i campi`() = runBlocking {
        val dueDate = LocalDate.of(2026, 11, 3)
        val id = dao.insert(
            entity(
                title = "Pagare bolletta",
                priority = TodoPriority.URGENT,
                createdAt = 1_000L,
                dueDate = dueDate,
                note = "Entro lunedì",
            ),
        )

        val loaded = dao.getById(id)

        assertEquals(id, loaded?.id)
        assertEquals("Pagare bolletta", loaded?.title)
        assertEquals("pagare bolletta", loaded?.normalizedTitle)
        assertEquals(TodoPriority.URGENT, loaded?.priority)
        assertEquals(dueDate, loaded?.dueDate)
        assertEquals("Entro lunedì", loaded?.note)
        assertEquals(TodoStatus.OPEN, loaded?.status)
        assertEquals(1_000L, loaded?.createdAt)
        assertEquals(1_000L, loaded?.statusChangedAt)
        assertNull(loaded?.completedAt)
    }

    @Test
    fun `insert e update persistono le modifiche`() = runBlocking {
        val id = dao.insert(entity(title = "Titolo originale", createdAt = 1_000L))
        val existing = dao.getById(id)!!

        val updated = existing.copy(
            title = "Titolo aggiornato",
            normalizedTitle = "titolo aggiornato",
            priority = TodoPriority.URGENT,
            dueDate = LocalDate.of(2026, 12, 24),
            note = "Nota aggiornata",
            status = TodoStatus.COMPLETED,
            statusChangedAt = 2_000L,
            completedAt = 2_000L,
        )

        assertEquals(1, dao.update(updated))
        assertEquals(updated, dao.getById(id))
    }

    private fun entity(
        title: String,
        priority: TodoPriority = TodoPriority.NORMAL,
        status: TodoStatus = TodoStatus.OPEN,
        createdAt: Long,
        statusChangedAt: Long = createdAt,
        dueDate: LocalDate? = null,
        note: String? = null,
        categoryId: Long? = null,
    ): TodoTaskEntity = TodoTaskEntity(
        title = title,
        normalizedTitle = title.lowercase(),
        categoryId = categoryId,
        priority = priority,
        dueDate = dueDate,
        note = note,
        status = status,
        createdAt = createdAt,
        statusChangedAt = statusChangedAt,
        completedAt = if (status == TodoStatus.COMPLETED) statusChangedAt else null,
    )
}
