package it.togo.app.presentation.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
                    Text("Elimina", color = colors.error)
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
                label = { Text("Cerca per espressione o prodotto", style = typography.caption) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            if (uiState.isEmpty) {
                EmptyState(query = uiState.searchQuery, colors = colors, typography = typography, spacing = spacing)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    items(uiState.filteredRules, key = { it.id }) { rule ->
                        RuleItemRow(
                            rule = rule,
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
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing
) {
    // Per MVP: selezione da lista L3 fissa (in futuro: dropdown tassonomia)
    val categories = listOf(
        5L to "Latticini",
        12L to "Bevande",
        13L to "Frutta e Verdura",
        14L to "Carne e Pesce",
        15L to "Pasta e Riso"
    )
    var selectedLevel3Id by remember { mutableStateOf(rule.level3Id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica categoria", style = typography.itemTitle) },
        text = {
            Column {
                Text("Regola: ${rule.userExpression}", style = typography.itemMeta)
                Spacer(modifier = Modifier.height(spacing.space2))
                categories.forEach { (id, name) ->
                    TextButton(
                        onClick = { selectedLevel3Id = id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = name,
                            color = if (selectedLevel3Id == id) colors.accent else colors.inkPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedLevel3Id) }) {
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
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (query.isBlank()) "Nessuna regola appresa" else "Nessun risultato per '$query'",
            style = typography.itemMeta,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RuleItemRow(
    rule: LearnedRule,
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

            // Nome prodotto e categoria (da catalogo, con fallback a ID)
            Text(
                text = "Prodotto #${rule.productId} • L3: ${rule.level3Id}",
                style = typography.itemMeta,
                color = colors.inkSecondary
            )

            // Data ultima applicazione
            Text(
                text = "Ultima applicazione: ${dateFormat.format(Date(rule.lastAppliedAt))}",
                style = typography.caption,
                color = colors.inkTertiary
            )

            // Stato
            Text(
                text = if (rule.isActive) "Attiva" else "Disattivata",
                style = typography.caption,
                color = if (rule.isActive) colors.success else colors.warning
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
                    TextButton(onClick = onDeactivate) { Text("Disattiva", color = colors.warning) }
                } else {
                    TextButton(onClick = onReactivate) { Text("Riattiva", color = colors.accent) }
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Elimina", tint = colors.error)
                }
            }
        }
    }
}
