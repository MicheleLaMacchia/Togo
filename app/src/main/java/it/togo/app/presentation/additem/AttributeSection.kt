package it.togo.app.presentation.additem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ProductAttribute
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import java.util.Locale

/**
 * Sezione attributi contestuali per il prodotto selezionato.
 *
 * Marca e variante sono SEMPRE presenti e modificabili per ogni prodotto (a
 * testo libero, con i valori già usati dall'utente in cima ai suggerimenti):
 * un valore nuovo digitato una volta resta disponibile anche in seguito.
 *
 * La conservazione resta contestuale al Livello 3, perché ha senso solo per
 * alcuni prodotti (freschi, surgelati, ...).
 */
@Composable
fun AttributeSection(
    product: CanonicalProduct,
    brand: String,
    onBrandChange: (String) -> Unit,
    brandSuggestions: List<String>,
    variant: String,
    onVariantChange: (String) -> Unit,
    variantSuggestions: List<String>,
    condition: String,
    onConditionChange: (String) -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val attributeDef = ProductAttribute.forLevel3(product.level3Id, product.name)

    // I valori ricordati vengono prima di quelli predefiniti; il confronto è
    // case-insensitive ("Coop"/"coop" sono la stessa marca).
    val brands = mergeSuggestions(brandSuggestions, attributeDef.brandSuggestions)
    val variants = mergeSuggestions(variantSuggestions)

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

        // Marca: sempre presente, testo libero + suggerimenti ricordati.
        SuggestionTextField(
            label = "Marca",
            placeholder = "Seleziona o digita…",
            value = brand,
            onValueChange = onBrandChange,
            suggestions = brands,
        )

        // Variante: sempre presente, testo libero + suggerimenti ricordati.
        SuggestionTextField(
            label = "Variante",
            placeholder = "Es. integrale, biologico…",
            value = variant,
            onValueChange = onVariantChange,
            suggestions = variants,
        )

        // Conservazione: solo per i prodotti per cui è pertinente.
        if (attributeDef.hasCondition) {
            ConditionDropdown(
                value = condition,
                onValueChange = onConditionChange,
                options = attributeDef.conditionOptions,
            )
        }
    }
}

/** Deduplica case-insensitive preservando l'ordine (prima i valori ricordati). */
private fun mergeSuggestions(vararg lists: List<String>): List<String> =
    lists.asSequence()
        .flatten()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .toList()

/**
 * Campo testo con dropdown di suggerimenti (valore libero sempre accettato).
 * Se non ci sono suggerimenti si comporta come un normale campo di testo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
) {
    val typography = togoTypography()
    var expanded by remember { mutableStateOf(false) }
    val hasSuggestions = suggestions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && hasSuggestions,
        onExpandedChange = { if (hasSuggestions) expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = typography.caption) },
            placeholder = { Text(placeholder, style = typography.itemMeta) },
            singleLine = true,
            trailingIcon = if (hasSuggestions) {
                { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            } else {
                null
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )

        if (hasSuggestions) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                suggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion, style = typography.itemMeta) },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                        },
                    )
                }
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
