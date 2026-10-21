package it.togo.app.presentation.activelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.domain.voice.VoiceRecognitionState
import it.togo.app.ui.theme.TogoColorScheme
import it.togo.app.ui.theme.TogoColorTokens
import it.togo.app.ui.theme.VOICE_OVERLAY_SCRIM_ALPHA
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
    val isProcessing = state is VoiceRecognitionState.Processing

    // Animazione pulse per Listening
    val pulseScale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1.0f,
        label = "pulse",
    )

    val fabColor = voiceFabContainerColor(state, colors)
    val fabContentColor = voiceFabContentColor(state, colors)

    val icon = when {
        isListening -> Icons.Filled.Mic
        isProcessing -> Icons.Filled.Mic
        isError -> Icons.Filled.MicOff
        isResult -> Icons.Filled.Mic
        else -> Icons.Filled.Mic
    }

    // Etichetta di stato: comunica anche l'azione disponibile (riprova / nuovo).
    val stateLabel = when {
        isListening -> "Ascolto..."
        isProcessing -> "Elaborazione in corso..."
        isError -> "Errore di riconoscimento. Tocca per riprovare."
        isResult -> "Prodotto riconosciuto. Tocca per aggiungere un altro prodotto."
        else -> ""
    }

    val fabContentDescription = when {
        isListening -> "Interrompi ascolto"
        isProcessing -> "Elaborazione in corso. Attendere."
        // La label visibile contiene già il messaggio completo: qui esponiamo
        // solo l'azione per evitare che TalkBack ripeta la stessa frase.
        isError -> "Riprova"
        isResult -> "Aggiungi un altro prodotto"
        else -> "Aggiungi prodotto con la voce. Pulsante."
    }

    // Durante l'elaborazione il FAB non avvia una nuova ascolto.
    val enabled = !isProcessing

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        // Label stato
        AnimatedVisibility(
            visible = isListening || isProcessing || isError || isResult,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                shape = CircleShape,
                color = colors.surface,
                shadowElevation = 2.dp,
            ) {
                Text(
                    text = stateLabel,
                    style = typography.caption,
                    color = colors.inkPrimary,
                    modifier = Modifier.padding(horizontal = spacing.space3, vertical = spacing.space1),
                )
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { if (enabled) onClick() },
            containerColor = if (enabled) fabColor else colors.surfaceSubtle,
            // inkMuted su surfaceSubtle scende sotto 3:1 in light; inkSecondary è leggibile.
            contentColor = if (enabled) fabContentColor else colors.inkSecondary,
            modifier = modifier
                .size(56.dp)
                .scale(pulseScale)
                .semantics { if (isProcessing) disabled() },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = fabContentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Colore di sfondo del FAB vocale per stato.
 *
 * Estratto per poter verificare nei test la coppia sfondo/contenuto senza
 * dipendere da un rendering pixel.
 */
internal fun voiceFabContainerColor(state: VoiceRecognitionState, colors: TogoColorScheme): Color {
    val isListening = state.isListening
    val isError = state.isError
    val isResult = state.isResult
    return when {
        isListening -> colors.error
        isError -> colors.error
        isResult -> colors.success
        // Deviazione documentata: l'idle resta accentHighlight (blu) e non
        // componentTokens().voiceFab.background (accentAction, cerchio scuro),
        // per non cambiare lo stato visivo consolidato della 6.1.
        else -> colors.accent
    }
}

/**
 * Colore del contenuto del FAB vocale.
 *
 * - accentHighlight cambia luminosità tra i temi → inkInverse, che si inverte.
 * - error/success sono condivisi e scuri in entrambi i temi → bianco fisso.
 */
internal fun voiceFabContentColor(state: VoiceRecognitionState, colors: TogoColorScheme): Color {
    val isListening = state.isListening
    val isError = state.isError
    val isResult = state.isResult
    return if (isListening || isError || isResult) {
        TogoColorTokens.OnAccent
    } else {
        colors.inkInverse
    }
}

/**
 * Overlay schermo durante l'ascolto vocale.
 *
 * Mostra un indicatore visivo a schermo intero quando il microfono è attivo.
 * Non è cablato nella UI corrente: il flusso reale usa [VoiceBottomSheet].
 * Resta come componente predisposto e viene coperto dai test di contrasto.
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
                .background(colors.scrim.copy(alpha = VOICE_OVERLAY_SCRIM_ALPHA)),
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
                            color = TogoColorTokens.OnAccent.copy(alpha = 0.2f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = TogoColorTokens.OnAccent,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Text(
                    text = "Ascolto in corso...",
                    style = typography.sectionHeader,
                    color = TogoColorTokens.LightInkInverse,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Dimmi un prodotto da aggiungere alla lista",
                    style = typography.itemMeta,
                    color = TogoColorTokens.LightInkInverse.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Surface(
                    onClick = onCancel,
                    shape = CircleShape,
                    color = Color.Transparent,
                    contentColor = TogoColorTokens.LightInkInverse,
                    border = BorderStroke(1.dp, TogoColorTokens.LightInkInverse),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        text = "Annulla",
                        style = typography.itemTitle,
                        color = TogoColorTokens.LightInkInverse,
                        modifier = Modifier.padding(horizontal = spacing.space4, vertical = spacing.space2),
                    )
                }
            }
        }
    }
}
