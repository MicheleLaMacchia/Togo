package it.togo.app.presentation.additem

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.usecase.CorrectCategoryUseCase
import it.togo.app.domain.usecase.CreateCanonicalProductUseCase
import it.togo.app.domain.usecase.CreateUserLevel3UseCase
import it.togo.app.ui.theme.SHEET_SCRIM_ALPHA
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import android.os.Vibrator
import android.content.Context
import android.os.VibrationEffect
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Bottom Sheet "Aggiungi/Modifica Voce Manuale" - flusso multi-step.
 *
 * Step:
 * 1. Ricerca prodotto (autocomplete catalogo)
 * 2. Quantità + Unità compatibili
 * 3. Attributi opzionali (marca, variante, conservazione)
 * 4. Conferma
 *
 * Supporta due modalità:
 * - INSERT: nuova voce (parte da step 1)
 * - EDIT: modifica voce esistente (parte da step 2, pre-popolato)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemBottomSheet(
    onDismiss: () -> Unit,
    editItem: ShoppingItem? = null,
) {
    // La factory esplicita permette di passare l'item in modifica; per
    // l'inserimento editItem è null e il ViewModel parte dallo step 1.
    // La chiave cambia a ogni apertura della sheet: evitando di riusare un
    // ViewModel con il vecchio editItem, la riapertura mostra i dati aggiornati.
    val viewModelKey = remember(editItem) {
        "add-item-${editItem?.id ?: "new"}-${System.nanoTime()}"
    }
    val viewModel: AddItemViewModel = viewModel(
        key = viewModelKey,
        factory = viewModelFactory {
            initializer {
                AddItemViewModel(
                    catalogRepository = GlobalContext.get().get<CatalogRepository>(),
                    shoppingRepository = GlobalContext.get().get<ShoppingListRepository>(),
                    createProductUseCase = GlobalContext.get().get<CreateCanonicalProductUseCase>(),
                    createLevel3UseCase = GlobalContext.get().get<CreateUserLevel3UseCase>(),
                    correctCategoryUseCase = GlobalContext.get().get<CorrectCategoryUseCase>(),
                    editItem = editItem,
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { onDismiss() }
    }

    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Solo la X chiude la sheet: tap sullo sfondo, swipe verso il basso e back
    // non devono interrompere l'inserimento. `allowHideByUser` sblocca la
    // transizione a Hidden nel solo handler della X.
    var allowHideByUser by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            target != SheetValue.Hidden || allowHideByUser
        },
    )

    ModalBottomSheet(
        onDismissRequest = {
            // Intenzionalmente vuoto: la chiusura avviene solo con la X.
        },
        sheetState = sheetState,
        containerColor = colors.surfaceBase,
        scrimColor = colors.scrim.copy(alpha = SHEET_SCRIM_ALPHA),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
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
                            1 -> when {
                                viewModel.isEditMode -> "Modifica voce"
                                uiState.isNewProductFormVisible -> "Nuovo prodotto"
                                else -> "Cerca prodotto"
                            }
                            2 -> "Quantità e unità"
                            3 -> "Attributi (opzionali)"
                            4 -> "Conferma"
                            else -> if (viewModel.isEditMode) "Modifica voce" else "Aggiungi voce"
                        },
                        style = typography.titleScreen,
                        color = colors.inkPrimary,
                    )

                    // Chiudi
                    IconButton(onClick = {
                        // Il ViewModel viene ricreato a ogni apertura (chiave
                        // basata su nanoTime): non serve resettarlo qui, così
                        // il contenuto resta stabile durante l'animazione.
                        allowHideByUser = true
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Chiudi",
                            tint = colors.inkPrimary,
                        )
                    }
                }

                // Progress indicator step (nascosto nel form "Nuovo prodotto" della Story 7.1)
                if (!uiState.isNewProductFormVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.space4, end = spacing.space4, bottom = spacing.space2),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    val totalSteps = if (viewModel.isEditMode) 3 else 4
                    val startStep = if (viewModel.isEditMode) 2 else 1
                    
                    (startStep..4).forEach { step ->
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                        ) {
                            // Circle step
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (uiState.currentStep == step) colors.accentHighlight else if (uiState.currentStep >= step) colors.inkPrimary else colors.borderHairline,
                                        shape = androidx.compose.foundation.shape.CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (step - startStep + 1).toString(),
                                    style = typography.caption.copy(color = if (uiState.currentStep >= step) colors.inkInverse else colors.inkMuted),
                                )
                            }
                            // Linea progress (dopo il cerchio, tranne l'ultimo step)
                            if (step < 4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(if (uiState.currentStep > step) colors.accentHighlight else colors.borderHairline),
                                )
                            }
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
                        1 -> when {
                            viewModel.isEditMode -> Unit
                            uiState.isNewProductFormVisible -> NewProductForm(
                                name = uiState.newProductName,
                                taxonomy = uiState.taxonomy,
                                level1Id = uiState.newProductLevel1Id,
                                level2Id = uiState.newProductLevel2Id,
                                level3Id = uiState.newProductLevel3Id,
                                error = uiState.newProductError,
                                isSaving = uiState.isCreatingProduct,
                                isLevel3ProposalVisible = uiState.isLevel3ProposalVisible,
                                newLevel3Name = uiState.newLevel3Name,
                                level3ProposalError = uiState.level3ProposalError,
                                isCreatingLevel3 = uiState.isCreatingLevel3,
                                onNameChange = { viewModel.onEvent(AddItemUiEvent.NewProductNameChanged(it)) },
                                onLevel1Select = { viewModel.onEvent(AddItemUiEvent.NewProductLevel1Selected(it.id)) },
                                onLevel2Select = { viewModel.onEvent(AddItemUiEvent.NewProductLevel2Selected(it.id)) },
                                onLevel3Select = { viewModel.onEvent(AddItemUiEvent.NewProductLevel3Selected(it.id)) },
                                onShowLevel3Proposal = { viewModel.onEvent(AddItemUiEvent.ShowLevel3Proposal) },
                                onHideLevel3Proposal = { viewModel.onEvent(AddItemUiEvent.HideLevel3Proposal) },
                                onNewLevel3NameChange = { viewModel.onEvent(AddItemUiEvent.NewLevel3NameChanged(it)) },
                                onCreateLevel3 = { viewModel.onEvent(AddItemUiEvent.CreateNewLevel3) },
                                onCancel = { viewModel.onEvent(AddItemUiEvent.HideNewProductForm) },
                                onCreate = { viewModel.onEvent(AddItemUiEvent.CreateNewProduct) },
                            )
                            else -> SearchStep(
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.onEvent(AddItemUiEvent.SearchQueryChanged(it)) },
                                results = uiState.searchResults,
                                isSearching = uiState.isLoading,
                                onProductClick = { viewModel.onEvent(AddItemUiEvent.ProductSelected(it)) },
                                onAddNewProduct = { viewModel.onEvent(AddItemUiEvent.ShowNewProductForm) },
                            )
                        }
                        2 -> QuantityUnitStep(
                            quantityText = uiState.quantityText,
                            onQuantityChange = { viewModel.onEvent(AddItemUiEvent.QuantityChanged(it)) },
                            selectedUnit = uiState.selectedUnit,
                            onUnitChange = { viewModel.onEvent(AddItemUiEvent.UnitChanged(it)) },
                            compatibleUnits = uiState.compatibleUnits,
                            errorQuantity = uiState.errors["quantity"],
                            errorUnit = uiState.errors["unit"],
                        )
                        3 -> uiState.selectedProduct?.let { product ->
                            AttributeStep(
                                product = product,
                                brand = uiState.brand,
                                onBrandChange = { viewModel.onEvent(AddItemUiEvent.BrandChanged(it)) },
                                brandSuggestions = uiState.brandSuggestions,
                                variant = uiState.variant,
                                onVariantChange = { viewModel.onEvent(AddItemUiEvent.VariantChanged(it)) },
                                variantSuggestions = uiState.variantSuggestions,
                                condition = uiState.condition,
                                onConditionChange = { viewModel.onEvent(AddItemUiEvent.ConditionChanged(it)) },
                            )
                        }
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
                                categoryPath = uiState.selectedCategoryPath,
                                categoryMessage = uiState.categoryCorrectionMessage,
                                errorQuantity = uiState.errors["quantity"],
                                onCorrectCategory = {
                                    viewModel.onEvent(AddItemUiEvent.ShowCategoryCorrection)
                                },
                                onConfirm = {
                                    viewModel.onEvent(AddItemUiEvent.Confirm)
                                },
                                onBack = { viewModel.onEvent(AddItemUiEvent.PreviousStep) },
                                colors = togoColors(),
                                typography = togoTypography(),
                                spacing = togoSpacing(),
                            )
                        }
                    }
                }

                // Bottoni navigazione (nascosti nel form "Nuovo prodotto": le azioni sono nel form)
                if (!uiState.isNewProductFormVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = spacing.space4, top = spacing.space3, end = spacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                ) {
                    val minStep = if (viewModel.isEditMode) 2 else 1
                    if (uiState.currentStep > minStep) {
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
                        val canProceed = when (uiState.currentStep) {
                            1 -> uiState.canProceedToStep2
                            // Anche con quantità vuota l'utente può premere
                            // "Avanti": il ViewModel mostra l'errore e resta qui.
                            2 -> uiState.selectedUnit != null
                            else -> true
                        }
                        Button(
                            onClick = { viewModel.onEvent(AddItemUiEvent.NextStep) },
                            modifier = Modifier.weight(1f),
                            enabled = canProceed,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colors.accentAction,
                                contentColor = colors.inkInverse,
                                // Disabilitato leggibile: borderHairline/inkMuted sono lo stesso
                                // #64748B in dark e renderebbero il testo invisibile.
                                disabledContainerColor = colors.surfaceSubtle,
                                disabledContentColor = colors.inkSecondary,
                            ),
                        ) {
                            Text("Avanti", style = typography.sectionHeader)
                        }
                    } else {
                        // Step 4 - Conferma
                        Button(
                            onClick = { viewModel.onEvent(AddItemUiEvent.Confirm) },
                            modifier = Modifier.weight(1f),
                            // La validazione finale (quantità inclusa) avviene
                            // in handleConfirm, così il messaggio è visibile.
                            enabled = uiState.selectedProduct != null && uiState.selectedUnit != null,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colors.accentSuccess,
                                // Il verde è condiviso tra i temi: il testo resta bianco fisso.
                                contentColor = tokens.bottomSheet.checkoutButtonContent,
                                disabledContainerColor = colors.surfaceSubtle,
                                disabledContentColor = colors.inkSecondary,
                            ),
                        ) {
                            Text(
                                text = if (viewModel.isEditMode) "Salva" else "Aggiungi",
                                style = typography.sectionHeader,
                            )
                        }
                    }
                }
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = spacing.space4))
            }
    }

    // Story 7.1: avviso duplicato applicativo (nessun vincolo UNIQUE nel DB).
    uiState.duplicateCandidate?.let { candidate ->
        DuplicateProductWarningDialog(
            candidateName = candidate.name,
            newProductName = uiState.newProductName,
            onConfirm = { viewModel.onEvent(AddItemUiEvent.ConfirmDuplicateProduct) },
            onDismiss = { viewModel.onEvent(AddItemUiEvent.DismissDuplicateWarning) },
        )
    }

    // Story 7.2: avviso duplicato per il nuovo Livello 3. Il ViewModel azzera
    // l'altro candidato, quindi i due dialog non sono mai visibili insieme.
    uiState.duplicateLevel3Candidate?.let { candidate ->
        DuplicateLevel3WarningDialog(
            candidateName = candidate.name,
            newLevel3Name = uiState.newLevel3Name,
            onConfirm = { viewModel.onEvent(AddItemUiEvent.ConfirmDuplicateLevel3) },
            onDismiss = { viewModel.onEvent(AddItemUiEvent.DismissDuplicateLevel3Warning) },
        )
    }

    // Story 3.6: correzione categoria + possibilità di ricordare la scelta.
    if (uiState.isCategoryCorrectionVisible) {
        CategoryCorrectionDialog(
            taxonomy = uiState.taxonomy,
            level1Id = uiState.categoryCorrectionLevel1Id,
            level2Id = uiState.categoryCorrectionLevel2Id,
            level3Id = uiState.categoryCorrectionLevel3Id,
            expression = uiState.categoryCorrectionExpression,
            error = uiState.categoryCorrectionError,
            isSaving = uiState.isSavingCategoryRule,
            onLevel1Select = {
                viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel1Selected(it))
            },
            onLevel2Select = {
                viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel2Selected(it))
            },
            onLevel3Select = {
                viewModel.onEvent(AddItemUiEvent.CategoryCorrectionLevel3Selected(it))
            },
            onExpressionChange = {
                viewModel.onEvent(AddItemUiEvent.CategoryCorrectionExpressionChanged(it))
            },
            onConfirm = { viewModel.onEvent(AddItemUiEvent.ApplyCategoryCorrection) },
            onDismiss = { viewModel.onEvent(AddItemUiEvent.HideCategoryCorrection) },
        )
    }

    if (uiState.pendingCategoryCorrection != null) {
        RememberCategoryDialog(
            expression = uiState.pendingCategoryAssignment
                ?.userExpression
                .orEmpty(),
            isSaving = uiState.isSavingCategoryRule,
            onConfirm = { viewModel.onEvent(AddItemUiEvent.ConfirmRememberCategory) },
            onDismiss = { viewModel.onEvent(AddItemUiEvent.DismissRememberCategory) },
        )
    }
}

/** Step 1: Ricerca prodotto */
@Composable
fun SearchStep(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<CanonicalProduct>,
    isSearching: Boolean,
    onProductClick: (CanonicalProduct) -> Unit,
    onAddNewProduct: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        ProductSearchField(
            query = query,
            onQueryChange = onQueryChange,
            results = results,
            isSearching = isSearching,
            onProductClick = onProductClick,
        )

        // Story 7.1: accesso alla creazione manuale dopo una ricerca senza match.
        if (query.trim().length >= 2) {
            TextButton(
                onClick = onAddNewProduct,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = spacing.touchTargetMin),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.accentAction),
            ) {
                Text(
                    text = "Non trovi il prodotto? Aggiungilo al catalogo",
                    style = typography.itemMeta,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Step 2: Quantità + Unità */
@Composable
fun QuantityUnitStep(
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    selectedUnit: StandardUnit?,
    onUnitChange: (StandardUnit) -> Unit,
    compatibleUnits: List<StandardUnit>,
    errorQuantity: String?,
    errorUnit: String?,
) {
    QuantityUnitPicker(
        quantityText = quantityText,
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
    AttributeSection(
        product = product,
        brand = brand,
        onBrandChange = onBrandChange,
        brandSuggestions = brandSuggestions,
        variant = variant,
        onVariantChange = onVariantChange,
        variantSuggestions = variantSuggestions,
        condition = condition,
        onConditionChange = onConditionChange,
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
    errorQuantity: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
    categoryPath: String = "",
    categoryMessage: String? = null,
    onCorrectCategory: () -> Unit = {},
) {
    val colorsLocal = colors
    val typographyLocal = typography
    val spacingLocal = spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacingLocal.space3),
    ) {
        Text("Riepilogo", style = typographyLocal.sectionHeader, color = colorsLocal.inkPrimary)

        // Riepilogo prodotto
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryRow("Prodotto", product.name, colorsLocal, typographyLocal)
            SummaryRow("Quantità", "${formatQuantityText(quantity)} ${unit.displayName}", colorsLocal, typographyLocal)
            if (brand.isNotBlank()) SummaryRow("Marca", brand, colorsLocal, typographyLocal)
            if (variant.isNotBlank()) SummaryRow("Variante", variant, colorsLocal, typographyLocal)
            if (condition.isNotBlank()) SummaryRow("Conservazione", condition, colorsLocal, typographyLocal)
            SummaryRow(
                "Categoria",
                categoryPath.ifBlank { "Categoria non assegnata" },
                colorsLocal,
                typographyLocal,
            )
        }

        TextButton(
            onClick = onCorrectCategory,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text("Correggi categoria", style = typographyLocal.itemMeta)
        }

        if (categoryMessage != null) {
            val messageColor = when {
                categoryMessage == "Regola appresa salvata" -> colorsLocal.successInk
                categoryMessage == "Nessuna regola salvata" -> colorsLocal.inkSecondary
                else -> colorsLocal.warningInk
            }
            Text(
                text = categoryMessage,
                style = typographyLocal.caption,
                color = messageColor,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (errorQuantity != null) {
            Text(
                text = errorQuantity,
                style = typographyLocal.caption,
                color = colorsLocal.warningInk,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, colors: it.togo.app.ui.theme.TogoColorScheme, typography: it.togo.app.ui.theme.TogoTypography) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = typography.itemMeta, color = colors.inkSecondary)
        Text(value, style = typography.itemMeta.copy(color = colors.inkPrimary))
    }
}
