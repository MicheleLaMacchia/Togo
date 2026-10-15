package it.togo.app.presentation.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.domain.parser.ParsedCommand
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.TogoColorScheme
import it.togo.app.ui.theme.TogoSpacingTokens
import it.togo.app.ui.theme.TogoTypographyTokens
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypographyTokens

/**
 * Bottom sheet di conferma per comandi vocali.
 *
 * Mostra:
 * - Interpretazione strutturata (prodotto, quantità, unità, categoria)
 * - Avviso se `needsClarification`
 * - Azioni: Conferma / Modifica / Annulla
 * - In edit mode: input locale con "Fine modifica" (re-parsing su click)
 *
 * Design System: 100% token TogoTheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceConfirmationSheet(
    uiState: VoiceConfirmationUiState,
    onConfirm: () -> Unit,
    onEdit: (String) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypographyTokens()
    val spacing = togoSpacing()

    if (uiState.isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.surfaceBase,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            when (val state = uiState) {
                is VoiceConfirmationUiState.Ready -> ReadyContent(
                    state = state,
                    colors = colors,
                    typography = typography,
                    spacing = spacing,
                    onConfirm = onConfirm,
                    onEdit = onEdit,
                    onCancel = onCancel,
                )

                else -> { /* Hidden: non visibile */ }
            }

            Spacer(modifier = Modifier.height(spacing[2]))
        }
    }
}

@Composable
private fun ReadyContent(
    state: VoiceConfirmationUiState.Ready,
    colors: TogoColorScheme,
    typography: TogoTypographyTokens,
    spacing: TogoSpacingTokens,
    onConfirm: () -> Unit,
    onEdit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    // Stato locale per input testo (evita re-parsing su ogni tasto)
    var editText by remember(state.parsedCommand) { mutableStateOf(state.parsedCommand.productName) }
    var isEditing by remember(state.isEditing) { mutableStateOf(state.isEditing) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing[4]),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing[3]),
    ) {
        // Handle bar
        Surface(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp),
            shape = RoundedCornerShape(2.dp),
            color = colors.borderCrisp,
        ) {}

        Text(
            text = "Conferma prodotto",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        // Avviso needsClarification
        if (state.parsedCommand.needsClarification) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing[2]),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = colors.accentWarning,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Manca l'unità di misura, specifica",
                    style = typography.itemMeta,
                    color = colors.accentWarning,
                )
            }
        }

        // Interpretazione strutturata (o input modifica)
        if (isEditing) {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                label = { Text("Modifica prodotto", style = typography.caption) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Pulsante "Fine modifica" → re-parsing con testo corrente
            OutlinedButton(
                onClick = {
                    onEdit(editText)
                    isEditing = false
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fine modifica")
            }
        } else {
            CommandDisplay(
                command = state.displayCommand,
                colors = colors,
                typography = typography,
                spacing = spacing,
            )
        }

        // Azioni
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing[2]),
        ) {
            if (!isEditing) {
                OutlinedButton(
                    onClick = { isEditing = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(spacing[1]))
                    Text("Modifica")
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(spacing[1]))
                Text("Annulla")
            }
        }

        if (!isEditing) {
            Button(
                onClick = onConfirm,
                enabled = state.canConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(spacing[1]))
                Text("Conferma")
            }
        }
    }
}

@Composable
private fun CommandDisplay(
    command: ParsedCommand,
    colors: TogoColorScheme,
    typography: TogoTypographyTokens,
    spacing: TogoSpacingTokens,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.surfaceSubtle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(spacing[3]),
            verticalArrangement = Arrangement.spacedBy(spacing[1]),
        ) {
            Text(
                text = command.productName,
                style = typography.itemName,
                color = colors.inkPrimary,
            )

            if (command.quantity != null) {
                Text(
                    text = "Quantità: ${command.quantity} ${command.unit?.displayName ?: ""}",
                    style = typography.itemMeta,
                    color = colors.inkSecondary,
                )
            }

            if (command.categoryHint != null) {
                Text(
                    text = "Categoria: ${command.categoryHint}",
                    style = typography.itemMeta,
                    color = colors.inkSecondary,
                )
            }

            if (command.confidence > 0) {
                Text(
                    text = "Confidenza: ${(command.confidence * 100).toInt()}%",
                    style = typography.caption,
                    color = colors.inkMuted,
                )
            }
        }
    }
}
