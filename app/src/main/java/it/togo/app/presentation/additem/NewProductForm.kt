package it.togo.app.presentation.additem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Form compatto per la creazione di un prodotto canonico user-defined
 * su una categoria esistente (Story 7.1).
 *
 * Vive dentro [AddItemBottomSheet] come sotto-flusso dello step 1.
 * L1/L2 sono selezionabili solo tra i livelli esistenti; il L3 può essere
 * scelto tra quelli esistenti oppure proposto come nuovo (Story 7.2).
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
    isLevel3ProposalVisible: Boolean,
    newLevel3Name: String,
    level3ProposalError: String?,
    isCreatingLevel3: Boolean,
    onNameChange: (String) -> Unit,
    onLevel1Select: (TaxonomyLevel.Level1) -> Unit,
    onLevel2Select: (TaxonomyLevel.Level2) -> Unit,
    onLevel3Select: (TaxonomyLevel.Level3) -> Unit,
    onShowLevel3Proposal: () -> Unit,
    onHideLevel3Proposal: () -> Unit,
    onNewLevel3NameChange: (String) -> Unit,
    onCreateLevel3: () -> Unit,
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

    val busy = isSaving || isCreatingLevel3

    val canCreate = name.isNotBlank() &&
        level1Id != null &&
        level2Id != null &&
        level3Id != null &&
        !busy

    val canCreateLevel3 = newLevel3Name.isNotBlank() && !busy

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
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )

        TaxonomyDropdown(
            label = "Livello 1 *",
            placeholder = "Seleziona Livello 1",
            selectedName = level1Options.firstOrNull { it.id == level1Id }?.name,
            options = level1Options.map { it.id to it.name },
            enabled = !busy,
            onSelect = { id -> level1Options.firstOrNull { it.id == id }?.let(onLevel1Select) },
        )

        TaxonomyDropdown(
            label = "Livello 2 *",
            placeholder = "Seleziona Livello 2",
            selectedName = level2Options.firstOrNull { it.id == level2Id }?.name,
            options = level2Options.map { it.id to it.name },
            enabled = level1Id != null && !busy,
            onSelect = { id -> level2Options.firstOrNull { it.id == id }?.let(onLevel2Select) },
        )

        TaxonomyDropdown(
            label = "Livello 3 *",
            placeholder = "Seleziona Livello 3",
            selectedName = level3Options.firstOrNull { it.id == level3Id }?.name,
            options = level3Options.map { it.id to it.name },
            enabled = level2Id != null && !busy,
            onSelect = { id -> level3Options.firstOrNull { it.id == id }?.let(onLevel3Select) },
        )

        // Story 7.2: proposta di un nuovo Livello 3, solo sotto un L2 esistente.
        // Non esistono azioni per creare L1/L2.
        if (level2Id != null) {
            if (!isLevel3ProposalVisible) {
                TextButton(
                    onClick = onShowLevel3Proposal,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = spacing.touchTargetMin),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.accentAction),
                ) {
                    Text(
                        text = "Proponi nuovo Livello 3",
                        style = typography.itemMeta,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    OutlinedTextField(
                        value = newLevel3Name,
                        onValueChange = onNewLevel3NameChange,
                        label = { Text("Nome nuovo Livello 3 *", style = typography.caption) },
                        singleLine = true,
                        enabled = !busy,
                        isError = level3ProposalError != null && newLevel3Name.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (level3ProposalError != null) {
                        Text(
                            text = level3ProposalError,
                            color = colors.warningInk,
                            style = typography.caption,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    ) {
                        OutlinedButton(
                            onClick = onHideLevel3Proposal,
                            enabled = !busy,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = spacing.touchTargetMin),
                        ) {
                            Text("Annulla proposta", style = typography.sectionHeader)
                        }
                        Button(
                            onClick = onCreateLevel3,
                            enabled = canCreateLevel3,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = spacing.touchTargetMin),
                        ) {
                            Text(
                                text = if (isCreatingLevel3) "Creazione..." else "Crea Livello 3",
                                style = typography.sectionHeader,
                            )
                        }
                    }
                }
            }
        }

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
                enabled = !busy,
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
