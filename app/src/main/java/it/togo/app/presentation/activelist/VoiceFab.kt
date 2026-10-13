package it.togo.app.presentation.activelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.domain.voice.VoiceRecognitionState
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Floating Action Button per avviare il riconoscimento vocale.
 *
 * Stati visivi:
 * - Idle: FAB con icona microfono, colore accent
 * - Listening: FAB con icona microfono, colore error, animazione pulse
 * - Processing: FAB disabilitato, icona microfono
 * - Result: FAB con icona check, colore success (transitorio)
 * - Error: FAB con icona mic-off, colore error
 *
 * Design System: 100% token TogoTheme.
 */
@Composable
fun VoiceFab(
    state: VoiceRecognitionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val isListening = state.isListening
    val isError = state.isError
    val isResult = state.isResult

    // Animazione pulse per Listening
    val pulseScale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1.0f,
        label = "pulse",
    )

    val fabColor = when {
        isListening -> colors.error
        isError -> colors.error
        isResult -> colors.success
        else -> colors.accent
    }

    val icon = when {
        isListening -> Icons.Filled.Mic
        isError -> Icons.Filled.MicOff
        isResult -> Icons.Filled.Mic
        else -> Icons.Filled.Mic
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        // Label stato
        AnimatedVisibility(
            visible = isListening || isError || isResult,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surface,
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = when {
                        isListening -> "Ascolto..."
                        isError -> "Errore"
                        isResult -> "Riconosciuto!"
                        else -> ""
                    },
                    style = typography.caption,
                    color = colors.inkPrimary,
                    modifier = Modifier.padding(horizontal = spacing.space3, vertical = spacing.space1),
                )
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onClick,
            containerColor = fabColor,
            contentColor = Color.White,
            modifier = modifier
                .size(56.dp)
                .scale(pulseScale),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isListening) "Interrompi ascolto" else "Avvia comando vocale",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Overlay schermo durante l'ascolto vocale.
 *
 * Mostra un indicatore visivo a schermo intero quando il microfono è attivo.
 */
@Composable
fun VoiceListeningOverlay(
    visible: Boolean,
    onCancel: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
                modifier = Modifier.padding(spacing.space6),
            ) {
                // Indicatore animato
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = colors.error.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Text(
                    text = "Ascolto in corso...",
                    style = typography.sectionHeader,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Dimmi un prodotto da aggiungere alla lista",
                    style = typography.itemMeta,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Surface(
                    onClick = onCancel,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = "Annulla",
                        style = typography.itemTitle,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = spacing.space4, vertical = spacing.space2),
                    )
                }
            }
        }
    }
}