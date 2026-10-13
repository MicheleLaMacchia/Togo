package it.togo.app.presentation.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Bottom sheet per il riconoscimento vocale.
 *
 * Stati:
 * - Listening: indicatore pulsante + "Ascolto in corso..."
 * - Processing: spinner + "Elaborazione..."
 * - Ready: testo riconosciuto + azioni (Conferma / Modifica / Riprova)
 * - Error: messaggio errore + azioni (Riprova / Annulla)
 *
 * Design System: 100% token TogoTheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceBottomSheet(
    uiState: VoiceBottomSheetUiState,
    onEdit: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    // ModalBottomSheet gestisce nativamente animazione e scrim
    if (uiState.isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.space4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                // Handle bar
                Surface(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = colors.outline,
                ) {}

                when (uiState) {
                    is VoiceBottomSheetUiState.Listening -> ListeningContent(
                        colors = colors,
                        typography = typography,
                        spacing = spacing,
                        onCancel = onCancel,
                    )

                    is VoiceBottomSheetUiState.Processing -> ProcessingContent(
                        colors = colors,
                        typography = typography,
                        spacing = spacing,
                    )

                    is VoiceBottomSheetUiState.Ready -> ReadyContent(
                        uiState = uiState,
                        colors = colors,
                        typography = typography,
                        spacing = spacing,
                        onEdit = onEdit,
                        onConfirm = onConfirm,
                        onRetry = onRetry,
                    )

                    is VoiceBottomSheetUiState.Error -> ErrorContent(
                        uiState = uiState,
                        colors = colors,
                        typography = typography,
                        spacing = spacing,
                        onRetry = onRetry,
                        onCancel = onCancel,
                    )

                    is VoiceBottomSheetUiState.Hidden -> { /* non visibile */ }
                }

                Spacer(modifier = Modifier.height(spacing.space2))
            }
        }
    }
}

@Composable
private fun ListeningContent(
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    onCancel: () -> Unit,
) {
    // Indicatore pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
        modifier = Modifier.padding(spacing.space4),
    ) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .scale(scale),
            shape = CircleShape,
            color = colors.error.copy(alpha = 0.15f),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier
                    .padding(spacing.space4)
                    .size(32.dp),
            )
        }

        Text(
            text = "Ascolto in corso...",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        Text(
            text = "Dimmi un prodotto da aggiungere alla lista",
            style = typography.itemMeta,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
        )

        TextButton(onClick = onCancel) {
            Text("Annulla", color = colors.inkSecondary)
        }
    }
}

@Composable
private fun ProcessingContent(
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
        modifier = Modifier.padding(spacing.space4),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = colors.accent,
        )

        Text(
            text = "Elaborazione...",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )
    }
}

@Composable
private fun ReadyContent(
    uiState: VoiceBottomSheetUiState.Ready,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    onEdit: (String) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
        modifier = Modifier.padding(horizontal = spacing.space4),
    ) {
        Text(
            text = "Prodotto riconosciuto",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        // Testo riconosciuto (modificabile)
        if (uiState.isEditing) {
            OutlinedTextField(
                value = uiState.editedText,
                onValueChange = onEdit,
                label = { Text("Modifica testo", style = typography.caption) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = uiState.editedText,
                    style = typography.itemTitle,
                    color = colors.inkPrimary,
                    modifier = Modifier.padding(spacing.space3),
                )
            }
        }

        // Azioni: toggle Modifica/Annulla modifica in base a isEditing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            if (uiState.isEditing) {
                OutlinedButton(
                    onClick = { onEdit(uiState.recognizedText) }, // Ripristina testo originale
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(spacing.space1))
                    Text("Annulla modifica")
                }
            } else {
                OutlinedButton(
                    onClick = { onEdit(uiState.editedText) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(spacing.space1))
                    Text("Modifica")
                }
            }

            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(spacing.space1))
                Text("Riprova")
            }
        }

        Button(
            onClick = onConfirm,
            enabled = uiState.canConfirm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Conferma")
        }
    }
}

@Composable
private fun ErrorContent(
    uiState: VoiceBottomSheetUiState.Error,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
        modifier = Modifier.padding(horizontal = spacing.space4),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = colors.error,
            modifier = Modifier.size(48.dp),
        )

        Text(
            text = "Errore",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        Text(
            text = uiState.message,
            style = typography.itemMeta,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Annulla")
            }

            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
            ) {
                Text("Riprova")
            }
        }
    }
}