package it.togo.app.presentation.additem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.StandardUnit
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Selettore quantità (stepper) + dropdown unità compatibili.
 *
 * - Stepper quantità: - / valore / + (step 0.1 per unità continue, 1 per discrete)
 * - Dropdown unità: solo unità compatibili con il prodotto
 */
@Composable
fun QuantityUnitPicker(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    selectedUnit: StandardUnit?,
    onUnitChange: (StandardUnit) -> Unit,
    compatibleUnits: List<StandardUnit>,
    errorQuantity: String?,
    errorUnit: String?,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        // --- Quantità ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text("Quantità", style = typography.sectionHeader, color = togoColors().inkPrimary)

            // Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                // Pulsante meno
                Button(
                    onClick = { onQuantityChange(maxOf(0.1, quantity - getStep(quantity))) },
                    modifier = Modifier.size(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = togoColors().surfaceSubtle,
                        contentColor = togoColors().inkPrimary,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("−", style = typography.titleScreen, color = togoColors().inkPrimary)
                }

                // Valore quantità (modificabile)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.space3)
                        .background(
                            color = togoColors().surfaceCard,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = if (errorQuantity != null) togoColors().accentWarning else togoColors().borderHairline,
                            shape = RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.TextField(
                        value = quantity.toString().trimEnd('0').trimEnd('.'),
                        onValueChange = { text ->
                            text.toDoubleOrNull()?.let { onQuantityChange(maxOf(0.1, it)) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        singleLine = true,
                        textStyle = togoTypography().itemName,
                        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                            textColor = togoColors().inkPrimary,
                            cursorColor = togoColors().accentHighlight,
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = togoColors().accentWarning,
                        ),
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                        ),
                        label = { Text("Quantità", style = togoTypography().caption, color = togoColors().inkMuted) },
                    )
                }

                // Pulsante più
                Button(
                    onClick = { onQuantityChange(minOf(9999.0, quantity + getStep(quantity))) },
                    modifier = Modifier.size(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = togoColors().surfaceSubtle,
                        contentColor = togoColors().inkPrimary,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("+", style = typography.titleScreen, color = togoColors().inkPrimary)
                }
            }

            if (errorQuantity != null) {
                Text(errorQuantity, style = typography.caption, color = togoColors().accentWarning)
            }
        }

        // --- Unità ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text("Unità", style = typography.sectionHeader, color = togoColors().inkPrimary)

            // Dropdown unità
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = togoColors().surfaceCard,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = if (errorUnit != null) togoColors().accentWarning else togoColors().borderHairline,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                androidx.compose.material3.DropdownMenu(
                    expanded = false, // simplified - would need state for full dropdown
                    onDismissRequest = {},
                ) {
                    // Per semplicità: chip orizzontali scrollabili
                }

                // Chip orizzontali per unità
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.foundation.lazy.items(compatibleUnits) { unit ->
                        UnitChip(
                            unit = unit,
                            selected = selectedUnit == unit,
                            onClick = { onUnitChange(unit) },
                            error = errorUnit != null,
                        )
                    }
                }
            }

            if (errorUnit != null) {
                Text(errorUnit, style = typography.caption, color = togoColors().accentWarning)
            }
        }
    }
}

/** Chip per selezione unità */
@Composable
private fun UnitChip(
    unit: StandardUnit,
    selected: Boolean,
    onClick: () -> Unit,
    error: Boolean,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .background(
                color = if (selected) colors.inkPrimary else colors.surfaceCard,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (error && !selected) colors.accentWarning else colors.borderHairline,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = unit.displayName,
            style = typography.itemMeta.copy(color = if (selected) colors.inkInverse else colors.inkPrimary),
        )
    }
}

/** Determina step per unità */
private fun getStep(quantity: Double): Double {
    return if (quantity < 10) 0.1 else 1.0
}