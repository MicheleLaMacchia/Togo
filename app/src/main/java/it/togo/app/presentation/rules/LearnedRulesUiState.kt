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
    val snackbarMessage: String? = null
) {
    val hasActiveSearch: Boolean get() = searchQuery.isNotBlank()
}