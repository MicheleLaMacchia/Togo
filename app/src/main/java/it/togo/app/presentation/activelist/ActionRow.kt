package it.togo.app.presentation.activelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.ui.theme.ActionRowTokens
import it.togo.app.ui.theme.TogoColorScheme
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import java.util.Locale

/**
 * Riga di un'azione nella pagina "Da fare" (Story 8.4, DESIGN.md §7.7).
 *
 * Struttura:
 * - checkbox a sinistra, con area di tocco da 48dp;
 * - testo al centro, senza troncamento: la riga cresce con Dynamic Type;
 * - sotto il testo i badge urgenza e ambito, sempre leggibili come testo;
 * - accento verticale interno a sinistra con il colore dell'urgenza.
 *
 * Le azioni non sono prodotti: qui non esistono quantità, unità, marca o
 * variante, quindi nessun badge quantità viene disegnato.
 *
 * [onClick] è opzionale: la modifica (Story 8.7) e lo swipe cestino (Story 8.8)
 * verranno collegati in seguito, senza cambiare questa struttura.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ActionRow(
    action: ActionItem,
    onCheckChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    rowActionDescription: String? = null,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()
    val actionTokens = tokens.actionRow
    val context = LocalContext.current

    val isChecked = action.isChecked
    val stateText = if (isChecked) "spuntata" else "non spuntata"

    // La riga annuncia dati e stato; la checkbox resta un controllo separato
    // con la propria azione di spunta/deselezione (come per ItemRow).
    val rowDescription = buildString {
        append(action.text)
        append(", ")
        append(action.urgency.label)
        append(", ")
        append(action.scope.label)
        append(", ")
        append(stateText)
        append(".")
        rowActionDescription?.let { append(" $it") }
    }
    val checkboxDescription = if (isChecked) {
        "Deseleziona ${action.text}."
    } else {
        "Spunta ${action.text}."
    }

    val urgencyColors = action.urgency.badgeColors(colors)
    val accentColor = action.urgency.accentColor(colors)
    val borderColor = if (isChecked) actionTokens.checkedBorderColor else actionTokens.borderColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space1)
            .heightIn(min = actionTokens.minHeight)
            .clip(actionTokens.radius)
            .background(actionTokens.background)
            .border(actionTokens.borderWidth, borderColor, actionTokens.radius)
            .drawBehind {
                // Accento discreto sul bordo interno sinistro. Il clip della
                // card lo costringe dentro gli angoli arrotondati.
                drawRect(
                    color = accentColor,
                    size = Size(
                        width = actionTokens.accentWidth.toPx(),
                        height = size.height,
                    ),
                )
            }
            // Lo swipe orizzontale su una riga non deve cambiare pagina
            // (CAP-1). La rivelazione del cestino arriva con la storia 8.8:
            // fino ad allora il gesto viene solo riservato alla riga.
            .pointerInput(action.id) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                }
            }
            .testTag("action_row"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = actionTokens.accentWidth +
                        actionTokens.paddingHorizontal +
                        tokens.checkboxUtility.size +
                        spacing.space3,
                    end = actionTokens.paddingHorizontal,
                    top = actionTokens.paddingVertical,
                    bottom = actionTokens.paddingVertical,
                )
                .then(
                    if (onClick != null) {
                        Modifier.combinedClickable(onClick = { onClick?.invoke() })
                    } else {
                        Modifier
                    }
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = rowDescription
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = action.text,
                    style = typography.itemName,
                    color = colors.inkPrimary,
                    textDecoration = if (isChecked) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    ActionBadge(
                        text = action.urgency.label,
                        background = urgencyColors.background,
                        contentColor = urgencyColors.content,
                        borderColor = urgencyColors.border,
                        tokens = actionTokens,
                        tag = "action_urgency_badge",
                    )
                    ActionBadge(
                        text = action.scope.label,
                        background = actionTokens.areaBadgeBackground,
                        contentColor = actionTokens.areaBadgeContent,
                        borderColor = actionTokens.areaBadgeBorder,
                        tokens = actionTokens,
                        tag = "action_scope_badge",
                    )
                }
            }
        }

        // Checkbox sibling: TalkBack la espone come controllo separato dalla
        // riga, con stato checked/unchecked e azione di toggle.
        CheckboxUtility(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = actionTokens.accentWidth + actionTokens.paddingHorizontal)
                .testTag("action_checkbox"),
            checked = isChecked,
            contentDescription = checkboxDescription,
            onCheckedChange = { newChecked ->
                performShortHapticFeedback(context)
                onCheckChange(newChecked)
            },
            tokens = tokens.checkboxUtility,
            checkboxBorderColor = if (isChecked) colors.accentSuccess else actionTokens.borderColor,
            checkboxBgColor = if (isChecked) colors.accentAction else Color.Transparent,
        )
    }
}

/** Badge compatto con testo esplicito; il colore è solo un rinforzo. */
@Composable
private fun ActionBadge(
    text: String,
    background: Color,
    contentColor: Color,
    borderColor: Color,
    tokens: ActionRowTokens,
    tag: String,
) {
    val typography = togoTypography()

    Box(
        modifier = Modifier
            .clip(tokens.badgeRadius)
            .background(background)
            .border(tokens.badgeBorderWidth, borderColor, tokens.badgeRadius)
            .padding(
                horizontal = tokens.badgePaddingHorizontal,
                vertical = tokens.badgePaddingVertical,
            ),
    ) {
        Text(
            text = text.uppercase(Locale.ROOT),
            style = typography.badgeLabel,
            color = contentColor,
            modifier = Modifier.testTag(tag),
        )
    }
}

private data class UrgencyBadgeColors(
    val background: Color,
    val content: Color,
    val border: Color,
)

private fun ActionUrgency.badgeColors(colors: TogoColorScheme): UrgencyBadgeColors =
    when (this) {
        ActionUrgency.URGENTE -> UrgencyBadgeColors(
            background = colors.urgencyUrgentBg,
            content = colors.urgencyUrgentInk,
            border = colors.urgencyUrgentBorder,
        )

        ActionUrgency.A_TEMPO_PERSO -> UrgencyBadgeColors(
            background = colors.urgencyRelaxedBg,
            content = colors.urgencyRelaxedInk,
            border = colors.urgencyRelaxedBorder,
        )

        ActionUrgency.ECCEZIONALE -> UrgencyBadgeColors(
            background = colors.urgencyExceptionalBg,
            content = colors.urgencyExceptionalInk,
            border = colors.urgencyExceptionalBorder,
        )
    }

/** Colore dell'accento verticale: la sfumatura di bordo dell'urgenza. */
private fun ActionUrgency.accentColor(colors: TogoColorScheme): Color =
    when (this) {
        ActionUrgency.URGENTE -> colors.urgencyUrgentBorder
        ActionUrgency.A_TEMPO_PERSO -> colors.urgencyRelaxedBorder
        ActionUrgency.ECCEZIONALE -> colors.urgencyExceptionalBorder
    }
