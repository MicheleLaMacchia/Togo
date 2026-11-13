package it.togo.app.domain.action

import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionShareFormatterTest {

    @Test
    fun `raggruppa per ambito urgenza e alfabetico con formato esatto`() {
        val actions = listOf(
            ActionItem(text = "Passeggiare", scope = ActionScope.ALTRO),
            ActionItem(text = "Zaino", scope = ActionScope.CASA, urgency = ActionUrgency.URGENTE),
            ActionItem(text = "Inviare relazione", scope = ActionScope.LAVORO),
            ActionItem(text = "Ristrutturare", scope = ActionScope.CASA, urgency = ActionUrgency.ECCEZIONALE),
            ActionItem(text = "Prenotare visita", scope = ActionScope.SALUTE),
            ActionItem(text = "Riordinare", scope = ActionScope.CASA),
            ActionItem(text = "Pagare bollo", scope = ActionScope.PRATICHE),
            ActionItem(text = "aprire finestre", scope = ActionScope.CASA, urgency = ActionUrgency.URGENTE),
            ActionItem(text = "Pulire", scope = ActionScope.CASA),
        )
        val expected = """
            [DA FARE]
            + CASA
            - URGENTE
              aprire finestre
              Zaino
            - A TEMPO PERSO
              Pulire
              Riordinare
            - ECCEZIONALE
              Ristrutturare
            + PRATICHE
            - A TEMPO PERSO
              Pagare bollo
            + SALUTE
            - A TEMPO PERSO
              Prenotare visita
            + LAVORO
            - A TEMPO PERSO
              Inviare relazione
            + ALTRO
            - A TEMPO PERSO
              Passeggiare
        """.trimIndent()

        assertEquals(expected, buildSharedActionsText(actions))
        assertEquals(expected, buildSharedActionsText(actions.reversed()))
    }

    @Test
    fun `esclude spuntate e gruppi che contengono soltanto spuntate`() {
        val actions = listOf(
            ActionItem(text = "Fatta in casa", scope = ActionScope.CASA, isChecked = true),
            ActionItem(text = "Chiamare", scope = ActionScope.LAVORO, urgency = ActionUrgency.URGENTE),
            ActionItem(text = "Gia chiamato", scope = ActionScope.LAVORO, urgency = ActionUrgency.URGENTE, isChecked = true),
            ActionItem(text = "Fatta con calma", scope = ActionScope.LAVORO, isChecked = true),
        )
        val snapshot = actions.toList()

        assertEquals("[DA FARE]\n+ LAVORO\n- URGENTE\n  Chiamare", buildSharedActionsText(actions))
        assertEquals(snapshot, actions)
    }

    @Test
    fun `non produce blocchi vuoti se mancano azioni attive`() {
        assertEquals("", buildSharedActionsText(emptyList()))
        assertEquals("", buildSharedActionsText(listOf(ActionItem(text = "Fatta", isChecked = true))))
    }

    @Test
    fun `preserva testo numeri accenti e spazi senza aggiungere quantita`() {
        val actions = listOf(ActionItem(text = "Chiamare papà  alle 15"))

        assertEquals(
            "[DA FARE]\n+ ALTRO\n- A TEMPO PERSO\n  Chiamare papà  alle 15",
            buildSharedActionsText(actions),
        )
    }

    @Test
    fun `ordina ignorando maiuscole ma preservando accenti e spareggio sul testo`() {
        val actions = listOf("è pronto", "zaino", "aprire", "Aprire", "e pronto")
            .map { ActionItem(text = it) }
        val expected = "[DA FARE]\n+ ALTRO\n- A TEMPO PERSO\n  Aprire\n  aprire\n  e pronto\n  zaino\n  è pronto"

        assertEquals(expected, buildSharedActionsText(actions))
        assertEquals(expected, buildSharedActionsText(actions.reversed()))
    }

    @Test
    fun `mantiene lo stesso testo in ambiti e urgenze differenti`() {
        val actions = listOf(
            ActionItem(text = "Telefonare", scope = ActionScope.LAVORO),
            ActionItem(text = "Telefonare", scope = ActionScope.CASA),
            ActionItem(text = "Telefonare", scope = ActionScope.CASA, urgency = ActionUrgency.URGENTE),
        )

        assertEquals(
            "[DA FARE]\n+ CASA\n- URGENTE\n  Telefonare\n- A TEMPO PERSO\n  Telefonare\n+ LAVORO\n- A TEMPO PERSO\n  Telefonare",
            buildSharedActionsText(actions),
        )
    }

    @Test
    fun `indenta anche il testo che inizia con marcatori del formato`() {
        val actions = listOf("[DA FARE]", "- URGENTE", "+ CASA")
            .map { ActionItem(text = it) }

        assertEquals(
            "[DA FARE]\n+ ALTRO\n- A TEMPO PERSO\n  + CASA\n  - URGENTE\n  [DA FARE]",
            buildSharedActionsText(actions),
        )
    }
}
