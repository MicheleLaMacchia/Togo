package it.togo.app.presentation.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.LearnedRule
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schermata Gestione Regole Apprese.
 *
 * Mostra lista regole con: espressione, nome prodotto, nome categoria, data.
 * Azioni: modifica categoria, disattiva/riattiva, elimina (con conferma).
 *
 * Design System: 100% token TogoTheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnedRulesScreen(
    viewModel: LearnedRulesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    // Dialog conferma eliminazione
    if (uiState.isDeleteDialogOpen && uiState.ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Elimina regola", style = typography.itemTitle) },
            text = { Text("Eliminare la regola per '${uiState.ruleToDelete?.userExpression}'?", style = typography.itemMeta) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Elimina", color = colors.warningInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Annulla")
                }
            }
        )
    }

    // Dialog modifica categoria
    uiState.editingRule?.let { rule ->
        EditCategoryDialog(
            rule = rule,
            categoryOptions = uiState.categoryOptions,
            onConfirm = { newLevel3Id -> viewModel.updateCategory(newLevel3Id) },
            onDismiss = { viewModel.cancelEdit() },
            colors = colors,
            typography = typography,
            spacing = spacing,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Regole apprese", style = typography.sectionHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.inkPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Cerca per espressione, prodotto o categoria", style = typography.caption) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Text(
                text = "Filtra solo le regole esistenti: non cerca nel catalogo prodotti.",
                style = typography.caption,
                color = colors.inkSecondary,
            )

            if (uiState.isEmpty) {
                EmptyState(
                    query = uiState.searchQuery,
                    hasRules = uiState.hasRules,
                    colors = colors,
                    typography = typography,
                    spacing = spacing,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    items(uiState.filteredRules, key = { it.id }) { rule ->
                        RuleItemRow(
                            rule = rule,
                            productName = uiState.productNames[rule.productId],
                            categoryName = uiState.categoryNames[rule.id],
                            categoryPath = uiState.categoryPaths[rule.id],
                            onEditCategory = { viewModel.startEditCategory(rule) },
                            onDeactivate = { viewModel.deactivateRule(rule) },
                            onReactivate = { viewModel.reactivateRule(rule) },
                            onDelete = { viewModel.requestDelete(rule) },
                            colors = colors,
                            typography = typography,
                            spacing = spacing
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditCategoryDialog(
    rule: LearnedRule,
    categoryOptions: List<CategoryOption>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing
) {
    var selectedLevel3Id by remember(rule.id, categoryOptions) {
        mutableStateOf(
            categoryOptions.firstOrNull { it.id == rule.level3Id }?.id
                ?: categoryOptions.firstOrNull()?.id
                ?: rule.level3Id
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica categoria", style = typography.itemTitle) },
        text = {
            Column {
                Text("Regola: ${rule.userExpression}", style = typography.itemMeta)
                Spacer(modifier = Modifier.height(spacing.space2))
                if (categoryOptions.isEmpty()) {
                    Text(
                        text = "Categorie non disponibili",
                        style = typography.itemMeta,
                        color = colors.inkSecondary,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                    ) {
                        items(categoryOptions, key = { it.id }) { option ->
                            TextButton(
                                onClick = { selectedLevel3Id = option.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = option.label,
                                    color = if (selectedLevel3Id == option.id) colors.accent else colors.inkPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedLevel3Id) },
                enabled = categoryOptions.isNotEmpty(),
            ) {
                Text("Conferma")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun EmptyState(
    query: String,
    hasRules: Boolean,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            spacing.space2,
            Alignment.CenterVertically,
        )
    ) {
        val title = if (query.isBlank() || !hasRules) {
            "Nessuna regola appresa"
        } else {
            "Nessun risultato per «$query»"
        }
        val explanation = when {
            !hasRules ->
                "Le regole vengono create quando correggi la categoria di un prodotto " +
                    "e scegli «Ricorda questa scelta»."
            query.isNotBlank() ->
                "La ricerca filtra solo le regole esistenti per espressione, " +
                    "nome prodotto o categoria."
            else -> null
        }

        Text(
            text = title,
            style = typography.itemTitle,
            color = colors.inkPrimary,
            textAlign = TextAlign.Center
        )
        if (explanation != null) {
            Text(
                text = explanation,
                style = typography.itemMeta,
                color = colors.inkSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RuleItemRow(
    rule: LearnedRule,
    productName: String?,
    categoryName: String?,
    categoryPath: String?,
    onEditCategory: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (rule.isActive) colors.surfaceVariant else colors.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space1)
        ) {
            // Espressione utente
            Text(
                text = rule.userExpression,
                style = typography.itemTitle,
                color = colors.inkPrimary
            )

            // Nome prodotto e categoria risolti da catalogo/tassonomia,
            // con fallback leggibile se il dato non è più disponibile.
            Text(
                text = "Prodotto: ${productName ?: "Prodotto #${rule.productId}"}",
                style = typography.itemMeta,
                color = colors.inkSecondary
            )
            Text(
                text = "Categoria: ${categoryName ?: "Categoria #${rule.level3Id}"}",
                style = typography.itemMeta,
                color = colors.inkSecondary
            )
            if (!categoryPath.isNullOrBlank() && categoryPath != categoryName) {
                Text(
                    text = categoryPath,
                    style = typography.caption,
                    color = colors.inkSecondary
                )
            }

            // Data ultima applicazione
            Text(
                text = "Ultima applicazione: ${dateFormat.format(Date(rule.lastAppliedAt))}",
                style = typography.caption,
                color = colors.inkSecondary
            )

            // Stato
            Text(
                text = if (rule.isActive) "Attiva" else "Disattivata",
                style = typography.caption,
                color = if (rule.isActive) colors.successInk else colors.warningInk
            )

            // Azioni
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEditCategory) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Modifica categoria",
                        tint = colors.accent
                    )
                }
                if (rule.isActive) {
                    TextButton(onClick = onDeactivate) { Text("Disattiva", color = colors.warningInk) }
                } else {
                    TextButton(onClick = onReactivate) { Text("Riattiva", color = colors.accent) }
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Elimina", tint = colors.warningInk)
                }
            }
        }
    }
}
