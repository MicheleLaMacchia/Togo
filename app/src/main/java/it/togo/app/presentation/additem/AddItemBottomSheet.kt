package it.togo.app.presentation.additem

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import androidx.compose.material3.ModalBottomSheetLayout
import androidx.compose.material3.ModalBottomSheetState
import androidx.compose.material3.ModalBottomSheetValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import android.os.Vibrator
import android.content.Context
import android.os.VibrationEffect

/**
 * Bottom Sheet "Aggiungi Voce Manuale" - flusso multi-step.
 *
 * Step:
 * 1. Ricerca prodotto (autocomplete catalogo)
 * 2. Quantità + Unità compatibili
 * 3. Attributi opzionali (marca, variante, conservazione)
 * 4. Conferma
 */
@Composable
fun AddItemBottomSheet(
    sheetState: ModalBottomSheetState,
    onConfirm: (it.togo.app.presentation.additem.AddItemConfirmed) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<AddItemViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.space3)
                    .background(colors.surfaceBase),
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(horizontal = spacing.space4),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp, 4.dp)
                            .background(colors.borderHairline, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                    )
                }

                // Titolo step
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space4, vertical = spacing.space2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (uiState.currentStep) {
                            1 -> "Cerca prodotto"
                            2 -> "Quantità e unità"
                            3 -> "Attributi (opzionali)"
                            4 -> "Conferma"
                            else -> "Aggiungi voce"
                        },
                        style = typography.titleScreen,
                        color = colors.inkPrimary,
                    )

                    // Chiudi
                    IconButton(onClick = {
                        viewModel.onEvent(AddItemUiEvent.Reset)
                        scope.launch { sheetState.hide() }
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                            contentDescription = "Chiudi",
                            tint = colors.inkPrimary,
                        )
                    }
                }

                // Progress indicator step
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space4, bottom = spacing.space2),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    (1..4).forEach { step ->
                        val isActive = uiState.currentStep >= step
                        val isCurrent = uiState.currentStep == step
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                        ) {
                            // Linea progress
                            if (step < 4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(if (isActive) colors.accentHighlight else colors.borderHairline),
                                )
                            }
                            // Circle step
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (isCurrent) colors.accentHighlight else if (isActive) colors.inkPrimary else colors.borderHairline,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = step.toString(),
                                    style = typography.caption.copy(color = if (isCurrent || isActive) colors.inkInverse else colors.inkMuted),
                                )
                            }
                        }
                    }
                }

                // Contenuto step
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.space4, vertical = spacing.space2)
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    when (uiState.currentStep) {
                        1 -> SearchStep(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.onEvent(AddItemUiEvent.SearchQueryChanged(it)) },
                            results = uiState.searchResults,
                            isSearching = uiState.isLoading,
                            onProductClick = { viewModel.onEvent(AddItemUiEvent.ProductSelected(it)) },
                        )
                        2 -> QuantityUnitStep(
                            quantity = uiState.quantity,
                            onQuantityChange = { viewModel.onEvent(AddItemUiEvent.QuantityChanged(it)) },
                            selectedUnit = uiState.selectedUnit,
                            onUnitChange = { viewModel.onEvent(AddItemUiEvent.UnitChanged(it)) },
                            compatibleUnits = uiState.compatibleUnits,
                            errorQuantity = uiState.errors["quantity"],
                            errorUnit = uiState.errors["unit"],
                        )
                        3 -> AttributeStep(
                            brand = uiState.brand,
                            onBrandChange = { viewModel.onEvent(AddItemUiEvent.BrandChanged(it)) },
                            variant = uiState.variant,
                            onVariantChange = { viewModel.onEvent(AddItemUiEvent.VariantChanged(it)) },
                            condition = uiState.condition,
                            onConditionChange = { viewModel.onEvent(AddItemUiEvent.ConditionChanged(it)) },
                            showBrand = uiState.selectedProduct?.showBrand ?? true,
                            showVariant = uiState.selectedProduct?.showVariant ?? true,
                            showCondition = uiState.selectedProduct?.showCondition ?? true,
                            commonBrands = uiState.selectedProduct?.commonBrands ?? emptyList(),
                            commonConditions = uiState.selectedProduct?.commonConditions ?? emptyList(),
                        )
                        4 -> {
                            val product = uiState.selectedProduct ?: return@Column
                            val unit = uiState.selectedUnit ?: return@Column
                            ConfirmStep(
                                product = product,
                                quantity = uiState.quantity,
                                unit = unit,
                                brand = uiState.brand,
                                variant = uiState.variant,
                                condition = uiState.condition,
                                onConfirm = {
                                    viewModel.onEvent(AddItemUiEvent.Confirm)
                                },
                                onBack = { viewModel.onEvent(AddItemUiEvent.PreviousStep) },
                            )
                        }
                    }
                }

                // Bottoni navigazione
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.space3, horizontal = spacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    if (uiState.currentStep > 1) {
                        Button(
                            onClick = { viewModel.onEvent(AddItemUiEvent.PreviousStep) },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceSubtle,
                                contentColor = colors.inkPrimary,
                            ),
                        ) {
                            Text("Indietro", style = typography.sectionHeader, color = colors.inkPrimary)
                        }
                    }

                    if (uiState.currentStep < 4) {
                        Button(
                            onClick = { viewModel.onEvent(AddItemUiEvent.NextStep) },
                            modifier = Modifier.weight(1f),
                            enabled = when (uiState.currentStep) {
                                1 -> uiState.canProceedToStep2
                                2 -> uiState.canProceedToStep3
                                3 -> true
                                else -> true
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (uiState.currentStep == 1 && !uiState.canProceedToStep2
                                    || uiState.currentStep == 2 && !uiState.canProceedToStep3
                                ) colors.borderHairline else colors.accentAction,
                                contentColor = if (uiState.currentStep == 1 && !uiState.canProceedToStep2
                                    || uiState.currentStep == 2 && !uiState.canProceedToStep3
                                ) colors.inkMuted else colors.inkInverse,
                            ),
                        ) {
                            Text("Avanti", style = typography.sectionHeader, color = if (uiState.currentStep == 1 && !uiState.canProceedToStep2
                                || uiState.currentStep == 2 && !uiState.canProceedToStep3
                            ) colors.inkMuted else colors.inkInverse)
                        }
                    } else {
                        // Step 4 - Conferma
                        Button(
                            onClick = { viewModel.onEvent(AddItemUiEvent.Confirm) },
                            modifier = Modifier.weight(1f),
                            enabled = uiState.isValidForConfirm,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isValidForConfirm) colors.accentSuccess else colors.borderHairline,
                                contentColor = if (uiState.isValidForConfirm) colors.inkInverse else colors.inkMuted,
                            ),
                        ) {
                            Text("Aggiungi", style = typography.sectionHeader, color = if (uiState.isValidForConfirm) colors.inkInverse else colors.inkMuted)
                        }
                    }
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = spacing.space4))
            }
        },
        sheetBackgroundColor = colors.surfaceBase,
        sheetShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetPeekHeight = 0.dp,
        confirmStateChange = { it == ModalBottomSheetValue.Expanded },
    )
}

/** Step 1: Ricerca prodotto */
@Composable
fun SearchStep(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<CanonicalProduct>,
    isSearching: Boolean,
    onProductClick: (CanonicalProduct) -> Unit,
) {
    ProductSearchField(
        query = query,
        onQueryChange = onQueryChange,
        results = results,
        isSearching = isSearching,
        onProductClick = onProductClick,
    )
}

/** Step 2: Quantità + Unità */
@Composable
fun QuantityUnitStep(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    selectedUnit: StandardUnit?,
    onUnitChange: (StandardUnit) -> Unit,
    compatibleUnits: List<StandardUnit>,
    errorQuantity: String?,
    errorUnit: String?,
) {
    QuantityUnitPicker(
        quantity = quantity,
        onQuantityChange = onQuantityChange,
        selectedUnit = selectedUnit,
        onUnitChange = onUnitChange,
        compatibleUnits = compatibleUnits,
        errorQuantity = errorQuantity,
        errorUnit = errorUnit,
    )
}

/** Step 3: Attributi */
@Composable
fun AttributeStep(
    brand: String,
    onBrandChange: (String) -> Unit,
    variant: String,
    onVariantChange: (String) -> Unit,
    condition: String,
    onConditionChange: (String) -> Unit,
    showBrand: Boolean,
    showVariant: Boolean,
    showCondition: Boolean,
    commonBrands: List<String>,
    commonConditions: List<String>,
) {
    AttributeSection(
        brand = brand,
        onBrandChange = onBrandChange,
        variant = variant,
        onVariantChange = onVariantChange,
        condition = condition,
        onConditionChange = onConditionChange,
        showBrand = showBrand,
        showVariant = showVariant,
        showCondition = showCondition,
        commonBrands = commonBrands,
        commonConditions = commonConditions,
    )
}

/** Step 4: Conferma */
@Composable
fun ConfirmStep(
    product: CanonicalProduct,
    quantity: Double,
    unit: StandardUnit,
    brand: String,
    variant: String,
    condition: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text("Riepilogo", style = togoTypography().sectionHeader, color = togoColors().inkPrimary)

        // Riepilogo prodotto
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryRow("Prodotto", product.name)
            SummaryRow("Quantità", "${quantity.toString().trimEnd('0').trimEnd('.')} ${unit.displayName}")
            if (brand.isNotBlank()) SummaryRow("Marca", brand)
            if (variant.isNotBlank()) SummaryRow("Variante", variant)
            if (condition.isNotBlank()) SummaryRow("Conservazione", condition)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = togoTypography().itemMeta, color = togoColors().inkSecondary)
        Text(value, style = togoTypography().itemMeta.copy(color = togoColors().inkPrimary))
    }
}