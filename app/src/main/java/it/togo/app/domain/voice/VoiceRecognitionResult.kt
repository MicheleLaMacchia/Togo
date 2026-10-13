package it.togo.app.domain.voice

/**
 * Risultato del riconoscimento vocale.
 *
 * Sealed interface con 4 varianti:
 * - Success: testo riconosciuto
 * - NoSpeech: nessun parlato rilevato
 * - Error: errore durante riconoscimento
 * - Cancelled: utente ha annullato
 */
sealed interface VoiceRecognitionResult {

    /** Testo riconosciuto con successo */
    data class Success(val text: String) : VoiceRecognitionResult

    /** Nessun parlato rilevato (timeout o silenzio) */
    data object NoSpeech : VoiceRecognitionResult

    /** Errore durante il riconoscimento */
    data class Error(
        val code: ErrorCode,
        val message: String
    ) : VoiceRecognitionResult

    /** Utente ha annullato il riconoscimento */
    data object Cancelled : VoiceRecognitionResult

    /** Verifica se il risultato contiene testo valido */
    val isValidText: Boolean
        get() = this is Success && text.isNotBlank()

    /** Estrae il testo riconosciuto, o null se non presente */
    val recognizedText: String?
        get() = (this as? Success)?.text
}

/**
 * Codici di errore per il riconoscimento vocale.
 */
enum class ErrorCode {
    /** Permesso RECORD_AUDIO non concesso */
    MISSING_PERMISSION,

    /** RecognizerIntent non disponibile sul device */
    NOT_AVAILABLE,

    /** Errore di rete durante riconoscimento remoto */
    NETWORK_ERROR,

    /** Errore del riconoscitore vocale */
    RECOGNIZER_ERROR,

    /** Audio non valido o troppo breve */
    AUDIO_ERROR,

    /** Client di riconoscimento occupato */
    CLIENT_BUSY,

    /** Errore non classificato */
    UNKNOWN
}