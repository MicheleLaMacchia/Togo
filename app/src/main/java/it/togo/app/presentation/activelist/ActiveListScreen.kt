package it.togo.app.presentation.activelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Schermata principale Lista Attiva (Spesa).
 *
 * Struttura:
 * - Scaffold con AppBar custom (titolo + azioni Storico/Condividi)
 * - LazyColumn: EmptyState OPPURE gruppi categoria + sezione Presi
 * - FAB microfono per cattura vocale
 */
@Composable
fun ActiveListScreen(onEvent: (UiEvent) -> Unit) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ActiveListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    Scaffold(
        topBar = { ActiveListAppBar(
            onHistoryClick = { onEvent(UiEvent.NavigateToHistory) },
            onShareClick = { onEvent(UiEvent.ShareList) },
            activeCount = uiState.activeCount,
        ) },
        floatingActionButton = {
            VoiceFab(onClick = { onEvent(UiEvent.NavigateToVoice) })
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        content = { innerPadding ->
            Column(
                modifier = androidx.compose.foundation.layout.padding(innerPadding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                if (uiState.isLoading) {
                    // Teoricamente non si vede mai (Room Flow istantaneo)
                    LoadingPlaceholder(colors, typography, spacing)
                } else if (uiState.error != null) {
                    ErrorPlaceholder(uiState.error!!, colors, typography, spacing)
                } else if (uiState.activeGroups.isEmpty()) {
                    EmptyState(
                        onAddManual = { onEvent(UiEvent.AddItemManual) },
                        onVoice = { onEvent(UiEvent.NavigateToVoice) },
                        colors = colors,
                        typography = typography,
                        spacing = spacing,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        verticalArrangement = Arrangement.spacedBy(spacing.space2)
                    ) {
                        // Gruppi categoria
                        items(uiState.activeGroups, key = { group -> group.level1.id.toString() }) { group ->
                            CategorySection(
                                group = group,
                                onCheckOff = { itemId, checked -> onEvent(UiEvent.CheckOff(itemId, checked)) },
                                onEdit = { item -> onEvent(UiEvent.EditItem(item)) },
                                onDelete = { itemId -> onEvent(UiEvent.DeleteItem(itemId)) },
                                colors = colors,
                                typography = typography,
                                spacing = spacing,
                                tokens = tokens,
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space3))
                        }

                        // Sezione "Presi (N)" collassabile
                        if (uiState.hasCheckedItems) {
                            PresiSection(
                                items = uiState.checkedItems,
                                onUncheck = { itemId -> onEvent(UiEvent.CheckOff(itemId, false)) },
                                onCheckout = { onEvent(UiEvent.Checkout) },
                                colors = colors,
                                typography = typography,
                                spacing = spacing,
                                tokens = tokens,
                            )
                        }
                    }
                }
            }
        }
    )
}

/** AppBar personalizzata con titolo e azioni */
@Composable
fun ActiveListAppBar(
    onHistoryClick: () -> Unit,
    onShareClick: () -> Unit,
    activeCount: Int,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val tokens = componentTokens()

    androidx.compose.material3.TopAppBar(
        title = {
            Text(
                text = "Spesa",
                style = typography.titleScreen,
                color = colors.inkPrimary,
            )
        },
        navigationIcon = {
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.History,
                    contentDescription = "Storico",
                    tint = colors.inkPrimary,
                )
            }
        },
        actions = {
            IconButton(onClick = onShareClick, enabled = activeCount > 0) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Share,
                    contentDescription = "Condividi lista",
                    tint = colors.inkPrimary,
                )
            }
        },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = tokens.appBar.background,
            titleContentColor = colors.inkPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Placeholder caricamento (teorico) */
@Composable
private fun LoadingPlaceholder(
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = colors.accentHighlight,
            strokeWidth = 2.dp,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space3))
        Text("Caricamento lista...", style = typography.itemMeta, color = colors.inkSecondary)
    }
}

/** Placeholder errore */
@Composable
private fun ErrorPlaceholder(
    message: String,
    colors: it.togo.app.ui.theme.TogoColorScheme,
    typography: it.togo.app.ui.theme.TogoTypography,
    spacing: it.togo.app.ui.theme.TogoSpacing,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.ErrorOutline,
            contentDescription = "Errore",
            tint = colors.accentWarning,
            modifier = Modifier.size(48.dp),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(spacing.space2))
        Text(message, style = typography.itemMeta, color = colors.accentWarning, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}