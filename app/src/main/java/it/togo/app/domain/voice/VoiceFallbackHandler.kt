package it.togo.app.domain.voice

/**
 * Gestore del fallback per il riconoscimento vocale.
 *
 * Traccia i fallimenti consecutivi e decide quando attivare il fallback
 * a input manuale (dopo 3 tentativi falliti).
 */
class VoiceFallbackHandler(
    private val policy: VoiceResiliencePolicy = VoiceResiliencePolicy.DEFAULT
) {
    private var consecutiveFailures = 0
    private var lastErrorCode: ErrorCode? = null

    /**
     * Registra un fallimento e determina se attivare fallback.
     *
     * @param code errore riscontrato
     * @return true se è stato raggiunto il limite e bisogna fare fallback
     */
    fun recordFailure(code: ErrorCode): Boolean {
        consecutiveFailures++
        lastErrorCode = code
        return consecutiveFailures >= policy.maxAttempts || !VoiceErrorRecovery.canRetry(code)
    }

    /**
     * Registra un successo e resetta il contatore.
     */
    fun recordSuccess() {
        consecutiveFailures = 0
        lastErrorCode = null
    }

    /**
     * Verifica se il fallback è attivo.
     */
    fun isFallbackActive(): Boolean = consecutiveFailures >= policy.maxAttempts

    /**
     * Numero di fallimenti consecutivi.
     */
    fun getConsecutiveFailures(): Int = consecutiveFailures

    /**
     * Ultimo errore registrato.
     */
    fun getLastErrorCode(): ErrorCode? = lastErrorCode

    /**
     * Reset completo dello stato.
     */
    fun reset() {
        consecutiveFailures = 0
        lastErrorCode = null
    }

    /**
     * Delay di backoff per il prossimo tentativo.
     */
    fun getNextRetryDelayMs(): Long {
        return policy.getDelayMs(consecutiveFailures)
    }
}