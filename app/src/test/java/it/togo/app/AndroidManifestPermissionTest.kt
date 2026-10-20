package it.togo.app

import android.content.pm.PackageManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifica i permessi dichiarati leggendo il PackageManager a runtime,
 * non il testo del manifest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidManifestPermissionTest {

    private val requestedPermissions: Set<String> by lazy {
        val context = RuntimeEnvironment.getApplication()
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toSet()
            .orEmpty()
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
}
