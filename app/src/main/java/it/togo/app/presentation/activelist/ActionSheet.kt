package it.togo.app.presentation.activelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Foglio azione "Aggiungi/Modifica azione" per la pagina "Da fare" (Story 8.5).
 *
 * È un foglio separato da [AddItemBottomSheet]: nessuna marca, variante,
 * unità, quantità e nessun riferimento al catalogo prodotti. Espone solo
 * testo a una riga, urgenza (3 chip) e ambito (5 chip).
 *
 * Il componente è presentazionale: raccoglie i valori e li restituisce tramite
 * [onConfirm]; la scrittura sul repository spetta al chiamante (ViewModel).
 *
 * @param onDismiss chiude il foglio senza scrivere (pulsante "Annulla" o back).
 * @param editingAction quando valorizzato il foglio è in modalità modifica
 *   (pulsante primario "Salva", campi precompilati); altrimenti "Aggiungi".
 * @param onConfirm invocato con testo, ambito e urgenza quando l'utente
 *   conferma e il testo non è vuoto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    onDismiss: () -> Unit,
    editingAction: ActionItem? = null,
    /** Errore inline mostrato sotto il campo testo (es. "Azione già presente"). */
    errorMessage: String? = null,
    onConfirm: (text: String, scope: ActionScope, urgency: ActionUrgency) -> Unit = { _, _, _ -> },
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val isEditMode = editingAction != null

    // Stato locale del foglio: si resetta a ogni apertura, precompilato in
    // modalità modifica. Default: ambito Altro, urgenza A tempo perso (CAP-7/CAP-8).
    val textState = remember(editingAction?.id) { mutableStateOf(editingAction?.text ?: "") }
    val scopeState = remember(editingAction?.id) { mutableStateOf(editingAction?.scope ?: ActionScope.ALTRO) }
    val urgencyState = remember(editingAction?.id) { mutableStateOf(editingAction?.urgency ?: ActionUrgency.A_TEMPO_PERSO) }

    val primaryLabel = if (isEditMode) "Modifica cosa da fare" else "Aggiungi"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceBase,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Titolo
            Text(
                text = if (isEditMode) "Modifica cosa da fare" else "Nuova azione",
                style = typography.titleScreen,
                color = colors.inkPrimary,
            )

            // Campo testo a una riga. L'errore inline (es. "Azione già presente")
            // viene mostrato sotto il campo senza chiudere il foglio: i valori
            // scelti dall'utente restano conservati nello stato locale.
            TextField(
                value = textState.value,
                onValueChange = { newText: String -> textState.value = newText },
                label = { Text("Testo") },
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth(),
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = typography.itemMeta,
                    color = colors.warningInk,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }

            // Selettore urgenza: tre opzioni (Urgente, A tempo perso, Eccezionale).
            Text(
                text = "Urgenza",
                style = typography.itemMeta,
                color = colors.inkSecondary,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionUrgency.ordered.forEach { u ->
                    val isSelected = urgencyState.value == u
                    TextButton(
                        onClick = { urgencyState.value = u },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = u.label,
                            style = typography.itemMeta,
                            color = if (isSelected) colors.inkInverse else colors.inkPrimary,
                        )
                    }
                }
            }

            // Selettore ambito: cinque opzioni (Casa, Pratiche, Salute, Lavoro, Altro).
            Text(
                text = "Ambito",
                style = typography.itemMeta,
                color = colors.inkSecondary,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionScope.ordered.forEach { s ->
                    val isSelected = scopeState.value == s
                    TextButton(
                        onClick = { scopeState.value = s },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = s.label,
                            style = typography.itemMeta,
                            color = if (isSelected) colors.inkInverse else colors.inkPrimary,
                        )
                    }
                }
            }

            // Pulsanti: secondario "Annulla", primario "Aggiungi"/"Salva".
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text(
                        text = "Annulla",
                        style = typography.itemTitle,
                        color = colors.inkPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (textState.value.isNotBlank()) {
                            onConfirm(textState.value, scopeState.value, urgencyState.value)
                        }
                    },
                    enabled = textState.value.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentAction,
                        contentColor = colors.inkInverse,
                        disabledContainerColor = colors.surfaceSubtle,
                        disabledContentColor = colors.inkSecondary,
                    ),
                ) {
                    Text(text = primaryLabel, style = typography.sectionHeader)
                }
            }
        }
    }
}
