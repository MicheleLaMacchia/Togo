package it.togo.app.presentation.activelist

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
 * 1. Checkbox quadrata (touch target 48dp)
 * 2. Nome prodotto + meta opzionale (brand/variante)
 * 3. Badge quantità
 *
 * Stati:
 * - Attivo: bordo crisp, testo inkPrimary, badge scuro
 * - Checked: testo barrato, opacità 45%, inkMuted, checkbox riempita verde/nero
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
    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    val isChecked = item.isChecked
    val alpha = if (isChecked) 0.45f else 1f
    val textColor = if (isChecked) colors.inkMuted else colors.inkPrimary
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

    var horizontalDrag by remember(item.id) { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space1)
            .clip(tokens.itemRow.radius)
            .background(tokens.itemRow.swipeDeleteBackground),
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Elimina",
            tint = tokens.itemRow.swipeDeleteIconColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = spacing.space4),
        )

        val baseModifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(horizontalDrag.roundToInt(), 0) }
            .pointerInput(item.id, onDelete) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        horizontalDrag = (horizontalDrag + dragAmount).coerceIn(-swipeThresholdPx, 0f)
                    },
                    onDragEnd = {
                        if (horizontalDrag <= -swipeThresholdPx) onDelete()
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f },
                )
            }

        // Le righe "Presi" (rowActionDescription == null) non hanno azione di
        // tap: si rimuove combinedClickable per non esporre un'azione vuota, ma
        // i dettagli accessibili restano (mergeDescendants) e la checkbox
        // continua a funzionare. Le righe attive restano cliccabili → modifica.
        val interactiveModifier = if (rowActionDescription != null) {
            baseModifier.combinedClickable(
                onClick = onClick,
                onLongClick = { /* long press shows swipe hint */ },
            )
        } else {
            baseModifier
        }

        Surface(
            modifier = interactiveModifier.semantics(mergeDescendants = true) {
                contentDescription = rowDescription
            },
            shape = tokens.itemRow.radius,
            border = BorderStroke(tokens.itemRow.borderWidth, borderColor),
            color = tokens.itemRow.background.copy(alpha = alpha),
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = tokens.itemRow.paddingHorizontal,
                        vertical = tokens.itemRow.paddingVertical,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                // 1. Checkbox quadrata utility (48dp touch target)
                CheckboxUtility(
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

                // 2. Centro: nome + meta (brand/variante)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = productName,
                        style = typography.itemName,
                        color = textColor.copy(alpha = alpha),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                            color = colors.inkSecondary.copy(alpha = alpha),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }

                // 3. Destra: badge quantità
                QuantityBadge(
                    quantity = item.quantity,
                    unit = item.unit.displayName,
                    tokens = tokens.quantityBadge,
                    typography = typography,
                    textColor = if (isChecked) colors.inkMuted else tokens.quantityBadge.textColor,
                    backgroundColor = if (isChecked) colors.surfaceSubtle else tokens.quantityBadge.background,
                )
            }
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
 * Checkbox quadrata stile utility (48dp touch target, bordo 2dp).
 *
 * È un controllo accessibile vero e proprio: [Modifier.toggleable] espone
 * ruolo `Checkbox`, stato di spunta e azione di toggle; la descrizione
 * contiene nome prodotto e azione ("Spunta"/"Deseleziona"). L'icona interna è
 * decorativa (`contentDescription = null`) per evitare annunci duplicati.
 */
@Composable
private fun CheckboxUtility(
    checked: Boolean,
    contentDescription: String,
    onCheckedChange: (Boolean) -> Unit,
    tokens: it.togo.app.ui.theme.CheckboxUtilityTokens,
    checkboxBorderColor: Color,
    checkboxBgColor: Color,
) {
    val size = tokens.size // 48dp per touch target
    val borderWidth = tokens.borderWidth

    Box(
        modifier = Modifier
            .size(size)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
            .background(checkboxBgColor, tokens.radius)
            .border(borderWidth, checkboxBorderColor, tokens.radius)
            .clip(tokens.radius),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = tokens.checkColor,
                modifier = Modifier.size(size * 0.55f),
            )
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

/** Badge quantità (pill compatta) */
@Composable
private fun QuantityBadge(
    quantity: Double,
    unit: String,
    tokens: it.togo.app.ui.theme.QuantityBadgeTokens,
    typography: it.togo.app.ui.theme.TogoTypography,
    textColor: Color,
    backgroundColor: Color,
) {
    // Condivide il formato robusto con TalkBack: no notazione scientifica o
    // troncamento errato degli zeri nelle quantità grandi.
    val text = "${formatQuantity(quantity)} $unit"
    Surface(
        modifier = Modifier
            .padding(horizontal = tokens.paddingHorizontal, vertical = tokens.paddingVertical)
            .heightIn(min = 24.dp)
        ,
        shape = tokens.radius,
        color = backgroundColor,
        border = BorderStroke(1.dp, tokens.borderColor),
    ) {
        Text(
            text = text,
            style = typography.itemMeta, // usa typography passata, non togoTypography() globale
            color = textColor,
        )
    }
}
