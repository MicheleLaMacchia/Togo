package it.togo.app.presentation.additem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.StandardUnit
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Campo quantità modificabile con tastiera + selettore delle unità compatibili.
 *
 * - La quantità viene inserita direttamente dall'utente nel campo di testo.
 * - Le unità mostrate sono solo quelle compatibili con il prodotto.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuantityUnitPicker(
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    selectedUnit: StandardUnit?,
    onUnitChange: (StandardUnit) -> Unit,
    compatibleUnits: List<StandardUnit>,
    errorQuantity: String?,
    errorUnit: String?,
    onClearQuantityAndUnit: () -> Unit,
) {
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = "La quantità e l'unità sono opzionali.",
            style = typography.caption,
            color = togoColors().inkSecondary,
        )

        // --- Quantità ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text("Quantità", style = typography.sectionHeader, color = togoColors().inkPrimary)

            // Campo quantità modificabile direttamente con la tastiera.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    value = quantityText,
                    onValueChange = onQuantityChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    singleLine = true,
                    textStyle = togoTypography().itemName,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedTextColor = togoColors().inkPrimary,
                        unfocusedTextColor = togoColors().inkPrimary,
                        cursorColor = togoColors().accentHighlight,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = togoColors().accentWarning,
                        errorContainerColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                    ),
                    label = { Text("Quantità", style = togoTypography().caption, color = togoColors().inkSecondary) },
                )
            }

            if (errorQuantity != null) {
                Text(errorQuantity, style = typography.caption, color = togoColors().warningInk)
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
                    items(compatibleUnits) { unit ->
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
                Text(errorUnit, style = typography.caption, color = togoColors().warningInk)
            }
        }

        // Azzera entrambi i campi: la voce verrà salvata senza quantità/unità.
        TextButton(
            onClick = onClearQuantityAndUnit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.touchTargetMin),
            colors = ButtonDefaults.textButtonColors(
                contentColor = togoColors().accentAction,
            ),
        ) {
            Text(
                text = "Azzera quantità e unità",
                style = typography.itemMeta,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Chip per selezione unità */
@OptIn(ExperimentalFoundationApi::class)
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
            .defaultMinSize(
                minWidth = spacing.touchTargetMin,
                minHeight = spacing.touchTargetMin,
            )
            .combinedClickable(onClick = onClick)
            .background(
                color = if (selected) colors.inkPrimary else colors.surfaceCard,
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (error && !selected) colors.accentWarning else colors.borderHairline,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = unit.displayName,
            style = typography.itemMeta.copy(color = if (selected) colors.inkInverse else colors.inkPrimary),
        )
    }
}
