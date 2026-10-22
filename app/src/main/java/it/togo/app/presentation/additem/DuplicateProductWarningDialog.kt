package it.togo.app.presentation.additem

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Avviso applicativo di possibile duplicato (Story 7.1).
 *
 * Il catalogo non ha vincoli UNIQUE sul nome: l'utente può sempre decidere
 * di procedere con la creazione (AC: "avviso con opzione di procedere o
 * annullare"). Estratto da [AddItemBottomSheet] per essere testabile in
 * isolamento.
 */
@Composable
fun DuplicateProductWarningDialog(
    candidateName: String,
    newProductName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Possibile duplicato",
                style = typography.titleScreen,
                color = colors.inkPrimary,
            )
        },
        text = {
            Text(
                text = "Esiste già «$candidateName» in questa categoria. " +
                    "Vuoi creare comunque «$newProductName»?",
                style = typography.itemMeta,
                color = colors.inkPrimary,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = spacing.touchTargetMin),
            ) {
                Text("Procedi", style = typography.sectionHeader, color = colors.accentAction)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = spacing.touchTargetMin),
            ) {
                Text("Annulla", style = typography.sectionHeader, color = colors.inkSecondary)
            }
        },
        containerColor = colors.surfaceBase,
    )
}
