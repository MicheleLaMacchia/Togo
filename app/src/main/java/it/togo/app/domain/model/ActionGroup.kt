package it.togo.app.domain.model

import java.util.Locale

/**
 * Gruppo di azioni dello stesso ambito, pronto per la pagina "Da fare".
 *
 * L'urgenza non è un header: è il primo criterio di ordinamento dentro
 * l'ambito (Urgente, A tempo perso, Eccezionale), seguito dall'alfabetico.
 */
data class ActionGroup(
    val scope: ActionScope,
    val actions: List<ActionItem>,
)

/**
 * Raggruppa le azioni attive per ambito nell'ordine Casa, Pratiche, Salute,
 * Lavoro, Altro, poi per urgenza e infine alfabeticamente.
 *
 * Il confronto alfabetico ignora maiuscole/minuscole ma non gli accenti: è
 * solo un criterio di visualizzazione, non la deduplica delle azioni.
 *
 * Questa logica vive nel domain layer (AD-1): è puro Kotlin, senza dipendenze
 * da Android, ed è indipendente dalla persistenza Room. Il repository espone
 * la lista così come è persistita; l'ordinamento per ambito → urgenza →
 * alfabetico è responsabilità del domain.
 */
fun groupActionsByScope(actions: List<ActionItem>): List<ActionGroup> {
    val scopeRank = ActionScope.ordered
        .withIndex()
        .associate { (index, scope) -> scope to index }
    val urgencyRank = ActionUrgency.ordered
        .withIndex()
        .associate { (index, urgency) -> urgency to index }

    return actions
        .groupBy { it.scope }
        .entries
        .sortedBy { entry -> scopeRank[entry.key] ?: Int.MAX_VALUE }
        .map { entry ->
            ActionGroup(
                scope = entry.key,
                actions = entry.value.sortedWith(
                    compareBy<ActionItem> { action ->
                        urgencyRank[action.urgency] ?: Int.MAX_VALUE
                    }
                        .thenBy { action -> action.text.lowercase(Locale.ROOT) }
                        .thenBy { action -> action.text },
                ),
            )
        }
}
