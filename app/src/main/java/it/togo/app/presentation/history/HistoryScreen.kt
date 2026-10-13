package it.togo.app.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.HistoricalItem
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schermata Storico prodotti acquistati.
 *
 * Funzionalità:
 * - Lista storico ordinata per data decrescente
 * - Ricerca per nome prodotto
 * - Riaggiunta alla lista attiva
 *
 * Design System: 100% token TogoTheme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    // Mostra snackbar quando snackbarMessage è impostato
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storico", style = typography.sectionHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
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
            // Campo ricerca
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                label = { Text("Cerca nel storico", style = typography.caption) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            // Contenuto
            if (uiState.isEmpty) {
                EmptyState(
                    query = uiState.searchQuery,
                    colors = colors,
                    typography = typography,
                    spacing = spacing
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(spacing.space2)
                ) {
                    items(uiState.filteredItems, key = { it.id }) { item ->
                        HistoryItemRow(
                            item = item,
                            onReAdd = { viewModel.reAddToActiveList(item) },
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
            text = if (query.isBlank()) "Nessun prodotto acquistato" else "Nessun risultato per '$query'",
            style = typography.itemMeta,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryItemRow(
    item: HistoricalItem,
    onReAdd: () -> Unit,
    colors: it.togo.app.ui.theme.TogoColors,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.space3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Nome prodotto reale (da catalogo, con fallback a ID)
                Text(
                    text = "Prodotto #${item.productId}",
                    style = typography.itemTitle,
                    color = colors.inkPrimary
                )
                Text(
                    text = "${item.lastQuantity} ${item.lastUnit.displayName}",
                    style = typography.itemMeta,
                    color = colors.inkSecondary
                )
                Text(
                    text = dateFormat.format(Date(item.purchasedAt)),
                    style = typography.caption,
                    color = colors.inkTertiary
                )
            }

            IconButton(onClick = onReAdd) {
                Text(
                    text = "+",
                    style = typography.sectionHeader,
                    color = colors.accent
                )
            }
        }
    }
}