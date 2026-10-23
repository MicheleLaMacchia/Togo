package it.togo.app.presentation.activelist

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Riga prodotto nella lista attiva.
 *
 * Struttura (3 zone orizzontali):
 * 1. Checkbox compatta (area di tocco 48dp)
 * 2. Nome prodotto + meta opzionale (brand/variante)
 * 3. Quantità allineata a destra, senza badge
 *
 * Stati:
 * - Attivo: bordo crisp, testo inkPrimary
 * - Checked: testo barrato inkPrimary, bordo/checkbox verde
 *
 * Dynamic Type:
 * - Il nome prodotto non viene mai troncato (niente maxLines/ellissi): la riga
 *   cresce in altezza e la quantità resta un fratello del nome nella Row, mai
 *   sovrapposta. `minHeight` garantisce il target da 48dp anche a font piccolo.
 *
 * Accessibilità:
 * - Attivo: la riga è cliccabile → modifica; la checkbox → spunta.
 * - In "Presi" (rowActionDescription == null): la riga non è cliccabile, i
 *   dettagli restano annunciati e la checkbox deseleziona.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    item: ShoppingItem,
    productName: String = "Prodotto #${item.productId}",
    categoryLabel: String? = null,
    rowActionDescription: String? = "Doppio tocco per modificare.",
    onCheckChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Larghezza dell'azione rivelata dallo swipe (touch target ≥48dp).
    val revealWidth = 88.dp
    val revealWidthPx = with(LocalDensity.current) { revealWidth.toPx() }
    val settleThresholdPx = revealWidthPx / 2f

    var horizontalDrag by remember(item.id) { mutableStateOf(0f) }
    var isRevealed by remember(item.id) { mutableStateOf(false) }
    var settleJob by remember(item.id) { mutableStateOf<Job?>(null) }

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

    val isChecked = item.isChecked
    // Il testo barrato resta inkPrimary: inkMuted/inkSecondary non raggiungono
    // il rapporto 7:1 richiesto su entrambi i temi.
    val textColor = colors.inkPrimary
    val borderColor = if (isChecked) colors.accentSuccess else colors.borderCrisp
    val checkboxBorderColor = if (isChecked) colors.accentSuccess else colors.borderCrisp
    val checkboxBgColor = if (isChecked) colors.accentAction else Color.Transparent

    // --- Semantica accessibile (TalkBack) ---
    // La riga annuncia dati + stato; la checkbox annuncia l'azione di spunta.
    // Le due azioni restano separate: tap sulla riga = modifica, tap sulla
    // checkbox = spunta/deseleziona.
    // La spunta è comunicata dalla semantica toggleable (Role.Checkbox), non
    // ripetuta nella descrizione.
    val quantityText = quantityLabel(item.quantity, item.unit)
    val categoryText = categoryLabel?.takeIf { it.isNotBlank() } ?: "Categoria non assegnata"
    val stateText = if (isChecked) "selezionato" else "non selezionato"
    val rowDescription = buildString {
        append("$productName, $quantityText, $categoryText, $stateText.")
        rowActionDescription?.let { append(" $it") }
    }
    val checkboxDescription = if (isChecked) {
        "Deseleziona $productName."
    } else {
        "Spunta $productName."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space1)
            .clip(tokens.itemRow.radius)
            .background(tokens.itemRow.swipeDeleteBackground),
    ) {
        // Azione dietro la card: resta scoperta quando la riga è spostata a
        // sinistra. È un vero bottone, apribile con TalkBack, non solo un'icona.
        if (rowActionDescription != null) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier
                        .width(revealWidth)
                        .fillMaxHeight(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Elimina $productName",
                        tint = tokens.itemRow.swipeDeleteIconColor,
                    )
                }
            }
        }

        val baseModifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(horizontalDrag.roundToInt(), 0) }

        // Le righe "Presi" (rowActionDescription == null) non hanno azione di
        // tap né di swipe: niente combinedClickable e niente gesture di
        // eliminazione, così TalkBack non espone azioni vuote. I dettagli
        // accessibili restano (mergeDescendants) e la checkbox continua a
        // funzionare. Le righe attive restano cliccabili → modifica e
        // trascinabili → rivelano il bottone elimina (poi tap per confermare).
        val swipeModifier = if (rowActionDescription != null) {
            baseModifier.pointerInput(item.id, onDelete) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        // Cancella il timer di auto-chiusura mentre l'utente
                        // trascina: la riga non deve richiudersi sotto il dito.
                        settleJob?.cancel()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        horizontalDrag = (horizontalDrag + dragAmount)
                            .coerceIn(-revealWidthPx, 0f)
                    },
                    onDragEnd = {
                        // Lo swipe non elimina più: apre (e la riga resta
                        // aperta) oppure richiude se non ha superato la metà.
                        settleTo(
                            if (horizontalDrag <= -settleThresholdPx) -revealWidthPx else 0f
                        )
                    },
                    onDragCancel = { settleTo(0f) },
                )
            }
        } else {
            baseModifier
        }

        val rowClickModifier = if (rowActionDescription != null) {
            Modifier.combinedClickable(
                onClick = {
                    // Con la riga aperta il tap la richiude invece di aprire
                    // l'editor: evita tocchi accidentali sull'azione scoperta.
                    if (isRevealed) settleTo(0f) else onClick()
                },
            )
        } else {
            Modifier
        }

        // Il contenitore disegna card e bordo; la Row cliccabile e la
        // checkbox sono sibling, così la semantica non viene fusa.
        // heightIn(min) garantisce il target da 48dp anche a font piccolo.
        Box(
            modifier = swipeModifier
                .testTag("item_row")
                .heightIn(min = tokens.itemRow.minHeight)
                .clip(tokens.itemRow.radius)
                .background(tokens.itemRow.background)
                .border(tokens.itemRow.borderWidth, borderColor, tokens.itemRow.radius),
        ) {
            Row(
                modifier = rowClickModifier
                    .semantics(mergeDescendants = true) {
                        contentDescription = rowDescription
                    }
                    .fillMaxWidth()
                    .padding(
                        // Spazio per la checkbox sibling, dentro la card.
                        start = tokens.itemRow.paddingHorizontal +
                            tokens.checkboxUtility.size + spacing.space3,
                        end = tokens.itemRow.paddingHorizontal,
                        top = tokens.itemRow.paddingVertical,
                        bottom = tokens.itemRow.paddingVertical,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                // Centro: nome + meta (brand/variante)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = productName,
                        style = typography.itemName,
                        color = textColor,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    )

                    // Meta opzionale: brand + variante + condition
                    val metaParts = buildList {
                        item.brand?.takeIf { it.isNotBlank() }?.let(::add)
                        item.variant?.takeIf { it.isNotBlank() }?.let(::add)
                        item.condition?.takeIf { it.isNotBlank() }?.let(::add)
                    }
                    if (metaParts.isNotEmpty()) {
                        Text(
                            text = metaParts.joinToString(" · "),
                            style = typography.caption,
                            color = colors.inkSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }

                // Destra: quantità senza badge, allineata al bordo della riga.
                QuantityText(
                    quantity = item.quantity,
                    unit = item.unit.displayName,
                    typography = typography,
                    color = colors.inkPrimary,
                )
            }

            // Checkbox sibling: non è discendente della Row cliccabile, quindi
            // TalkBack la espone come controllo separato con il proprio toggle.
            CheckboxUtility(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = tokens.itemRow.paddingHorizontal),
                checked = isChecked,
                contentDescription = checkboxDescription,
                onCheckedChange = { newChecked ->
                    performShortHapticFeedback(context)
                    onCheckChange(newChecked)
                },
                tokens = tokens.checkboxUtility,
                checkboxBorderColor = checkboxBorderColor,
                checkboxBgColor = checkboxBgColor,
            )
        }
    }
}

internal fun performShortHapticFeedback(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(15)
    }
}

/**
 * Checkbox compatta con area di tocco accessibile da 48dp.
 *
 * Il riquadro visivo è più piccolo (`visualSize` del token) ma l'area
 * interattiva resta 48dp. È un controllo accessibile vero e proprio:
 * [Modifier.toggleable] espone ruolo `Checkbox`, stato di spunta e azione di
 * toggle; la descrizione contiene nome prodotto e azione
 * ("Spunta"/"Deseleziona"). L'icona interna è decorativa
 * (`contentDescription = null`) per evitare annunci duplicati.
 */
@Composable
private fun CheckboxUtility(
    modifier: Modifier = Modifier,
    checked: Boolean,
    contentDescription: String,
    onCheckedChange: (Boolean) -> Unit,
    tokens: it.togo.app.ui.theme.CheckboxUtilityTokens,
    checkboxBorderColor: Color,
    checkboxBgColor: Color,
) {
    val touchTargetSize = tokens.size
    val visualSize = tokens.visualSize
    val borderWidth = tokens.borderWidth

    Box(
        modifier = modifier
            .size(touchTargetSize)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .testTag("checkbox_visual")
                .background(checkboxBgColor, tokens.radius)
                .border(borderWidth, checkboxBorderColor, tokens.radius)
                .clip(tokens.radius),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = tokens.checkColor,
                    modifier = Modifier.size(visualSize * 0.55f),
                )
            }
        }
    }
}

/**
 * Etichetta parlata di quantità + unità.
 *
 * Numeri in formato italiano (separatore decimale virgola, nessuna notazione
 * scientifica) e nomi unità completi al singolare/plurale.
 */
internal fun quantityLabel(quantity: Double, unit: StandardUnit): String {
    return "${formatQuantity(quantity)} ${unit.italianName(quantity)}"
}

/** Formatta una quantità in modo stabile e leggibile (es. "1500,5", "2"). */
internal fun formatQuantity(quantity: Double): String {
    if (quantity.isNaN() || quantity.isInfinite()) return "0"
    val value = java.math.BigDecimal.valueOf(quantity)
        .stripTrailingZeros()
    return if (value.signum() == 0) "0" else value.toPlainString().replace('.', ',')
}

/** Nome italiano completo dell'unità, al singolare quando la quantità è 1. */
internal fun StandardUnit.italianName(quantity: Double): String {
    val singular = quantity == 1.0
    return when (this) {
        StandardUnit.GRAM -> if (singular) "grammo" else "grammi"
        StandardUnit.KILOGRAM -> if (singular) "chilogrammo" else "chilogrammi"
        StandardUnit.MILLILITER -> if (singular) "millilitro" else "millilitri"
        StandardUnit.LITER -> if (singular) "litro" else "litri"
        StandardUnit.PIECE -> if (singular) "pezzo" else "pezzi"
        StandardUnit.PACKAGE -> if (singular) "confezione" else "confezioni"
        StandardUnit.BOTTLE -> if (singular) "bottiglia" else "bottiglie"
        StandardUnit.JAR -> if (singular) "vasetto" else "vasetti"
        StandardUnit.CAN -> if (singular) "lattina" else "lattine"
        StandardUnit.BOX -> if (singular) "scatola" else "scatole"
        StandardUnit.ROLL -> if (singular) "rotolo" else "rotoli"
        StandardUnit.POT -> if (singular) "pentola" else "pentole"
    }
}

/** Quantità + unità, senza sfondo, allineata al bordo destro della riga. */
@Composable
private fun QuantityText(
    quantity: Double,
    unit: String,
    typography: it.togo.app.ui.theme.TogoTypography,
    color: Color,
) {
    // Condivide il formato robusto con TalkBack: no notazione scientifica o
    // troncamento errato degli zeri nelle quantità grandi.
    Text(
        text = "${formatQuantity(quantity)} $unit",
        style = typography.itemMeta,
        color = color,
    )
}
