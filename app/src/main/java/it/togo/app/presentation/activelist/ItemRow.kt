package it.togo.app.presentation.activelist

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
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
import kotlin.math.roundToInt
import it.togo.app.domain.model.ShoppingItem
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
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    item: ShoppingItem,
    productName: String = "Prodotto #${item.productId}",
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

        Surface(
            modifier = Modifier
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
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { /* long press shows swipe hint */ },
                ),
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

/** Checkbox quadrata stile utility (48dp touch target, bordo 2dp) */
@Composable
private fun CheckboxUtility(
    checked: Boolean,
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
            .background(checkboxBgColor, tokens.radius)
            .border(borderWidth, checkboxBorderColor, tokens.radius)
            .clip(tokens.radius),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.Check,
                contentDescription = "Spuntato",
                tint = tokens.checkColor,
                modifier = Modifier.size(size * 0.55f),
            )
        }
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
    // Gestione quantity zero e formattazione senza decimali inutili
    val displayQuantity = if (quantity == 0.0) "0" else quantity.toString().trimEnd('0').trimEnd('.')
    val text = "$displayQuantity $unit"
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
