package it.togo.app.presentation.activelist

import it.togo.app.domain.model.ActionItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test JVM dell'ordinamento della sezione "Fatte" (Story 9.1).
 *
 * La sezione mostra per prima l'azione completata più di recente: il criterio
 * è `updatedAt` (valorizzato alla spunta), con `createdAt` come tie-break.
 */
class ActiveListActionOrderingTest {

    private fun action(
        id: String,
        createdAt: Long,
        updatedAt: Long,
        isChecked: Boolean = true,
    ) = ActionItem(
        id = id,
        text = "Azione $id",
        isChecked = isChecked,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    @Test
    fun `sorts by updatedAt descending`() {
        val oldest = action(id = "oldest", createdAt = 1L, updatedAt = 10L)
        val newest = action(id = "newest", createdAt = 2L, updatedAt = 30L)
        val middle = action(id = "middle", createdAt = 3L, updatedAt = 20L)

        val sorted = listOf(oldest, newest, middle).mostRecentFirst()

        assertEquals(listOf("newest", "middle", "oldest"), sorted.map { it.id })
    }

    @Test
    fun `uses createdAt as tie break for equal updatedAt`() {
        val older = action(id = "older", createdAt = 10L, updatedAt = 50L)
        val newer = action(id = "newer", createdAt = 20L, updatedAt = 50L)

        val sorted = listOf(older, newer).mostRecentFirst()

        assertEquals(listOf("newer", "older"), sorted.map { it.id })
    }

    @Test
    fun `does not filter or mutate the input list`() {
        val checked = action(id = "checked", createdAt = 1L, updatedAt = 10L)
        val unchecked = action(
            id = "unchecked",
            createdAt = 2L,
            updatedAt = 20L,
            isChecked = false,
        )
        val input = listOf(checked, unchecked)
        val snapshot = input.toList()

        val sorted = input.mostRecentFirst()

        // Il filtro isChecked avviene a monte: l'helper ordina tutto.
        assertEquals(listOf("unchecked", "checked"), sorted.map { it.id })
        assertEquals(snapshot, input)
    }
}
