package it.togo.app.domain.voice

/**
 * Azione di recupero da un errore vocale.
 */
sealed interface VoiceRecoveryAction {

    /** Ritenta il riconoscimento vocale */
    data object Retry : VoiceRecoveryAction

    /** Torna all'input manuale */
    data object UseManualInput : VoiceRecoveryAction

    /** Guida l'utente alle impostazioni di sistema */
    data object OpenSettings : VoiceRecoveryAction

    /** Nessuna azione disponibile */
    data object NoAction : VoiceRecoveryAction
}

/**
 * Mappa [ErrorCode] a [VoiceRecoveryAction] appropriata.
 */
object VoiceErrorRecovery {

    /**
     * Determina l'azione di recupero per un errore specifico.
     *
     * @param code errore riscontrato
     * @param attempt numero del tentativo corrente (1-based)
     * @param maxAttempts numero massimo di tentativi
     * @return azione di recupero consigliata
     */
    fun getRecoveryAction(
        code: ErrorCode,
        attempt: Int = 1,
        maxAttempts: Int = 3
    ): VoiceRecoveryAction = when (code) {
        ErrorCode.MISSING_PERMISSION -> VoiceRecoveryAction.OpenSettings
        ErrorCode.NOT_AVAILABLE -> VoiceRecoveryAction.UseManualInput
        ErrorCode.NETWORK_ERROR,
        ErrorCode.RECOGNIZER_ERROR,
        ErrorCode.AUDIO_ERROR,
        ErrorCode.CLIENT_BUSY -> if (attempt < maxAttempts) {
            VoiceRecoveryAction.Retry
        } else {
            VoiceRecoveryAction.UseManualInput
        }
        ErrorCode.UNKNOWN -> VoiceRecoveryAction.Retry
    }

    /**
     * Verifica se l'errore permette retry.
     */
    fun canRetry(code: ErrorCode): Boolean = when (code) {
        ErrorCode.MISSING_PERMISSION -> false
        ErrorCode.NOT_AVAILABLE -> false
        else -> true
    }

    /**
     * Messaggio user-friendly per l'errore.
     */
    fun getUserMessage(code: ErrorCode): String = when (code) {
        ErrorCode.MISSING_PERMISSION -> "Permesso microfono non concesso. Vai a Impostazioni per attivarlo."
        ErrorCode.NOT_AVAILABLE -> "Riconoscimento vocale non disponibile su questo dispositivo."
        ErrorCode.NETWORK_ERROR -> "Errore di rete. Riprova."
        ErrorCode.RECOGNIZER_ERROR -> "Errore del riconoscitore. Riprova."
        ErrorCode.AUDIO_ERROR -> "Audio non valido o troppo breve. Riprova più forte."
        ErrorCode.CLIENT_BUSY -> "Riconoscitore occupato. Riprova tra poco."
        ErrorCode.UNKNOWN -> "Errore imprevisto. Riprova."
    }
}