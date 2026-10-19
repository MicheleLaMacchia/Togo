package it.togo.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test di analisi statica: verifica che AndroidManifest.xml dichiari i permessi richiesti.
 *
 * Fix 2: permesso VIBRATE per haptic feedback
 * Fix 3: permesso RECORD_AUDIO per riconoscimento vocale
 */
class AndroidManifestPermissionTest {

    private val manifestFile: File
        // Il working directory durante i test è la root del modulo (app/)
        // Risaliamo alla root del progetto per trovare AndroidManifest
        get() = File("src/main/AndroidManifest.xml").let { appLevel ->
            if (appLevel.exists()) appLevel
            else File("app/src/main/AndroidManifest.xml")
        }

    @Test
    fun `manifest esiste`() {
        assertTrue(
            "AndroidManifest.xml non trovato in: ${manifestFile.absolutePath}",
            manifestFile.exists()
        )
    }

    // ==================== FIX 2: VIBRATE ====================

    @Test
    fun `manifest dichiara permesso VIBRATE`() {
        val manifest = manifestFile.readText()
        assertTrue(
            "AndroidManifest deve dichiarare android.permission.VIBRATE",
            manifest.contains("android.permission.VIBRATE")
        )
    }

    // ==================== FIX 3: RECORD_AUDIO ====================

    @Test
    fun `manifest dichiara permesso RECORD_AUDIO`() {
        val manifest = manifestFile.readText()
        assertTrue(
            "AndroidManifest deve dichiarare android.permission.RECORD_AUDIO",
            manifest.contains("android.permission.RECORD_AUDIO")
        )
    }
}
