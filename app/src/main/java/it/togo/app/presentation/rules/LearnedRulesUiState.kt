package it.togo.app.presentation.rules

import it.togo.app.domain.model.LearnedRule

/**
 * Stato UI per la schermata Gestione Regole Apprese.
 */
data class LearnedRulesUiState(
    val rules: List<LearnedRule> = emptyList(),
    val filteredRules: List<LearnedRule> = emptyList(),
    val searchQuery: String = "",
    val isEmpty: Boolean = true,
    val isDeleteDialogOpen: Boolean = false,
    val ruleToDelete: LearnedRule? = null,
    val editingRule: LearnedRule? = null,
    val snackbarMessage: String? = null,
    /** Nome del prodotto canonico per `productId`. */
    val productNames: Map<Long, String> = emptyMap(),
    /** Nome del Livello 3 della regola, per `ruleId`. */
    val categoryNames: Map<String, String> = emptyMap(),
    /** Percorso L1 > L2 > L3 della regola, per `ruleId`. */
    val categoryPaths: Map<String, String> = emptyMap(),
    /** Categorie selezionabili nel dialog di modifica. */
    val categoryOptions: List<CategoryOption> = emptyList(),
) {
    val hasActiveSearch: Boolean get() = searchQuery.isNotBlank()
    val hasRules: Boolean get() = rules.isNotEmpty()
}

/** Voce selezionabile per correggere la categoria di una regola. */
data class CategoryOption(
    val id: Long,
    val label: String,
)