package it.togo.app.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.domain.usecase.ParseVoiceCommandUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel MVI per il bottom sheet di conferma vocale.
 *
 * Flusso:
 * 1. `showForRecognition(rawText)` → parsing → stato `Ready`
 * 2. `confirm()` → callback `onCommandConfirmed(ParsedCommand)` → chiudi
 * 3. `cancel()` → chiudi, nessuna azione
 * 4. `editText(newText)` → re-parsing → stato `Ready` aggiornato
 *
 * Privacy: il comando interpretato resta solo nello stato in memoria. Non viene
 * persistito, loggato o inviato a endpoint remoti. [cancel] e [reset] lo
 * eliminano dalla UI; [onCleared] rilascia anche il callback di conferma.
 */
class VoiceConfirmationViewModel(
    private val parseUseCase: ParseVoiceCommandUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceConfirmationUiState>(VoiceConfirmationUiState.Hidden)
    val uiState: StateFlow<VoiceConfirmationUiState> = _uiState.asStateFlow()

    /** Callback invocato quando l'utente conferma il comando */
    var onCommandConfirmed: ((ParsedCommand) -> Unit)? = null

    /**
     * Mostra il bottom sheet per un testo riconosciuto.
     * Esegue il parsing e imposta lo stato Ready.
     */
    fun showForRecognition(rawText: String) {
        val parsed = parseUseCase(rawText)
        _uiState.value = VoiceConfirmationUiState.Ready(parsedCommand = parsed)
    }

    /**
     * Conferma il comando e invoca il callback.
     */
    fun confirm() {
        val current = _uiState.value
        if (current is VoiceConfirmationUiState.Ready && current.canConfirm) {
            onCommandConfirmed?.invoke(current.displayCommand)
            _uiState.value = VoiceConfirmationUiState.Hidden
        }
    }

    /**
     * Annulla e chiude il bottom sheet.
     */
    fun cancel() {
        _uiState.value = VoiceConfirmationUiState.Hidden
    }

    /**
     * Modifica il testo riconosciuto e riesegue il parsing.
     */
    fun editText(newText: String) {
        val reparsed = parseUseCase(newText)
        _uiState.value = VoiceConfirmationUiState.Ready(parsedCommand = reparsed)
    }

    /**
     * Alterna modalità modifica (mostra/nascondi input inline).
     */
    fun toggleEdit() {
        val current = _uiState.value
        if (current is VoiceConfirmationUiState.Ready) {
            _uiState.value = current.copy(isEditing = !current.isEditing)
        }
    }

    /**
     * Resetta lo stato a Hidden.
     */
    fun reset() {
        _uiState.value = VoiceConfirmationUiState.Hidden
    }

    /**
     * Alla distruzione del ViewModel il comando interpretato viene eliminato e
     * il callback che trattiene la schermata viene rilasciato.
     */
    override fun onCleared() {
        _uiState.value = VoiceConfirmationUiState.Hidden
        onCommandConfirmed = null
        super.onCleared()
    }
}