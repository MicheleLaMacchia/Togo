package it.togo.app.presentation.activelist

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors

/**
 * FAB microfono per cattura vocale (Story 4.x placeholder).
 *
 * Design System: VoiceFabTokens (64dp, bg accentAction, icona inkInverse, bordo borderCrisp).
 * Touch target ≥48dp garantito da size 64dp.
 */
@Composable
fun VoiceFab(
    onClick: () -> Unit,
) {
    val colors = togoColors()
    val tokens = componentTokens()

    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(tokens.voiceFab.size),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(tokens.voiceFab.radius),
        containerColor = tokens.voiceFab.background,
        contentColor = tokens.voiceFab.textColor,
        border = androidx.compose.ui.graphics.BorderStroke(
            width = tokens.voiceFab.borderWidth,
            color = tokens.voiceFab.borderColor,
        ),
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            focusedElevation = 8.dp,
        ),
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.Mic,
            contentDescription = "Aggiungi a voce",
            tint = tokens.voiceFab.textColor,
            modifier = Modifier
                .size(tokens.voiceFab.size * 0.5f)
                .align(Alignment.Center),
        )
    }
}