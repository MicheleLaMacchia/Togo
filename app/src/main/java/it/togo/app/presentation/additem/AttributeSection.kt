package it.togo.app.presentation.additem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ProductAttribute
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Sezione attributi contestuali per il prodotto selezionato.
 *
 * Mostra solo gli attributi pertinenti al Livello 3 del prodotto:
 * - Marca: dropdown con suggerimenti + free text
 * - Variante: campo testo libero
 * - Conservazione: dropdown con valori controllati
 *
 * Se il L3 non supporta attributi, la sezione non viene renderizzata.
 */
@Composable
fun AttributeSection(
    product: CanonicalProduct,
    brand: String,
    onBrandChange: (String) -> Unit,
    variant: String,
    onVariantChange: (String) -> Unit,
    condition: String,
    onConditionChange: (String) -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val attributeDef = ProductAttribute.forLevel3(product.level3Id, product.name)

    if (!attributeDef.hasAnyAttribute) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = "Attributi (opzionali)",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        // Marca
        AnimatedVisibility(visible = attributeDef.hasBrand) {
            BrandField(
                value = brand,
                onValueChange = onBrandChange,
                suggestions = attributeDef.brandSuggestions,
            )
        }

        // Variante
        AnimatedVisibility(visible = attributeDef.hasVariant) {
            OutlinedTextField(
                value = variant,
                onValueChange = onVariantChange,
                label = { Text("Variante", style = typography.caption) },
                placeholder = { Text("Es. integrale, biologico…", style = typography.itemMeta) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Conservazione
        AnimatedVisibility(visible = attributeDef.hasCondition) {
            ConditionDropdown(
                value = condition,
                onValueChange = onConditionChange,
                options = attributeDef.conditionOptions,
            )
        }
    }
}

/** Campo marca con dropdown suggerimenti + input libero */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrandField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
) {
    val colors = togoColors()
    val typography = togoTypography()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Marca", style = typography.caption) },
            placeholder = { Text("Seleziona o digita…", style = typography.itemMeta) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            suggestions.forEach { brand ->
                DropdownMenuItem(
                    text = { Text(brand, style = typography.itemMeta) },
                    onClick = {
                        onValueChange(brand)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Dropdown conservazione con valori controllati */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
) {
    val typography = togoTypography()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text("Conservazione", style = typography.caption) },
            placeholder = { Text("Seleziona…", style = typography.itemMeta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { condition ->
                DropdownMenuItem(
                    text = { Text(condition, style = typography.itemMeta) },
                    onClick = {
                        onValueChange(condition)
                        expanded = false
                    },
                )
            }
        }
    }
}