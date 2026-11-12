package it.togo.app.presentation.activelist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.ui.theme.ActionRowTokens
import it.togo.app.ui.theme.TogoColorScheme
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import java.util.Locale
import kotlin.math.roundToInt

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
 * [onClick] apre l'editor di modifica (Story 8.7). [onDelete] espone lo swipe
 * cestino e l'azione accessibile "Elimina" con annulla (Story 8.8).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ActionRow(
    action: ActionItem,
    onCheckChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** Eliminazione azione con cestino/undo (Story 8.8). */
    onDelete: (() -> Unit)? = null,
    rowActionDescription: String? = null,
    /** Quando vera (sezione "Fatte"), i badge urgenza/ambito sono resi attenuati. */
    badgesMuted: Boolean = false,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()
    val actionTokens = tokens.actionRow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Larghezza dell'azione rivelata dallo swipe (touch target ≥48dp).
    val revealWidth = 88.dp
    val revealWidthPx = with(LocalDensity.current) { revealWidth.toPx() }
    val settleThresholdPx = revealWidthPx / 2f

    var horizontalDrag by remember(action.id) { mutableStateOf(0f) }
    var isRevealed by remember(action.id) { mutableStateOf(false) }
    var settleJob by remember(action.id) { mutableStateOf<Job?>(null) }

    /**
     * Porta la riga alla posizione [target] (0 = chiusa, -revealWidth = aperta)
     * e, se aperta, la richiude da sola dopo 3 secondi. Il timer vive nello
     * stesso job dell'animazione: un nuovo drag lo cancella (onDragStart), così
     * la riga non si richiude mentre l'utente la sta trascinando.
     */
    fun settleTo(target: Float) {
        settleJob?.cancel()
        isRevealed = target < 0f
        settleJob = scope.launch {
            animate(
                initialValue = horizontalDrag,
                targetValue = target,
                animationSpec = tween(durationMillis = 180),
            ) { value, _ -> horizontalDrag = value }
            horizontalDrag = target

            if (target < 0f) {
                delay(3000)
                animate(
                    initialValue = horizontalDrag,
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180),
                ) { value, _ -> horizontalDrag = value }
                horizontalDrag = 0f
                isRevealed = false
            }
        }
    }

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

    // Badge attenuati nella sezione Fatte: solo testo, colore neutro, senza
    // rilevare l'urgenza visivamente (l'azione è già conclusa).
    val badgeBackground = if (badgesMuted) colors.surfaceSubtle else urgencyColors.background
    val badgeContent = if (badgesMuted) colors.inkMuted else urgencyColors.content
    val badgeBorder = if (badgesMuted) colors.borderHairline else urgencyColors.border

    val scopeBadgeBackground = if (badgesMuted) colors.surfaceSubtle else actionTokens.areaBadgeBackground
    val scopeBadgeContent = if (badgesMuted) colors.inkMuted else actionTokens.areaBadgeContent
    val scopeBadgeBorder = if (badgesMuted) colors.borderHairline else actionTokens.areaBadgeBorder

    val canDelete = onDelete != null && !action.isChecked

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space1)
            .heightIn(min = actionTokens.minHeight)
            .clip(actionTokens.radius)
            .background(actionTokens.background)
            .testTag("action_row"),
    ) {
        // Cestino rivelato dallo swipe: resta dietro la card e viene scoperto
        // quando la riga si sposta a sinistra. Touch target ≥48dp.
        if (canDelete) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    // Anche l'area cestino esposta fa parte della riga: il suo
                    // drag non deve propagarsi al pager.
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { },
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(
                    onClick = { onDelete?.invoke() },
                    modifier = Modifier
                        .width(revealWidth)
                        .fillMaxHeight()
                        .background(colors.error, shape = actionTokens.radius),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Elimina ${action.text}",
                        tint = colors.inkInverse,
                    )
                }
            }
        }

        // Contenuto della riga: si sposta orizzontalmente per rivelare il
        // cestino. Il drag orizzontale viene consumato qui, quindi il pager
        // non cambia pagina (AD-8, CAP-1).
        val contentModifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(horizontalDrag.roundToInt(), 0) }
            .heightIn(min = actionTokens.minHeight)
            .clip(actionTokens.radius)
            .background(actionTokens.background)
            .border(actionTokens.borderWidth, borderColor, actionTokens.radius)
            .drawBehind {
                drawRect(
                    color = accentColor,
                    size = Size(
                        width = actionTokens.accentWidth.toPx(),
                        height = size.height,
                    ),
                )
            }

        val swipeModifier = if (canDelete) {
            contentModifier.draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { dragAmount ->
                    horizontalDrag = (horizontalDrag + dragAmount)
                        .coerceIn(-revealWidthPx, 0f)
                },
                onDragStarted = {
                    settleJob?.cancel()
                },
                onDragStopped = {
                    settleTo(
                        if (horizontalDrag <= -settleThresholdPx) -revealWidthPx else 0f
                    )
                },
            )
        } else {
            contentModifier.draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { },
            )
        }

        Box(
            modifier = swipeModifier
                .testTag("action_row_swipe_content")
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
                        Modifier.combinedClickable(
                            onClick = {
                                // Con la riga aperta il tap la richiude invece
                                // di aprire l'editor.
                                if (isRevealed) settleTo(0f) else onClick?.invoke()
                            },
                        )
                    } else {
                        Modifier
                    }
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = rowDescription + (if (onDelete != null) ". Tocca due volte per modificare. Azione disponibile: Elimina." else ".")
                }
                .testTag("action_row_foreground"),
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
                        background = badgeBackground,
                        contentColor = badgeContent,
                        borderColor = badgeBorder,
                        tokens = actionTokens,
                        tag = "action_urgency_badge",
                    )
                    ActionBadge(
                        text = action.scope.label,
                        background = scopeBadgeBackground,
                        contentColor = scopeBadgeContent,
                        borderColor = scopeBadgeBorder,
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
