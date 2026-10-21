package it.togo.app.data.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.runBlocking
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

    /**
     * Apre la finestra di risultato senza avviare una vera Activity.
     * Il launcher finto non fa nulla: il test consegna il risultato a mano.
     */
    private fun startWithFakeLauncher(gateway: AndroidVoiceRecognitionGateway) {
        gateway.registerLauncher(
            object : ActivityResultLauncher<Intent>() {
                override val contract: ActivityResultContract<Intent, *> =
                    ActivityResultContracts.StartActivityForResult()

                override fun launch(input: Intent, options: ActivityOptionsCompat?) = Unit
                override fun unregister() = Unit
            }
        )
        runBlocking { gateway.startListening() }
        assertTrue(gateway.state.value.isListening)
    }

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
    fun `reset torna a Idle e invalida un risultato tardivo`() {
        val gateway = AndroidVoiceRecognitionGateway()
        startWithFakeLauncher(gateway)

        gateway.reset()

        val lateIntent = Intent().apply {
            putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("latte"))
        }
        gateway.onActivityResult(Activity.RESULT_OK, lateIntent)

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `stopListening invalida un risultato tardivo`() {
        val gateway = AndroidVoiceRecognitionGateway()
        startWithFakeLauncher(gateway)

        gateway.stopListening()

        val lateIntent = Intent().apply {
            putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("latte"))
        }
        gateway.onActivityResult(Activity.RESULT_OK, lateIntent)

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `onActivityResult RESULT_OK con testo valido`() {
        val gateway = AndroidVoiceRecognitionGateway()
        startWithFakeLauncher(gateway)
        val intent = Intent().apply {
            putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, arrayListOf("latte"))
        }

        gateway.onActivityResult(Activity.RESULT_OK, intent)

        val state = gateway.state.value
        assertTrue(state is VoiceRecognitionState.Result)
        assertEquals("latte", (state as VoiceRecognitionState.Result).text)
    }

    @Test
    fun `onActivityResult RESULT_OK senza testo ritorna Error`() {
        val gateway = AndroidVoiceRecognitionGateway()
        startWithFakeLauncher(gateway)
        val intent = Intent()

        gateway.onActivityResult(Activity.RESULT_OK, intent)

        val state = gateway.state.value
        assertTrue(state is VoiceRecognitionState.Error)
        assertEquals(ErrorCode.AUDIO_ERROR, (state as VoiceRecognitionState.Error).code)
    }

    @Test
    fun `onActivityResult RESULT_CANCELED torna a Idle`() {
        val gateway = AndroidVoiceRecognitionGateway()
        startWithFakeLauncher(gateway)

        gateway.onActivityResult(Activity.RESULT_CANCELED, null)

        assertEquals(VoiceRecognitionState.Idle, gateway.state.value)
    }

    @Test
    fun `onActivityResult errore generico ritorna Error`() {
        val gateway = AndroidVoiceRecognitionGateway()
        startWithFakeLauncher(gateway)

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
        startWithFakeLauncher(gateway)

        gateway.onActivityResult(Activity.RESULT_OK, null)
        gateway.reset()
    }
}