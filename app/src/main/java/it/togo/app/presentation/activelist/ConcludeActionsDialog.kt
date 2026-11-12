package it.togo.app.presentation.activelist

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Dialog di conferma di "Concludi attività" (Story 9.2, DESIGN.md §7.10).
 *
 * - Titolo "Concludere le attività?" e corpo con il conteggio delle azioni
 *   spuntate e l'avviso di irreversibilità.
 * - Primario "Concludi attività" distruttivo: pieno `accentAction`, icona
 *   warning decorativa (`contentDescription = null`), testo `inkInverse`.
 * - Secondario testuale "Annulla".
 * - Annulla, back di sistema e tap fuori dal dialog chiudono senza confermare.
 *
 * Il dialog è solo presentazionale: la conferma invoca [onConfirm] e la
 * cancellazione delle azioni spuntate resta responsabilità della Story 9.3.
 */
@Composable
fun ConcludeActionsDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    // Contratto 9.2: il dialog si apre solo con N > 0 (la sezione Fatte
    // non emette pulsante con N = 0). Guardia difensiva.
    require(count > 0) { "ConcludeActionsDialog richiede count > 0" }

    // "1 attività spuntata" al singolare; "N attività spuntate" al plurale.
    val body = if (count == 1) {
        "Verrà eliminata 1 attività spuntata. L'operazione non si può annullare."
    } else {
        "Verranno eliminate $count attività spuntate. " +
            "L'operazione non si può annullare."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .testTag("conclude_actions_dialog")
            .border(
                width = tokens.duplicateDialog.borderWidth,
                color = tokens.duplicateDialog.borderColor,
                shape = tokens.duplicateDialog.radius,
            ),
        shape = tokens.duplicateDialog.radius,
        containerColor = tokens.duplicateDialog.background,
        title = {
            Text(
                text = "Concludere le attività?",
                style = typography.titleScreen,
                color = colors.inkPrimary,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Text(
                text = body,
                style = typography.itemMeta,
                color = colors.inkPrimary,
                // TalkBack annuncia titolo + corpo all'apertura del dialog.
                modifier = Modifier.semantics { contentDescription = body },
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = spacing.touchTargetMin)
                    .testTag("conclude_actions_confirm_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentAction,
                    contentColor = colors.inkInverse,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        // Decorativa: il testo del corpo e l'etichetta del
                        // pulsante portano già il significato dell'azione.
                        contentDescription = null,
                        tint = colors.accentWarning,
                        modifier = Modifier
                            .size(18.dp)
                            .testTag("conclude_actions_warning_icon"),
                    )
                    Spacer(modifier = Modifier.width(spacing.space1))
                    Text(
                        text = "Concludi attività",
                        style = typography.sectionHeader,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = spacing.touchTargetMin)
                    .testTag("conclude_actions_cancel_button"),
            ) {
                Text(
                    text = "Annulla",
                    style = typography.sectionHeader,
                    color = colors.inkSecondary,
                )
            }
        },
    )
}
