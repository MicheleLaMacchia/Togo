package it.togo.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Verifica i permessi nel manifest merged prodotto da AGP.
 *
 * Non usa Robolectric: il test deve solo leggere il manifest, quindi evitiamo
 * l'avvio della sandbox Android e il download dell'artifact android-all.
 */
class AndroidManifestPermissionTest {

    private val requestedPermissions: Set<String> by lazy {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(mergedManifestFile())

        val nodes = document.getElementsByTagName("uses-permission")
        buildSet {
            for (index in 0 until nodes.length) {
                val attributes = nodes.item(index).attributes ?: continue
                val name = attributes.getNamedItemNS(ANDROID_NAMESPACE, "name")?.nodeValue
                    ?: attributes.getNamedItem("android:name")?.nodeValue
                if (!name.isNullOrBlank()) add(name)
            }
        }
    }

    @Test
    fun `manifest dichiara permesso VIBRATE`() {
        assertTrue(
            "AndroidManifest deve dichiarare android.permission.VIBRATE",
            "android.permission.VIBRATE" in requestedPermissions,
        )
    }

    @Test
    fun `manifest dichiara permesso RECORD_AUDIO`() {
        assertTrue(
            "AndroidManifest deve dichiarare android.permission.RECORD_AUDIO",
            "android.permission.RECORD_AUDIO" in requestedPermissions,
        )
    }

    /**
     * AGP espone il percorso del manifest merged in `test_config.properties`.
     * Il percorso è relativo alla directory del modulo: proviamo le posizioni
     * possibili per funzionare sia da Gradle sia dall'IDE.
     */
    private fun mergedManifestFile(): File {
        val configStream = javaClass.classLoader
            .getResourceAsStream(TEST_CONFIG_RESOURCE)
            ?: error("Risorsa $TEST_CONFIG_RESOURCE non trovata nel classpath dei test")

        val properties = Properties().apply {
            configStream.use { load(it) }
        }
        val manifestPath = properties.getProperty("android_merged_manifest")
            ?: error("android_merged_manifest assente in $TEST_CONFIG_RESOURCE")

        val workingDir = File(System.getProperty("user.dir"))
        return sequenceOf(
            File(manifestPath),
            workingDir.resolve(manifestPath),
            workingDir.resolve("app").resolve(manifestPath),
            workingDir.parentFile?.resolve(manifestPath),
        )
            .filterNotNull()
            .firstOrNull { it.isFile }
            ?: error(
                "Manifest merged non trovato: $manifestPath " +
                    "(user.dir=${workingDir.absolutePath})",
            )
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val TEST_CONFIG_RESOURCE = "com/android/tools/test_config.properties"
    }
}
