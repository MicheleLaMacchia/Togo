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
 * Avviso applicativo di possibile duplicato di Livello 3 (Story 7.2).
 *
 * Il catalogo non ha vincoli UNIQUE sul nome: l'utente può sempre decidere
 * di procedere con la creazione (AC: "avviso con il candidato duplicato").
 * Estratto da [AddItemBottomSheet] per essere testabile in isolamento.
 */
@Composable
fun DuplicateLevel3WarningDialog(
    candidateName: String,
    newLevel3Name: String,
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
                text = "Esiste già il Livello 3 «$candidateName» in questa categoria. " +
                    "Vuoi creare comunque «$newLevel3Name»?",
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
