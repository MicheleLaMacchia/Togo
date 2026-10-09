package it.togo.app.presentation.activelist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.swipeable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
@Composable
fun ItemRow(
    item: ShoppingItem,
    onCheckChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    val isChecked = item.isChecked
    val alpha = if (isChecked) 0.45f else 1f
    val textColor = if (isChecked) colors.inkMuted else colors.inkPrimary
    val borderColor = if (isChecked) colors.accentSuccess else colors.borderCrisp
    val checkboxBorderColor = if (isChecked) colors.accentSuccess else colors.borderCrisp
    val checkboxBgColor = if (isChecked) colors.accentAction else Color.Transparent

    // Swipe-to-delete state
    val swipeState = remember {
        androidx.compose.foundation.gestures.SwipeableState(0f)
    }
    val swipeableSize = with(LocalDensity.current) { tokens.itemRow.paddingHorizontal + 80.dp }.toPx()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space2, vertical = spacing.space1)
            .swipeable(
                state = swipeState,
                anchors = mapOf(0f to 0f, -swipeableSize to -swipeableSize),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                resistance = { _ -> 10f },
            )
            .background(
                color = colors.accentSuccess,
                shape = RoundedCornerShape(tokens.itemRow.radius),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = spacing.space4),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                        contentDescription = "Elimina",
                        tint = colors.inkInverse,
                    )
                }
            }
            .clip(RoundedCornerShape(tokens.itemRow.radius))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { /* long press shows swipe hint */ },
            )
        ,
        shape = RoundedCornerShape(tokens.itemRow.radius),
        border = androidx.compose.ui.graphics.BorderStroke(tokens.itemRow.borderWidth, borderColor),
        color = tokens.itemRow.background.copy(alpha = alpha),
        elevation = 0.dp,
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
                    // Haptic feedback 15ms on check-off
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(15, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    }
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
                    text = item.name,
                    style = typography.itemName,
                    color = textColor.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                )

                // Meta opzionale: brand + variante + condition
                val metaParts = buildList {
                    if (item.brand.isNotBlank()) add(item.brand)
                    if (item.variant.isNotBlank()) add(item.variant)
                    if (item.condition.isNotBlank()) add(item.condition)
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
                unit = item.unit.shortName,
                tokens = tokens.quantityBadge,
                typography = typography,
                textColor = if (isChecked) colors.inkMuted else tokens.quantityBadge.textColor,
                backgroundColor = if (isChecked) colors.surfaceSubtle else tokens.quantityBadge.background,
            )
        }
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
            .background(checkboxBgColor, RoundedCornerShape(tokens.radius))
            .border(borderWidth, checkboxBorderColor, RoundedCornerShape(tokens.radius))
            .clip(RoundedCornerShape(tokens.radius)),
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
        shape = RoundedCornerShape(tokens.radius),
        color = backgroundColor,
        border = androidx.compose.ui.graphics.BorderStroke(1.dp, textColor),
    ) {
        Text(
            text = text,
            style = typography.itemMeta, // usa typography passata, non togoTypography() globale
            color = textColor,
        )
    }
}