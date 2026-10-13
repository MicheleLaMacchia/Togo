package it.togo.app.domain.voice

/**
 * Politica di retry per il riconoscimento vocale.
 *
 * Backoff esponenziale:
 * - Tentativo 1: 500ms
 * - Tentativo 2: 1000ms
 * - Tentativo 3: 2000ms
 *
 * Max 3 tentativi prima di attivare fallback.
 */
data class VoiceResiliencePolicy(
    val maxAttempts: Int = 3,
    val baseDelayMs: Long = 500L,
    val backoffMultiplier: Double = 2.0
) {
    /**
     * Calcola il delay per il tentativo specificato (1-based).
     *
     * @param attempt numero del tentativo (1, 2, 3)
     * @return delay in millisecondi
     */
    fun getDelayMs(attempt: Int): Long {
        if (attempt <= 0) return 0L
        val multiplier = Math.pow(backoffMultiplier, (attempt - 1).toDouble())
        return (baseDelayMs * multiplier).toLong()
    }

    /**
     * Verifica se è possibile ritentare.
     *
     * @param currentAttempt tentativo corrente (1-based)
     * @return true se currentAttempt < maxAttempts
     */
    fun canRetry(currentAttempt: Int): Boolean = currentAttempt < maxAttempts

    companion object {
        /** Default policy: max 3 tentativi, 500ms base, x2 backoff */
        val DEFAULT = VoiceResiliencePolicy()
    }
}