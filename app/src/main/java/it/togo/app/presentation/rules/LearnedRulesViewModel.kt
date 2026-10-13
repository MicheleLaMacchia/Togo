package it.togo.app.presentation.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.usecase.LearnedRuleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * ViewModel MVI per la schermata Gestione Regole Apprese.
 *
 * Espone regole (filtrate da orfane), gestisce ricerca, CRUD completo.
 */
class LearnedRulesViewModel(
    private val learnedRuleManager: LearnedRuleManager,
    private val catalogRepository: CatalogRepository,
    private val learnedRulesRepository: LearnedRulesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isDeleteDialogOpen = MutableStateFlow(false)
    private val _ruleToDelete = MutableStateFlow<LearnedRule?>(null)
    private val _editingRule = MutableStateFlow<LearnedRule?>(null)

    private val _uiState = MutableStateFlow(LearnedRulesUiState())
    val uiState: StateFlow<LearnedRulesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                learnedRulesRepository.getAll(),
                _searchQuery,
                _snackbarMessage,
                _isDeleteDialogOpen,
                _ruleToDelete,
                _editingRule
            ) { rules, query, snackbar, dialogOpen, deleteRule, editingRule ->
                // Filtra regole orfane (prodotto non più nel catalogo)
                val validRules = rules.filter { rule ->
                    catalogRepository.getById(rule.productId) != null
                }
                // Ricerca combinata: espressione OR nome prodotto
                val filtered = if (query.isBlank()) {
                    validRules
                } else {
                    validRules.filter { rule ->
                        rule.userExpression.contains(query, ignoreCase = true) ||
                            (catalogRepository.getById(rule.productId)?.name?.contains(query, ignoreCase = true) == true)
                    }
                }
                LearnedRulesUiState(
                    rules = validRules,
                    filteredRules = filtered,
                    searchQuery = query,
                    isEmpty = filtered.isEmpty(),
                    isDeleteDialogOpen = dialogOpen,
                    ruleToDelete = deleteRule,
                    editingRule = editingRule,
                    snackbarMessage = snackbar
                )
            }.distinctUntilChanged().collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deactivateRule(rule: LearnedRule) {
        viewModelScope.launch {
            try {
                learnedRuleManager.deactivate(rule.id)
                _snackbarMessage.value = "Regola disattivata"
            } catch (e: Exception) {
                _snackbarMessage.value = "Errore durante la disattivazione"
            }
        }
    }

    fun reactivateRule(rule: LearnedRule) {
        viewModelScope.launch {
            try {
                learnedRuleManager.reactivate(rule.id)
                _snackbarMessage.value = "Regola riattivata"
            } catch (e: Exception) {
                _snackbarMessage.value = "Errore durante la riattivazione"
            }
        }
    }

    /**
     * Avvia la modifica della categoria di una regola.
     */
    fun startEditCategory(rule: LearnedRule) {
        _editingRule.value = rule
    }

    /**
     * Conferma la modifica della categoria con nuovo level3Id.
     */
    fun updateCategory(newLevel3Id: Long) {
        viewModelScope.launch {
            try {
                _editingRule.value?.let { rule ->
                    learnedRuleManager.update(rule.id, level3Id = newLevel3Id)
                    _snackbarMessage.value = "Categoria aggiornata"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Errore durante l'aggiornamento"
            }
            _editingRule.value = null
        }
    }

    fun cancelEdit() {
        _editingRule.value = null
    }

    fun requestDelete(rule: LearnedRule) {
        _ruleToDelete.value = rule
        _isDeleteDialogOpen.value = true
    }

    fun confirmDelete() {
        viewModelScope.launch {
            try {
                _ruleToDelete.value?.let { rule ->
                    learnedRuleManager.delete(rule.id)
                    _snackbarMessage.value = "Regola eliminata"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Errore durante l'eliminazione"
            }
            _isDeleteDialogOpen.value = false
            _ruleToDelete.value = null
        }
    }

    fun cancelDelete() {
        _isDeleteDialogOpen.value = false
        _ruleToDelete.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}