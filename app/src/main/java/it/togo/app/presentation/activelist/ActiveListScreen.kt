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
import androidx.compose.material3.ModalBottomSheetLayout
import androidx.compose.material3.ModalBottomSheetState
import androidx.compose.material3.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.togo.app.presentation.additem.AddItemBottomSheet
import it.togo.app.presentation.additem.AddItemConfirmed
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
 * - FAB "+" per inserimento manuale
 * - ModalBottomSheetLayout per AddItemBottomSheet
 */
@Composable
fun ActiveListScreen(onEvent: (UiEvent) -> Unit) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<ActiveListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()
    val tokens = componentTokens()

    // Bottom sheet state
    val sheetState = remember { ModalBottomSheetState(ModalBottomSheetValue.Hidden) }
    val scope = rememberCoroutineScope()
    val sheetContent = remember { derivedStateOf { sheetState.value != ModalBottomSheetValue.Hidden } }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            AddItemBottomSheet(
                sheetState = sheetState,
                onConfirm = { confirmed ->
                    viewModel.onAddItemConfirmed(confirmed)
                },
                onDismiss = { /* sheet hides automatically */ }
            )
        },
        sheetBackgroundColor = colors.surfaceBase,
        sheetShape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetPeekHeight = 0.dp,
        confirmStateChange = { it == ModalBottomSheetValue.Expanded },
    ) {
        Scaffold(
            topBar = { ActiveListAppBar(
                onHistoryClick = { onEvent(UiEvent.NavigateToHistory) },
                onShareClick = { onEvent(UiEvent.ShareList) },
                activeCount = uiState.activeCount,
            ) },
            floatingActionButton = {
                // Dual FAB: Voice + Add Item
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(androidx.compose.ui.Alignment.BottomEnd),
                ) {
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        // Voice FAB
                        VoiceFab(onClick = { onEvent(UiEvent.NavigateToVoice) })
                        
                        // Add Item FAB
                        androidx.compose.material3.FloatingActionButton(
                            onClick = { scope.launch { sheetState.show() } },
                            containerColor = colors.accentAction,
                            contentColor = colors.inkInverse,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Add,
                                contentDescription = "Aggiungi voce",
                                tint = colors.inkInverse,
                            )
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        content = { innerPadding ->
            Column(
                modifier = androidx.compose.foundation.layout.padding(innerPadding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing.space2)
            ) {
                if (uiState.error != null) {
                    ErrorPlaceholder(uiState.error!!, colors, typography, spacing)
                } else if (uiState.activeGroups.isEmpty()) {
                    EmptyState(
                        onAddManual = { scope.launch { sheetState.show() } },
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