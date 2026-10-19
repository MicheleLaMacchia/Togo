package it.togo.app.data.voice

import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test JVM (Robolectric) per AndroidVoiceRecognitionGateway.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidVoiceRecognitionGatewayTest {

    @Before
    fun setup() { stopKoin() }

    @Test
    fun `stato iniziale e Idle`() {
        val gateway = AndroidVoiceRecognitionGateway()

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
        assertFalse(gateway.state.value.isListening)
    }

    @Test
    fun `startListening senza launcher ritorna Error NOT_AVAILABLE`() = runTest {
        val gateway = AndroidVoiceRecognitionGateway()

        gateway.startListening()

        val state = gateway.state.value
        assertTrue(state is VoiceRecognitionState.Error)
        assertEquals(ErrorCode.NOT_AVAILABLE, (state as VoiceRecognitionState.Error).code)
    }

    @Test
    fun `stopListening senza start e no-op`() {
        val gateway = AndroidVoiceRecognitionGateway()

        gateway.stopListening()

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `reset torna a Idle`() {
        val gateway = AndroidVoiceRecognitionGateway()

        // Simula uno stato non-Idle
        gateway.onActivityResult(android.app.Activity.RESULT_OK, null)

        gateway.reset()

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `onActivityResult RESULT_OK con testo valido`() {
        val gateway = AndroidVoiceRecognitionGateway()
        val intent = android.content.Intent().apply {
            putStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS, arrayListOf("latte"))
        }

        gateway.onActivityResult(android.app.Activity.RESULT_OK, intent)

        val state = gateway.state.value
        assertTrue(state is VoiceRecognitionState.Result)
        assertEquals("latte", (state as VoiceRecognitionState.Result).text)
    }

    @Test
    fun `onActivityResult RESULT_OK senza testo ritorna Error`() {
        val gateway = AndroidVoiceRecognitionGateway()
        val intent = android.content.Intent()

        gateway.onActivityResult(android.app.Activity.RESULT_OK, intent)

        val state = gateway.state.value
        assertTrue(state is VoiceRecognitionState.Error)
        assertEquals(ErrorCode.AUDIO_ERROR, (state as VoiceRecognitionState.Error).code)
    }

    @Test
    fun `onActivityResult RESULT_CANCELED torna a Idle`() {
        val gateway = AndroidVoiceRecognitionGateway()

        gateway.onActivityResult(android.app.Activity.RESULT_CANCELED, null)

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `onActivityResult errore generico ritorna Error`() {
        val gateway = AndroidVoiceRecognitionGateway()

        gateway.onActivityResult(-999, null)

        val state = gateway.state.value
        assertTrue(state is VoiceRecognitionState.Error)
        assertEquals(ErrorCode.RECOGNIZER_ERROR, (state as VoiceRecognitionState.Error).code)
    }

    @Test
    fun `EXTRA_LANGUAGE_VALUE e it-IT`() {
        assertEquals("it-IT", AndroidVoiceRecognitionGateway.EXTRA_LANGUAGE_VALUE)
    }

    @Test(timeout = 100)
    fun `operazioni gateway completate sotto 100ms`() {
        val gateway = AndroidVoiceRecognitionGateway()

        gateway.onActivityResult(android.app.Activity.RESULT_OK, null)
        gateway.reset()
    }
}