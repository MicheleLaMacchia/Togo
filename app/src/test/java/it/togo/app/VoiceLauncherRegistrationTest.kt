package it.togo.app

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import io.mockk.mockk
import it.togo.app.data.voice.AndroidVoiceRecognitionGateway
import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifica il lifecycle del launcher vocale sul gateway reale.
 * La registrazione dalla schermata è coperta dal test di composizione.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceLauncherRegistrationTest {

    @Test
    fun `gateway registers and unregisters launcher`() {
        val gateway = AndroidVoiceRecognitionGateway()
        val launcher = mockk<ActivityResultLauncher<Intent>>(relaxed = true)

        assertFalse("il gateway parte senza launcher", gateway.isLauncherRegistered)

        gateway.registerLauncher(launcher)
        assertTrue("il launcher deve essere registrato", gateway.isLauncherRegistered)

        gateway.unregisterLauncher()
        assertFalse("il launcher deve essere rilasciato", gateway.isLauncherRegistered)
    }

    @Test
    fun `startListening without launcher reports not available`() = runTest {
        val gateway = AndroidVoiceRecognitionGateway()

        gateway.startListening()

        val state = gateway.state.value
        assertTrue(
            "senza launcher lo stato deve essere Error(NOT_AVAILABLE)",
            state is VoiceRecognitionState.Error && state.code == ErrorCode.NOT_AVAILABLE,
        )
    }
}
