package it.togo.app.presentation.voice

import it.togo.app.domain.parser.ParsedCommand

/**
 * Stato UI per il bottom sheet di conferma vocale.
 */
sealed interface VoiceConfirmationUiState {

    /** Bottom sheet nascosto */
    data object Hidden : VoiceConfirmationUiState

    /** Mostra comando interpretato, in attesa di conferma */
    data class Ready(
        val parsedCommand: ParsedCommand,
        val isEditing: Boolean = false,
        /** Testo grezzo riconosciuto, usato come base per la modifica. */
        val sourceText: String = "",
    ) : VoiceConfirmationUiState {
        /** Comando da visualizzare (già re-parseato se editText è stato chiamato) */
        val displayCommand: ParsedCommand get() = parsedCommand

        val canConfirm: Boolean
            get() = displayCommand.isValid
    }

    /** Verifica se il bottom sheet è visibile */
    val isVisible: Boolean get() = this !is Hidden
}