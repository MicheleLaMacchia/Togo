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
import androidx.compose.material3.TextField
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
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Sezione attributi opzionali per il prodotto selezionato.
 *
 * Mostra solo attributi pertinenti al Level3:
 * - Marca: dropdown con valori comuni + free text
 * - Variante: free text
 * - Conservazione: dropdown valori controllati
 */
@Composable
fun AttributeSection(
    brand: String,
    onBrandChange: (String) -> Unit,
    variant: String,
    onVariantChange: (String) -> Unit,
    condition: String,
    onConditionChange: (String) -> Unit,
    showBrand: Boolean = true,
    showVariant: Boolean = true,
    showCondition: Boolean = true,
    commonBrands: List<String> = emptyList(),
    commonConditions: List<String> = emptyList(),
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text("Attributi (opzionali)", style = typography.sectionHeader, color = togoColors().inkPrimary)

        // --- Marca ---
        if (showBrand) {
            AttributeField(
                label = "Marca",
                value = brand,
                onValueChange = onBrandChange,
                placeholder = "Es. Coop, Consilia, Mulino Bianco...",
                suggestions = commonBrands,
                isRequired = false,
            )
        }

        // --- Variante ---
        if (showVariant) {
            AttributeField(
                label = "Variante",
                value = variant,
                onValueChange = onVariantChange,
                placeholder = "Es. integrale, biologico, senza lattosio...",
                suggestions = emptyList(),
                isRequired = false,
            )
        }

        // --- Conservazione ---
        if (showCondition) {
            AttributeField(
                label = "Conservazione",
                value = condition,
                onValueChange = onConditionChange,
                placeholder = "Seleziona o digita...",
                suggestions = commonConditions,
                isRequired = false,
            )
        }
    }
}

/** Campo attributo con dropdown suggerimenti opzionale */
@Composable
private fun AttributeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suggestions: List<String>,
    isRequired: Boolean,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    val expanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space1),
    ) {
        // Label + campo
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(label, style = typography.sectionHeader, color = togoColors().inkPrimary)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = togoColors().surfaceCard,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = togoColors().borderHairline,
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                TextField(
                    value = value,
                    onValueChange = { if (it.length <= 100) onValueChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text(placeholder, style = togoTypography().itemMeta, color = togoColors().inkMuted) },
                    singleLine = true,
                    textStyle = togoTypography().itemName,
                    colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
                        textColor = togoColors().inkPrimary,
                        cursorColor = togoColors().accentHighlight,
                        backgroundColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )

                // Toggle suggerimenti
                if (suggestions.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded.value)
                            androidx.compose.material.icons.Icons.Filled.ExpandLess
                        else
                            androidx.compose.material.icons.Icons.Filled.ExpandMore,
                        contentDescription = if (expanded.value) "Nascondi suggerimenti" else "Mostra suggerimenti",
                        tint = togoColors().inkSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 16.dp)
                            .fillMaxHeight()
                            .wrapContentSize(Alignment.CenterEnd),
                    )
                        .combinedClickable(onClick = { expanded.value = !expanded.value })
                }
            }
        }

        // Suggerimenti dropdown
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded.value,
            enter = androidx.compose.animation.expandVertically(
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
            exit = androidx.compose.animation.shrinkVertically(
                animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 200.0f)
            ),
        ) {
            if (suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.space1)
                        .background(
                            color = togoColors().surfaceCard,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .border(1.dp, togoColors().borderHairline, RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    suggestions.forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .background(Color.Transparent)
                                .combinedClickable(onClick = { onValueChange(suggestion) }),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(suggestion, style = togoTypography().itemMeta, color = togoColors().inkSecondary)
                        }
                    }
                }
            }
        }
    }
}