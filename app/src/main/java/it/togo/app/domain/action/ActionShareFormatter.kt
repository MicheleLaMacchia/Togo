package it.togo.app.domain.action

import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.groupActionsByScope
import java.util.Locale

/**
 * Testo condivisibile delle azioni attive (Story 11.2).
 * Riusa l'ordine della pagina Da fare e preserva il testo senza aggiungere quantità.
 */
fun buildSharedActionsText(actions: List<ActionItem>): String {
    val groups = groupActionsByScope(actions.filterNot { it.isChecked })
    if (groups.isEmpty()) return ""

    return buildString {
        appendLine("[DA FARE]")
        groups.forEach { group ->
            appendLine("+ ${group.scope.label.uppercase(Locale.ROOT)}")
            // groupBy preserva l'ordine delle urgenze già applicato dal dominio.
            group.actions.groupBy { it.urgency }.forEach { (urgency, items) ->
                appendLine("- ${urgency.label.uppercase(Locale.ROOT)}")
                items.forEach { action -> appendLine("  ${action.text}") }
            }
        }
    }.removeSuffix("\n")
}
