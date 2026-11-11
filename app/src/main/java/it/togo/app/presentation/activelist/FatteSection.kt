package it.togo.app.presentation.activelist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.ActionItem
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Sezione collassabile "Fatte (N)" in fondo alla pagina "Da fare"
 * (CAP-3, storie 8.6 e 9.1).
 *
 * - Header collassabile "Fatte (N)" con espandi/collassa; default espansa
 *   alla prima composizione con N > 0.
 * - Se [items] è vuota la sezione non emette alcun nodo: niente "Fatte (0)"
 *   e nessun placeholder (Story 9.1).
 * - Animazione expand/collapse spring (stesso pattern di [PresiSection]).
 * - Righe spuntate: testo barrato, checkbox spuntata, badge urgenza e ambito
 *   attenuati, nell'ordine ricevuto (dalla più recente, deciso dal ViewModel).
 * - Nessun tap-edit ([rowActionDescription] = null) e nessun cestino.
 * - L'unica azione sulle righe è la checkbox: deselezionando, l'azione torna
 *   nel gruppo del suo ambito e della sua urgenza (spunta invertita).
 * - In fondo al contenuto espanso c'è il pulsante largo "Concludi attività"
 *   (storia 9.2). Apre [ConcludeActionsDialog]; la conferma invoca
 *   [onConclude], che la schermata inoltra al ViewModel.
 *
 * [onUncheck] invoca l'evento di spunta invertita ([UiEvent.CheckOff(id, false)]),
 * gestito dal [ActiveListViewModel] che riporta l'azione nel gruppo attivo.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FatteSection(
    items: List<ActionItem>,
    onUncheck: (String) -> Unit,
    onConclude: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    tokens: it.togo.app.ui.theme.TogoComponentTokens,
) {
    // Lo stato resta vivo anche mentre la sezione è vuota: se N torna a 0 e
    // poi ricompare, la sezione riparte espansa come richiesto ("default
    // espansa se N > 0"), senza ereditare un collapse precedente.
    var expanded by rememberSaveable { mutableStateOf(true) }
    // Dialog di conferma locale alla sezione: se N torna a 0 la sezione esce
    // dalla composizione e il dialog non deve riaprirsi alla ricomparsa.
    var showConcludeDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(items.isEmpty()) {
        if (items.isEmpty()) {
            expanded = true
            showConcludeDialog = false
        }
    }

    // Contratto 9.1: con N = 0 la sezione non emette alcun nodo.
    if (items.isEmpty()) return

    val count = items.size
    val stateText = if (expanded) "espansa" else "collassata"
    val toggleAction = if (expanded) "collassare" else "espandere"
    val headerDescription =
        "Fatte, $count ${if (count == 1) "azione" else "azioni"}. Doppio tocco per $toggleAction."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fatto_section")
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            )
        ,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        // Header collassabile
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space2, vertical = spacing.space2)
                .background(colors.surfaceSubtle)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = colors.borderHairline,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = strokeWidth,
                    )
                }
                .heightIn(min = 48.dp) // Touch target minimo 48dp
                .combinedClickable(
                    onClick = { expanded = !expanded },
                    role = Role.Button,
                )
                .semantics {
                    heading()
                    contentDescription = headerDescription
                    stateDescription = stateText
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Fatte ($count)",
                style = typography.sectionHeader,
                color = colors.inkPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded)
                    Icons.Filled.ExpandLess
                else
                    Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.inkSecondary,
                modifier = Modifier.size(24.dp),
            )
        }

        // Contenuto animato
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space2)
                    .padding(bottom = spacing.space3),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                items.forEach { action ->
                    ActionRow(
                        action = action,
                        // In "Fatte" il tap sulla riga non fa nulla: l'unica azione
                        // è la checkbox, che deseleziona e riporta l'azione in attivo.
                        onCheckChange = { checked ->
                            if (!checked) onUncheck(action.id)
                        },
                        onClick = null,
                        rowActionDescription = null,
                        badgesMuted = true,
                    )
                }

                // Pulsante largo in fondo alla sezione espansa (Story 9.2).
                Button(
                    onClick = { showConcludeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.space2)
                        .heightIn(min = spacing.touchTargetMin)
                        .testTag("conclude_actions_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentAction,
                        contentColor = colors.inkInverse,
                    ),
                    shape = tokens.bottomSheet.radiusTop,
                ) {
                    Text(
                        text = "Concludi attività",
                        style = typography.sectionHeader,
                    )
                }
            }
        }
    }

    if (showConcludeDialog) {
        ConcludeActionsDialog(
            count = count,
            onConfirm = {
                showConcludeDialog = false
                onConclude()
            },
            onDismiss = { showConcludeDialog = false },
        )
    }
}
