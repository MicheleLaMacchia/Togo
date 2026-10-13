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
        val delay1 = handler.getNextRetryDelayMs()
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        val delay2 = handler.getNextRetryDelayMs()
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        val delay3 = handler.getNextRetryDelayMs()

        assertEquals(500L, delay1)
        assertEquals(1000L, delay2)
        assertEquals(2000L, delay3)
    }

    @Test
    fun `MISSING_PERMISSION non permette fallback immediato`() {
        val shouldFallback = handler.recordFailure(ErrorCode.MISSING_PERMISSION)

        assertFalse(shouldFallback)
        assertFalse(handler.isFallbackActive())
    }

    @Test
    fun `NOT_AVAILABLE non attiva fallback con un solo fallimento`() {
        val shouldFallback = handler.recordFailure(ErrorCode.NOT_AVAILABLE)

        assertFalse(shouldFallback)
    }

    @Test(timeout = 100)
    fun `operazioni handler completate sotto 100ms`() {
        handler.recordFailure(ErrorCode.NETWORK_ERROR)
        handler.recordSuccess()
        handler.reset()
    }
}