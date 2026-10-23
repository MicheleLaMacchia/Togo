package it.togo.app.presentation.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.LearnedRule
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.usecase.LearnedRuleManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
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
    private val learnedRulesRepository: LearnedRulesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _isDeleteDialogOpen = MutableStateFlow(false)
    private val _ruleToDelete = MutableStateFlow<LearnedRule?>(null)
    private val _editingRule = MutableStateFlow<LearnedRule?>(null)

    private val _uiState = MutableStateFlow(LearnedRulesUiState())
    val uiState: StateFlow<LearnedRulesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            // Se la tassonomia non è disponibile la schermata resta usabile:
            // mostra i fallback con gli ID invece di non emettere alcuno stato.
            val taxonomyFlow = try {
                catalogRepository.getTaxonomy()
                    .catch { emit(emptyList<TaxonomyLevel>()) }
            } catch (_: Exception) {
                flowOf(emptyList<TaxonomyLevel>())
            }
            val content = combine(
                learnedRulesRepository.getAll(),
                taxonomyFlow,
                _searchQuery,
                _snackbarMessage,
            ) { rules, taxonomy, query, snackbar ->
                Content(rules, taxonomy, query, snackbar)
            }

            val dialogState = combine(
                _isDeleteDialogOpen,
                _ruleToDelete,
                _editingRule,
            ) { dialogOpen, deleteRule, editingRule -> Triple(dialogOpen, deleteRule, editingRule) }

            combine(content, dialogState) { current, (dialogOpen, deleteRule, editingRule) ->
                val level1ById = current.taxonomy
                    .filterIsInstance<TaxonomyLevel.Level1>()
                    .associateBy { it.id }
                val level2ById = current.taxonomy
                    .filterIsInstance<TaxonomyLevel.Level2>()
                    .associateBy { it.id }
                val level3ById = current.taxonomy
                    .filterIsInstance<TaxonomyLevel.Level3>()
                    .associateBy { it.id }

                val categoryOptions = level3ById.values
                    .filterNot { it.isUserDefined }
                    .sortedWith(compareBy({ it.sortOrder }, { it.name }))
                    .map { level3 ->
                        val level2 = level2ById[level3.level2Id]
                        val level1 = level2?.let { level1ById[it.level1Id] }
                        CategoryOption(
                            id = level3.id,
                            label = listOfNotNull(level1?.name, level2?.name, level3.name)
                                .joinToString(" > "),
                        )
                    }

                // Filtra regole orfane (prodotto non più nel catalogo) e
                // risolve i nomi da mostrare: prima erano visibili solo gli ID.
                val productNames = mutableMapOf<Long, String>()
                val categoryNames = mutableMapOf<String, String>()
                val categoryPaths = mutableMapOf<String, String>()
                val products = mutableMapOf<Long, CanonicalProduct?>()
                val validRules = mutableListOf<LearnedRule>()

                for (rule in current.rules) {
                    val product = if (products.containsKey(rule.productId)) {
                        products[rule.productId]
                    } else {
                        catalogRepository.getById(rule.productId).also {
                            products[rule.productId] = it
                        }
                    } ?: continue

                    validRules += rule
                    productNames[rule.productId] = product.name

                    val level3 = level3ById[rule.level3Id]
                    val level2 = level3?.let { level2ById[it.level2Id] }
                    val level1 = level2?.let { level1ById[it.level1Id] }
                    // Se la tassonomia non è disponibile ma la regola punta alla
                    // categoria di default del prodotto, i nomi sul prodotto sono
                    // un fallback affidabile. Per categorie corrette diverse non
                    // inventiamo un nome: mostriamo l'ID.
                    val useProductNames = level3 == null && rule.level3Id == product.level3Id
                    categoryNames[rule.id] = level3?.name
                        ?: product.level3Name.takeIf { useProductNames }
                        ?: "#${rule.level3Id}"
                    categoryPaths[rule.id] = if (useProductNames) {
                        listOfNotNull(
                            product.level1Name,
                            product.level2Name,
                            product.level3Name,
                        ).joinToString(" > ").ifBlank { categoryNames.getValue(rule.id) }
                    } else {
                        listOfNotNull(level1?.name, level2?.name, level3?.name)
                            .joinToString(" > ")
                            .ifBlank { categoryNames.getValue(rule.id) }
                    }
                }

                // Ricerca combinata: espressione, nome prodotto o categoria.
                val query = current.query.trim()
                val filtered = if (query.isEmpty()) {
                    validRules
                } else {
                    validRules.filter { rule ->
                        val productName = productNames[rule.productId].orEmpty()
                        val categoryName = categoryNames[rule.id].orEmpty()
                        val categoryPath = categoryPaths[rule.id].orEmpty()
                        rule.userExpression.contains(query, ignoreCase = true) ||
                            productName.contains(query, ignoreCase = true) ||
                            categoryName.contains(query, ignoreCase = true) ||
                            categoryPath.contains(query, ignoreCase = true)
                    }
                }

                LearnedRulesUiState(
                    rules = validRules,
                    filteredRules = filtered,
                    searchQuery = current.query,
                    isEmpty = filtered.isEmpty(),
                    isDeleteDialogOpen = dialogOpen,
                    ruleToDelete = deleteRule,
                    editingRule = editingRule,
                    snackbarMessage = current.snackbar,
                    productNames = productNames,
                    categoryNames = categoryNames,
                    categoryPaths = categoryPaths,
                    categoryOptions = categoryOptions,
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

    private data class Content(
        val rules: List<LearnedRule>,
        val taxonomy: List<TaxonomyLevel>,
        val query: String,
        val snackbar: String?,
    )
}
