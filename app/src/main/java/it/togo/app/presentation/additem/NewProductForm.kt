package it.togo.app.presentation.additem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Form compatto per la creazione di un prodotto canonico user-defined
 * su una categoria esistente (Story 7.1).
 *
 * Vive dentro [AddItemBottomSheet] come sotto-flusso dello step 1.
 * L1/L2/L3 sono selezionabili solo tra i livelli esistenti: la creazione
 * di nuovi livelli è fuori perimetro (Story 7.2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductForm(
    name: String,
    taxonomy: List<TaxonomyLevel>,
    level1Id: Long?,
    level2Id: Long?,
    level3Id: Long?,
    error: String?,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onLevel1Select: (TaxonomyLevel.Level1) -> Unit,
    onLevel2Select: (TaxonomyLevel.Level2) -> Unit,
    onLevel3Select: (TaxonomyLevel.Level3) -> Unit,
    onCancel: () -> Unit,
    onCreate: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val level1Options = taxonomy.filterIsInstance<TaxonomyLevel.Level1>()
    val level2Options = taxonomy.filterIsInstance<TaxonomyLevel.Level2>()
        .filter { it.level1Id == level1Id }
    val level3Options = taxonomy.filterIsInstance<TaxonomyLevel.Level3>()
        .filter { it.level2Id == level2Id }

    val canCreate = name.isNotBlank() &&
        level1Id != null &&
        level2Id != null &&
        level3Id != null &&
        !isSaving

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nome prodotto *", style = typography.caption) },
            singleLine = true,
            // L'errore può riguardare anche la selezione della categoria:
            // non marcare il nome come invalido se è già stato compilato.
            isError = error != null && name.isBlank(),
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        TaxonomyDropdown(
            label = "Livello 1 *",
            placeholder = "Seleziona Livello 1",
            selectedName = level1Options.firstOrNull { it.id == level1Id }?.name,
            options = level1Options.map { it.id to it.name },
            enabled = !isSaving,
            onSelect = { id -> level1Options.firstOrNull { it.id == id }?.let(onLevel1Select) },
        )

        TaxonomyDropdown(
            label = "Livello 2 *",
            placeholder = "Seleziona Livello 2",
            selectedName = level2Options.firstOrNull { it.id == level2Id }?.name,
            options = level2Options.map { it.id to it.name },
            enabled = level1Id != null && !isSaving,
            onSelect = { id -> level2Options.firstOrNull { it.id == id }?.let(onLevel2Select) },
        )

        TaxonomyDropdown(
            label = "Livello 3 *",
            placeholder = "Seleziona Livello 3",
            selectedName = level3Options.firstOrNull { it.id == level3Id }?.name,
            options = level3Options.map { it.id to it.name },
            enabled = level2Id != null && !isSaving,
            onSelect = { id -> level3Options.firstOrNull { it.id == id }?.let(onLevel3Select) },
        )

        if (error != null) {
            Text(
                text = error,
                color = colors.warningInk,
                style = typography.caption,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = spacing.touchTargetMin),
            ) {
                Text("Annulla", style = typography.sectionHeader)
            }
            Button(
                onClick = onCreate,
                enabled = canCreate,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = spacing.touchTargetMin),
            ) {
                Text(
                    text = if (isSaving) "Creazione..." else "Crea prodotto",
                    style = typography.sectionHeader,
                )
            }
        }
    }
}

/**
 * Dropdown tassonomico basato sul pattern di `ProductCategorizationWizard`:
 * `ExposedDropdownMenuBox` + `OutlinedTextField(readOnly)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaxonomyDropdown(
    label: String,
    placeholder: String,
    selectedName: String?,
    options: List<Pair<Long, String>>,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
) {
    val typography = togoTypography()
    val spacing = togoSpacing()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedName ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label, style = typography.caption) },
            placeholder = { Text(placeholder, style = typography.itemMeta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.touchTargetMin)
                .menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, style = typography.itemMeta) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}
