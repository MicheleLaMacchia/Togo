package it.togo.app.domain.voice

import org.junit.Assert.*
import org.junit.Test

/**
 * Test JVM per VoiceRecognitionResult e VoiceRecognitionState.
 */
class VoiceRecognitionResultTest {

    // --- VoiceRecognitionResult ---

    @Test
    fun `Success contiene testo valido`() {
        val result = VoiceRecognitionResult.Success("latte")

        assertTrue(result.isValidText)
        assertEquals("latte", result.recognizedText)
    }

    @Test
    fun `Success con testo vuoto non e valido`() {
        val result = VoiceRecognitionResult.Success("")

        assertFalse(result.isValidText)
        assertEquals("", result.recognizedText)
    }

    @Test
    fun `NoSpeech non contiene testo`() {
        val result = VoiceRecognitionResult.NoSpeech

        assertFalse(result.isValidText)
        assertNull(result.recognizedText)
    }

    @Test
    fun `Error contiene codice e messaggio`() {
        val result = VoiceRecognitionResult.Error(ErrorCode.NETWORK_ERROR, "Errore di rete")

        assertFalse(result.isValidText)
        assertNull(result.recognizedText)
        assertEquals(ErrorCode.NETWORK_ERROR, result.code)
        assertEquals("Errore di rete", result.message)
    }

    @Test
    fun `Cancelled non contiene testo`() {
        val result = VoiceRecognitionResult.Cancelled

        assertFalse(result.isValidText)
        assertNull(result.recognizedText)
    }

    @Test
    fun `Success con spazi bianchi non e valido`() {
        val result = VoiceRecognitionResult.Success("   ")

        assertFalse(result.isValidText)
    }

    // --- VoiceRecognitionState ---

    @Test
    fun `Idle non e listening ne errore ne result`() {
        val state = VoiceRecognitionState.Idle

        assertFalse(state.isListening)
        assertFalse(state.isError)
        assertFalse(state.isResult)
        assertNull(state.resultText)
    }

    @Test
    fun `Listening e in ascolto attivo`() {
        val state = VoiceRecognitionState.Listening

        assertTrue(state.isListening)
        assertFalse(state.isError)
        assertFalse(state.isResult)
    }

    @Test
    fun `Result contiene testo`() {
        val state = VoiceRecognitionState.Result("latte")

        assertFalse(state.isListening)
        assertFalse(state.isError)
        assertTrue(state.isResult)
        assertEquals("latte", state.resultText)
    }

    @Test
    fun `VoiceRecognitionState Error contiene codice e messaggio`() {
        val state = VoiceRecognitionState.Error(ErrorCode.NOT_AVAILABLE, "Non disponibile")

        assertFalse(state.isListening)
        assertTrue(state.isError)
        assertFalse(state.isResult)
        assertNull(state.resultText)
    }

    @Test
    fun `Processing e transitorio`() {
        val state = VoiceRecognitionState.Processing

        assertFalse(state.isListening)
        assertFalse(state.isError)
        assertFalse(state.isResult)
    }

    // --- ErrorCode ---

    @Test
    fun `ErrorCode ha 7 valori`() {
        assertEquals(7, ErrorCode.entries.size)
    }

    @Test
    fun `ErrorCode contiene valori attesi`() {
        assertNotNull(ErrorCode.valueOf("MISSING_PERMISSION"))
        assertNotNull(ErrorCode.valueOf("NOT_AVAILABLE"))
        assertNotNull(ErrorCode.valueOf("NETWORK_ERROR"))
        assertNotNull(ErrorCode.valueOf("RECOGNIZER_ERROR"))
        assertNotNull(ErrorCode.valueOf("AUDIO_ERROR"))
        assertNotNull(ErrorCode.valueOf("CLIENT_BUSY"))
        assertNotNull(ErrorCode.valueOf("UNKNOWN"))
    }
}