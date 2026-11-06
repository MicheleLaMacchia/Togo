package it.togo.app.presentation.activelist

import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ChipDefaults
import androidx.compose.material3.ChoiceChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionSheet(
    onDismiss: () -> Unit,
    editingAction: ActionItem? = null,
    onConfirm: (text: String, scope: ActionScope, urgency: ActionUrgency) -> Unit = {},
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val isEditMode = editingAction != null

    // Stato locale del foglio: si resetta a ogni apertura, precompilato in
    // modalità modifica. Default: ambito Altro, urgenza A tempo perso (CAP-7/CAP-8).
    val text by remember(editingAction?.id) { mutableStateOf(editingAction?.text ?: "") }
    var scope by remember(editingAction?.id) { mutableStateOf(editingAction?.scope ?: ActionScope.ALTRO) }
    var urgency by remember(editingAction?.id) { mutableStateOf(editingAction?.urgency ?: ActionUrgency.A_TEMPO_PERSO) }

    // Autofocus sul campo testo appena il foglio appare.
    val focusRequester = FocusRequester()
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val isBlank = text.isBlank()
    val primaryLabel = if (isEditMode) "Salva" else "Aggiungi"

    val chipColors = ChipDefaults.choiceChipColors(
        selectedContainerColor = colors.accentAction,
        selectedContentColor = colors.inkInverse,
        unselectedContainerColor = colors.surfaceSubtle,
        unselectedContentColor = colors.inkPrimary,
    )

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
                text = if (isEditMode) "Modifica azione" else "Nuova azione",
                style = typography.titleScreen,
                color = colors.inkPrimary,
            )

            // Campo testo a una riga, autofocus e placeholder.
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Es. Chiamare il dentista") },
                label = { Text("Testo") },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Text,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (!isBlank) onConfirm(text, scope, urgency) },
                ),
                focusRequester = focusRequester,
                colors = TextFieldDefaults.textFieldColors(
                    unfocusedContainerColor = colors.surfaceSubtle,
                    focusedContainerColor = colors.surfaceCard,
                ),
            )

            // Selettore urgenza: tre chip (Urgente, A tempo perso, Eccezionale).
            Text(
                text = "Urgenza",
                style = typography.itemMeta,
                color = colors.inkSecondary,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionUrgency.ordered.forEach { u ->
                    ChoiceChip(
                        selected = urgency == u,
                        onClick = { urgency = u },
                        label = { Text(u.label) },
                        colors = chipColors,
                        modifier = Modifier.testTag("action_chip_urgency_${u.name}"),
                    )
                }
            }

            // Selettore ambito: cinque chip (Casa, Pratiche, Salute, Lavoro, Altro).
            Text(
                text = "Ambito",
                style = typography.itemMeta,
                color = colors.inkSecondary,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionScope.ordered.forEach { s ->
                    ChoiceChip(
                        selected = scope == s,
                        onClick = { scope = s },
                        label = { Text(s.label) },
                        colors = chipColors,
                        modifier = Modifier.testTag("action_chip_scope_${s.name}"),
                    )
                }
            }

            // Pulsanti: secondario "Annulla", primario "Aggiungi"/"Salva".
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("action_sheet_cancel"),
                ) {
                    Text(
                        text = "Annulla",
                        style = typography.itemTitle,
                        color = colors.inkPrimary,
                    )
                }
                Spacer(modifier = androidx.compose.foundation.layout.width(8.dp))
                Button(
                    onClick = { if (!isBlank) onConfirm(text, scope, urgency) },
                    enabled = !isBlank,
                    modifier = Modifier.testTag("action_sheet_primary"),
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
