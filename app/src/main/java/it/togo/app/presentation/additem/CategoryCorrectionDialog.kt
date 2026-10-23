package it.togo.app.presentation.additem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Dialog per correggere la categoria canonica di un prodotto.
 *
 * L'utente sceglie L1 → L2 → L3 e indica l'espressione da usare per la regola
 * appresa. La regola viene salvata solo dopo la conferma esplicita nel dialog
 * successivo ("Ricorda questa scelta?").
 */
@Composable
fun CategoryCorrectionDialog(
    taxonomy: List<TaxonomyLevel>,
    level1Id: Long?,
    level2Id: Long?,
    level3Id: Long?,
    expression: String,
    error: String?,
    isSaving: Boolean,
    onLevel1Select: (Long) -> Unit,
    onLevel2Select: (Long) -> Unit,
    onLevel3Select: (Long) -> Unit,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val level1Options = remember(taxonomy) {
        taxonomy.filterIsInstance<TaxonomyLevel.Level1>()
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            .map { it.id to it.name }
    }
    val level2Options = remember(taxonomy, level1Id) {
        taxonomy.filterIsInstance<TaxonomyLevel.Level2>()
            .filter { level1Id == null || it.level1Id == level1Id }
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            .map { it.id to it.name }
    }
    val level3Options = remember(taxonomy, level2Id) {
        taxonomy.filterIsInstance<TaxonomyLevel.Level3>()
            .filter { !it.isUserDefined }
            .filter { level2Id == null || it.level2Id == level2Id }
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            .map { it.id to it.name }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Correggi categoria", style = typography.itemTitle) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(
                    text = "Scegli la categoria corretta e l'espressione da ricordare.",
                    style = typography.itemMeta,
                    color = colors.inkSecondary,
                )

                TaxonomyDropdown(
                    label = "Livello 1",
                    options = level1Options,
                    selectedId = level1Id,
                    enabled = !isSaving,
                    onSelected = onLevel1Select,
                )
                TaxonomyDropdown(
                    label = "Livello 2",
                    options = level2Options,
                    selectedId = level2Id,
                    enabled = !isSaving && level1Id != null,
                    onSelected = onLevel2Select,
                )
                TaxonomyDropdown(
                    label = "Livello 3",
                    options = level3Options,
                    selectedId = level3Id,
                    enabled = !isSaving && level2Id != null,
                    onSelected = onLevel3Select,
                )

                OutlinedTextField(
                    value = expression,
                    onValueChange = onExpressionChange,
                    label = { Text("Espressione da ricordare", style = typography.caption) },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "La regola userà questa espressione per applicare la categoria scelta.",
                    style = typography.caption,
                    color = colors.inkSecondary,
                )

                if (error != null) {
                    Text(
                        text = error,
                        style = typography.caption,
                        color = colors.warningInk,
                    )
                }
            }
        },
        confirmButton = {
            val selectedLevel3Valid = level3Id != null &&
                level3Options.any { it.first == level3Id }
            TextButton(
                onClick = onConfirm,
                enabled = !isSaving && selectedLevel3Valid,
            ) {
                Text("Conferma")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Annulla")
            }
        },
    )
}

@Composable
private fun TaxonomyDropdown(
    label: String,
    options: List<Pair<Long, String>>,
    selectedId: Long?,
    enabled: Boolean,
    onSelected: (Long) -> Unit,
) {
    val typography = togoTypography()
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = typography.caption,
            color = togoColors().inkSecondary,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled && options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selectedLabel ?: "Seleziona…",
                    style = typography.itemMeta,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            expanded = false
                            onSelected(id)
                        },
                    )
                }
            }
        }
    }
}

/** Prompt esplicito prima di creare una regola appresa. */
@Composable
fun RememberCategoryDialog(
    expression: String,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val typography = togoTypography()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Ricorda questa scelta?", style = typography.itemTitle) },
        text = {
            Text(
                text = "Vuoi creare una regola per «$expression»? " +
                    "La regola sarà salvata per questo prodotto e potrà essere riutilizzata.",
                style = typography.itemMeta,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSaving) {
                Text("Ricorda")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Non salvare")
            }
        },
    )
}
