package it.togo.app.presentation.activelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Stato vuoto Lista Attiva.
 *
 * Messaggio accogliente + due azioni:
 * - "Voce" → inserimento manuale (Story 2.2)
 * - "Microfono" → cattura vocale (Story 4.x)
 */
@Composable
fun EmptyState(
    onAddManual: () -> Unit,
    onVoice: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icona carrello
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(colors.surfaceSubtle, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.ShoppingCart,
                contentDescription = "Carrello vuoto",
                tint = colors.inkMuted,
                modifier = Modifier.size(48.dp),
            )
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space4))

        Text(
            text = "La tua lista è vuota",
            style = typography.titleScreen,
            color = colors.inkPrimary,
            textAlign = TextAlign.Center,
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space2))

        Text(
            text = "Tocca il microfono o premi + per aggiungere prodotti",
            style = typography.itemMeta,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space5))

        // Pulsanti azione
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            // Pulsante "Voce" (primario)
            androidx.compose.material3.Button(
                onClick = onAddManual,
                modifier = Modifier.width(240.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.inkPrimary,
                    contentColor = colors.inkInverse,
                ),
            ) {
                Text("+ Voce", style = typography.sectionHeader.copy(color = colors.inkInverse))
            }

            // Pulsante microfono (secondario, icona)
            IconButton(
                onClick = onVoice,
                modifier = Modifier.size(56.dp),
                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                    containerColor = colors.surfaceSubtle,
                    contentColor = colors.inkPrimary,
                ),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Mic,
                    contentDescription = "Aggiungi a voce",
                    tint = colors.inkPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}