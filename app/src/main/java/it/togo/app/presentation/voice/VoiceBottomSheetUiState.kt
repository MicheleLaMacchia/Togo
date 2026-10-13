package it.togo.app.presentation.voice

/**
 * Stato UI del bottom sheet vocale.
 */
sealed interface VoiceBottomSheetUiState {

    /** Bottom sheet nascosto (idle) */
    data object Hidden : VoiceBottomSheetUiState

    /** In ascolto: microfono attivo */
    data object Listening : VoiceBottomSheetUiState

    /** Elaborazione testo riconosciuto */
    data object Processing : VoiceBottomSheetUiState

    /** Testo riconosciuto, in attesa di conferma */
    data class Ready(
        val recognizedText: String,
        val editedText: String = recognizedText,
        val isEditing: Boolean = false
    ) : VoiceBottomSheetUiState {
        val canConfirm: Boolean get() = editedText.isNotBlank()
    }

    /** Errore durante riconoscimento */
    data class Error(
        val code: String,
        val message: String
    ) : VoiceBottomSheetUiState

    /** Verifica se il bottom sheet è visibile */
    val isVisible: Boolean get() = this !is Hidden
}