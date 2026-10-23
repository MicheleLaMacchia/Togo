package it.togo.app.domain.voice

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test JVM per VoiceFallbackHandler.
 */
class VoiceFallbackHandlerTest {

    private lateinit var handler: VoiceFallbackHandler

    @Before
    fun setup() {
        handler = VoiceFallbackHandler()
    }

    @Test
    fun `stato iniziale nessun fallimento`() {
        assertEquals(0, handler.getConsecutiveFailures())
        assertFalse(handler.isFallbackActive())
        assertNull(handler.getLastErrorCode())
    }

    @Test
    fun `fallimento singolo non attiva fallback`() {
        val shouldFallback = handler.recordFailure(ErrorCode.NETWORK_ERROR)

        assertFalse(shouldFallback)
        assertEquals(1, handler.getConsecutiveFailures())
        assertFalse(handler.isFallbackActive())
    }

    @Test
    fun `tre fallimenti consecutivi attivano fallback`() {
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        val shouldFallback = handler.recordFailure(ErrorCode.NETWORK_ERROR)

        assertTrue(shouldFallback)
        assertTrue(handler.isFallbackActive())
        assertEquals(3, handler.getConsecutiveFailures())
    }

    @Test
    fun `successo resetta contatore`() {
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        handler.recordFailure(ErrorCode.NETWORK_ERROR)

        handler.recordSuccess()

        assertEquals(0, handler.getConsecutiveFailures())
        assertFalse(handler.isFallbackActive())
        assertNull(handler.getLastErrorCode())
    }

    @Test
    fun `reset completo`() {
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        handler.recordFailure(ErrorCode.NETWORK_ERROR)

        handler.reset()

        assertEquals(0, handler.getConsecutiveFailures())
        assertFalse(handler.isFallbackActive())
    }

    @Test
    fun `delay backoff aumenta con fallimenti`() {
        // Prima di qualsiasi fallimento: consecutiveFailures=0 → delay 0
        val delay0 = handler.getNextRetryDelayMs()
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        // Dopo 1° fallimento: consecutiveFailures=1 → delay 500
        val delay1 = handler.getNextRetryDelayMs()
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        // Dopo 2° fallimento: consecutiveFailures=2 → delay 1000
        val delay2 = handler.getNextRetryDelayMs()

        assertEquals(0L, delay0)
        assertEquals(500L, delay1)
        assertEquals(1000L, delay2)
    }

    @Test
    fun `MISSING_PERMISSION non permette retry`() {
        val shouldFallback = handler.recordFailure(ErrorCode.MISSING_PERMISSION)

        // canRetry(MISSING_PERMISSION) = false → fallback attivato
        assertTrue(shouldFallback)
    }

    @Test
    fun `NOT_AVAILABLE non permette retry`() {
        val shouldFallback = handler.recordFailure(ErrorCode.NOT_AVAILABLE)

        // canRetry(NOT_AVAILABLE) = false → fallback attivato
        assertTrue(shouldFallback)
    }

    // La guardia JUnit assorbe l'avvio della classe; la soglia di 100ms misura
    // solo le operazioni, dopo un warm-up.
    @Test(timeout = 15_000)
    fun `operazioni handler completate sotto 100ms`() {
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        handler.recordSuccess()
        handler.reset() // warm-up

        val start = System.nanoTime()
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        handler.recordSuccess()
        handler.reset()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("operazioni handler hanno impiegato ${elapsedMs}ms", elapsedMs < 100)
    }
}