package it.togo.app.domain.voice

import org.junit.Assert.*
import org.junit.Test

/**
 * Test JVM per VoiceErrorRecovery.
 * Verifica mappa ErrorCode → VoiceRecoveryAction e messaggi utente.
 */
class VoiceErrorRecoveryTest {

    @Test
    fun `MISSING_PERMISSION ritorna OpenSettings`() {
        val action = VoiceErrorRecovery.getRecoveryAction(ErrorCode.MISSING_PERMISSION)

        assertEquals(VoiceRecoveryAction.OpenSettings, action)
    }

    @Test
    fun `NOT_AVAILABLE ritorna UseManualInput`() {
        val action = VoiceErrorRecovery.getRecoveryAction(ErrorCode.NOT_AVAILABLE)

        assertEquals(VoiceRecoveryAction.UseManualInput, action)
    }

    @Test
    fun `NETWORK_ERROR tentativo 1 ritorna Retry`() {
        val action = VoiceErrorRecovery.getRecoveryAction(ErrorCode.NETWORK_ERROR, attempt = 1, maxAttempts = 3)

        assertEquals(VoiceRecoveryAction.Retry, action)
    }

    @Test
    fun `NETWORK_ERROR tentativo 3 ritorna UseManualInput`() {
        val action = VoiceErrorRecovery.getRecoveryAction(ErrorCode.NETWORK_ERROR, attempt = 3, maxAttempts = 3)

        assertEquals(VoiceRecoveryAction.UseManualInput, action)
    }

    @Test
    fun `AUDIO_ERROR tentativo 1 ritorna Retry`() {
        val action = VoiceErrorRecovery.getRecoveryAction(ErrorCode.AUDIO_ERROR, attempt = 1, maxAttempts = 3)

        assertEquals(VoiceRecoveryAction.Retry, action)
    }

    @Test
    fun `RECOGNIZER_ERROR tentativo 2 ritorna Retry`() {
        val action = VoiceErrorRecovery.getRecoveryAction(ErrorCode.RECOGNIZER_ERROR, attempt = 2, maxAttempts = 3)

        assertEquals(VoiceRecoveryAction.Retry, action)
    }

    @Test
    fun `MISSING_PERMISSION non permette retry`() {
        assertFalse(VoiceErrorRecovery.canRetry(ErrorCode.MISSING_PERMISSION))
    }

    @Test
    fun `NOT_AVAILABLE non permette retry`() {
        assertFalse(VoiceErrorRecovery.canRetry(ErrorCode.NOT_AVAILABLE))
    }

    @Test
    fun `NETWORK_ERROR permette retry`() {
        assertTrue(VoiceErrorRecovery.canRetry(ErrorCode.NETWORK_ERROR))
    }

    @Test
    fun `messaggio MISSING_PERMISSION user-friendly`() {
        val msg = VoiceErrorRecovery.getUserMessage(ErrorCode.MISSING_PERMISSION)

        assertTrue(msg.contains("Permesso microfono"))
        assertTrue(msg.contains("Impostazioni"))
    }

    @Test
    fun `messaggio NOT_AVAILABLE user-friendly`() {
        val msg = VoiceErrorRecovery.getUserMessage(ErrorCode.NOT_AVAILABLE)

        assertTrue(msg.contains("non disponibile"))
    }

    @Test
    fun `messaggio NETWORK_ERROR user-friendly`() {
        val msg = VoiceErrorRecovery.getUserMessage(ErrorCode.NETWORK_ERROR)

        assertTrue(msg.contains("rete"))
    }

    @Test
    fun `messaggio AUDIO_ERROR user-friendly`() {
        val msg = VoiceErrorRecovery.getUserMessage(ErrorCode.AUDIO_ERROR)

        assertTrue(msg.contains("Audio"))
    }

    @Test(timeout = 100)
    fun `operazioni recovery completate sotto 100ms`() {
        VoiceErrorRecovery.getRecoveryAction(ErrorCode.NETWORK_ERROR)
        VoiceErrorRecovery.canRetry(ErrorCode.MISSING_PERMISSION)
        VoiceErrorRecovery.getUserMessage(ErrorCode.AUDIO_ERROR)
    }
}