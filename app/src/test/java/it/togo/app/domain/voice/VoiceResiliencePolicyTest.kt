package it.togo.app.domain.voice

import org.junit.Assert.*
import org.junit.Test

/**
 * Test JVM per VoiceResiliencePolicy.
 */
class VoiceResiliencePolicyTest {

    @Test
    fun `default policy ha max 3 tentativi`() {
        val policy = VoiceResiliencePolicy.DEFAULT

        assertEquals(3, policy.maxAttempts)
        assertEquals(500L, policy.baseDelayMs)
    }

    @Test
    fun `delay tentativo 1 e 500ms`() {
        val policy = VoiceResiliencePolicy.DEFAULT

        assertEquals(500L, policy.getDelayMs(1))
    }

    @Test
    fun `delay tentativo 2 e 1000ms`() {
        val policy = VoiceResiliencePolicy.DEFAULT

        assertEquals(1000L, policy.getDelayMs(2))
    }

    @Test
    fun `delay tentativo 3 e 2000ms`() {
        val policy = VoiceResiliencePolicy.DEFAULT

        assertEquals(2000L, policy.getDelayMs(3))
    }

    @Test
    fun `canRetry true se sotto max`() {
        val policy = VoiceResiliencePolicy.DEFAULT

        assertTrue(policy.canRetry(1))
        assertTrue(policy.canRetry(2))
    }

    @Test
    fun `canRetry false se a max`() {
        val policy = VoiceResiliencePolicy.DEFAULT

        assertFalse(policy.canRetry(3))
    }

    @Test
    fun `delay tentativo 0 e zero`() {
        val policy = VoiceResiliencePolicy.DEFAULT

        assertEquals(0L, policy.getDelayMs(0))
    }

    @Test
    fun `policy custom con moltiplicatore diverso`() {
        val policy = VoiceResiliencePolicy(maxAttempts = 5, baseDelayMs = 100L, backoffMultiplier = 3.0)

        assertEquals(100L, policy.getDelayMs(1))
        assertEquals(300L, policy.getDelayMs(2))
        assertEquals(900L, policy.getDelayMs(3))
    }

    @Test(timeout = 100)
    fun `calcolo delay completato sotto 100ms`() {
        val policy = VoiceResiliencePolicy.DEFAULT
        policy.getDelayMs(1)
        policy.getDelayMs(2)
        policy.getDelayMs(3)
    }
}