package it.togo.app.presentation.settings

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.TopAppBarDefaults
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
import it.togo.app.domain.model.UserAttribute
import it.togo.app.domain.model.UserAttributeType
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography

/**
 * Sezione "Le mie marche" / "Le mie varianti": elenca SOLO i valori aggiunti
 * dall'utente (registro `USER_ATTRIBUTE`) e permette di rinominarli o
 * eliminarli.
 *
 * I valori predefiniti (es. le marche Coop, Conad, ...) non compaiono qui:
 * non sono modificabili e restano sempre disponibili nei suggerimenti.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAttributesScreen(
    viewModel: UserDataViewModel,
    type: UserAttributeType,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    val isBrand = type == UserAttributeType.BRAND
    val title = if (isBrand) "Le mie marche" else "Le mie varianti"
    val description = if (isBrand) {
        "Qui compaiono solo le marche che hai aggiunto tu. Le marche predefinite " +
            "(Coop, Conad, Esselunga, ...) restano disponibili nei suggerimenti e " +
            "non si modificano da qui."
    } else {
        "Qui compaiono solo le varianti che hai aggiunto tu. I valori predefiniti " +
            "restano disponibili nei suggerimenti."
    }
    val values = if (isBrand) uiState.brands else uiState.variants
    val emptyTitle = if (isBrand) "Nessuna marca aggiunta da te" else "Nessuna variante aggiunta da te"
    val emptyExplanation = if (isBrand) {
        "Quando digiti una marca nuova nel form «Aggiungi voce» la ritrovi qui."
    } else {
        "Quando digiti una variante nuova nel form «Aggiungi voce» la ritrovi qui."
    }

    var editingValue by remember { mutableStateOf<UserAttribute?>(null) }
    var deletingValue by remember { mutableStateOf<UserAttribute?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                UserDataEvent.AttributeUpdated,
                UserDataEvent.AttributeDeleted -> {
                    editingValue = null
                    deletingValue = null
                }

                is UserDataEvent.Error -> snackbarHostState.showSnackbar(event.message)
                else -> Unit
            }
        }
    }

    editingValue?.let { attribute ->
        EditAttributeDialog(
            attribute = attribute,
            onConfirm = { newValue -> viewModel.renameAttribute(attribute, newValue) },
            onDismiss = { editingValue = null },
        )
    }

    deletingValue?.let { attribute ->
        AlertDialog(
            onDismissRequest = { deletingValue = null },
            title = { Text("Elimina valore", style = typography.itemTitle) },
            text = {
                Text(
                    text = "«${attribute.value}» non comparirà più tra i suggerimenti." +
                        "\n\nLe voci già presenti nella lista o nello storico non " +
                        "vengono modificate.",
                    style = typography.itemMeta,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAttribute(attribute) }) {
                    Text("Elimina", color = colors.warningInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingValue = null }) {
                    Text("Annulla")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = typography.sectionHeader) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Indietro",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.inkPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            item {
                Text(
                    text = description,
                    style = typography.caption,
                    color = colors.inkSecondary,
                    modifier = Modifier.padding(bottom = spacing.space2),
                )
            }

            if (values.isEmpty()) {
                item {
                    EmptyAttributesState(
                        title = emptyTitle,
                        explanation = emptyExplanation,
                    )
                }
            } else {
                items(values, key = { "${it.type}-${it.id}" }) { attribute ->
                    UserAttributeRow(
                        attribute = attribute,
                        onEdit = { editingValue = attribute },
                        onDelete = { deletingValue = attribute },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAttributeRow(
    attribute: UserAttribute,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(
                start = spacing.space3,
                top = spacing.space2,
                bottom = spacing.space2,
                end = spacing.space1,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = attribute.value,
                style = typography.itemTitle,
                color = colors.inkPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Modifica ${attribute.value}",
                    tint = colors.accent,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Elimina ${attribute.value}",
                    tint = colors.warningInk,
                )
            }
        }
    }
}

@Composable
private fun EditAttributeDialog(
    attribute: UserAttribute,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val typography = togoTypography()
    var value by remember(attribute.id) { mutableStateOf(attribute.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica valore", style = typography.itemTitle) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = {
                    Text(
                        text = if (attribute.type == UserAttributeType.BRAND) "Marca" else "Variante",
                        style = typography.caption,
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value.isNotBlank(),
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
    )
}

@Composable
private fun EmptyAttributesState(
    title: String,
    explanation: String,
) {
    val colors = togoColors()
    val typography = togoTypography()
    val spacing = togoSpacing()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.space6, start = spacing.space4, end = spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(
            text = title,
            style = typography.itemTitle,
            color = colors.inkPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = explanation,
            style = typography.itemMeta,
            color = colors.inkSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
