package it.togo.app.data.database

import org.junit.Assert.assertEquals
import org.junit.Test

class LearnedRulesMigrationTest {

    @Test
    fun `accented expression is folded`() {
        val kept = normalizeLearnedRules(
            listOf(LearnedRuleMigrationRow(id = "r1", expression = "Caffè", lastAppliedAt = 10))
        )

        assertEquals(listOf("r1" to "caffe"), kept)
    }

    @Test
    fun `collision keeps the most recently applied rule`() {
        val kept = normalizeLearnedRules(
            listOf(
                LearnedRuleMigrationRow(id = "old", expression = "caffè", lastAppliedAt = 10),
                LearnedRuleMigrationRow(id = "new", expression = "caffe", lastAppliedAt = 20),
            )
        )

        assertEquals(listOf("new" to "caffe"), kept)
    }

    @Test
    fun `same timestamp uses id as deterministic tie breaker`() {
        val kept = normalizeLearnedRules(
            listOf(
                LearnedRuleMigrationRow(id = "b", expression = "caffè", lastAppliedAt = 10),
                LearnedRuleMigrationRow(id = "a", expression = "caffe", lastAppliedAt = 10),
            )
        )

        assertEquals(listOf("b" to "caffe"), kept)
    }

    @Test
    fun `empty expression is left untouched`() {
        val kept = normalizeLearnedRules(
            listOf(LearnedRuleMigrationRow(id = "empty", expression = "   ", lastAppliedAt = 1))
        )

        assertEquals(listOf("empty" to "   "), kept)
    }
}
