package it.togo.app.presentation.additem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.ProductAttribute
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.wizard.NewProductDraft
import it.togo.app.domain.wizard.WizardStep
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Wizard di categorizzazione per prodotti sconosciuti (non nel catalogo).
 *
 * Step sequenziali:
 * 1. SELECT_L1 — Scelta Livello 1
 * 2. SELECT_L2 — Scelta Livello 2 figlio di L1
 * 3. SELECT_L3 — Scelta Livello 3 o creazione nuovo L3
 * 4. ENTER_DETAILS — Nome prodotto, quantità, unità, attributi
 * 5. CONFIRM — Riepilogo e conferma creazione
 *
 * Design System: 100% token TogoTheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCategorizationWizard(
    draft: NewProductDraft,
    level1Options: List<TaxonomyLevel.Level1>,
    level2Options: List<TaxonomyLevel.Level2>,
    level3Options: List<TaxonomyLevel.Level3>,
    newLevel3Name: String,
    onNewLevel3NameChange: (String) -> Unit,
    onLevel1Select: (TaxonomyLevel.Level1) -> Unit,
    onLevel2Select: (TaxonomyLevel.Level2) -> Unit,
    onLevel3Select: (TaxonomyLevel.Level3) -> Unit,
    onNewLevel3Confirm: () -> Unit,
    onProductNameChange: (String) -> Unit,
    onUnitSelect: (StandardUnit) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onBrandChange: (String) -> Unit,
    onVariantChange: (String) -> Unit,
    onConditionChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        // Header con step indicator
        WizardStepIndicator(
            currentStep = draft.currentStep,
            colors = colors,
            typography = typography,
        )

        // Contenuto step corrente
        when (draft.currentStep) {
            WizardStep.SELECT_L1 -> StepContent(
                title = "Scegli la categoria principale",
                level1Options = level1Options,
                onLevel1Select = onLevel1Select,
            )

            WizardStep.SELECT_L2 -> StepContent(
                title = "Scegli la sottocategoria",
                level2Options = level2Options,
                onLevel2Select = onLevel2Select,
            )

            WizardStep.SELECT_L3 -> StepContent(
                title = "Scegli la categoria specifica",
                level3Options = level3Options,
                newLevel3Name = newLevel3Name,
                onNewLevel3NameChange = onNewLevel3NameChange,
                onLevel3Select = onLevel3Select,
                onNewLevel3Confirm = onNewLevel3Confirm,
            )

            WizardStep.ENTER_DETAILS -> EnterDetailsStep(
                draft = draft,
                onProductNameChange = onProductNameChange,
                onUnitSelect = onUnitSelect,
                onQuantityChange = onQuantityChange,
                onBrandChange = onBrandChange,
                onVariantChange = onVariantChange,
                onConditionChange = onConditionChange,
            )

            WizardStep.CONFIRM -> ConfirmStep(
                draft = draft,
            )
        }

        // Navigation buttons
        WizardNavigation(
            currentStep = draft.currentStep,
            canGoBack = draft.canGoBack,
            canProceed = draft.canProceed,
            onBack = onBack,
            onNext = onNext,
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun WizardStepIndicator(
    currentStep: WizardStep,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
) {
    val steps = listOf("L1", "L2", "L3", "Dettagli", "Conferma")
    val currentIndex = when (currentStep) {
        WizardStep.SELECT_L1 -> 0
        WizardStep.SELECT_L2 -> 1
        WizardStep.SELECT_L3 -> 2
        WizardStep.ENTER_DETAILS -> 3
        WizardStep.CONFIRM -> 4
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        steps.forEachIndexed { index, label ->
            val isActive = index == currentIndex
            val isCompleted = index < currentIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${index + 1}",
                    style = typography.caption,
                    color = if (isActive || isCompleted) colors.accent else colors.inkSecondary,
                )
                Text(
                    text = label,
                    style = typography.caption,
                    color = if (isActive || isCompleted) colors.inkPrimary else colors.inkSecondary,
                )
            }
        }
    }
}

@Composable
private fun StepContent(
    title: String,
    level1Options: List<TaxonomyLevel.Level1> = emptyList(),
    level2Options: List<TaxonomyLevel.Level2> = emptyList(),
    level3Options: List<TaxonomyLevel.Level3> = emptyList(),
    newLevel3Name: String = "",
    onLevel1Select: ((TaxonomyLevel.Level1) -> Unit)? = null,
    onLevel2Select: ((TaxonomyLevel.Level2) -> Unit)? = null,
    onLevel3Select: ((TaxonomyLevel.Level3) -> Unit)? = null,
    onNewLevel3NameChange: ((String) -> Unit)? = null,
    onNewLevel3Confirm: (() -> Unit)? = null,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = title,
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        // L1 options
        if (level1Options.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                items(level1Options) { level1 ->
                    CategoryOptionCard(
                        label = level1.name,
                        onClick = { onLevel1Select?.invoke(level1) },
                    )
                }
            }
        }

        // L2 options
        if (level2Options.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                items(level2Options) { level2 ->
                    CategoryOptionCard(
                        label = level2.name,
                        onClick = { onLevel2Select?.invoke(level2) },
                    )
                }
            }
        }

        // L3 options + "Nuovo..."
        if (level3Options.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                items(level3Options) { level3 ->
                    CategoryOptionCard(
                        label = level3.name,
                        isNewOption = level3.id == -1L,
                        onClick = { onLevel3Select?.invoke(level3) },
                    )
                }
            }

            // Input per nuovo L3
            if (newLevel3Name.isNotEmpty() || level3Options.any { it.id == -1L }) {
                OutlinedTextField(
                    value = newLevel3Name,
                    onValueChange = { onNewLevel3NameChange?.invoke(it) },
                    label = { Text("Nome nuova categoria", style = typography.caption) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (newLevel3Name.isNotBlank()) {
                    Button(
                        onClick = { onNewLevel3Confirm?.invoke() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Crea categoria e continua")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryOptionCard(
    label: String,
    isNewOption: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNewOption) colors.accentSoft else colors.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.space3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = typography.itemTitle,
                color = colors.inkPrimary,
            )
            if (isNewOption) {
                Text(
                    text = "+",
                    style = typography.itemTitle,
                    color = colors.accent,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnterDetailsStep(
    draft: NewProductDraft,
    onProductNameChange: (String) -> Unit,
    onUnitSelect: (StandardUnit) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onBrandChange: (String) -> Unit,
    onVariantChange: (String) -> Unit,
    onConditionChange: (String) -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    // Stato locale per input quantità come stringa (permette digitazione decimali)
    var quantityText by remember(draft.quantity) { mutableStateOf(formatQuantity(draft.quantity)) }
    var unitExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = "Dettagli prodotto",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        // Nome prodotto
        OutlinedTextField(
            value = draft.productName,
            onValueChange = onProductNameChange,
            label = { Text("Nome prodotto *", style = typography.caption) },
            placeholder = { Text("Es. Latte di mandorla bio", style = typography.itemMeta) },
            isError = draft.errors.containsKey("productName"),
            supportingText = draft.errors["productName"]?.let { error ->
                { Text(error, color = colors.error) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Quantità + Unità
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            // Quantità: input stringa con validazione differita
            OutlinedTextField(
                value = quantityText,
                onValueChange = { newValue ->
                    // Permetti: vuoto, numeri, un solo punto decimale
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    val sanitized = if (filtered.count { it == '.' } <= 1) filtered else filtered.replaceFirst(".", "")
                    quantityText = sanitized
                    // Notifica solo se valore valido e > 0
                    sanitized.toDoubleOrNull()?.let { qty ->
                        if (qty > 0) onQuantityChange(qty)
                    }
                },
                label = { Text("Quantità *", style = typography.caption) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )

            // Unità: dropdown interattivo
            if (draft.compatibleUnits.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = draft.selectedUnit?.displayName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unità *", style = typography.caption) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )

                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false },
                    ) {
                        draft.compatibleUnits.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName, style = typography.itemMeta) },
                                onClick = {
                                    onUnitSelect(unit)
                                    unitExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }

        // Attributi contestuali (da Story 3.4)
        draft.selectedLevel3?.let { level3 ->
            val attributeDef = ProductAttribute.forLevel3(level3.id, draft.productName)

            if (attributeDef.hasAnyAttribute) {
                Text(
                    text = "Attributi (opzionali)",
                    style = typography.sectionHeader,
                    color = colors.inkPrimary,
                )

                AnimatedVisibility(visible = attributeDef.hasBrand) {
                    OutlinedTextField(
                        value = draft.brand ?: "",
                        onValueChange = onBrandChange,
                        label = { Text("Marca", style = typography.caption) },
                        placeholder = { Text("Seleziona o digita…", style = typography.itemMeta) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AnimatedVisibility(visible = attributeDef.hasVariant) {
                    OutlinedTextField(
                        value = draft.variant ?: "",
                        onValueChange = onVariantChange,
                        label = { Text("Variante", style = typography.caption) },
                        placeholder = { Text("Es. integrale, biologico…", style = typography.itemMeta) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AnimatedVisibility(visible = attributeDef.hasCondition) {
                    // Dropdown conservazione interattivo
                    ExposedDropdownMenuBox(
                        expanded = conditionExpanded,
                        onExpandedChange = { conditionExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = draft.condition ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Conservazione", style = typography.caption) },
                            placeholder = { Text("Seleziona…", style = typography.itemMeta) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )

                        ExposedDropdownMenu(
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false },
                        ) {
                            attributeDef.conditionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, style = typography.itemMeta) },
                                    onClick = {
                                        onConditionChange(option)
                                        conditionExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Formatta quantità per display: rimuove .0 se intero, altrimenti mostra decimali */
private fun formatQuantity(qty: Double): String {
    return if (qty == qty.toLong().toDouble()) {
        qty.toLong().toString()
    } else {
        qty.toString()
    }
}

@Composable
private fun ConfirmStep(
    draft: NewProductDraft,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = "Riepilogo",
            style = typography.sectionHeader,
            color = colors.inkPrimary,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
        ) {
            Column(
                modifier = Modifier.padding(spacing.space4),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(
                    text = "Prodotto",
                    style = typography.caption,
                    color = colors.inkSecondary,
                )
                Text(
                    text = draft.productName,
                    style = typography.itemTitle,
                    color = colors.inkPrimary,
                )

                Text(
                    text = "Categoria",
                    style = typography.caption,
                    color = colors.inkSecondary,
                )
                Text(
                    text = draft.selectedLevel1?.name + " > " + draft.selectedLevel2?.name + " > " + (draft.selectedLevel3?.name ?: draft.newLevel3Name),
                    style = typography.itemTitle,
                    color = colors.inkPrimary,
                )

                Text(
                    text = "Quantità",
                    style = typography.caption,
                    color = colors.inkSecondary,
                )
                Text(
                    text = "${formatQuantity(draft.quantity)} ${draft.selectedUnit?.displayName ?: ""}",
                    style = typography.itemTitle,
                    color = colors.inkPrimary,
                )

                draft.brand?.let {
                    Text(text = "Marca: $it", style = typography.itemMeta, color = colors.inkSecondary)
                }
                draft.variant?.let {
                    Text(text = "Variante: $it", style = typography.itemMeta, color = colors.inkSecondary)
                }
                draft.condition?.let {
                    Text(text = "Conservazione: $it", style = typography.itemMeta, color = colors.inkSecondary)
                }
            }
        }

        Text(
            text = "Il prodotto verrà creato localmente nel catalogo e aggiunto alla lista.",
            style = typography.itemMeta,
            color = colors.inkSecondary,
        )
    }
}

@Composable
private fun WizardNavigation(
    currentStep: WizardStep,
    canGoBack: Boolean,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = togoColors()
    val spacing = togoSpacing()

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        // Primary action: solo a CONFIRM è "Crea prodotto", negli step intermedi è "Avanti"
        if (currentStep == WizardStep.CONFIRM) {
            Button(
                onClick = onConfirm,
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Crea prodotto e aggiungi alla lista")
            }
        } else {
            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Avanti →")
            }
        }

        // Back (solo se non al primo step)
        if (canGoBack) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Indietro")
            }
        }

        // Cancel
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Annulla", color = colors.inkSecondary)
        }
    }
}
