package it.togo.app.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import it.togo.app.data.database.dao.LearnedRulesDao
import it.togo.app.data.database.entity.LearnedRuleEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LearnedRulesDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TogoDatabase
    private lateinit var dao: LearnedRulesDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, TogoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.learnedRulesDao()
    }

    @Test
    fun `insert and getAll emits rule`() = runBlocking {
        val rule = LearnedRuleEntity(
            id = "rule-1",
            userExpression = "latte ps",
            productId = 100,
            level3Id = 10,
            lastAppliedAt = System.currentTimeMillis(),
            isActive = true
        )

        dao.insert(rule)

        val allRules = dao.getAll().first()
        assertEquals(1, allRules.size)
        assertEquals("latte ps", allRules[0].userExpression)
        assertEquals(100, allRules[0].productId)
        assertTrue(allRules[0].isActive)
    }

    @Test
    fun `getByExpression finds rule`() = runBlocking {
        val rule = LearnedRuleEntity(
            id = "rule-2",
            userExpression = "pane integrale",
            productId = 200,
            level3Id = 20,
            lastAppliedAt = System.currentTimeMillis(),
            isActive = true
        )

        dao.insert(rule)

        val found = dao.getByExpression("pane integrale")
        assertNotNull(found)
        assertEquals("rule-2", found?.id)

        val notFound = dao.getByExpression("inesistente")
        assertNull(notFound)
    }

    @Test
    fun `update deactivates rule`() = runBlocking {
        val rule = LearnedRuleEntity(
            id = "rule-3",
            userExpression = "yogurt greco",
            productId = 300,
            level3Id = 30,
            lastAppliedAt = System.currentTimeMillis(),
            isActive = true
        )

        dao.insert(rule)

        var allRules = dao.getAll().first()
        assertEquals(1, allRules.size)
        assertTrue(allRules[0].isActive)

        dao.deactivate("rule-3")

        allRules = dao.getAll().first()
        assertEquals(0, allRules.size) // Inactive rules filtered out
    }

    @Test
    fun `getActiveByProductId finds active rule`() = runBlocking {
        val rule = LearnedRuleEntity(
            id = "rule-4",
            userExpression = "formaggio grana",
            productId = 400,
            level3Id = 40,
            lastAppliedAt = System.currentTimeMillis(),
            isActive = true
        )

        dao.insert(rule)

        val found = dao.getActiveByProductId(400)
        assertNotNull(found)
        assertEquals("rule-4", found?.id)

        dao.deactivate("rule-4")

        val notFound = dao.getActiveByProductId(400)
        assertNull(notFound)
    }

    @Test
    fun `updateLastAppliedAt updates timestamp`() = runBlocking {
        val rule = LearnedRuleEntity(
            id = "rule-5",
            userExpression = "prosciutto crudo",
            productId = 500,
            level3Id = 50,
            lastAppliedAt = 1000L,
            isActive = true
        )

        dao.insert(rule)

        var found = dao.getById("rule-5")
        assertNotNull(found)
        assertEquals(1000L, found?.lastAppliedAt)

        val newTime = System.currentTimeMillis()
        dao.updateLastAppliedAt("rule-5", newTime)

        found = dao.getById("rule-5")
        assertNotNull(found)
        assertEquals(newTime, found?.lastAppliedAt)
    }

    @Test
    fun `deleteById removes rule`() = runBlocking {
        val rule = LearnedRuleEntity(
            id = "rule-6",
            userExpression = "salame milano",
            productId = 600,
            level3Id = 60,
            lastAppliedAt = System.currentTimeMillis(),
            isActive = true
        )

        dao.insert(rule)
        var allRules = dao.getAll().first()
        assertEquals(1, allRules.size)

        dao.deleteById("rule-6")

        allRules = dao.getAll().first()
        assertEquals(0, allRules.size)
    }
}