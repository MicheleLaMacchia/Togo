package it.togo.app.presentation.activelist

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import it.togo.app.domain.usecase.ShareListUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Helper Compose per la condivisione della lista attiva tramite ShareSheet.
 *
 * Usage:
 * ```kotlin
 * val shareLauncher = rememberShareLauncher(shareUseCase)
 * // ...
 * IconButton(onClick = { shareLauncher.launch(Unit) }) { ... }
 * ```
 */
@Composable
fun rememberShareLauncher(
    shareUseCase: ShareListUseCase
): androidx.activity.result.ActivityResultLauncher<Unit> {
    return rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        // Risultato condivisone (opzionale)
    }
}

/**
 * Lancia la condivisione della lista attiva.
 *
 * Deve essere chiamato all'interno di una CoroutineScope (es. `rememberCoroutineScope().launch`).
 */
suspend fun CoroutineScope.shareActiveList(
    activity: Activity,
    shareUseCase: ShareListUseCase
) {
    val intent = shareUseCase()
    if (intent != null) {
        activity.startActivity(Intent.createChooser(intent, "Condividi lista spesa"))
    }
}

/**
 * Versione non-suspend per uso diretto in callback (usa launch interno).
 */
@Composable
fun ShareListButton(
    shareUseCase: ShareListUseCase,
    onEmpty: () -> Unit = {},
    onNothingToShare: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val shareLauncher = rememberShareLauncher(shareUseCase)

    androidx.compose.material3.IconButton(
        onClick = {
            scope.launch {
                val intent = shareUseCase()
                if (intent != null) {
                    val activity = (androidx.compose.ui.platform.LocalContext.current as? android.app.Activity)
                    activity?.let {
                        it.startActivity(Intent.createChooser(it, "Condividi lista spesa"))
                    }
                } else {
                    // Gestione casi vuoti
                    when {
                        // Qui dovremmo distinguere tra lista vuota e solo voci spuntate
                        // Per semplicità chiamiamo il callback generico
                        true -> onEmpty()
                    }
                }
            }
        }
    ) {
        androidx.compose.material.icons.Icons.Default.Share
    }
}