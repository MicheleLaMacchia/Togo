package it.togo.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica: verifica che ActiveListScreen.kt registri il launcher vocale.
 *
 * Fix 4: Launcher vocale registrato sul gateway con StartActivityForResult + registerLauncher via LaunchedEffect
 */
class VoiceLauncherRegistrationTest {

    private val sourceFile: File
        get() = File("app/src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt").let {
            if (it.exists()) it
            else File("src/main/java/it/togo/app/presentation/activelist/ActiveListScreen.kt")
        }

    @Test
    fun `ActiveListScreen registra launcher vocale sul gateway`() {
        val source = sourceFile.readText()

        assertTrue(
            "ActiveListScreen deve usare registerLauncher",
            source.contains("registerLauncher")
        )
        assertTrue(
            "ActiveListScreen deve usare StartActivityForResult",
            source.contains("StartActivityForResult")
        )
        assertTrue(
            "ActiveListScreen deve gestire onActivityResult",
            source.contains("onActivityResult")
        )
    }

    @Test
    fun `ActiveListScreen usa koinInject per gateway vocale`() {
        val source = sourceFile.readText()
        assertTrue(
            "ActiveListScreen deve iniettare AndroidVoiceRecognitionGateway via Koin",
            source.contains("koinInject") || source.contains("AndroidVoiceRecognitionGateway")
        )
    }

    @Test
    fun `ActiveListScreen controlla permesso RECORD_AUDIO a runtime`() {
        val source = sourceFile.readText()
        assertTrue(
            "Deve usare ContextCompat.checkSelfPermission per RECORD_AUDIO",
            source.contains("RECORD_AUDIO") && source.contains("checkSelfPermission")
        )
    }
}
