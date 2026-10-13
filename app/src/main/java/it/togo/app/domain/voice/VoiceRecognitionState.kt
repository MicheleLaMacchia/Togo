package it.togo.app.domain.voice

/**
 * Stato del riconoscimento vocale osservabile da UI.
 *
 * Stati:
 * - Idle: pronto per avviare
 * - Listening: microfono attivo, in ascolto
 * - Processing: riconoscimento completato, testo in elaborazione (transitorio)
 * - Result: testo riconosciuto con successo
 * - Error: errore durante riconoscimento
 */
sealed interface VoiceRecognitionState {

    /** Pronto per avviare il riconoscimento */
    data object Idle : VoiceRecognitionState

    /** Microfono attivo, in ascolto */
    data object Listening : VoiceRecognitionState

    /** Elaborazione del testo riconosciuto (transitorio) */
    data object Processing : VoiceRecognitionState

    /** Testo riconosciuto con successo */
    data class Result(val text: String) : VoiceRecognitionState

    /** Errore durante il riconoscimento */
    data class Error(val code: ErrorCode, val message: String) : VoiceRecognitionState

    /** Verifica se è in ascolto attivo */
    val isListening: Boolean
        get() = this is Listening

    /** Verifica se c'è un errore */
    val isError: Boolean
        get() = this is Error

    /** Verifica se c'è un risultato valido */
    val isResult: Boolean
        get() = this is Result

    /** Estrae il testo risultato, o null */
    val resultText: String?
        get() = (this as? Result)?.text
}