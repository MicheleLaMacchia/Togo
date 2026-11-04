package it.togo.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.repository.UserAttributeRepository
import it.togo.app.domain.usecase.ImportExecutionResult
import it.togo.app.domain.usecase.ImportPlan
import it.togo.app.domain.usecase.ImportPreviewResult
import it.togo.app.domain.usecase.ImportShoppingListUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Stato della schermata "Importa lista". */
data class ImportListUiState(
    val text: String = "",
    val isAnalyzing: Boolean = false,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val confirmation: ImportPlan? = null,
    val successMessage: String? = null,
)

/**
 * ViewModel dell'importazione di una lista esportata da TOGO.
 *
 * Il testo viene prima analizzato: se il formato non è valido viene mostrato
 * l'errore, altrimenti la UI apre la conferma con il numero di prodotti. Solo
 * dopo l'accettazione esplicita vengono scritte categorie, prodotti e voci.
 */
class ImportListViewModel(
    catalogRepository: CatalogRepository,
    shoppingListRepository: ShoppingListRepository,
    userAttributeRepository: UserAttributeRepository,
) : ViewModel() {

    private val importUseCase = ImportShoppingListUseCase(
        catalogRepository = catalogRepository,
        shoppingListRepository = shoppingListRepository,
        userAttributeRepository = userAttributeRepository,
    )

    private val _uiState = MutableStateFlow(ImportListUiState())
    val uiState = _uiState.asStateFlow()

    fun onTextChange(value: String) {
        _uiState.update {
            it.copy(
                text = value,
                errorMessage = null,
            )
        }
    }

    fun analyze() {
        val state = _uiState.value
        if (state.isAnalyzing || state.isImporting) return

        _uiState.update {
            it.copy(
                isAnalyzing = true,
                errorMessage = null,
                confirmation = null,
            )
        }

        viewModelScope.launch {
            try {
                when (val result = importUseCase.preview(_uiState.value.text)) {
                    is ImportPreviewResult.Ready -> _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            confirmation = result.plan,
                        )
                    }

                    is ImportPreviewResult.Error -> _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            errorMessage = result.message,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = "Errore durante la lettura del testo: " +
                            (e.message ?: "errore sconosciuto"),
                    )
                }
            }
        }
    }

    fun confirmImport() {
        val plan = _uiState.value.confirmation ?: return
        if (_uiState.value.isImporting) return

        _uiState.update { it.copy(isImporting = true) }

        viewModelScope.launch {
            try {
                when (val result = importUseCase.execute(plan)) {
                    is ImportExecutionResult.Success -> _uiState.update {
                        it.copy(
                            isImporting = false,
                            confirmation = null,
                            text = "",
                            successMessage = buildSuccessMessage(result),
                        )
                    }

                    is ImportExecutionResult.Error -> _uiState.update {
                        it.copy(
                            isImporting = false,
                            confirmation = null,
                            errorMessage = result.message,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        confirmation = null,
                        errorMessage = "Importazione non riuscita: " +
                            (e.message ?: "errore sconosciuto"),
                    )
                }
            }
        }
    }

    fun cancelImport() {
        if (_uiState.value.isImporting) return
        _uiState.update { it.copy(confirmation = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    private fun buildSuccessMessage(result: ImportExecutionResult.Success): String =
        buildString {
            if (result.imported == 1) {
                append("Importato 1 prodotto")
            } else {
                append("Importati ")
                append(result.imported)
                append(" prodotti")
            }
            if (result.updatedExisting > 0) {
                append(", ")
                if (result.updatedExisting == 1) {
                    append("1 voce già presente aggiornata")
                } else {
                    append(result.updatedExisting)
                    append(" voci già presenti aggiornate")
                }
            }
            append(".")
        }
}
