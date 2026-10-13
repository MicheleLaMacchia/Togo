package it.togo.app.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel MVI per il bottom sheet vocale.
 *
 * Osserva il [VoiceRecognitionGateway] e traduce i suoi stati in [VoiceBottomSheetUiState].
 * Espone azioni per conferma, modifica, riprova e annullamento.
 */
class VoiceBottomSheetViewModel(
    private val gateway: VoiceRecognitionGateway
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceBottomSheetUiState>(VoiceBottomSheetUiState.Hidden)
    val uiState: StateFlow<VoiceBottomSheetUiState> = _uiState.asStateFlow()

    /** Callback invocato quando l'utente conferma il testo riconosciuto */
    var onTextConfirmed: ((String) -> Unit)? = null

    init {
        viewModelScope.launch {
            gateway.state.collect { state ->
                _uiState.value = when (state) {
                    VoiceRecognitionState.Idle -> VoiceBottomSheetUiState.Hidden
                    VoiceRecognitionState.Listening -> VoiceBottomSheetUiState.Listening
                    VoiceRecognitionState.Processing -> VoiceBottomSheetUiState.Processing
                    is VoiceRecognitionState.Result -> VoiceBottomSheetUiState.Ready(
                        recognizedText = state.text
                    )
                    is VoiceRecognitionState.Error -> VoiceBottomSheetUiState.Error(
                        code = state.code.name,
                        message = state.message
                    )
                }
            }
        }
    }

    /** Avvia il riconoscimento vocale */
    fun startListening() {
        viewModelScope.launch {
            gateway.startListening()
        }
    }

    /** Modifica il testo riconosciuto */
    fun editText(newText: String) {
        val current = _uiState.value
        if (current is VoiceBottomSheetUiState.Ready) {
            _uiState.value = current.copy(editedText = newText, isEditing = true)
        }
    }

    /** Conferma il testo e chiude il bottom sheet */
    fun confirm() {
        val current = _uiState.value
        if (current is VoiceBottomSheetUiState.Ready) {
            onTextConfirmed?.invoke(current.editedText)
            gateway.reset()
            _uiState.value = VoiceBottomSheetUiState.Hidden
        }
    }

    /** Riprova il riconoscimento */
    fun retry() {
        // Imposta subito Listening per evitare flicker (reset emette Idle sincrono)
        _uiState.value = VoiceBottomSheetUiState.Listening
        viewModelScope.launch {
            gateway.reset()
            gateway.startListening()
        }
    }

    /** Annulla e chiude il bottom sheet */
    fun cancel() {
        gateway.reset()
        _uiState.value = VoiceBottomSheetUiState.Hidden
    }
}